package com.example.toolsappkotlin

import com.example.toolsappkotlin.data.LinearAccelerationReading
import com.example.toolsappkotlin.data.PressureReading
import org.junit.Assert.*
import org.junit.Test

class AdditionalSensorAnalysisTest {
    @Test fun pressureIncludesEveryScalarReading() {
        val rows = (0..204).map { PressureReading(recordedAtMillis = 0,
            sensorTimestampNanos = it * 75000000L, pressure = 1000f + it) }
        val prompt = PressureAnalysis.buildPrompt(rows)
        assertTrue(prompt.contains("hPa"))
        assertFalse(prompt.contains("include gravity"))
        val csv = prompt.substringAfter("elapsed_ms,pressure\n").trim().lines()
        assertEquals(205, csv.size)
        assertEquals("15300.0,1204.0", csv.last())
    }
    @Test fun linearAccelerationExcludesGravity() {
        val prompt = LinearAccelerationAnalysis.buildPrompt(listOf(
            LinearAccelerationReading(recordedAtMillis = 0, sensorTimestampNanos = 1,
                x = 1f, y = 2f, z = 3f)))
        assertTrue(prompt.contains("exclude gravity"))
        assertTrue(prompt.trim().endsWith("0.0,1.0,2.0,3.0"))
    }
}
