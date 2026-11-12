package com.example.toolsappkotlin

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toolsappkotlin.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LabWorkflowTest {
    private fun views(root: View): List<View> = listOf(root) +
        if (root is ViewGroup) (0 until root.childCount).flatMap { views(root.getChildAt(it)) } else emptyList()
    private fun waitFor(scenario: ActivityScenario<HomeActivity>, text: String) {
        repeat(100) {
            var found = false
            scenario.onActivity { found = views(it.window.decorView).filterIsInstance<TextView>().any { v -> v.text.toString() == text } }
            if (found) return
            Thread.sleep(50)
        }
        error("Missing screen text: $text")
    }
    private fun click(scenario: ActivityScenario<HomeActivity>, text: String) {
        waitFor(scenario, text)
        scenario.onActivity { activity ->
            views(activity.window.decorView).filterIsInstance<Button>().first { it.text.toString() == text }.performClick()
        }
    }
    private fun login(name: String, role: String) =
        LabSession.login(User(name, role, byteArrayOf(), byteArrayOf(), 1, "test"))

    @Test fun studentSubmitsAndTeacherReviewsOnlyAssignedTools() {
        val db = LabDatabase.getInstance(ApplicationProvider.getApplicationContext())
        val id = UUID.randomUUID().toString()
        val student = "test-student-$id"
        val teacher = "test-teacher-$id"
        db.labs().create(Lab(id, teacher, "Workflow test", "Measure acceleration", 1, 1), listOf(student))
        try {
            login(student, "Student")
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                click(scenario, "Open lab")
                waitFor(scenario, "Record evidence")
                scenario.onActivity { activity ->
                    val all = views(activity.window.decorView)
                    assertFalse(all.filterIsInstance<Button>().any { it.text.toString() == "Pressure" })
                    all.filterIsInstance<EditText>().single().setText("Acceleration remained steady.")
                }
                click(scenario, "Submit results")
                waitFor(scenario, "Submitted · results and recordings are read-only")
                assertEquals("Acceleration remained steady.", db.labs().assignment(id, student)!!.results)
            }
            login(teacher, "Teacher")
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                click(scenario, "View lab & results")
                click(scenario, "Review results")
                waitFor(scenario, "Acceleration remained steady.")
                waitFor(scenario, "View Accelerometer")
                scenario.onActivity { activity ->
                    assertTrue(views(activity.window.decorView).filterIsInstance<EditText>().isEmpty())
                }
            }
        } finally {
            db.openHelper.writableDatabase.execSQL("DELETE FROM assignments WHERE labId = ?", arrayOf(id))
            db.openHelper.writableDatabase.execSQL("DELETE FROM labs WHERE id = ?", arrayOf(id))
            LabSession.logout()
        }
    }
}
