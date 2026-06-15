package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log // Imported Android Log utility
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.concurrent.thread

/**
 * Activity managing the user registration wizard profile pipeline.
 * Provides real-time field evaluation feedback, dynamic password entropy analytics calculation wrappers,
 * and duplicate account checks prior to persisting structured SQLite records.
 */
class Register : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private lateinit var etName:             TextInputEditText
    private lateinit var etEmail:            TextInputEditText
    private lateinit var etPassword:         TextInputEditText
    private lateinit var etConfirmPassword:  TextInputEditText
    private lateinit var tvPasswordStrength: TextView
    private lateinit var passwordStrengthBar: ProgressBar

    // TextInputLayout wrappers — used to show inline errors under each field
    private lateinit var tilName:    TextInputLayout
    private lateinit var tilEmail:   TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Launching User Registration view layout components.")
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // activity_register.xml uses android.R.id.content as the root inset target
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        etName             = findViewById(R.id.etName)
        etEmail            = findViewById(R.id.etEmail)
        etPassword         = findViewById(R.id.etPassword)
        etConfirmPassword  = findViewById(R.id.etConfirmPassword)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar)

        // Walk up the view hierarchy: EditText → TextInputLayout (two levels up)
        tilName     = etName.parent.parent            as TextInputLayout
        tilEmail    = etEmail.parent.parent           as TextInputLayout
        tilPassword = etPassword.parent.parent        as TextInputLayout
        tilConfirm  = etConfirmPassword.parent.parent as TextInputLayout

        // Live password strength feedback as user types
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val inputStr = s.toString()
                Log.v(TAG, "etPassword structural update callback caught. Processing length: ${inputStr.length}")
                updatePasswordStrength(inputStr)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ── Password strength ─────────────────────────────────────────

    /**
     * Determines the visual layout properties representing password security characteristics.
     * Maps entropy values to human-readable strings and structural progress bar tint changes.
     */
    private fun updatePasswordStrength(pw: String) {
        val score = calcPasswordScore(pw)
        passwordStrengthBar.progress = score

        val (label, color) = when {
            pw.isEmpty() -> "—"     to 0xFFBCC8CC.toInt()
            score < 30   -> "Weak"  to 0xFFF44336.toInt()
            score < 60   -> "Fair"  to 0xFFFF9800.toInt()
            score < 85   -> "Good"  to 0xFF8BC34A.toInt()
            else         -> "Strong" to 0xFF4CAF50.toInt()
        }

        tvPasswordStrength.text = "Password strength: $label"
        tvPasswordStrength.setTextColor(color)
        passwordStrengthBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }

    /**
     * Computes a linear integer metric score grading alphanumeric password complexity parameters.
     */
    private fun calcPasswordScore(pw: String): Int {
        var score = 0
        if (pw.length >= 8)               score += 25
        if (pw.length >= 12)              score += 10
        if (pw.any { it.isUpperCase() })  score += 25
        if (pw.any { it.isDigit() })      score += 20
        if (pw.any { !it.isLetterOrDigit() }) score += 20

        val finalizedValue = score.coerceIn(0, 100)
        Log.v(TAG, "calcPasswordScore: Evaluated entropy score payload mapping: $finalizedValue/100")
        return finalizedValue
    }

    // ── Validation ────────────────────────────────────────────────

    /** Returns true if the email matches user@domain.tld */
    private fun isValidEmail(email: String): Boolean =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)

    /**
     * Returns null if the password is valid, or an error string explaining what's missing.
     * Rules: min 8 chars, at least 1 uppercase, at least 1 special character.
     */
    private fun passwordError(pw: String): String? = when {
        pw.length < 8                         -> "Must be at least 8 characters"
        !pw.any { it.isUpperCase() }          -> "Must contain at least 1 uppercase letter"
        !pw.any { !it.isLetterOrDigit() }     -> "Must contain at least 1 special character"
        else                                  -> null
    }

    // ── Register button ───────────────────────────────────────────

    /**
     * Parses account components, validates strings against policy criteria,
     * launches an asynchronous background thread to inspect collision sets, and logs errors.
     */
    fun registerUser(view: View) {
        val name     = etName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm  = etConfirmPassword.text.toString()

        Log.d(TAG, "registerUser: Evaluating client side inputs for registration processing loop.")

        // Clear any previous inline errors
        tilName.error     = null
        tilEmail.error    = null
        tilPassword.error = null
        tilConfirm.error  = null

        var hasError = false

        if (name.isEmpty()) {
            tilName.error = "Full name is required"
            hasError = true
        }

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            hasError = true
        } else if (!isValidEmail(email)) {
            tilEmail.error = "Enter a valid email (e.g. user@example.com)"
            hasError = true
        }

        val pwErr = passwordError(password)
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            hasError = true
        } else if (pwErr != null) {
            tilPassword.error = pwErr
            hasError = true
        }

        if (confirm.isEmpty()) {
            tilConfirm.error = "Please confirm your password"
            hasError = true
        } else if (password != confirm) {
            tilConfirm.error = "Passwords do not match"
            hasError = true
        }

        if (hasError) {
            Log.w(TAG, "registerUser: Registration aborted due to client-side input constraint validation failures.")
            return
        }

        // All client-side checks passed — check DB for duplicate email records asynchronously
        thread {
            Log.d(TAG, "Checking Room local database records for pre-existing account matches tied to: $email")
            val db       = AppDatabase.getDatabase(this)
            val existing = db.userDao().getUserByEmail(email)

            runOnUiThread {
                if (existing != null) {
                    Log.w(TAG, "Collision found: A transaction model profile tracking object already maps to email: $email")
                    tilEmail.error = "An account with this email already exists"
                } else {
                    // Unique email verified. Proceeding with record initialization on background thread
                    thread {
                        Log.i(TAG, "Unique account verified. Inserting new User record into SQLite schema layout logs.")
                        db.userDao().insertUser(
                            User(name = name, email = email, passwordHash = password)
                        )
                        runOnUiThread {
                            Log.d(TAG, "User registration process complete. Redirecting context window over to Login view activity.")
                            Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@Register, Login::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    /**
     * Redirects the runtime user display view back over to the Login activity container.
     */
    fun navigateToLogin(view: View) {
        Log.d(TAG, "navigateToLogin: Redirecting navigation pipeline target frame back to Login form.")
        startActivity(Intent(this@Register, Login::class.java))
    }
}