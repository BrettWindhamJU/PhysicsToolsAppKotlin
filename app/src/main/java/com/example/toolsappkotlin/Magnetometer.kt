package com.example.toolsappkotlin

import android.hardware.Sensor
import android.app.AlertDialog
import android.util.Log
import android.widget.ScrollView
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import com.google.firebase.appcheck.FirebaseAppCheck
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.toolsappkotlin.data.MagnetometerReading
import com.example.toolsappkotlin.data.MagnetometerRecording
import com.example.toolsappkotlin.data.MagnetometerDatabase
import com.example.toolsappkotlin.data.RecordingSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class Magnetometer : ComponentActivity(), SensorEventListener {
    private lateinit var labScope: SensorScope
    private val labSensor = 4
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isTracking = false
    private var activeRecording: String? = null
    private lateinit var sensorRecordButton: Button
    private lateinit var status: TextView
    private lateinit var heading: TextView
    private lateinit var history: ListView
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var back: Button
    private lateinit var analyze: Button
    private var selectedRecording: RecordingSummary? = null
    private var pageOffset = 0
    private var historyRequest = 0

    private lateinit var accelerometerGraph: LiveAccelerometerGraph

    // Serial execution ensures session creation, readings, finalization and queries stay in order.
    // shutdown() drains accepted writes when the Activity is destroyed.
    private val databaseWorker = Executors.newSingleThreadExecutor()
    private val dao by lazy { MagnetometerDatabase.getInstance(applicationContext).magnetometerDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labScope = LabSensors.admit(this, labSensor) ?: return
        enableEdgeToEdge()
        setContentView(R.layout.activity_accelerometer)
        findViewById<Button>(R.id.btnBackToSelector).setOnClickListener { finish() }
        val padding = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + padding, bars.top, bars.right + padding, bars.bottom)
            insets
        }
        findViewById<TextView>(R.id.xValue).text = "X: — µT"
        findViewById<TextView>(R.id.yValue).text = "Y: — µT"
        findViewById<TextView>(R.id.zValue).text = "Z: — µT"
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorRecordButton = findViewById(R.id.btnSensorRecord)
        status = findViewById(R.id.recordingStatus)
        heading = findViewById(R.id.historyHeading)
        history = findViewById(R.id.recordingsList)
        previous = findViewById(R.id.btnPrevious)
        next = findViewById(R.id.btnNext)
        back = findViewById(R.id.btnBackRecordings)
        analyze = findViewById(R.id.btnAnalyze)
        analyze.setOnClickListener { selectedRecording?.let { analyzeRecording(it) } }

        accelerometerGraph = findViewById(R.id.accelerometerGraph)

        sensorRecordButton.isEnabled = !labScope.readOnly
        sensorRecordButton.setOnClickListener {
            if (labScope.readOnly) return@setOnClickListener
            if (isTracking) {
                stopTracking()
            } else {
                if (startTracking()) {
                    startRecording()
                }
            }
        }

        findViewById<Button>(R.id.btnViewData).setOnClickListener { showRecordings() }
        back.setOnClickListener { showRecordings() }
        previous.setOnClickListener { selectedRecording?.let { showRecording(it, pageOffset - 100) } }
        next.setOnClickListener { selectedRecording?.let { showRecording(it, pageOffset + 100) } }
        if (accelerometer == null) {
            sensorRecordButton.isEnabled = false
            status.setText(R.string.no_magnetometer)
        }
        showRecordings()
    }

    private fun startTracking(): Boolean {
        val sensor = accelerometer ?: return false
        isTracking = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        if (isTracking) {
            sensorRecordButton.setText(R.string.stop_sensors_recording)
            status.setText(R.string.recording)
        } else {
            status.setText(R.string.sensor_start_failed)
        }

        return isTracking
    }

    private fun startRecording() {
        accelerometerGraph.clearGraph()
        val recording = MagnetometerRecording(UUID.randomUUID().toString(), System.currentTimeMillis())
        activeRecording = recording.id
        status.setText(R.string.recording)
        save(recording.id) {
            val labs = com.example.toolsappkotlin.data.LabDatabase.getInstance(this).labs()
            val labId = labScope.labId
            if (labId != null) {
                val assignment = labs.assignment(labId, labScope.owner)
                check(assignment != null && assignment.submittedAt == null && labScope.owner == LabSession.username)
            }
            dao.insertRecording(recording)
            labs.link(com.example.toolsappkotlin.data.LabRecording(recording.id, labScope.owner, labScope.labId, labSensor))
        }
    }

    private fun stopRecording() {
        val id = activeRecording ?: return
        val endedAt = System.currentTimeMillis()
        activeRecording = null
        status.setText(R.string.recording_stopped)
        save(id) { dao.finishRecording(id, endedAt) }
        showRecordings()
    }

    private fun save(recordingId: String, action: () -> Unit) {
        databaseWorker.execute {
            try {
                action()
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isDestroyed) {
                        if (activeRecording == recordingId) stopRecording()
                        status.setText(R.string.save_failed)
                    }
                }
            }
        }
    }

    private fun stopTracking() {
        stopRecording()
        sensorManager.unregisterListener(this)
        isTracking = false
        sensorRecordButton.setText(R.string.start_sensors_recording)
    }

    override fun onPause() {
        if (::sensorManager.isInitialized) stopTracking()
        super.onPause()
    }

    override fun onDestroy() {
        databaseWorker.shutdown()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // Update live accelerometer graph
        accelerometerGraph.addReading(
            event.timestamp,
            x,
            y,
            z
        )

        findViewById<TextView>(R.id.xValue).text = getString(R.string.magnetic_axis_value, "X", x)
        findViewById<TextView>(R.id.yValue).text = getString(R.string.magnetic_axis_value, "Y", y)
        findViewById<TextView>(R.id.zValue).text = getString(R.string.magnetic_axis_value, "Z", z)
        activeRecording?.let { id ->
            val reading = MagnetometerReading(
                recordedAtMillis = System.currentTimeMillis() -
                    (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000,
                sensorTimestampNanos = event.timestamp,
                x = x, y = y, z = z, recordingId = id,
            )
            save(id) { dao.insert(reading) }
        }
    }

    private fun formatTime(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(time))

    private fun recordingLabel(recording: RecordingSummary): String {
        val end = recording.endedAtMillis
        val description = if (end != null) getString(R.string.recording_time_range,
            formatTime(recording.startedAtMillis), formatTime(end),
            (end - recording.startedAtMillis).coerceAtLeast(0) / 1000.0)
        else getString(R.string.recording_open, formatTime(recording.startedAtMillis),
            getString(if (recording.id == activeRecording) R.string.recording else R.string.recording_interrupted))
        return getString(R.string.recording_summary, description, recording.readingCount)
    }

    private fun prepareHistory(): Int {
        history.adapter = null
        history.onItemClickListener = null
        heading.setText(R.string.loading_readings)
        previous.isEnabled = false
        next.isEnabled = false
        return ++historyRequest
    }

    private fun showRecordings() {
        selectedRecording = null
        analyze.visibility = View.GONE
        back.visibility = View.GONE
        findViewById<View>(R.id.pageControls).visibility = View.GONE
        val request = prepareHistory()
        databaseWorker.execute {
            try {
                val permitted = com.example.toolsappkotlin.data.LabDatabase.getInstance(this).labs()
                    .recordingIds(labScope.owner, labScope.labId, labSensor).toSet()
                val linked = com.example.toolsappkotlin.data.LabDatabase.getInstance(this).labs().linkedRecordingIds().toSet()
                // Earlier recordings had no owner; keep them in teacher practice only.
                val legacy = labScope.labId == null && LabSession.role == "Teacher"
                val recordings = dao.recordings().filter { it.id in permitted || (legacy && it.id !in linked) }
                runOnUiThread {
                    if (!isDestroyed && request == historyRequest) {
                        heading.setText(if (recordings.isEmpty()) R.string.no_recordings else R.string.recordings_heading)
                        history.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                            recordings.map { recordingLabel(it) })
                        history.setOnItemClickListener { _, _, position, _ -> showRecording(recordings[position], 0) }
                    }
                }
            } catch (error: Exception) { historyError(request) }
        }
    }

    private fun showRecording(recording: RecordingSummary, offset: Int) {
        selectedRecording = recording
        analyze.visibility = View.VISIBLE
        analyze.isEnabled = recording.id != activeRecording
        pageOffset = offset.coerceAtLeast(0)
        back.visibility = View.VISIBLE
        findViewById<View>(R.id.pageControls).visibility = View.VISIBLE
        val request = prepareHistory()
        val queryOffset = pageOffset
        databaseWorker.execute {
            try {
                val rows = dao.recordingReadings(recording.id, queryOffset)
                val labels = rows.take(100).map {
                    getString(R.string.saved_reading_row, formatTime(it.recordedAtMillis), it.x, it.y, it.z)
                }
                runOnUiThread {
                    if (!isDestroyed && request == historyRequest) {
                        heading.text = getString(R.string.magnetic_recording_detail, formatTime(recording.startedAtMillis),
                            if (labels.isEmpty()) 0 else queryOffset + 1, queryOffset + labels.size)
                        history.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                            if (labels.isEmpty()) listOf(getString(R.string.no_saved_readings)) else labels)
                        previous.isEnabled = queryOffset > 0
                        next.isEnabled = rows.size > 100
                    }
                }
            } catch (error: Exception) { historyError(request) }
        }
    }

    private fun analyzeRecording(recording: RecordingSummary) {
        val text = TextView(this).apply {
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
            setText(R.string.analysis_preparing)
        }
        val scroll = ScrollView(this).apply { addView(text) }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.analysis_title)
            .setView(scroll)
            .setPositiveButton(R.string.analysis_close, null)
            .create()
        dialog.show()
        // Keep the action visible with the app theme, including while loading.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setTextColor(getColor(R.color.white))
            backgroundTintList = null
            setBackgroundResource(R.drawable.analysis_close_background)
            setText(R.string.analysis_close)
            minHeight = (48 * resources.displayMetrics.density).toInt()
        }
        val job = lifecycleScope.launch {
            var stage = getString(R.string.analysis_stage_readings)
            try {
                // Use the same worker as recording writes so previously queued saves finish first.
                val prompt = withContext(databaseWorker.asCoroutineDispatcher()) {
                    val rows = dao.allRecordingReadings(recording.id)
                    if (rows.isEmpty()) null else MagnetometerAnalysis.buildPrompt(rows)
                }
                if (prompt == null) {
                    text.setText(R.string.analysis_empty)
                } else {
                    stage = getString(R.string.analysis_stage_app_check)
                    text.setText(R.string.analysis_checking)
                    Log.d("GeminiAnalysis", "Verifying App Check")
                    withTimeout(20_000) {
                        FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                    }
                    stage = getString(R.string.analysis_stage_gemini)
                    text.setText(R.string.analysis_loading)
                    Log.d("GeminiAnalysis", "App Check verified; sending Gemini request")
                    val response = withTimeout(180_000) { MagnetometerAnalysis.analyze(prompt) }
                    Log.d("GeminiAnalysis", "Gemini request completed")
                    text.text = if (response.isNullOrBlank()) getString(R.string.analysis_no_response)
                        else response + "\n\n" + getString(R.string.analysis_note)
                }
            } catch (error: TimeoutCancellationException) {
                Log.w("GeminiAnalysis", "Timed out during $stage")
                text.text = getString(R.string.analysis_timeout, stage)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("GeminiAnalysis", "Recording analysis failed during $stage", error)
                text.text = getString(R.string.analysis_failed_stage, stage)
            }
        }
        dialog.setOnDismissListener { job.cancel() }
    }

    private fun historyError(request: Int) {
        runOnUiThread {
            if (!isDestroyed && request == historyRequest) heading.setText(R.string.load_failed)
        }
    }
}






