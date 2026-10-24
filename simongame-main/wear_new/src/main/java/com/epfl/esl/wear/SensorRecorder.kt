package com.epfl.esl.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

object SensorRecorder : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var isInitialized = false

    @Volatile
    var isRecording = false
        private set

    @Volatile
    private var dataAlreadySent = false

    private val accBuffer = mutableListOf<FloatArray>()
    private val gyroBuffer = mutableListOf<FloatArray>()

    fun init(context: Context) {
        if (isInitialized) return
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        isInitialized = true
    }

    fun startRecording() {
        if (!isInitialized) {
            return
        }

        dataAlreadySent = false

        synchronized(this) {
            accBuffer.clear()
            gyroBuffer.clear()
        }

        val sm = sensorManager!!
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        isRecording = true
    }

    fun stopRecordingAndGetData(): Pair<List<FloatArray>, List<FloatArray>> {
        synchronized(this) {
            if (dataAlreadySent) {
                return emptyList<FloatArray>() to emptyList<FloatArray>()
            }
            dataAlreadySent = true
        }

        isRecording = false
        sensorManager?.unregisterListener(this)

        val result: Pair<List<FloatArray>, List<FloatArray>>
        synchronized(this) {
            result = accBuffer.toList() to gyroBuffer.toList()
            accBuffer.clear()
            gyroBuffer.clear()
        }
        return result
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isRecording) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                synchronized(this) {
                    accBuffer.add(event.values.clone())
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                synchronized(this) {
                    gyroBuffer.add(event.values.clone())
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}