package com.example.toolsappkotlin

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.widget.TextView
import android.widget.Button
import kotlin.math.round

class Accelerometer : ComponentActivity(), SensorEventListener {

    // Access the phone's sensors
    private lateinit var sensorManager: SensorManager

    // Accelerometer
    private var accelerometer: Sensor? = null

    // Keeps track of whether we are tracking
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_accelerometer)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Pause and resume button
        val pauseButton: Button = findViewById(R.id.btnPauseResume)

        // Start the page paused
        pauseButton.text = "Resume"

        pauseButton.setOnClickListener {
            if (isTracking) {
                // Currently running, so pause it
                stopTracking()
                pauseButton.text = "Resume"
            } else {
                // Currently paused, so start it
                startTracking()
                pauseButton.text = "Pause"
            }
        }

        // Make sure the values start blank
        findViewById<TextView>(R.id.xValue).text = "X: - "
        findViewById<TextView>(R.id.yValue).text = "Y: - "
        findViewById<TextView>(R.id.zValue).text = "Z: - "
    }

    // Start tracking the accelerometer
    private fun startTracking() {
        accelerometer?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        isTracking = true
    }

    // Stop tracking the accelerometer
    private fun stopTracking() {
        sensorManager.unregisterListener(this)

        isTracking = false

        findViewById<TextView>(R.id.xValue).text = "X: - "
        findViewById<TextView>(R.id.yValue).text = "Y: - "
        findViewById<TextView>(R.id.zValue).text = "Z: - "
    }

    // Android calls this when the app becomes active
    override fun onResume() {
        super.onResume()

        // IMPORTANT:
        // Do NOT automatically start the sensor here.
        //
        // The page is supposed to start paused.
        // The button controls whether tracking starts.
    }

    // Android calls this when the app goes into the background
    override fun onPause() {
        super.onPause()

        // Always stop the sensor when leaving the page
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isTracking && event != null) {

            val x = round(event.values[0] * 100) / 100
            val y = round(event.values[1] * 100) / 100
            val z = round(event.values[2] * 100) / 100

            findViewById<TextView>(R.id.xValue).text = "X: $x"
            findViewById<TextView>(R.id.yValue).text = "Y: $y"
            findViewById<TextView>(R.id.zValue).text = "Z: $z"
        }
    }
}
