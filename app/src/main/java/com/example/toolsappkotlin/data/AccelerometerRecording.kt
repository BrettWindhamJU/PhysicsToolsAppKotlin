package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accelerometer_recordings")
data class AccelerometerRecording(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
)

data class RecordingSummary(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val readingCount: Int,
)
