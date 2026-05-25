package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Transaction
import kotlin.concurrent.thread

class Expense : AppCompatActivity() {

    lateinit var btnHome: Button
    lateinit var btnExpInc: Button
    lateinit var btnProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense)

        setupNavigation(this, R.id.btnExpInc)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




    }

    fun saveTransaction(view: View) {
        val amount: EditText = findViewById(R.id.etAmnt)
        val transType: EditText = findViewById(R.id.etTransactionType)
        val category: EditText = findViewById(R.id.etCategory)
        val date: EditText = findViewById(R.id.etTransactionDate)

        if (amount.text.isEmpty() || transType.text.isEmpty() || category.text.isEmpty() || date.text.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            amount = amount.text.toString().toDoubleOrNull() ?: 0.0,
            transactionType = transType.text.toString().lowercase(),
            category = category.text.toString().lowercase(),
            date = date.text.toString()
        )

        thread {
            AppDatabase.getDatabase(this).transactionDao().insertTransaction(transaction)
            runOnUiThread {
                Toast.makeText(this, "Transaction Saved!", Toast.LENGTH_LONG).show()
            }
        }
    }


}