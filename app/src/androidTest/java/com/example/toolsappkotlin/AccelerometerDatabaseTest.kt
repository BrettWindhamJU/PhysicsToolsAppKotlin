package com.example.toolsappkotlin

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toolsappkotlin.data.AccelerometerReading
import com.example.toolsappkotlin.data.AccelerometerRecording
import com.example.toolsappkotlin.data.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AccelerometerDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun withDatabase(name: String, block: (AppDatabase) -> Unit) {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try { block(db) } finally { db.close() }
    }

    @Test
    fun recordingsPersistSeparatelyAndAllPagesAreAccessible() {
        val name = "recordings-test-${UUID.randomUUID()}.db"
        try {
            withDatabase(name) { db ->
                val dao = db.accelerometerDao()
                dao.insertRecording(AccelerometerRecording("first", 1000))
                dao.insertRecording(AccelerometerRecording("second", 2000))
                dao.insertRecording(AccelerometerRecording("empty", 3000))
                repeat(205) { index ->
                    dao.insert(AccelerometerReading(recordedAtMillis = index.toLong(),
                        sensorTimestampNanos = index * 1000L, x = 1.234567f,
                        y = -2.345678f, z = 9.876543f, recordingId = "first"))
                }
                dao.insert(AccelerometerReading(recordedAtMillis = 5000,
                    sensorTimestampNanos = 5000, x = 42f, y = 0f, z = 0f, recordingId = "second"))
                dao.finishRecording("first", 1500)
                dao.finishRecording("second", 2500)
            }
            withDatabase(name) { db ->
                val dao = db.accelerometerDao()
                val recordings = dao.recordings()
                assertEquals(listOf("empty", "second", "first"), recordings.map { it.id })
                assertEquals(listOf(0, 1, 205), recordings.map { it.readingCount })
                assertNull(recordings.first().endedAtMillis)
                assertEquals(1500L, recordings.last().endedAtMillis)
                val pageOne = dao.recordingReadings("first", 0)
                assertEquals(101, pageOne.size) // Extra row indicates another page.
                val all = pageOne.take(100) + dao.recordingReadings("first", 100).take(100) +
                    dao.recordingReadings("first", 200)
                assertEquals((0L..204L).toList(), all.map { it.recordedAtMillis })
                assertEquals(1.234567f, all.first().x, 0f)
                assertEquals(-2.345678f, all.first().y, 0f)
                assertEquals(9.876543f, all.first().z, 0f)
                assertEquals(204000L, all.last().sensorTimestampNanos)
                assertEquals(42f, dao.recordingReadings("second", 0).single().x, 0f)
                assertTrue(dao.recordingReadings("empty", 0).isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }
}
