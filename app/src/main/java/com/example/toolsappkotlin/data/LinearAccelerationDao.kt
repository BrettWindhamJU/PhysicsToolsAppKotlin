package com.example.toolsappkotlin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Blocking operations: call only from a background worker. */
@Dao
interface LinearAccelerationDao {
    @Insert
    fun insertRecording(recording: LinearAccelerationRecording)

    @Query("UPDATE linearacceleration_recordings SET endedAtMillis = :endedAt WHERE id = :id")
    fun finishRecording(id: String, endedAt: Long)

    @Query("""
        SELECT s.id, s.startedAtMillis, s.endedAtMillis, COUNT(r.id) AS readingCount
        FROM linearacceleration_recordings s
        LEFT JOIN linearacceleration_readings r ON r.recordingId = s.id
        GROUP BY s.id
        ORDER BY startedAtMillis DESC
    """)
    fun recordings(): List<RecordingSummary>

    @Query("SELECT * FROM linearacceleration_readings WHERE recordingId IS :recordingId ORDER BY id ASC LIMIT 101 OFFSET :offset")
    fun recordingReadings(recordingId: String, offset: Int): List<LinearAccelerationReading>

    @Query("SELECT * FROM linearacceleration_readings WHERE recordingId = :recordingId ORDER BY id ASC")
    fun allRecordingReadings(recordingId: String): List<LinearAccelerationReading>

    @Insert
    fun insert(reading: LinearAccelerationReading)

    @Query("SELECT * FROM linearacceleration_readings ORDER BY id DESC LIMIT 100")
    fun latestReadings(): List<LinearAccelerationReading>
}


