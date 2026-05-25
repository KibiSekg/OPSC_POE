package com.example.opsc_poe

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Budget
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class Profile : AppCompatActivity() {

    lateinit var btnSetMonthlyBudg: MaterialButton
    lateinit var btnThisWeek: MaterialButton
    lateinit var btnThisMonth: MaterialButton
    lateinit var btnLastMonth: MaterialButton

    lateinit var tvMonthlyBalance: TextView
    lateinit var etProfLossStatus: TextView
    lateinit var tvNumOfInc: TextView
    lateinit var tvNumOfExp: TextView
    lateinit var etMonthlyBudget: EditText
    lateinit var llResultsContainer: LinearLayout

    var monthlyBudget = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setupNavigation(this, R.id.btnProfile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnSetMonthlyBudg  = findViewById(R.id.btnSetMonthlyBudg)
        btnThisWeek        = findViewById(R.id.btnThisWeek)
        btnThisMonth       = findViewById(R.id.btnThisMonth)
        btnLastMonth       = findViewById(R.id.btnLastMonth)
        tvMonthlyBalance   = findViewById(R.id.tvMonthlyBalance)
        etProfLossStatus   = findViewById(R.id.etProfLossStatus)
        tvNumOfInc         = findViewById(R.id.tvNumOfInc)
        tvNumOfExp         = findViewById(R.id.tvNumOfExp)
        etMonthlyBudget    = findViewById(R.id.etMonthlyBudget)
        llResultsContainer = findViewById(R.id.llResultsContainer)

        // Load saved budget first, then calculate
        thread {
            val saved = AppDatabase.getDatabase(this).budgetDao().getLatestBudget()
            runOnUiThread {
                if (saved != null) {
                    monthlyBudget = saved.monthlyBudget
                    etMonthlyBudget.setText("%.2f".format(monthlyBudget))
                }
                loadTransactions()
            }
        }

        btnSetMonthlyBudg.setOnClickListener {
            val input = etMonthlyBudget.text.toString()
            if (input.isNotEmpty()) {
                monthlyBudget = input.toDoubleOrNull() ?: 0.0
                saveMonthlyBudget(monthlyBudget)
                loadTransactions()
                Toast.makeText(this, "Budget set to R %.2f".format(monthlyBudget), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            }
        }

        btnThisWeek.setOnClickListener  { filterByPeriod("thisWeek") }
        btnThisMonth.setOnClickListener { filterByPeriod("thisMonth") }
        btnLastMonth.setOnClickListener { filterByPeriod("lastMonth") }
    }

    fun loadTransactions() {
        thread {
            val data = AppDatabase.getDatabase(this).transactionDao().getAllTransactions()

            runOnUiThread {
                var totalIncome  = 0.0
                var totalExpense = 0.0
                var incomeCount  = 0
                var expenseCount = 0

                for (tx in data) {
                    when (tx.transactionType.lowercase()) {
                        "income"  -> { totalIncome  += tx.amount; incomeCount++ }
                        "expense" -> { totalExpense += tx.amount; expenseCount++ }
                    }
                }

                tvNumOfInc.text = incomeCount.toString()
                tvNumOfExp.text = expenseCount.toString()

                // Only show balance/status if a budget has been set
                if (monthlyBudget <= 0.0) {
                    tvMonthlyBalance.text = "R 0.00"
                    etProfLossStatus.text = "Set a budget above to see your status"
                    return@runOnUiThread
                }

                // Remaining = budget - expenses
                val remaining = monthlyBudget - totalExpense
                tvMonthlyBalance.text = "R %.2f".format(remaining)

                etProfLossStatus.text = when {
                    data.isEmpty()   -> "No transactions yet"
                    remaining < 0    -> "Over budget by R %.2f!".format(-remaining)
                    remaining == 0.0 -> "Exactly on budget!"
                    else             -> "R %.2f remaining this month".format(remaining)
                }
            }
        }
    }

    fun filterByPeriod(period: String) {
        val calendar   = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val startDate: String
        val endDate: String

        when (period) {
            "thisWeek" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startDate = dateFormat.format(calendar.time)
                endDate   = dateFormat.format(Calendar.getInstance().time)
            }
            "thisMonth" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startDate = dateFormat.format(calendar.time)
                endDate   = dateFormat.format(Calendar.getInstance().time)
            }
            "lastMonth" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startDate = dateFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endDate   = dateFormat.format(calendar.time)
            }
            else -> return
        }

        thread {
            val data = AppDatabase.getDatabase(this)
                .transactionDao()
                .getTransactionsByDateRange(startDate, endDate)

            runOnUiThread {
                llResultsContainer.removeAllViews()

                if (data.isEmpty()) {
                    val tv = TextView(this)
                    tv.text = "No transactions found for this period"
                    tv.setPadding(8, 8, 8, 8)
                    llResultsContainer.addView(tv)
                    return@runOnUiThread
                }

                // Categories header
                val tvCatHeader = TextView(this)
                tvCatHeader.text = "── Categories ──"
                tvCatHeader.textSize = 17f
                tvCatHeader.setPadding(8, 16, 8, 8)
                llResultsContainer.addView(tvCatHeader)

                val categories = data.map { it.category }.filter { it.isNotEmpty() }.toSet()
                for (cat in categories) {
                    val tv = TextView(this)
                    tv.text = "• $cat"
                    tv.textSize = 15f
                    tv.setPadding(8, 4, 8, 4)
                    llResultsContainer.addView(tv)
                }

                // Entries header
                val tvEntryHeader = TextView(this)
                tvEntryHeader.text = "── Entries ──"
                tvEntryHeader.textSize = 17f
                tvEntryHeader.setPadding(8, 16, 8, 8)
                llResultsContainer.addView(tvEntryHeader)

                for (tx in data) {
                    val rowLayout = LinearLayout(this)
                    rowLayout.orientation = LinearLayout.HORIZONTAL
                    rowLayout.setPadding(8, 6, 8, 6)

                    fun makeCell(text: String): TextView {
                        val tv = TextView(this)
                        tv.text = text
                        tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        return tv
                    }

                    rowLayout.addView(makeCell(tx.date))
                    rowLayout.addView(makeCell(tx.category))
                    rowLayout.addView(makeCell(tx.transactionType))
                    rowLayout.addView(makeCell("R %.2f".format(tx.amount)))
                    llResultsContainer.addView(rowLayout)
                }
            }
        }
    }

    fun saveMonthlyBudget(budget: Double) {
        thread {
            AppDatabase.getDatabase(this).budgetDao().insertBudget(Budget(monthlyBudget = budget))
        }
    }
}