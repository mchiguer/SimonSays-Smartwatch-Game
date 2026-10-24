package com.epfl.esl.wear.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.os.Build
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.epfl.esl.wear.WearListenerService
import com.google.android.gms.wearable.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), SensorEventListener {

    companion object {
        const val PATH_START_RECORDING = "/start_recording"
        const val PATH_STOP_RECORDING = "/stop_recording"
        const val PATH_SENSOR_BULK = "/sensor_bulk"
        const val PATH_ACK = "/ack"
        const val PATH_PING = "/ping"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var isRecording = mutableStateOf(false)
    private var moveName = mutableStateOf("")
    private var connectionStatus = mutableStateOf("Connecting...")
    private var messageCount = mutableIntStateOf(0)

    private var accelerometerValues = mutableStateOf(FloatArray(3) { 0f })
    private var gyroscopeValues = mutableStateOf(FloatArray(3) { 0f })

    private val launchTime: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date())

    private val wearReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WearListenerService.ACTION_START_RECORDING -> {
                    val gesture = intent.getStringExtra(WearListenerService.EXTRA_GESTURE) ?: ""
                    moveName.value = gesture
                    isRecording.value = true
                    connectionStatus.value = "Recording..."
                    messageCount.intValue++
                }
                WearListenerService.ACTION_STOP_RECORDING -> {
                    isRecording.value = false
                    connectionStatus.value = "Connected"
                    messageCount.intValue++
                }
                WearListenerService.ACTION_PING -> {
                    connectionStatus.value = "Connected"
                    messageCount.intValue++
                }
            }
        }
    }

    private val sendThread = Executors.newSingleThreadExecutor()
    private val uiHandler = Handler(Looper.getMainLooper())

    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try {
            setContent {
                SimonWatchUI(
                    isRecording = isRecording.value,
                    moveName = moveName.value,
                    connectionStatus = connectionStatus.value,
                    messageCount = messageCount.intValue,
                    accValues = accelerometerValues.value,
                    gyroValues = gyroscopeValues.value,
                    launchTime = launchTime
                )
            }

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            com.epfl.esl.wear.SensorRecorder.init(applicationContext)

            messageClient = Wearable.getMessageClient(this)
            nodeClient = Wearable.getNodeClient(this)

            val filter = IntentFilter().apply {
                addAction(WearListenerService.ACTION_START_RECORDING)
                addAction(WearListenerService.ACTION_STOP_RECORDING)
                addAction(WearListenerService.ACTION_PING)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(wearReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(wearReceiver, filter)
            }

            setupMessageListener()
            startSensors()
            checkPhoneConnection()

        } catch (e: Exception) {
        }
    }

    private fun setupMessageListener() {
        messageClient.addListener { messageEvent ->
            uiHandler.post {
                messageCount.intValue++
            }

            when (messageEvent.path) {
                PATH_START_RECORDING -> {
                    val gesture = String(messageEvent.data)

                    com.epfl.esl.wear.SensorRecorder.startRecording()

                    uiHandler.post {
                        moveName.value = gesture
                        isRecording.value = true
                        connectionStatus.value = "Recording..."
                    }

                    sendAckToPhone("start_ack_$gesture")
                }

                PATH_STOP_RECORDING -> {
                    uiHandler.post {
                        isRecording.value = false
                        connectionStatus.value = "Sending..."
                    }

                    val (accData, gyroData) = com.epfl.esl.wear.SensorRecorder.stopRecordingAndGetData()

                    if (accData.isNotEmpty() || gyroData.isNotEmpty()) {
                        sendBulkSensorDataFromRecorder(accData, gyroData)
                    }

                    uiHandler.post {
                        connectionStatus.value = "Connected"
                    }

                    sendAckToPhone("stop_ack")
                }

                PATH_PING -> {
                    sendAckToPhone("pong")
                }

                PATH_ACK -> {
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
                        messageClient.sendMessage(
                            node.id,
                            PATH_ACK,
                            message.toByteArray()
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun sendBulkSensorDataFromRecorder(accData: List<FloatArray>, gyroData: List<FloatArray>) {
        if (accData.isEmpty() && gyroData.isEmpty()) {
            return
        }

        val bufferSize = 4 + accData.size * 12 + 4 + gyroData.size * 12
        val byteBuffer = java.nio.ByteBuffer.allocate(bufferSize)
            .order(java.nio.ByteOrder.BIG_ENDIAN)

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

        sendThread.execute {
            try {
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        val node = nodes.first()
                        messageClient.sendMessage(node.id, PATH_SENSOR_BULK, payload)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun checkPhoneConnection() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val connected = nodes.isNotEmpty()
            uiHandler.post {
                connectionStatus.value = if (connected) "Connected" else "Disconnected"
            }
        }
    }

    private fun startSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                uiHandler.post {
                    accelerometerValues.value = event.values.clone()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                uiHandler.post {
                    gyroscopeValues.value = event.values.clone()
                }
            }
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
        try { unregisterReceiver(wearReceiver) } catch (_: Exception) {}
        sensorManager.unregisterListener(this)
        sendThread.shutdown()
    }
}

@Composable
fun SimonWatchUI(
    isRecording: Boolean,
    moveName: String,
    connectionStatus: String,
    messageCount: Int,
    accValues: FloatArray,
    gyroValues: FloatArray,
    launchTime: String = ""
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
                    text = "Simon $launchTime",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Text(
                    text = if (isRecording) "● RECORDING ●" else "○ READY ○",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Color.Red else Color.Green
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isRecording && moveName.isNotEmpty()) {
                    Text(
                        text = moveName.uppercase(),
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
                    color = if (connectionStatus.contains("Connected") || connectionStatus.contains("Recording")) Color.Green else Color.Red
                )

                Text(
                    text = "Msg: $messageCount",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}