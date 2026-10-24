package com.epfl.esl.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WearListenerService : WearableListenerService() {

    companion object {
        const val PATH_START_RECORDING = "/start_recording"
        const val PATH_STOP_RECORDING = "/stop_recording"
        const val PATH_SENSOR_BULK = "/sensor_bulk"
        const val PATH_ACK = "/ack"
        const val PATH_PING = "/ping"

        const val ACTION_START_RECORDING = "com.epfl.esl.wear.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.epfl.esl.wear.STOP_RECORDING"
        const val ACTION_PING = "com.epfl.esl.wear.PING"
        const val EXTRA_GESTURE = "gesture"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        SensorRecorder.init(applicationContext)

        when (messageEvent.path) {
            PATH_START_RECORDING -> {
                val gesture = String(messageEvent.data)
                SensorRecorder.startRecording()

                try {
                    val intent = android.content.Intent(ACTION_START_RECORDING)
                    intent.setPackage(packageName)
                    intent.putExtra(EXTRA_GESTURE, gesture)
                    sendBroadcast(intent)
                } catch (_: Exception) {}

                sendAck("start_ack_$gesture")
            }

            PATH_STOP_RECORDING -> {
                val (accData, gyroData) = SensorRecorder.stopRecordingAndGetData()

                if (accData.isNotEmpty() || gyroData.isNotEmpty()) {
                    sendBulkSensorData(accData, gyroData)
                }

                try {
                    val intent = android.content.Intent(ACTION_STOP_RECORDING)
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                } catch (_: Exception) {}

                sendAck("stop_ack")
            }

            PATH_PING -> {
                try {
                    val intent = android.content.Intent(ACTION_PING)
                    intent.setPackage(packageName)
                    sendBroadcast(intent)
                } catch (_: Exception) {}
                sendAck("pong")
            }

            PATH_ACK -> {
            }
        }
    }

    private fun sendBulkSensorData(accData: List<FloatArray>, gyroData: List<FloatArray>) {
        if (accData.isEmpty() && gyroData.isEmpty()) {
            return
        }

        val bufferSize = 4 + accData.size * 12 + 4 + gyroData.size * 12
        val byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.BIG_ENDIAN)

        byteBuffer.putInt(accData.size)
        for (sample in accData) {
            byteBuffer.putFloat(sample[0])
            byteBuffer.putFloat(sample[1])
            byteBuffer.putFloat(sample[2])
        }
        byteBuffer.putInt(gyroData.size)
        for (sample in gyroData) {
            byteBuffer.putFloat(sample[0])
            byteBuffer.putFloat(sample[1])
            byteBuffer.putFloat(sample[2])
        }

        val payload = byteBuffer.array()

        Thread {
            try {
                val nodeClient = Wearable.getNodeClient(this)
                val msgClient = Wearable.getMessageClient(this)

                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        val node = nodes.first()
                        msgClient.sendMessage(node.id, PATH_SENSOR_BULK, payload)
                    }
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun sendAck(message: String) {
        Thread {
            try {
                val nodeClient = Wearable.getNodeClient(this)
                val msgClient = Wearable.getMessageClient(this)

                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        val node = nodes.first()
                        msgClient.sendMessage(node.id, PATH_ACK, message.toByteArray())
                    }
                }
            } catch (e: Exception) {
            }
        }.start()
    }
}