package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

class Login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // CRASH FIX: Route edge-to-edge window processing through the base view content frame
        // to bypass custom or missing XML layout element identifiers.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun loginUser(view: View) {
        val email: TextInputEditText    = findViewById(R.id.etEmail)
        val password: TextInputEditText = findViewById(R.id.etPassword)

        if (email.text.isNullOrEmpty() || password.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            val user = AppDatabase.getDatabase(this)
                .userDao()
                .login(email.text.toString(), password.text.toString())

            runOnUiThread {
                if (user != null) {
                    // Record login for streak
                    val streak = GamificationManager.recordLogin(this)

                    // Award streak badges
                    if (streak >= 7) GamificationManager.unlockBadge(this, Badge.STREAK_7)
                    else if (streak >= 3) GamificationManager.unlockBadge(this, Badge.STREAK_3)

                    val msg = when {
                        streak >= 7  -> "🗓️ 7-day streak! You're a Finance Master!"
                        streak >= 3  -> "📅 ${streak}-day streak! Keep it up!"
                        streak == 1  -> "Welcome back, ${user.name}!"
                        else         -> "Welcome back! ${streak}-day streak 🔥"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                    // Store logged-in user name & email in SharedPreferences for Profile
                    getSharedPreferences("user_session", MODE_PRIVATE).edit()
                        .putString("user_name", user.name)
                        .putString("user_email", user.email)
                        .apply()

                    startActivity(Intent(this@Login, Home::class.java))
                    finish() // Close login screen context from activity stack
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun navigateToRegister(view: View) {
        startActivity(Intent(this@Login, Register::class.java))
    }
}