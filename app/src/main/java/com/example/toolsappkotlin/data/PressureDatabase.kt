package com.example.toolsappkotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PressureReading::class, PressureRecording::class],
    version = 1,
    exportSchema = false,
)
abstract class PressureDatabase : RoomDatabase() {
    abstract fun pressureDao(): PressureDao

    companion object {
        @Volatile
        private var instance: PressureDatabase? = null

        /** Returns the shared database without retaining an Activity context. */
        fun getInstance(context: Context): PressureDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PressureDatabase::class.java,
                    "pressure.db",
                ).build().also { instance = it }
            }
    }
}


