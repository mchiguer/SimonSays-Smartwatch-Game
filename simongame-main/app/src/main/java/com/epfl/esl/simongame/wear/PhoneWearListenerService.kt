package com.epfl.esl.simongame.wear

import com.epfl.esl.simongame.SimonGameApplication
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PhoneWearListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val container = (application as SimonGameApplication).container
        when (messageEvent.path) {
            WearPaths.ACK -> {
                val data = String(messageEvent.data)
                if (data == "pong" || data.contains("start_ack")) {
                    container.wearConnectionRepository.setConnected(true)
                }
            }

            WearPaths.SENSOR_BULK -> {
                try {
                    val motionWindow = parseBulkSensorData(messageEvent.data)
                    container.motionWindowRepository.emit(motionWindow)
                } catch (e: Exception) {
                }
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
}