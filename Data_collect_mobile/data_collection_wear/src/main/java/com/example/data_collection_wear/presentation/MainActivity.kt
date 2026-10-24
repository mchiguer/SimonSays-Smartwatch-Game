package com.example.data_collection_wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var accelerometerValues = mutableStateOf(FloatArray(3) { 0f })
    private var gyroscopeValues = mutableStateOf(FloatArray(3) { 0f })
    private var isRecording = mutableStateOf(false)
    private var currentGesture = mutableStateOf("")
    private var connectionStatus = mutableStateOf("Disconnected")
    private var lastUpdateTime = mutableStateOf("")

    private val sendThread = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())

    private val dataBuffer = mutableListOf<Pair<String, FloatArray>>()
    private var sendHandler: Handler? = null
    private var sendRunnable: Runnable? = null

    private val SEND_INTERVAL = 50L

    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try {
            setContent {
                WearApp(
                    isRecording = isRecording.value,
                    currentGesture = currentGesture.value,
                    connectionStatus = connectionStatus.value,
                    lastUpdateTime = lastUpdateTime.value,
                    accValues = accelerometerValues.value,
                    gyroValues = gyroscopeValues.value
                )
            }

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            messageClient = Wearable.getMessageClient(this)
            nodeClient = Wearable.getNodeClient(this)

            setupMessageListener()
            startSensors()
            startFixedSendTimer()
            checkPhoneConnection()
            startTimeUpdater()

        } catch (e: Exception) {
            setContent {
                Text(
                    text = "Erreur\n${e.message?.take(20)}...",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            when (messageEvent.path) {
                "/start_recording" -> {
                    val gesture = String(messageEvent.data)
                    uiHandler.post {
                        isRecording.value = true
                        currentGesture.value = gesture
                        connectionStatus.value = "Recording..."
                        lastUpdateTime.value = "Started"
                    }
                    sendAckToPhone("start_ack_$gesture")
                }
                "/stop_recording" -> {
                    uiHandler.post {
                        isRecording.value = false
                        currentGesture.value = ""
                        connectionStatus.value = "Connected"
                        lastUpdateTime.value = "Stopped"
                    }
                    sendAckToPhone("stop_ack")
                }
                "/ping" -> {
                    sendAckToPhone("pong")
                }
            }
        }
    }

    private fun sendAckToPhone(message: String) {
        sendThread.execute {
            try {
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        val node = nodes.first()
                        messageClient.sendMessage(node.id, "/ack", message.toByteArray())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun startTimeUpdater() {
        val timeHandler = Handler(Looper.getMainLooper())
        val timeRunnable = object : Runnable {
            override fun run() {
                val time = System.currentTimeMillis() % 60000 / 1000
                lastUpdateTime.value = "T+${time}s"
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable)
    }

    private fun checkPhoneConnection() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            uiHandler.post {
                connectionStatus.value = if (nodes.isNotEmpty()) "Connected" else "Disconnected"
            }
        }
    }

    private fun startSensors() {
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.also { gyro ->
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startFixedSendTimer() {
        sendHandler = Handler(Looper.getMainLooper())
        sendRunnable = object : Runnable {
            override fun run() {
                if (isRecording.value && dataBuffer.isNotEmpty()) {
                    sendBuffer()
                }
                sendHandler?.postDelayed(this, SEND_INTERVAL)
            }
        }
        sendHandler?.post(sendRunnable!!)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val values = event.values.clone()
                uiHandler.post { accelerometerValues.value = values }
                if (isRecording.value) {
                    dataBuffer.add("ACCELEROMETER" to values)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val values = event.values.clone()
                uiHandler.post { gyroscopeValues.value = values }
                if (isRecording.value) {
                    dataBuffer.add("GYROSCOPE" to values)
                }
            }
        }
    }

    private fun sendBuffer() {
        if (dataBuffer.isEmpty()) return

        sendThread.execute {
            try {
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        for ((sensorType, values) in dataBuffer.take(10)) {
                            val timestamp = System.currentTimeMillis()
                            val dataMap = DataMap().apply {
                                putString("sensorType", sensorType)
                                putFloatArray("sensorData", values)
                                putLong("timestamp", timestamp)
                            }
                            val putDataRequest = PutDataRequest.create("/sensor_data_$timestamp").apply {
                                data = dataMap.toByteArray()
                            }
                            Wearable.getDataClient(this@MainActivity).putDataItem(putDataRequest)
                        }

                        if (dataBuffer.size > 10) {
                            dataBuffer.subList(0, 10).clear()
                        } else {
                            dataBuffer.clear()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        startSensors()
        checkPhoneConnection()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        sendThread.shutdown()
        sendHandler?.removeCallbacks(sendRunnable!!)
    }
}

@Composable
fun WearApp(
    isRecording: Boolean,
    currentGesture: String,
    connectionStatus: String,
    lastUpdateTime: String,
    accValues: FloatArray,
    gyroValues: FloatArray
) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRecording) "● RECORDING ●" else "○ READY ○",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color.Red else Color.Green
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isRecording && currentGesture.isNotEmpty()) {
                    Text(
                        text = currentGesture.uppercase(),
                        fontSize = 14.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "ACC: ${"%.1f".format(accValues[0])}, ${"%.1f".format(accValues[1])}, ${"%.1f".format(accValues[2])}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = "GYR: ${"%.1f".format(gyroValues[0])}, ${"%.1f".format(gyroValues[1])}, ${"%.1f".format(gyroValues[2])}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = connectionStatus,
                    fontSize = 10.sp,
                    color = if (connectionStatus.contains("Connected")) Color.Green else Color.Red
                )

                Text(
                    text = lastUpdateTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}