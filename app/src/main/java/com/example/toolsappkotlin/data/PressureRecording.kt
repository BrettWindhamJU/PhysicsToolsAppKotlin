package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pressure_recordings")
data class PressureRecording(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
)



