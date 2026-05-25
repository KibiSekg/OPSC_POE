package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import kotlin.concurrent.thread

class Home : AppCompatActivity() {

    lateinit var btnHome: Button
    lateinit var btnExpInc: Button
    lateinit var btnProfile: Button
    lateinit var etMonthlyGoal: TextView
    lateinit var etTotalBalance: TextView
    lateinit var etTotalIncome: TextView
    lateinit var etTotalExp: TextView
    lateinit var tvTopCategory: TextView
    lateinit var tvDailyAvg: TextView

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





        etMonthlyGoal   = findViewById(R.id.etMonthlyGoal)
        etTotalBalance  = findViewById(R.id.etTotalBalance)
        etTotalIncome   = findViewById(R.id.etTotalIncome)
        etTotalExp      = findViewById(R.id.etTotalExp)
        tvTopCategory   = findViewById(R.id.tvTopCategory)
        tvDailyAvg      = findViewById(R.id.tvDailyAvg)

        loadTransactions()
        loadMonthlyBudget()


    }

    fun loadMonthlyBudget() {
        thread {
            val budget = AppDatabase.getDatabase(this).budgetDao().getLatestBudget()
            runOnUiThread {
                if (budget == null) {
                    etMonthlyGoal.text = "R 0.00 - No Budget Set"
                } else {
                    etMonthlyGoal.text = "R %.2f".format(budget.monthlyBudget)
                }
            }
        }
    }

    fun loadTransactions() {
        thread {
            val data = AppDatabase.getDatabase(this).transactionDao().getAllTransactions()

            runOnUiThread {
                if (data.isEmpty()) return@runOnUiThread

                var totalIncome = 0.0
                var totalExpense = 0.0
                val categoryCount = mutableMapOf<String, Int>()

                for (tx in data) {
                    when (tx.transactionType.lowercase()) {
                        "income"  -> totalIncome += tx.amount
                        "expense" -> totalExpense += tx.amount
                    }
                    if (tx.category.isNotEmpty()) {
                        categoryCount[tx.category] = (categoryCount[tx.category] ?: 0) + 1
                    }
                }

                val totalBalance = totalIncome - totalExpense
                val topCategory  = categoryCount.maxByOrNull { it.value }?.key ?: "No Category"
                val expenseCount = data.count { it.transactionType.lowercase() == "expense" }
                val dailyAvg     = if (expenseCount > 0) totalExpense / expenseCount else 0.0

                etTotalIncome.text  = "R %.2f".format(totalIncome)
                etTotalExp.text     = "R %.2f".format(totalExpense)
                etTotalBalance.text = "R %.2f".format(totalBalance)
                tvTopCategory.text  = topCategory
                tvDailyAvg.text     = if (dailyAvg > 0) "R %.2f".format(dailyAvg) else "R 0.00 - No Expenses"
            }
        }
    }


}