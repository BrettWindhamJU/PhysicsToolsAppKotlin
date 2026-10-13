package com.example.toolsappkotlin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Blocking operations: call only from a background worker. */
@Dao
interface AccelerometerDao {
    @Insert
    fun insertRecording(recording: AccelerometerRecording)

    @Query("UPDATE accelerometer_recordings SET endedAtMillis = :endedAt WHERE id = :id")
    fun finishRecording(id: String, endedAt: Long)

    @Query("""
        SELECT s.id, s.startedAtMillis, s.endedAtMillis, COUNT(r.id) AS readingCount
        FROM accelerometer_recordings s
        LEFT JOIN accelerometer_readings r ON r.recordingId = s.id
        GROUP BY s.id
        ORDER BY startedAtMillis DESC
    """)
    fun recordings(): List<RecordingSummary>

    @Query("SELECT * FROM accelerometer_readings WHERE recordingId IS :recordingId ORDER BY id ASC LIMIT 101 OFFSET :offset")
    fun recordingReadings(recordingId: String, offset: Int): List<AccelerometerReading>

    @Insert
    fun insert(reading: AccelerometerReading)

    @Query("SELECT * FROM accelerometer_readings ORDER BY id DESC LIMIT 100")
    fun latestReadings(): List<AccelerometerReading>
}
