package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pressure_readings", indices = [Index("recordingId")])
data class PressureReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAtMillis: Long,
    val sensorTimestampNanos: Long,
    val pressure: Float,


    val recordingId: String? = null,
)


