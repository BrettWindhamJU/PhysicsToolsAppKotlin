package com.example.toolsappkotlin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "linearacceleration_recordings")
data class LinearAccelerationRecording(
    @PrimaryKey val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
)



