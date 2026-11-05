package com.example.toolsappkotlin

import com.example.toolsappkotlin.data.AccelerometerReading
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingAnalysisTest {
    @Test
    fun entireLongRecordingIsSentInOrderWithoutRoundingAxes() {
        val rows = (0..9999).map { index ->
            AccelerometerReading(recordedAtMillis = 0,
                sensorTimestampNanos = 5_000_000_000L + index * 75_123_456L,
                x = index.toFloat(), y = -1.2345678f, z = 9.876543f)
        }
        val csv = RecordingAnalysis.buildPrompt(rows)
            .substringAfter("elapsed_ms,x,y,z\n").trim().lines()
        assertEquals(rows.size, csv.size)
        rows.forEachIndexed { index, row ->
            assertEquals("${index * 75_123_456L / 1_000_000.0},${row.x},${row.y},${row.z}", csv[index])
        }
    }

    @Test
    fun singleReadingStartsAtZeroElapsedTime() {
        val row = AccelerometerReading(recordedAtMillis = 0,
            sensorTimestampNanos = 123456789, x = 3f, y = 4f, z = 0f)
        val csv = RecordingAnalysis.buildPrompt(listOf(row))
            .substringAfter("elapsed_ms,x,y,z\n").trim()
        assertEquals("0.0,3.0,4.0,0.0", csv)
    }
}
