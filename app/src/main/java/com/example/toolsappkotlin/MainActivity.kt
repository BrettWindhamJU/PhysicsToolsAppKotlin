package com.example.toolsappkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.toolsappkotlin.data.UserDatabase
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val username = findViewById<EditText>(R.id.editUsername)
        val password = findViewById<EditText>(R.id.editPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val register = findViewById<Button>(R.id.btnCreateAccount)
        val error = findViewById<TextView>(R.id.txtError)
        val teacher = findViewById<RadioButton>(R.id.radioTeacher)
        val student = findViewById<RadioButton>(R.id.radioStudent)

        fun submit(create: Boolean) {
            val name = username.text.toString().trim()
            val secret = password.text.toString().toCharArray()
            val role = if (teacher.isChecked) "Teacher" else if (student.isChecked) "Student" else null
            if (name.isBlank() || name.length > 64 || secret.isEmpty() || secret.size > 128 ||
                (create && (secret.size < 8 ||
                    secret.none { !it.isLetterOrDigit() && !it.isWhitespace() && !it.isISOControl() } ||
                    role == null))) {
                secret.fill('\u0000')
                error.setText(R.string.account_validation)
                return
            }
            login.isEnabled = false
            register.isEnabled = false
            error.setText(R.string.account_working)
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val dao = UserDatabase.getInstance(applicationContext).users()
                        if (create) {
                            // Reject existing usernames before doing expensive password hashing.
                            if (dao.find(name) != null) return@withContext null
                            val user = PasswordHasher.create(name, secret, role!!)
                            if (dao.insert(user) == -1L) null else user
                        } else {
                            dao.find(name)?.takeIf { PasswordHasher.verify(secret, it) }
                        }
                    }
                    if (result == null) {
                        error.setText(if (create) R.string.account_exists else R.string.account_invalid)
                    } else {
                        password.text.clear()
                        error.text = ""
                        startActivity(Intent(this@MainActivity, ActivitySelector::class.java)
                            .putExtra("USER_ROLE", result.role)
                            .putExtra("USERNAME", result.username))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error.setText(R.string.account_failed)
                } finally {
                    secret.fill('\u0000')
                    login.isEnabled = true
                    register.isEnabled = true
                }
            }
        }
        login.setOnClickListener { submit(false) }
        register.setOnClickListener { submit(true) }
    }
}
