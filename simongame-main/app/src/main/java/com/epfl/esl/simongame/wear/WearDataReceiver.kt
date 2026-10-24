package com.epfl.esl.simongame.wear

class WearDataReceiver(
    private val motionWindowRepository: MotionWindowRepository
) {
    private val accelSamples = mutableListOf<Vec3Sample>()
    private val gyroSamples = mutableListOf<Vec3Sample>()

    private var lastAccData = FloatArray(3)
    private var lastGyroData = FloatArray(3)
    private var accDataReceived = false
    private var gyroDataReceived = false

    private var isRecording = false
    private var recordingStartTime = 0L

    fun startRecording() {
        synchronized(this) {
            accelSamples.clear()
            gyroSamples.clear()
            accDataReceived = false
            gyroDataReceived = false
            recordingStartTime = System.currentTimeMillis()
            isRecording = true
        }
    }

    fun stopRecording() {
        synchronized(this) {
            isRecording = false

            if (accelSamples.isNotEmpty() || gyroSamples.isNotEmpty()) {
                val motionWindow = MotionWindow(
                    turnId = recordingStartTime,
                    accel = accelSamples.toList(),
                    gyro = gyroSamples.toList()
                )
                motionWindowRepository.emit(motionWindow)
            }

            accelSamples.clear()
            gyroSamples.clear()
        }
    }

    fun getSampleCount(): Pair<Int, Int> {
        synchronized(this) {
            return accelSamples.size to gyroSamples.size
        }
    }

    fun onSensorDataReceived(sensorType: String, sensorData: FloatArray, timestamp: Long) {
        if (!isRecording) return

        synchronized(this) {
            when (sensorType) {
                "ACCELEROMETER" -> {
                    lastAccData = sensorData
                    accDataReceived = true
                }
                "GYROSCOPE" -> {
                    lastGyroData = sensorData
                    gyroDataReceived = true
                }
            }

            if (accDataReceived && gyroDataReceived) {
                val relativeTime = timestamp - recordingStartTime
                accelSamples.add(Vec3Sample(
                    tMs = relativeTime,
                    x = lastAccData[0],
                    y = lastAccData[1],
                    z = lastAccData[2]
                ))
                gyroSamples.add(Vec3Sample(
                    tMs = relativeTime,
                    x = lastGyroData[0],
                    y = lastGyroData[1],
                    z = lastGyroData[2]
                ))
            }
        }
    }
}