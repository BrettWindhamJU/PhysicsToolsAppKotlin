package com.example.toolsappkotlin.data

import androidx.room.*
import android.content.Context

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val role: String,
    val salt: ByteArray,
    val passwordHash: ByteArray,
    val iterations: Int,
    val algorithm: String,
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun find(username: String): User?
    @Query("SELECT username FROM users WHERE role = 'Student' ORDER BY username")
    fun students(): List<String>
}

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    companion object {
        @Volatile private var instance: UserDatabase? = null
        fun getInstance(context: Context): UserDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                UserDatabase::class.java, "users.db").build().also { instance = it }
        }
    }
}

