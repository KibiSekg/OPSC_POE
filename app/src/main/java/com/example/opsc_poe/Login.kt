package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.util.Log // Imported Android Log utility
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.google.android.material.textfield.TextInputEditText
import kotlin.concurrent.thread

/**
 * Activity responsible for authenticating existing app accounts against local data schemas.
 * Triggers session state initializations, Gamification daily streak updates, and routes
 * users to the core Dashboard layout.
 */
class Login : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Launching Login screen container.")
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

    /**
     * Extracts view inputs, runs form validations, and initiates an asynchronous query
     * thread to verify user credential matches in the local DB.
     */
    fun loginUser(view: View) {
        val email: TextInputEditText    = findViewById(R.id.etEmail)
        val password: TextInputEditText = findViewById(R.id.etPassword)

        Log.d(TAG, "loginUser: Authentication click triggered. Validating text fields.")

        if (email.text.isNullOrEmpty() || password.text.isNullOrEmpty()) {
            Log.w(TAG, "loginUser validation aborted: Email or password input fields left empty.")
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            val enteredEmail = email.text.toString().trim()
            Log.d(TAG, "Querying database validation records for user identity: $enteredEmail")

            val user = AppDatabase.getDatabase(this)
                .userDao()
                .login(enteredEmail, password.text.toString())

            // Switch runtime context back to the UI thread to update notification prompts and navigate
            runOnUiThread {
                if (user != null) {
                    Log.d(TAG, "Authentication SUCCESS. Initiating session setup for: ${user.email}")

                    // Record login to calculate continuity streaks
                    val streak = GamificationManager.recordLogin(this)
                    Log.d(TAG, "Current calculated user attendance streak metric: $streak")

                    // Award streak badges based on current continuity milestones
                    if (streak >= 7) {
                        Log.d(TAG, "7-Day streak threshold achieved. Triggering badge unlock mapping.")
                        GamificationManager.unlockBadge(this, Badge.STREAK_7)
                    } else if (streak >= 3) {
                        Log.d(TAG, "3-Day streak threshold achieved. Triggering badge unlock mapping.")
                        GamificationManager.unlockBadge(this, Badge.STREAK_3)
                    }

                    val msg = when {
                        streak >= 7  -> "🗓️ 7-day streak! You're a Finance Master!"
                        streak >= 3  -> "📅 ${streak}-day streak! Keep it up!"
                        streak == 1  -> "Welcome back, ${user.name}!"
                        else         -> "Welcome back! ${streak}-day streak 🔥"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                    // Store logged-in user name & email in SharedPreferences for Profile access
                    Log.d(TAG, "Caching active username and email descriptors into local SharedPreferences session state.")
                    getSharedPreferences("user_session", MODE_PRIVATE).edit()
                        .putString("user_name", user.name)
                        .putString("user_email", user.email)
                        .apply()

                    // Transition to the Home activity context and clear login out of the backstack
                    Log.d(TAG, "Navigating away -> Route profile target to Home dashboard view.")
                    startActivity(Intent(this@Login, Home::class.java))
                    finish()
                } else {
                    Log.w(TAG, "Authentication FAILURE: No database record matched the provided email and password pair.")
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Redirects the user interface stack to the alternative registration screen.
     */
    fun navigateToRegister(view: View) {
        Log.d(TAG, "navigateToRegister: Routing out to user account registration window layout.")
        startActivity(Intent(this@Login, Register::class.java))
    }
}