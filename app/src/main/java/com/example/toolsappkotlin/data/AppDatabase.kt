package com.example.toolsappkotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AccelerometerReading::class, AccelerometerRecording::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accelerometerDao(): AccelerometerDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Returns the shared database without retaining an Activity context. */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "physics_tools.db",
                ).build().also { instance = it }
            }
    }
}
