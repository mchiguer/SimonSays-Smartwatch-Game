package com.epfl.esl.simongame.solo_mode

import android.content.Context
import com.epfl.esl.simongame.game.Move
import com.epfl.esl.simongame.wear.MotionWindow
import com.epfl.esl.simongame.wear.Vec3Sample
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class TfLiteMotionToMovePredictor(
    context: Context,
    private val spec: ModelSpec
) : MotionToMovePredictor {

    companion object {
        const val TARGET_SEQUENCE_LENGTH = 258
        const val IDLE_ENERGY_THRESHOLD = 2.0f
        const val JUMP_ACCEL_PEAK_THRESHOLD = 18.0f
        const val JUMP_FREEFALL_THRESHOLD = 3.0f
        const val JUMP_GYRO_VARIANCE_MAX = 15.0f
        const val JUMP_BOOST_FACTOR = 2.5f
    }

    private val interpreter: Interpreter
    private val T: Int
    private val C: Int
    private val scalerMean: FloatArray
    private val scalerStd: FloatArray

    init {
        interpreter = Interpreter(loadModel(context, spec.assetName), Interpreter.Options().setNumThreads(2))
        scalerMean = loadFloatArray(context, "scaler_mean.bin")
        scalerStd = loadFloatArray(context, "scaler_std.bin")

        val shape = interpreter.getInputTensor(0).shape()
        require(shape.size == 3 && shape[0] == 1) {
            "Model input must be rank-3 [1, T, C]. Got ${shape.contentToString()}"
        }
        T = shape[1]
        C = shape[2]

        require(T == TARGET_SEQUENCE_LENGTH) {
            "Model expects T=$T but TARGET_SEQUENCE_LENGTH is $TARGET_SEQUENCE_LENGTH"
        }

        val expectedC = when (spec.channelMapping) {
            ChannelMapping.ACCEL_3 -> 3
            ChannelMapping.ACCEL_GYRO_6 -> 6
        }
        require(C == expectedC) {
            "Model expects C=$C channels, but channelMapping expects $expectedC."
        }
    }

    override fun predict(window: MotionWindow): Move {
        return predictWithConfidence(window).move
    }

    override fun predictWithConfidence(window: MotionWindow): PredictionResult {
        val accel = normalizeSequenceLength(window.accel, T)
        val gyro = normalizeSequenceLength(window.gyro, T)

        val energy = calculateEnergy(accel, gyro)

        if (energy < IDLE_ENERGY_THRESHOLD) {
            return PredictionResult(
                move = Move.IDLE,
                confidence = 1f - (energy / IDLE_ENERGY_THRESHOLD),
                allScores = mapOf(Move.IDLE to 1f)
            )
        }

        val input = ByteBuffer
            .allocateDirect(4 * 1 * T * C)
            .order(ByteOrder.nativeOrder())

        for (t in 0 until T) {
            when (spec.channelMapping) {
                ChannelMapping.ACCEL_3 -> {
                    for (c in 0 until 3) {
                        val pos = t * C + c
                        input.putFloat((accel[t][c] - scalerMean[pos]) / scalerStd[pos])
                    }
                }
                ChannelMapping.ACCEL_GYRO_6 -> {
                    for (c in 0 until 3) {
                        val pos = t * C + c
                        input.putFloat((accel[t][c] - scalerMean[pos]) / scalerStd[pos])
                    }
                    for (c in 0 until 3) {
                        val pos = t * C + (c + 3)
                        input.putFloat((gyro[t][c] - scalerMean[pos]) / scalerStd[pos])
                    }
                }
            }
        }
        input.rewind()

        val out = Array(1) { FloatArray(spec.labels.size) }
        interpreter.run(input, out)

        val logits = out[0]
        val rawProbabilities = softmax(logits)

        val excludedMoves = setOf(Move.CLAP)
        val filteredProbabilities = rawProbabilities.mapIndexed { idx, prob ->
            val move = spec.labels.getOrElse(idx) { Move.IDLE }
            if (move in excludedMoves) 0f else prob
        }.toFloatArray()

        val jumpScore = detectJumpHeuristic(accel, gyro)

        val boostedProbabilities = filteredProbabilities.copyOf()
        val jumpIdx = spec.labels.indexOf(Move.JUMP)
        if (jumpIdx >= 0) {
            if (jumpScore > 0.5f) {
                boostedProbabilities[jumpIdx] *= (1f + (jumpScore * JUMP_BOOST_FACTOR))
            } else {
                val penalty = 0.25f + (jumpScore * 1.5f)
                boostedProbabilities[jumpIdx] *= penalty.coerceIn(0.1f, 1f)
            }
        }

        val sum = boostedProbabilities.sum()
        val probabilities = if (sum > 0) {
            boostedProbabilities.map { it / sum }.toFloatArray()
        } else {
            boostedProbabilities
        }

        val bestIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[bestIdx]
        val predictedMove = spec.labels.getOrElse(bestIdx) { Move.IDLE }

        val allScores = spec.labels.mapIndexed { idx, move ->
            move to probabilities.getOrElse(idx) { 0f }
        }.toMap()

        return PredictionResult(
            move = predictedMove,
            confidence = confidence,
            allScores = allScores
        )
    }

    private fun detectJumpHeuristic(accel: Array<FloatArray>, gyro: Array<FloatArray>): Float {
        if (accel.isEmpty()) return 0f

        val accelMagnitudes = accel.map { sample ->
            kotlin.math.sqrt(sample[0] * sample[0] + sample[1] * sample[1] + sample[2] * sample[2])
        }

        val gyroMagnitudes = gyro.map { sample ->
            kotlin.math.sqrt(sample[0] * sample[0] + sample[1] * sample[1] + sample[2] * sample[2])
        }

        val maxAccel = accelMagnitudes.maxOrNull() ?: 0f
        val hasLandingPeak = maxAccel > JUMP_ACCEL_PEAK_THRESHOLD

        val freefallCount = accelMagnitudes.count { it < JUMP_FREEFALL_THRESHOLD }
        val freefallRatio = freefallCount.toFloat() / accelMagnitudes.size
        val hasFreefallPhase = freefallRatio > 0.05f

        val gyroVariance = if (gyroMagnitudes.isNotEmpty()) {
            val mean = gyroMagnitudes.average().toFloat()
            gyroMagnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        } else 0f
        val hasStableGyro = gyroVariance < JUMP_GYRO_VARIANCE_MAX

        val accelMean = accelMagnitudes.average().toFloat()
        val accelVariance = accelMagnitudes.map { (it - accelMean) * (it - accelMean) }.average().toFloat()
        val hasHighAccelVariance = accelVariance > 10f

        var score = 0f

        if (hasLandingPeak) {
            score += 0.4f * minOf(1f, maxAccel / (JUMP_ACCEL_PEAK_THRESHOLD * 1.5f))
        }

        if (hasFreefallPhase) {
            score += 0.25f * minOf(1f, freefallRatio * 5f)
        }

        if (hasStableGyro) {
            score += 0.2f * (1f - minOf(1f, gyroVariance / JUMP_GYRO_VARIANCE_MAX))
        }

        if (hasHighAccelVariance) {
            score += 0.15f * minOf(1f, accelVariance / 50f)
        }

        return score.coerceIn(0f, 1f)
    }

    private fun calculateEnergy(accel: Array<FloatArray>, gyro: Array<FloatArray>): Float {
        val accelMeans = FloatArray(3)
        val gyroMeans = FloatArray(3)

        for (t in accel.indices) {
            for (c in 0 until 3) {
                accelMeans[c] += accel[t][c]
                gyroMeans[c] += gyro[t][c]
            }
        }
        for (c in 0 until 3) {
            accelMeans[c] /= accel.size
            gyroMeans[c] /= gyro.size
        }

        var totalVariance = 0f
        for (t in accel.indices) {
            for (c in 0 until 3) {
                val accelDiff = accel[t][c] - accelMeans[c]
                val gyroDiff = gyro[t][c] - gyroMeans[c]
                totalVariance += accelDiff * accelDiff + gyroDiff * gyroDiff
            }
        }

        return totalVariance / accel.size
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps = exps.sum()
        return exps.map { it / sumExps }.toFloatArray()
    }

    private fun loadFloatArray(context: Context, assetName: String): FloatArray {
        val bytes = context.assets.open(assetName).use { it.readBytes() }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floats)
        return floats
    }

    private fun loadModel(context: Context, assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { ch ->
                return ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    private fun normalizeSequenceLength(samples: List<Vec3Sample>, targetLength: Int): Array<FloatArray> {
        if (samples.isEmpty()) {
            return Array(targetLength) { floatArrayOf(0f, 0f, 0f) }
        }

        val sorted = samples.sortedBy { it.tMs }
        val n = sorted.size

        if (n == targetLength) {
            return sorted.map { floatArrayOf(it.x, it.y, it.z) }.toTypedArray()
        }

        val out = Array(targetLength) { floatArrayOf(0f, 0f, 0f) }

        for (i in 0 until targetLength) {
            val pos = i.toFloat() / (targetLength - 1) * (n - 1)
            val idx0 = pos.toInt().coerceIn(0, n - 2)
            val idx1 = (idx0 + 1).coerceIn(0, n - 1)
            val frac = pos - idx0

            val s0 = sorted[idx0]
            val s1 = sorted[idx1]

            out[i][0] = s0.x + frac * (s1.x - s0.x)
            out[i][1] = s0.y + frac * (s1.y - s0.y)
            out[i][2] = s0.z + frac * (s1.z - s0.z)
        }

        return out
    }
}