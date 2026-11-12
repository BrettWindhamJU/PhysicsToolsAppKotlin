package com.example.toolsappkotlin

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toolsappkotlin.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabDatabaseTest {
    @Test fun assignmentsRecordingsAndSubmissionStaySeparate() {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LabDatabase::class.java)
            .build()
        try {
                val dao = db.labs()
                dao.create(Lab("lab", "teacher", "Motion", "Measure motion", 3, 1), listOf("alice", "bob"))
                dao.create(Lab("other", "otherTeacher", "Pressure", "Measure pressure", 8, 2), listOf("bob"))
                assertEquals(listOf("lab"), dao.studentLabs("alice").map { it.id })
                assertEquals(listOf("lab"), dao.teacherLabs("teacher").map { it.id })
                dao.link(LabRecording("a", "alice", "lab", 1))
                dao.link(LabRecording("b", "bob", "lab", 1))
                dao.link(LabRecording("c", "alice", "lab", 2))
                assertEquals(listOf("a"), dao.recordingIds("alice", "lab", 1))
                assertTrue(dao.recordingIds("alice", "other", 1).isEmpty())
                assertEquals(1, dao.saveDraft("lab", "alice", "Observation"))
                assertEquals(1, dao.submit("lab", "alice", "Final results", 123))
                assertEquals(0, dao.saveDraft("lab", "alice", "Overwrite"))
                assertEquals(0, dao.submit("lab", "alice", "Overwrite", 456))
                assertEquals("Final results", dao.assignment("lab", "alice")!!.results)
                assertNull(dao.assignment("lab", "bob")!!.submittedAt)
            } finally { db.close() }
    }
}

