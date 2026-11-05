package com.example.toolsappkotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MagnetometerReading::class, MagnetometerRecording::class],
    version = 1,
    exportSchema = false,
)
abstract class MagnetometerDatabase : RoomDatabase() {
    abstract fun magnetometerDao(): MagnetometerDao

    companion object {
        @Volatile
        private var instance: MagnetometerDatabase? = null

        /** Returns the shared database without retaining an Activity context. */
        fun getInstance(context: Context): MagnetometerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MagnetometerDatabase::class.java,
                    "magnetometer.db",
                ).build().also { instance = it }
            }
    }
}

