package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class Register : AppCompatActivity() {

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
            override fun afterTextChanged(s: Editable?) = updatePasswordStrength(s.toString())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ── Password strength ─────────────────────────────────────────

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
        passwordStrengthBar.progressTintList =
            android.content.res.ColorStateList.valueOf(color)
    }

    private fun calcPasswordScore(pw: String): Int {
        var score = 0
        if (pw.length >= 8)               score += 25
        if (pw.length >= 12)              score += 10
        if (pw.any { it.isUpperCase() })  score += 25
        if (pw.any { it.isDigit() })      score += 20
        if (pw.any { !it.isLetterOrDigit() }) score += 20
        return score.coerceIn(0, 100)
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

    fun registerUser(view: View) {
        val name     = etName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm  = etConfirmPassword.text.toString()

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

        if (hasError) return

        // All client-side checks passed — check DB for duplicate email
        thread {
            val db       = AppDatabase.getDatabase(this)
            val existing = db.userDao().getUserByEmail(email)

            runOnUiThread {
                if (existing != null) {
                    tilEmail.error = "An account with this email already exists"
                } else {
                    thread {
                        db.userDao().insertUser(
                            User(name = name, email = email, passwordHash = password)
                        )
                        runOnUiThread {
                            Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@Register, Login::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    fun navigateToLogin(view: View) {
        startActivity(Intent(this@Register, Login::class.java))
    }
}