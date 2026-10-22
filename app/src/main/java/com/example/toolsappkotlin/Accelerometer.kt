package com.example.toolsappkotlin

import android.hardware.Sensor
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.toolsappkotlin.data.AccelerometerReading
import com.example.toolsappkotlin.data.AccelerometerRecording
import com.example.toolsappkotlin.data.AppDatabase
import com.example.toolsappkotlin.data.RecordingSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class Accelerometer : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isTracking = false
    private var activeRecording: String? = null
    private lateinit var pauseButton: Button
    private lateinit var recordButton: Button
    private lateinit var status: TextView
    private lateinit var heading: TextView
    private lateinit var history: ListView
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var back: Button
    private var selectedRecording: RecordingSummary? = null
    private var pageOffset = 0
    private var historyRequest = 0

    // Serial execution ensures session creation, readings, finalization and queries stay in order.
    // shutdown() drains accepted writes when the Activity is destroyed.
    private val databaseWorker = Executors.newSingleThreadExecutor()
    private val dao by lazy { AppDatabase.getInstance(applicationContext).accelerometerDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_accelerometer)
        val padding = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + padding, bars.top, bars.right + padding, bars.bottom)
            insets
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        pauseButton = findViewById(R.id.btnPauseResume)
        recordButton = findViewById(R.id.btnRecord)
        status = findViewById(R.id.recordingStatus)
        heading = findViewById(R.id.historyHeading)
        history = findViewById(R.id.recordingsList)
        previous = findViewById(R.id.btnPrevious)
        next = findViewById(R.id.btnNext)
        back = findViewById(R.id.btnBackRecordings)
        pauseButton.setOnClickListener {
            if (isTracking) stopTracking() else startTracking()
        }
        recordButton.setOnClickListener {
            if (activeRecording != null) stopRecording()
            else if (isTracking || startTracking()) startRecording()
        }
        findViewById<Button>(R.id.btnViewData).setOnClickListener { showRecordings() }
        back.setOnClickListener { showRecordings() }
        previous.setOnClickListener { selectedRecording?.let { showRecording(it, pageOffset - 100) } }
        next.setOnClickListener { selectedRecording?.let { showRecording(it, pageOffset + 100) } }
        if (accelerometer == null) {
            pauseButton.isEnabled = false
            recordButton.isEnabled = false
            status.setText(R.string.no_accelerometer)
        }
        showRecordings()
    }

    private fun startTracking(): Boolean {
        val sensor = accelerometer ?: return false
        isTracking = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (isTracking) pauseButton.setText(R.string.pause_readings)
        else status.setText(R.string.sensor_start_failed)
        return isTracking
    }

    private fun startRecording() {
        val recording = AccelerometerRecording(UUID.randomUUID().toString(), System.currentTimeMillis())
        activeRecording = recording.id
        recordButton.setText(R.string.stop_recording)
        status.setText(R.string.recording)
        save(recording.id) { dao.insertRecording(recording) }
    }

    private fun stopRecording() {
        val id = activeRecording ?: return
        val endedAt = System.currentTimeMillis()
        activeRecording = null
        recordButton.setText(R.string.start_recording)
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
        pauseButton.setText(R.string.resume_readings)
    }

    override fun onPause() {
        stopTracking()
        super.onPause()
    }

    override fun onDestroy() {
        databaseWorker.shutdown()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        findViewById<TextView>(R.id.xValue).text = getString(R.string.axis_value, "X", x)
        findViewById<TextView>(R.id.yValue).text = getString(R.string.axis_value, "Y", y)
        findViewById<TextView>(R.id.zValue).text = getString(R.string.axis_value, "Z", z)
        activeRecording?.let { id ->
            val reading = AccelerometerReading(
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
        back.visibility = View.GONE
        findViewById<View>(R.id.pageControls).visibility = View.GONE
        val request = prepareHistory()
        databaseWorker.execute {
            try {
                val recordings = dao.recordings()
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
                        heading.text = getString(R.string.recording_detail, formatTime(recording.startedAtMillis),
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

    private fun historyError(request: Int) {
        runOnUiThread {
            if (!isDestroyed && request == historyRequest) heading.setText(R.string.load_failed)
        }
    }
}
