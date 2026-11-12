package com.example.toolsappkotlin

import android.app.Activity
import android.content.Intent
import com.example.toolsappkotlin.data.*

// Authentication lasts for this app process. A process restart requires login again.
object LabSession {
    var username: String? = null
        private set
    var role: String? = null
        private set
    var scope: SensorScope? = null
    fun login(user: User) { username = user.username; role = user.role; scope = null }
    fun logout() { username = null; role = null; scope = null }
}

data class SensorScope(val owner: String, val labId: String?, val sensors: Int, val readOnly: Boolean)

object LabSensors {
    val names = linkedMapOf(1 to "Accelerometer", 2 to "Without gravity", 4 to "Magnetometer", 8 to "Pressure")
    fun screen(sensor: Int): Class<out Activity> = when (sensor) {
        1 -> Accelerometer::class.java
        2 -> AccelerometerWithoutGravity::class.java
        4 -> Magnetometer::class.java
        else -> PressureSensor::class.java
    }
    fun admit(activity: Activity, sensor: Int): SensorScope? {
        val scope = LabSession.scope
        if (LabSession.username == null || scope == null || scope.sensors and sensor == 0 ||
            (LabSession.role != "Teacher" && scope.owner != LabSession.username)) {
            activity.startActivity(Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            activity.finish()
            return null
        }
        return scope
    }
}
