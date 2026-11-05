package com.example.toolsappkotlin

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val radioTeacher = findViewById<RadioButton>(R.id.radioTeacher)
        val radioStudent = findViewById<RadioButton>(R.id.radioStudent)

        val username = findViewById<EditText>(R.id.editUsername)
        val password = findViewById<EditText>(R.id.editPassword)

        val loginButton = findViewById<Button>(R.id.btnLogin)
        val errorText = findViewById<TextView>(R.id.txtError)

        loginButton.setOnClickListener {

            val enteredUsername = username.text.toString().trim()
            val enteredPassword = password.text.toString()

            val isTeacher = radioTeacher.isChecked
            val isStudent = radioStudent.isChecked

            val validLogin = when {
                isTeacher ->
                    enteredUsername == "teacher" &&
                            enteredPassword == "teacher123"

                isStudent ->
                    enteredUsername == "student" &&
                            enteredPassword == "student123"

                else -> false
            }

            if (validLogin) {

                val role = if (isTeacher) "Teacher" else "Student"

                // Open ActivitySelector
                val intent = Intent(
                    this,
                    ActivitySelector::class.java
                )

                // Pass the account type to ActivitySelector
                intent.putExtra("USER_ROLE", role)

                startActivity(intent)

            } else {

                errorText.text =
                    "Incorrect username or password."
            }
        }
    }
}
