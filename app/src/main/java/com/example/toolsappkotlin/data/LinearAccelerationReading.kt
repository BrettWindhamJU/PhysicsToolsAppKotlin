package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "linearacceleration_readings", indices = [Index("recordingId")])
data class LinearAccelerationReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAtMillis: Long,
    val sensorTimestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val recordingId: String? = null,
)


