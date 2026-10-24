package com.epfl.esl.simongame.solo_mode

import com.epfl.esl.simongame.game.Move
import com.epfl.esl.simongame.wear.MotionWindow
import kotlin.math.sqrt

data class PredictionResult(
    val move: Move,
    val confidence: Float,
    val allScores: Map<Move, Float> = emptyMap()
) {
    fun isConfident(threshold: Float = 0.6f): Boolean = confidence >= threshold
}

interface MotionToMovePredictor {
    fun predict(window: MotionWindow): Move

    fun predictWithConfidence(window: MotionWindow): PredictionResult
}

class SimpleEnergyMovePredictor(
    private val idleThreshold: Float = 2.0f
) : MotionToMovePredictor {

    override fun predict(window: MotionWindow): Move {
        return predictWithConfidence(window).move
    }

    override fun predictWithConfidence(window: MotionWindow): PredictionResult {
        val e = avgMagnitude(window.accel)
        if (e < idleThreshold) {
            return PredictionResult(Move.IDLE, confidence = 0.9f)
        }

        val activeMoves = listOf(
            Move.JUMP, Move.RAISE_ARMS, Move.SPIN_AROUND,
            Move.CLAP, Move.SAY_HI, Move.RUN_IN_PLACE
        )
        return PredictionResult(activeMoves.random(), confidence = 0.5f)
    }

    private fun avgMagnitude(samples: List<com.epfl.esl.simongame.wear.Vec3Sample>): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0f
        for (s in samples) sum += sqrt(s.x * s.x + s.y * s.y + s.z * s.z)
        return sum / samples.size
    }
}
