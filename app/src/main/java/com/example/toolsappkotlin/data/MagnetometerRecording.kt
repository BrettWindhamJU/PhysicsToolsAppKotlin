package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "magnetometer_recordings")
data class MagnetometerRecording(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
)


