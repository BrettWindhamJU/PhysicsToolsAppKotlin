package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "magnetometer_readings", indices = [Index("recordingId")])
data class MagnetometerReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAtMillis: Long,
    val sensorTimestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val recordingId: String? = null,
)

