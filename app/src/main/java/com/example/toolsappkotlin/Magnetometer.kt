package com.example.toolsappkotlin

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class Magnetometer : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null

    private var isTracking = false

    private lateinit var sensorRecordButton: Button
    private lateinit var status: TextView

    private lateinit var xValue: TextView
    private lateinit var yValue: TextView
    private lateinit var zValue: TextView

    private lateinit var magnetometerGraph: LiveAccelerometerGraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_accelerometer)

        val padding = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                bars.left + padding,
                bars.top,
                bars.right + padding,
                bars.bottom
            )

            insets
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Obtain the magnetometer sensor.
        magnetometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD
        )

        sensorRecordButton = findViewById(R.id.btnSensorRecord)
        status = findViewById(R.id.recordingStatus)

        xValue = findViewById(R.id.xValue)
        yValue = findViewById(R.id.yValue)
        zValue = findViewById(R.id.zValue)

        magnetometerGraph = findViewById(R.id.accelerometerGraph)

        sensorRecordButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }

        if (magnetometer == null) {
            sensorRecordButton.isEnabled = false
            status.text = "Magnetometer not available"
        }
    }

    private fun startTracking(): Boolean {

        val sensor = magnetometer ?: return false

        isTracking = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        if (isTracking) {
            sensorRecordButton.text = "Stop"
            status.text = "Reading magnetometer"
        } else {
            status.text = "Failed to start sensor"
        }

        return isTracking
    }

    private fun stopTracking() {

        sensorManager.unregisterListener(this)

        isTracking = false

        sensorRecordButton.text = "Start"
        status.text = "Magnetometer stopped"
    }

    override fun onPause() {
        stopTracking()
        super.onPause()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

    override fun onSensorChanged(event: SensorEvent?) {

        if (
            !isTracking ||
            event == null ||
            event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD
        ) return

        // Magnetometer measurements in microtesla (µT).
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Update live magnetometer graph.
        magnetometerGraph.addReading(
            event.timestamp,
            x,
            y,
            z
        )

        // Update displayed sensor values.
        xValue.text = String.format(
            Locale.getDefault(),
            "X: %.2f µT",
            x
        )

        yValue.text = String.format(
            Locale.getDefault(),
            "Y: %.2f µT",
            y
        )

        zValue.text = String.format(
            Locale.getDefault(),
            "Z: %.2f µT",
            z
        )
    }
}
