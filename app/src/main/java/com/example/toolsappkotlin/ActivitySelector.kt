package com.example.toolsappkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class ActivitySelector : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LabSession.username == null || LabSession.role != "Teacher") { finish(); return }
        setContentView(R.layout.activity_activityselector)
        val buttons = listOf(R.id.btnOpenAccelerometer, R.id.btnOpenAccelerometerWithoutGravity,
            R.id.btnOpenMagnetometer, R.id.btnOpenPressureSensor)
        LabSensors.names.keys.zip(buttons).forEach { (sensor, id) ->
            findViewById<Button>(id).setOnClickListener {
                LabSession.scope = SensorScope(LabSession.username!!, null, 15, false)
                startActivity(Intent(this, LabSensors.screen(sensor)))
            }
        }
        findViewById<Button>(R.id.btnSelectorHome).setOnClickListener { finish() }
    }
}
