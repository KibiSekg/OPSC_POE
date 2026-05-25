package com.example.opsc_poe

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Transaction
import com.google.android.material.button.MaterialButton
import kotlin.concurrent.thread

class Expense : AppCompatActivity() {

    // Dropdown for Income / Expense
    private lateinit var spinnerTransType: AutoCompleteTextView

    // Dropdown for category
    private lateinit var spinnerCategory: AutoCompleteTextView

    // Button to add a custom category
    private lateinit var btnAddCategory: MaterialButton

    // The live list of categories shown in the dropdown
    private val categories = mutableListOf(
        "Food", "Transport", "Housing", "Utilities",
        "Entertainment", "Health", "Education", "Clothing", "Savings", "Other"
    )

    private lateinit var categoryAdapter: ArrayAdapter<String>

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

        spinnerTransType = findViewById(R.id.spinnerTransType)
        spinnerCategory  = findViewById(R.id.spinnerCategory)
        btnAddCategory   = findViewById(R.id.btnAddCategory)

        // ── Transaction type dropdown ──────────────────────────────
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            listOf("Income", "Expense")
        )
        spinnerTransType.setAdapter(typeAdapter)
        spinnerTransType.inputType = 0  // disable keyboard — dropdown only

        // ── Category dropdown ──────────────────────────────────────
        categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.inputType = 0  // disable keyboard — dropdown only

        // ── Add custom category ────────────────────────────────────
        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "e.g. Gym, Petrol, Insurance"

        AlertDialog.Builder(this)
            .setTitle("Add Custom Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val newCat = input.text.toString().trim()
                if (newCat.isNotEmpty()) {
                    if (!categories.contains(newCat)) {
                        categories.add(newCat)
                        categoryAdapter.notifyDataSetChanged()
                        spinnerCategory.setText(newCat, false)
                        Toast.makeText(this, "\"$newCat\" added!", Toast.LENGTH_SHORT).show()
                    } else {
                        spinnerCategory.setText(newCat, false)
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun saveTransaction(view: View) {
        val amount   = findViewById<EditText>(R.id.etAmnt)
        val date     = findViewById<EditText>(R.id.etTransactionDate)
        val transType = spinnerTransType.text.toString().trim()
        val category  = spinnerCategory.text.toString().trim()

        if (amount.text.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (transType.isEmpty()) {
            Toast.makeText(this, "Please select Income or Expense", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        if (date.text.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            amount          = amount.text.toString().toDoubleOrNull() ?: 0.0,
            transactionType = transType.lowercase(),
            category        = category.lowercase(),
            date            = date.text.toString()
        )

        thread {
            AppDatabase.getDatabase(this).transactionDao().insertTransaction(transaction)
            runOnUiThread {
                Toast.makeText(this, "Transaction saved!", Toast.LENGTH_LONG).show()
                // Clear fields after save
                amount.text.clear()
                date.text.clear()
                spinnerTransType.text.clear()
                spinnerCategory.text.clear()
            }
        }
    }
}