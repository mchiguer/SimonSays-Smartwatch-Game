package com.example.data_collect_mobile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var spinner: Spinner
    private lateinit var startButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var watchStatusTextView: TextView
    private lateinit var testButton: Button
    private lateinit var trialCounterTextView: TextView
    private lateinit var timerTextView: TextView

    private var recording = false
    private var currentGesture = ""
    private val gestureTrialCounters = mutableMapOf<String, Int>()
    private var sampleCounter = 0

    private val recordingHandler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private var recordingRunnable: Runnable? = null
    private var timerRunnable: Runnable? = null
    private val RECORDING_DURATION_MS = 5000L
    private var recordingStartTime = 0L

    private lateinit var file: File
    private lateinit var writer: FileWriter
    private var sessionStarted = false

    private lateinit var messageClient: MessageClient
    private lateinit var nodeClient: NodeClient
    private lateinit var dataClient: DataClient

    private var lastAccData = FloatArray(3)
    private var lastGyroData = FloatArray(3)
    private var accDataReceived = false
    private var gyroDataReceived = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = androidx.constraintlayout.widget.ConstraintLayout(this)
        createViews(layout)
        setConstraints(layout)
        setContentView(layout)

        try {
            messageClient = Wearable.getMessageClient(this)
            nodeClient = Wearable.getNodeClient(this)
            dataClient = Wearable.getDataClient(this)
        } catch (_: Exception) {}

        setupListeners()
        requestStoragePermission()
        startListeningForData()
        checkWatchConnection()
        initializeSessionFile()

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val displayName = parent?.getItemAtPosition(position).toString()
                val gestureId = displayName.lowercase().replace(" ", "_")
                val currentCount = gestureTrialCounters.getOrDefault(gestureId, 0)
                trialCounterTextView.text = "$displayName: $currentCount/50"
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                trialCounterTextView.text = "Select a gesture to start"
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({ checkWatchConnection() }, 2000)
    }

    private fun createViews(layout: androidx.constraintlayout.widget.ConstraintLayout) {
        layout.setBackgroundColor(Color.WHITE)

        val titleTextView = TextView(this).apply {
            id = View.generateViewId()
            tag = "title_view"
            text = "Data Collection for Simon Says"
            textSize = 22f
            setTextColor(Color.parseColor("#212121"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        spinner = Spinner(this).apply {
            id = View.generateViewId()
            val gestures = listOf("Jump", "Raise Arms", "Spin Around", "Clap", "Say Hi", "Run In Place", "Not Listed")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, gestures)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            setBackgroundResource(android.R.drawable.btn_dropdown)
            setPadding(30, 20, 30, 20)
        }

        trialCounterTextView = TextView(this).apply {
            id = View.generateViewId()
            text = "Select a gesture to start"
            textSize = 16f
            setTextColor(Color.parseColor("#757575"))
        }

        timerTextView = TextView(this).apply {
            id = View.generateViewId()
            text = ""
            textSize = 28f
            setTextColor(Color.parseColor("#FF5252"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            visibility = View.GONE
        }

        startButton = Button(this).apply {
            id = View.generateViewId()
            text = "START RECORDING"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(60, 35, 60, 35)
            elevation = 8f
            setOnClickListener { startRecording() }
        }

        statusTextView = TextView(this).apply {
            id = View.generateViewId()
            text = "Select a gesture and press START"
            textSize = 14f
            setTextColor(Color.parseColor("#9E9E9E"))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        watchStatusTextView = TextView(this).apply {
            id = View.generateViewId()
            text = "Watch: Checking..."
            textSize = 12f
            setTextColor(Color.parseColor("#FF5252"))
            typeface = android.graphics.Typeface.MONOSPACE
        }

        testButton = Button(this).apply {
            id = View.generateViewId()
            text = "Test Connection"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setPadding(35, 20, 35, 20)
            setOnClickListener { testConnection() }
        }

        arrayOf(titleTextView, spinner, trialCounterTextView, timerTextView, startButton, watchStatusTextView, statusTextView, testButton).forEach {
            layout.addView(it)
        }
    }

    private fun setConstraints(layout: androidx.constraintlayout.widget.ConstraintLayout) {
        val titleView = layout.findViewWithTag<TextView>("title_view")
        val titleId = titleView?.id ?: View.NO_ID

        if (titleId != View.NO_ID) {
            titleView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 50.dpToPx()
            }
        }

        spinner.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = titleId
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 30.dpToPx()
            marginStart = 40.dpToPx()
            marginEnd = 40.dpToPx()
        }

        trialCounterTextView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = spinner.id
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 25.dpToPx()
        }

        timerTextView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = trialCounterTextView.id
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 15.dpToPx()
        }

        startButton.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = timerTextView.id
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 35.dpToPx()
        }

        watchStatusTextView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = startButton.id
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 35.dpToPx()
        }

        statusTextView.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToBottom = watchStatusTextView.id
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 15.dpToPx()
            marginStart = 40.dpToPx()
            marginEnd = 40.dpToPx()
        }

        testButton.layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = 30.dpToPx()
        }
    }

    private fun initializeSessionFile() {
        val dir = File("/sdcard/IMU_Data")
        try {
            if (!dir.exists()) dir.mkdirs()
        } catch (_: Exception) {}

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        file = File(dir, "imu_data_session_$timestamp.csv")

        try {
            writer = FileWriter(file, true)
            writer.append("timestamp,gesture_id,trial,sample_id,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z\n")
            writer.flush()
            sessionStarted = true
        } catch (_: Exception) {}
    }

    private fun setupListeners() {
        try {
            messageClient.addListener { messageEvent ->
                runOnUiThread {
                    when (messageEvent.path) {
                        "/ack" -> {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun testConnection() {
        updateStatus("Testing connection...")

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            runOnUiThread {
                if (nodes.isEmpty()) {
                    updateWatchStatus("Not connected", Color.parseColor("#FF5252"))
                    updateStatus("No watch found")
                } else {
                    updateWatchStatus("Connected (${nodes.size})", Color.parseColor("#4CAF50"))
                    updateStatus("${nodes.size} watch(es) found")
                    nodes.forEach { node -> sendPingToNode(node) }
                }
            }
        }.addOnFailureListener { e ->
            updateStatus("Error: ${e.message}")
        }
    }

    private fun sendPingToNode(node: Node) {
        messageClient.sendMessage(node.id, "/ping", "test_${System.currentTimeMillis()}".toByteArray())
    }

    private fun startRecording() {
        if (recording) return

        val displayName = spinner.selectedItem?.toString() ?: ""
        currentGesture = displayName.lowercase().replace(" ", "_")
        if (currentGesture.isEmpty()) return

        val currentCount = gestureTrialCounters.getOrDefault(currentGesture, 0)
        gestureTrialCounters[currentGesture] = currentCount + 1
        val trialNumber = gestureTrialCounters[currentGesture]!!

        sampleCounter = 0
        accDataReceived = false
        gyroDataReceived = false
        recordingStartTime = System.currentTimeMillis()

        recording = true
        runOnUiThread {
            startButton.text = "RECORDING..."
            startButton.setBackgroundColor(Color.parseColor("#FF5252"))
            startButton.isEnabled = false
            timerTextView.visibility = View.VISIBLE
            updateStatus("Recording: $displayName (Trial $trialNumber)")
            trialCounterTextView.text = "$displayName: $trialNumber/50"
            trialCounterTextView.setTextColor(Color.parseColor("#4CAF50"))
        }

        sendCommandToWatch("/start_recording", currentGesture)
        startTimerDisplay()

        recordingRunnable = Runnable { stopRecording() }
        recordingHandler.postDelayed(recordingRunnable!!, RECORDING_DURATION_MS)
    }

    private fun startTimerDisplay() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (recording) {
                    val elapsed = System.currentTimeMillis() - recordingStartTime
                    val remainingMs = RECORDING_DURATION_MS - elapsed
                    val remainingSeconds = remainingMs / 1000.0

                    runOnUiThread {
                        timerTextView.text = "%.1f s".format(remainingSeconds)
                    }

                    if (remainingMs > 0) {
                        timerHandler.postDelayed(this, 100)
                    }
                }
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopRecording() {
        if (!recording) return

        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        recording = false

        runOnUiThread {
            startButton.text = "COMPLETED"
            startButton.setBackgroundColor(Color.parseColor("#757575"))
            timerTextView.visibility = View.GONE
            updateStatus("Recording completed - $sampleCounter samples")
        }

        sendCommandToWatch("/stop_recording", "")

        try { writer.flush() } catch (_: Exception) {}

        Handler(Looper.getMainLooper()).postDelayed({
            runOnUiThread {
                startButton.text = "START RECORDING"
                startButton.setBackgroundColor(Color.parseColor("#4CAF50"))
                startButton.isEnabled = true

                val displayName = spinner.selectedItem?.toString() ?: currentGesture
                val currentCount = gestureTrialCounters.getOrDefault(currentGesture, 0)
                trialCounterTextView.text = "$displayName: $currentCount/50"
                trialCounterTextView.setTextColor(Color.parseColor("#757575"))

                updateStatus("Ready for next trial")
            }
        }, 1000)
    }

    private fun sendCommandToWatch(path: String, data: String) {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                runOnUiThread {
                    updateWatchStatus("Not connected", Color.parseColor("#FF5252"))
                    updateStatus("No watch available")
                }
                return@addOnSuccessListener
            }

            val node = nodes.first()
            messageClient.sendMessage(node.id, path, data.toByteArray())
        }
    }

    private fun checkWatchConnection() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            runOnUiThread {
                if (nodes.isNotEmpty()) {
                    updateWatchStatus("Connected", Color.parseColor("#4CAF50"))
                } else {
                    updateWatchStatus("Not connected", Color.parseColor("#FF5252"))
                }
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread { statusTextView.text = message }
    }

    private fun updateWatchStatus(text: String, color: Int) {
        runOnUiThread {
            watchStatusTextView.text = "Smartwatch: $text"
            watchStatusTextView.setTextColor(color)
        }
    }

    private fun writeSensorData(timestamp: Long, accData: FloatArray, gyroData: FloatArray) {
        if (!recording || !sessionStarted) return

        try {
            val trialNumber = gestureTrialCounters[currentGesture] ?: 0
            val line = "$timestamp,$currentGesture,$trialNumber,$sampleCounter," +
                    "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f\n".format(
                        accData[0], accData[1], accData[2],
                        gyroData[0], gyroData[1], gyroData[2]
                    )
            writer.append(line)
            sampleCounter++
        } catch (_: Exception) {}
    }

    private fun startListeningForData() {
        dataClient.addListener { dataEventBuffer ->
            try {
                dataEventBuffer.forEach { dataEvent ->
                    if (dataEvent.type == DataEvent.TYPE_CHANGED) {
                        try {
                            val uri = dataEvent.dataItem.uri.path
                            if (uri?.startsWith("/sensor_data") == true) {
                                val dataMapItem = DataMapItem.fromDataItem(dataEvent.dataItem)
                                val dataMap = dataMapItem.dataMap

                                val sensorType = dataMap.getString("sensorType", "")
                                val sensorData = dataMap.getFloatArray("sensorData") ?: FloatArray(3)

                                if (sensorType == "ACCELEROMETER") {
                                    lastAccData = sensorData
                                    accDataReceived = true
                                } else if (sensorType == "GYROSCOPE") {
                                    lastGyroData = sensorData
                                    gyroDataReceived = true
                                }

                                if (accDataReceived && gyroDataReceived) {
                                    writeSensorData(System.currentTimeMillis(), lastAccData, lastGyroData)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } finally {
                dataEventBuffer.release()
            }
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        checkWatchConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recording) stopRecording()

        try {
            writer.flush()
            writer.close()
        } catch (_: Exception) {}


        timerRunnable?.let { timerHandler.removeCallbacks(it) }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}