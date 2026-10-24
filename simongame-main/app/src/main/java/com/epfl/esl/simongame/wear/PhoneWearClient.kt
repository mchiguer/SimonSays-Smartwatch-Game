package com.epfl.esl.simongame.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class PhoneWearClient(
    private val context: Context,
    private val motionWindowRepository: MotionWindowRepository
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TIMEOUT_SECONDS = 5L
    }

    init {
        Wearable.getMessageClient(context).addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearPaths.SENSOR_BULK -> {
                try {
                    val motionWindow = parseBulkSensorData(messageEvent.data)
                    motionWindowRepository.emit(motionWindow)
                } catch (e: Exception) {
                }
            }
            WearPaths.ACK -> {
            }
        }
    }

    private fun parseBulkSensorData(data: ByteArray): MotionWindow {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        val numAcc = buffer.int
        val accSamples = mutableListOf<Vec3Sample>()
        for (i in 0 until numAcc) {
            accSamples.add(Vec3Sample(
                tMs = i.toLong(),
                x = buffer.float,
                y = buffer.float,
                z = buffer.float
            ))
        }

        val numGyro = buffer.int
        val gyroSamples = mutableListOf<Vec3Sample>()
        for (i in 0 until numGyro) {
            gyroSamples.add(Vec3Sample(
                tMs = i.toLong(),
                x = buffer.float,
                y = buffer.float,
                z = buffer.float
            ))
        }

        return MotionWindow(
            turnId = System.currentTimeMillis(),
            accel = accSamples,
            gyro = gyroSamples
        )
    }

    suspend fun sendStartRecording(moveName: String) {
        sendToAllNodes(WearPaths.START_RECORDING, moveName.toByteArray())
    }

    suspend fun sendStopRecording() {
        sendToAllNodes(WearPaths.STOP_RECORDING, byteArrayOf())
    }

    suspend fun sendGameState(state: GameState) {
    }

    suspend fun isWatchConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(
                Wearable.getNodeClient(context).connectedNodes,
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendToAllNodes(path: String, payload: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(
                Wearable.getNodeClient(context).connectedNodes,
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )

            if (nodes.isEmpty()) {
                return@withContext
            }

            val msgClient = Wearable.getMessageClient(context)
            for (n in nodes) {
                try {
                    Tasks.await(
                        msgClient.sendMessage(n.id, path, payload),
                        TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    )
                } catch (sendError: Exception) {
                }
            }
        } catch (e: Exception) {
        }
    }
}