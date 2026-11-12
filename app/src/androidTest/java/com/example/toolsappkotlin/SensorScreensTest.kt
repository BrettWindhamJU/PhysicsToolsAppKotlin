package com.example.toolsappkotlin

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorScreensTest {
    @Test fun allSensorScreensOpenAndReturn() {
        LabSession.login(com.example.toolsappkotlin.data.User("screen-test", "Teacher", byteArrayOf(), byteArrayOf(), 1, "test"))
        LabSession.scope = SensorScope("screen-test", null, 15, false)
        listOf(Accelerometer::class.java, Magnetometer::class.java,
            AccelerometerWithoutGravity::class.java, PressureSensor::class.java).forEach { screen ->
            val intent = Intent(ApplicationProvider.getApplicationContext(), screen)
            ActivityScenario.launch<android.app.Activity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    check(activity.findViewById<android.widget.Button>(R.id.btnBackToSelector).performClick())
                }
            }
        }
    }
}

