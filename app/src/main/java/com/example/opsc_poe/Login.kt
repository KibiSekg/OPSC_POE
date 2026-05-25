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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun loginUser(view: View) {
        val email: TextInputEditText = findViewById(R.id.etEmail)
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
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@Login, Home::class.java))
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