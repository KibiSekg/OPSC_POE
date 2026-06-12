package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.User
import kotlin.concurrent.thread

class Register : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun registerUser(view: View) {
        val name: EditText = findViewById(R.id.etName)
        val email: EditText = findViewById(R.id.etEmail)
        val password: EditText = findViewById(R.id.etPassword)

        val nameText = name.text.toString().trim()
        val emailText = email.text.toString().trim()
        val passwordText = password.text.toString().trim()

        if (nameText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            val db = AppDatabase.getDatabase(this)

            // 1. Check if the user already exists in your table structure
            val existingUser = db.userDao().getUserByEmail(emailText)

            if (existingUser != null) {
                // If user is found, update UI on main thread to warn them
                runOnUiThread {
                    Toast.makeText(this, "Email already exists! Please log in.", Toast.LENGTH_LONG).show()
                }
            } else {
                // 2. If no record exists, insert the new user profile safely
                val newUser = User(
                    name = nameText,
                    email = emailText,
                    password = passwordText
                )
                db.userDao().insertUser(newUser)

                runOnUiThread {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()

                    // Route user straight back to Login page cleanly
                    val intent = Intent(this@Register, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    fun navigateToLogin(view: View) {
        startActivity(Intent(this@Register, Login::class.java))
    }
}