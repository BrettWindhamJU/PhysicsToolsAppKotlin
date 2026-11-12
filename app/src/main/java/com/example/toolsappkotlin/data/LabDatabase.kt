package com.example.toolsappkotlin.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "labs")
data class Lab(@PrimaryKey val id: String, val teacher: String, val title: String,
    val description: String, val sensors: Int, val createdAt: Long)

@Entity(tableName = "assignments", primaryKeys = ["labId", "student"])
data class LabAssignment(val labId: String, val student: String,
    val results: String = "", val submittedAt: Long? = null)

@Entity(tableName = "lab_recordings")
data class LabRecording(@PrimaryKey val recordingId: String, val owner: String,
    val labId: String?, val sensor: Int)

@Dao
abstract class LabDao {
    @Insert abstract fun insertLab(lab: Lab)
    @Insert abstract fun insertAssignments(rows: List<LabAssignment>)
    @Insert abstract fun link(recording: LabRecording)
    @Query("SELECT * FROM labs WHERE teacher = :teacher ORDER BY createdAt DESC")
    abstract fun teacherLabs(teacher: String): List<Lab>
    @Query("SELECT labs.* FROM labs JOIN assignments ON labs.id = assignments.labId WHERE assignments.student = :student ORDER BY createdAt DESC")
    abstract fun studentLabs(student: String): List<Lab>
    @Query("SELECT * FROM labs WHERE id = :id") abstract fun lab(id: String): Lab?
    @Query("SELECT * FROM assignments WHERE labId = :id ORDER BY student")
    abstract fun assignments(id: String): List<LabAssignment>
    @Query("SELECT * FROM assignments WHERE labId = :id AND student = :student")
    abstract fun assignment(id: String, student: String): LabAssignment?
    @Query("UPDATE assignments SET results = :results WHERE labId = :id AND student = :student AND submittedAt IS NULL")
    abstract fun saveDraft(id: String, student: String, results: String): Int
    @Query("UPDATE assignments SET results = :results, submittedAt = :time WHERE labId = :id AND student = :student AND submittedAt IS NULL")
    abstract fun submit(id: String, student: String, results: String, time: Long): Int
    @Query("SELECT recordingId FROM lab_recordings WHERE owner = :owner AND labId IS :labId AND sensor = :sensor")
    abstract fun recordingIds(owner: String, labId: String?, sensor: Int): List<String>
    @Query("SELECT recordingId FROM lab_recordings")
    abstract fun linkedRecordingIds(): List<String>
    @Query("SELECT COUNT(*) FROM lab_recordings WHERE owner = :owner AND labId = :labId")
    abstract fun recordingCount(owner: String, labId: String): Int
    @Transaction open fun create(lab: Lab, students: List<String>) {
        require(lab.title.isNotBlank() && lab.description.isNotBlank() && lab.sensors in 1..15)
        require(students.isNotEmpty())
        insertLab(lab)
        insertAssignments(students.distinct().map { LabAssignment(lab.id, it) })
    }
}

@Database(entities = [Lab::class, LabAssignment::class, LabRecording::class], version = 1, exportSchema = false)
abstract class LabDatabase : RoomDatabase() {
    abstract fun labs(): LabDao
    companion object {
        @Volatile private var instance: LabDatabase? = null
        fun getInstance(context: Context): LabDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LabDatabase::class.java,
                "labs.db").build().also { instance = it }
        }
    }
}

