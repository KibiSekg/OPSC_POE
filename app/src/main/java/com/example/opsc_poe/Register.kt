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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun registerUser(view: View) {
        val name: EditText = findViewById(R.id.etName)
        val email: EditText = findViewById(R.id.etEmail)
        val password: EditText = findViewById(R.id.etPassword)

        if (name.text.isEmpty() || email.text.isEmpty() || password.text.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val user = User(
            name = name.text.toString(),
            email = email.text.toString(),
            password = password.text.toString()
        )

        thread {
            AppDatabase.getDatabase(this).userDao().insertUser(user)
            runOnUiThread {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun navigateToLogin(view: View) {
        startActivity(Intent(this@Register, Login::class.java))
    }
}