package com.example.toolsappkotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LinearAccelerationReading::class, LinearAccelerationRecording::class],
    version = 1,
    exportSchema = false,
)
abstract class LinearAccelerationDatabase : RoomDatabase() {
    abstract fun linearaccelerationDao(): LinearAccelerationDao

    companion object {
        @Volatile
        private var instance: LinearAccelerationDatabase? = null

        /** Returns the shared database without retaining an Activity context. */
        fun getInstance(context: Context): LinearAccelerationDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LinearAccelerationDatabase::class.java,
                    "linearacceleration.db",
                ).build().also { instance = it }
            }
    }
}


