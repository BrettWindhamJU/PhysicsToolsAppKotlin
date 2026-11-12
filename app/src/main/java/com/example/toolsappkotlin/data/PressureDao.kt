package com.example.toolsappkotlin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Blocking operations: call only from a background worker. */
@Dao
interface PressureDao {
    @Insert
    fun insertRecording(recording: PressureRecording)

    @Query("UPDATE pressure_recordings SET endedAtMillis = :endedAt WHERE id = :id")
    fun finishRecording(id: String, endedAt: Long)

    @Query("""
        SELECT s.id, s.startedAtMillis, s.endedAtMillis, COUNT(r.id) AS readingCount
        FROM pressure_recordings s
        LEFT JOIN pressure_readings r ON r.recordingId = s.id
        GROUP BY s.id
        ORDER BY startedAtMillis DESC
    """)
    fun recordings(): List<RecordingSummary>

    @Query("SELECT * FROM pressure_readings WHERE recordingId IS :recordingId ORDER BY id ASC LIMIT 101 OFFSET :offset")
    fun recordingReadings(recordingId: String, offset: Int): List<PressureReading>

    @Query("SELECT * FROM pressure_readings WHERE recordingId = :recordingId ORDER BY id ASC")
    fun allRecordingReadings(recordingId: String): List<PressureReading>

    @Insert
    fun insert(reading: PressureReading)

    @Query("SELECT * FROM pressure_readings ORDER BY id DESC LIMIT 100")
    fun latestReadings(): List<PressureReading>
}


