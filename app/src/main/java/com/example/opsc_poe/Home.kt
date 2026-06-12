package com.example.opsc_poe

import android.graphics.Color
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Budget // Ensure you import your Budget entity class
import kotlin.concurrent.thread

class Home : AppCompatActivity() {

    lateinit var etMonthlyGoal: TextView
    lateinit var etTotalBalance: TextView
    lateinit var etTotalIncome: TextView
    lateinit var etTotalExp: TextView
    lateinit var tvTopCategory: TextView
    lateinit var tvDailyAvg: TextView
    lateinit var progressBudget: ProgressBar
    lateinit var tvBudgetStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        setupNavigation(this, R.id.btnHome)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etMonthlyGoal  = findViewById(R.id.etMonthlyGoal)
        etTotalBalance = findViewById(R.id.etTotalBalance)
        etTotalIncome  = findViewById(R.id.etTotalIncome)
        etTotalExp     = findViewById(R.id.etTotalExp)
        tvTopCategory  = findViewById(R.id.tvTopCategory)
        tvDailyAvg     = findViewById(R.id.tvDailyAvg)
        progressBudget = findViewById(R.id.progressBudget)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)

        val btnSetBudget: com.google.android.material.button.MaterialButton = findViewById(R.id.btnSetBudget)

        btnSetBudget.setOnClickListener {
            showSetBudgetBottomSheet()
        }
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun showSetBudgetBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_set_budget, null)

        val etBudgetAmount = sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBudgetAmount)
        val btnSaveBudget = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveBudget)

        // Read the existing budget value from Room directly to pre-fill the field
        thread {
            val db = AppDatabase.getDatabase(this)
            val currentBudget = db.budgetDao().getLatestBudget()

            runOnUiThread {
                if (currentBudget != null && currentBudget.monthlyBudget > 0) {
                    etBudgetAmount.setText(currentBudget.monthlyBudget.toString())
                }
            }
        }

        btnSaveBudget.setOnClickListener {
            val budgetString = etBudgetAmount.text.toString()

            if (budgetString.isEmpty()) {
                etBudgetAmount.error = "Please enter an amount"
                return@setOnClickListener
            }

            val budgetAmount = budgetString.toDoubleOrNull()
            if (budgetAmount != null && budgetAmount > 0) {

                // Write directly into your Room DB backend database tables
                thread {
                    val db = AppDatabase.getDatabase(this)

                    // Create a budget object instance matching your project schema setup
                    val newBudget = Budget(monthlyBudget = budgetAmount)
                    db.budgetDao().insertBudget(newBudget)

                    runOnUiThread {
                        android.widget.Toast.makeText(this, "Monthly budget updated!", android.widget.Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Instantly redraw UI to display updated calculation formulas
                        loadData()
                    }
                }
            } else {
                etBudgetAmount.error = "Please enter a valid positive number"
            }
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun loadData() {
        thread {
            val db           = AppDatabase.getDatabase(this)
            val transactions = db.transactionDao().getAllTransactions()
            val budget       = db.budgetDao().getLatestBudget()

            var totalIncome  = 0.0
            var totalExpense = 0.0
            val categoryCount = mutableMapOf<String, Int>()

            for (tx in transactions) {
                when (tx.transactionType.lowercase()) {
                    "income"  -> totalIncome  += tx.amount
                    "expense" -> totalExpense += tx.amount
                }
                if (tx.category.isNotEmpty()) {
                    categoryCount[tx.category] = (categoryCount[tx.category] ?: 0) + 1
                }
            }

            val totalBalance = totalIncome - totalExpense
            val topCategory  = categoryCount.maxByOrNull { it.value }?.key ?: "None"
            val expenseCount = transactions.count { it.transactionType.lowercase() == "expense" }
            val dailyAvg     = if (expenseCount > 0) totalExpense / expenseCount else 0.0

            val budgetAmount   = budget?.monthlyBudget ?: 0.0
            val spentPercent   = if (budgetAmount > 0) ((totalExpense / budgetAmount) * 100).toInt().coerceIn(0, 100) else 0
            val remaining      = budgetAmount - totalExpense

            runOnUiThread {
                etMonthlyGoal.text = if (budgetAmount > 0) "R %.2f".format(budgetAmount) else "R 0.00 — No Budget Set"
                etTotalBalance.text = "R %.2f".format(totalBalance)
                etTotalIncome.text  = "R %.2f".format(totalIncome)
                etTotalExp.text     = "R %.2f".format(totalExpense)
                tvTopCategory.text  = topCategory
                tvDailyAvg.text     = if (dailyAvg > 0) "R %.2f".format(dailyAvg) else "R 0.00"

                // Progress bar updating calculations cleanly
                progressBudget.progress = spentPercent
                tvBudgetStatus.text = when {
                    budgetAmount <= 0    -> "Tap 'Set' to create your monthly budget goal"
                    remaining < 0        -> "Over budget by R %.2f  ($spentPercent%%)".format(-remaining)
                    spentPercent >= 90   -> "Almost at limit — R %.2f remaining ($spentPercent%%)".format(remaining)
                    else                 -> "R %.2f remaining of budget ($spentPercent%% used)".format(remaining)
                }
            }
        }
    }
}