package com.example.toolsappkotlin

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.toolsappkotlin.data.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class HomeActivity : ComponentActivity() {
    private lateinit var page: LinearLayout
    private val dao by lazy { LabDatabase.getInstance(this).labs() }
    private var selectedLab: String? = null
    private var selectedStudent: String? = null
    private var resultsField: EditText? = null
    private var restoredDraft: String? = null
    private var generation = 0
    private val user get() = LabSession.username!!
    private val teacher get() = LabSession.role == "Teacher"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredDraft = savedInstanceState?.getString("draft")
        selectedLab = savedInstanceState?.getString("lab")
        selectedStudent = savedInstanceState?.getString("student")
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { home() }
        })
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draft", resultsField?.text?.toString())
        outState.putString("lab", selectedLab)
        outState.putString("student", selectedStudent)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        super.onResume()
        if (LabSession.username == null) { logout(); return }
        selectedLab?.let { detail(it, selectedStudent) } ?: home()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun base(title: String, subtitle: String) {
        generation++
        resultsField = null
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(getColor(R.color.ju_background)) }
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(24)) }
        scroll.addView(page)
        setContentView(scroll)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        label(page, "JU  /  PHYSICS LABS", 12, true)
        label(page, title, 28, true)
        label(page, subtitle, 15)
    }
    private fun label(parent: LinearLayout, value: String, size: Int = 16, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(getColor(R.color.ju_text))
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(8)); setTextIsSelectable(true)
            parent.addView(this)
        }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(16))
        background = GradientDrawable().apply { setColor(getColor(R.color.white)); cornerRadius = dp(16).toFloat(); setStroke(dp(1), getColor(R.color.ju_disabled)) }
        page.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    }
    private fun button(parent: LinearLayout, title: String, action: () -> Unit): Button = Button(this).apply {
        text = title; isAllCaps = false; minHeight = dp(48)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
        setOnClickListener { action() }
    }
    private fun field(parent: LinearLayout, hintText: String, multiline: Boolean = false): EditText = EditText(this).apply {
        hint = hintText; textSize = 16f
        inputType = android.text.InputType.TYPE_CLASS_TEXT or if (multiline) android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
        minLines = if (multiline) 4 else 1
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    private fun notify(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun launchWork(work: suspend () -> Unit) {
        lifecycleScope.launch {
            try { work() } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { notify("Could not complete the action. Please try again.") }
        }
    }
    private fun logout() {
        LabSession.logout()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    private fun home() {
        if (LabSession.username == null) { logout(); return }
        selectedLab = null; selectedStudent = null; LabSession.scope = null
        base(if (teacher) "Teacher home" else "My labs", "Welcome, $user · ${LabSession.role}")
        val tools = card()
        label(tools, if (teacher) "Your classroom" else "Your workspace", 20, true)
        if (teacher) {
            button(tools, "Create a lab") { createLab() }
            button(tools, "Explore sensors") {
                LabSession.scope = SensorScope(user, null, 15, false)
                startActivity(Intent(this, ActivitySelector::class.java))
            }
        } else label(tools, "Open an assigned lab to use its sensors and submit your results.")
        button(tools, "Log out") { logout() }
        val request = generation
        launchWork {
            val labs = withContext(Dispatchers.IO) { if (teacher) dao.teacherLabs(user) else dao.studentLabs(user) }
            if (request != generation) return@launchWork
            if (labs.isEmpty()) label(card(), if (teacher) "No labs yet. Create your first assignment above." else "No assigned labs yet. Your teacher can assign labs to this account.")
            labs.forEach { lab ->
                val assignments = withContext(Dispatchers.IO) { dao.assignments(lab.id) }
                if (request != generation) return@launchWork
                val box = card()
                label(box, lab.title, 20, true)
                label(box, if (teacher) "${assignments.count { it.submittedAt != null }} / ${assignments.size} submitted" else
                    if (assignments.firstOrNull { it.student == user }?.submittedAt != null) "Submitted" else "To complete", 14, true)
                label(box, lab.description.take(180))
                label(box, sensorNames(lab.sensors), 13)
                button(box, if (teacher) "View lab & results" else "Open lab") { detail(lab.id, if (teacher) null else user) }
            }
        }
    }
    private fun sensorNames(mask: Int) = LabSensors.names.filterKeys { mask and it != 0 }.values.joinToString(" · ")
    private fun createLab() {
        if (!teacher) return
        base("Create a lab", "Choose the task, tools, and students.")
        val info = card()
        val title = field(info, "Lab title")
        val description = field(info, "Instructions and expected results", true)
        val sensorBox = card(); label(sensorBox, "Allowed sensors", 20, true)
        val sensorChecks = LabSensors.names.map { (id, name) ->
            id to CheckBox(this).apply { text = name; sensorBox.addView(this) }
        }
        val studentsBox = card(); label(studentsBox, "Assign students", 20, true)
        val students = mutableListOf<Pair<String, CheckBox>>()
        val save = button(page, "Create & assign") { }
        save.isEnabled = false
        val request = generation
        launchWork {
            val names = withContext(Dispatchers.IO) { UserDatabase.getInstance(this@HomeActivity).users().students() }
            if (request != generation) return@launchWork
            if (names.isEmpty()) label(studentsBox, "No student accounts yet. Log out and create student accounts first.")
            names.forEach { name -> students.add(name to CheckBox(this@HomeActivity).apply { text = name; studentsBox.addView(this) }) }
            save.isEnabled = names.isNotEmpty()
        }
        save.setOnClickListener {
            val selected = students.filter { it.second.isChecked }.map { it.first }
            val mask = sensorChecks.filter { it.second.isChecked }.sumOf { it.first }
            val name = title.text.toString().trim()
            val instructions = description.text.toString().trim()
            if (name.isEmpty() || instructions.isEmpty() || selected.isEmpty() || mask == 0) {
                notify("Add a title, description, at least one sensor, and at least one student."); return@setOnClickListener
            }
            save.isEnabled = false
            launchWork {
                try {
                    withContext(Dispatchers.IO) { dao.create(Lab(UUID.randomUUID().toString(), user, name, instructions, mask, System.currentTimeMillis()), selected) }
                    home()
                } finally { save.isEnabled = true }
            }
        }
        button(page, "Back to home") { home() }
    }
    private fun detail(id: String, student: String?) {
        selectedLab = id; selectedStudent = student
        base("Lab workspace", "Loading lab…")
        val request = generation
        launchWork {
            val lab = withContext(Dispatchers.IO) { dao.lab(id) } ?: run { home(); return@launchWork }
            val rows = withContext(Dispatchers.IO) { dao.assignments(id) }
            if (request != generation) return@launchWork
            if ((teacher && lab.teacher != user) || (!teacher && rows.none { it.student == user })) { home(); return@launchWork }
            base(lab.title, if (teacher) "Your lab · ${rows.size} students" else "Assigned by ${lab.teacher}")
            button(page, "Back to home") { home() }
            val instructions = card(); label(instructions, "Instructions", 20, true); label(instructions, lab.description)
            label(instructions, "Tools: ${sensorNames(lab.sensors)}", 13)
            if (teacher && student == null) {
                rows.forEach { assignment ->
                    val box = card(); label(box, assignment.student, 20, true)
                    label(box, if (assignment.submittedAt == null) "Awaiting submission" else "Submitted ${DateFormat.getDateTimeInstance().format(Date(assignment.submittedAt))}")
                    if (assignment.submittedAt != null) button(box, "Review results") { detail(id, assignment.student) }
                }
                return@launchWork
            }
            val assignment = rows.firstOrNull { it.student == (if (teacher) student else user) } ?: return@launchWork
            if (teacher && assignment.submittedAt == null) return@launchWork
            val readOnly = teacher || assignment.submittedAt != null
            val resultBox = card(); label(resultBox, if (teacher) "${assignment.student} · Results" else "My results", 20, true)
            if (readOnly) label(resultBox, assignment.results) else {
                resultsField = field(resultBox, "Describe your observations and conclusions", true).apply { setText(restoredDraft ?: assignment.results); restoredDraft = null }
                button(resultBox, "Save draft") { saveResults(assignment, false) {} }
            }
            val scope = SensorScope(assignment.student, lab.id, lab.sensors, readOnly)
            val tools = card(); label(tools, if (readOnly) "Submitted recordings" else "Record evidence", 20, true)
            label(tools, "Recordings made here belong to this lab and student.", 14)
            LabSensors.names.filterKeys { lab.sensors and it != 0 }.forEach { (sensor, name) ->
                button(tools, if (readOnly) "View $name" else name) {
                    val open = {
                        LabSession.scope = scope
                        startActivity(Intent(this@HomeActivity, LabSensors.screen(sensor)))
                    }
                    if (readOnly) open() else saveResults(assignment, false, open)
                }
            }
            if (!readOnly) {
                val submitBox = card(); label(submitBox, "Ready to submit?", 20, true)
                label(submitBox, "Your findings and this lab’s saved recordings will become read-only and visible to your teacher.", 14)
                val submit = button(submitBox, "Submit results") { }
                submit.setOnClickListener {
                    if (resultsField?.text.isNullOrBlank()) { notify("Write your findings before submitting."); return@setOnClickListener }
                    saveResults(assignment, true) { detail(id, user) }
                }
            } else label(card(), "Submitted · results and recordings are read-only", 14, true)
        }
    }
    private fun saveResults(assignment: LabAssignment, submit: Boolean, after: () -> Unit) {
        val value = resultsField?.text?.toString()?.trim() ?: assignment.results
        launchWork {
            val updated = withContext(Dispatchers.IO) {
                check(!teacher && assignment.student == user)
                if (submit) dao.submit(assignment.labId, user, value, System.currentTimeMillis())
                else dao.saveDraft(assignment.labId, user, value)
            }
            if (updated == 1) { if (submit) notify("Results submitted") else notify("Draft saved"); after() }
            else { notify("This lab is already submitted."); detail(assignment.labId, user) }
        }
    }
}

