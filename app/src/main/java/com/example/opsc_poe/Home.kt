package com.example.opsc_poe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log // Imported Android Log utility
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Budget
import com.google.android.material.button.MaterialButton
import kotlin.concurrent.thread

/**
 * Main dashboard view for the application. Displays financial summaries,
 * active budget goal status calculations, and a historical preview stream of recent records.
 */
class Home : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
    }

    // Dashboard Metrics Views
    private lateinit var tvUserName:         TextView
    private lateinit var etMonthlyGoal:      TextView
    private lateinit var tvBudgetPercentage: TextView
    private lateinit var tvBudgetStatus:     TextView
    private lateinit var etTotalBalance:     TextView
    private lateinit var etTotalIncome:      TextView
    private lateinit var etTotalExp:         TextView
    private lateinit var tvTopCategory:      TextView
    private lateinit var tvDailyAvg:         TextView

    // Controls and Layout Containers
    private lateinit var progressBudget:     ProgressBar
    private lateinit var btnSetBudget:       MaterialButton
    private lateinit var llTransactionList:  LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing application dashboard home components.")
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        setupNavigation(this, R.id.btnHome)

        // Adjust system layout window pads for edge-to-edge screens safely
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind layout views
        tvUserName         = findViewById(R.id.tvUserName)
        etMonthlyGoal      = findViewById(R.id.etMonthlyGoal)
        tvBudgetPercentage = findViewById(R.id.tvBudgetPercentage)
        tvBudgetStatus     = findViewById(R.id.tvBudgetStatus)
        etTotalBalance     = findViewById(R.id.etTotalBalance)
        etTotalIncome      = findViewById(R.id.etTotalIncome)
        etTotalExp         = findViewById(R.id.etTotalExp)
        tvTopCategory      = findViewById(R.id.tvTopCategory)
        tvDailyAvg         = findViewById(R.id.tvDailyAvg)
        progressBudget     = findViewById(R.id.progressBudget)
        btnSetBudget       = findViewById(R.id.btnSetBudget)
        llTransactionList  = findViewById(R.id.llHomeTransactionList)

        val ivProfileIcon = findViewById<ImageView>(R.id.ivProfileIcon)
        ivProfileIcon.setOnClickListener {
            Log.d(TAG, "Profile icon clicked. Navigating to Profile activity.")
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        // Recover existing user session validation information
        val session = getSharedPreferences("user_session", MODE_PRIVATE)
        val userNameStr = session.getString("user_name", "Welcome Back!") ?: "Welcome Back!"
        tvUserName.text = userNameStr
        Log.d(TAG, "User session matching loaded successfully. Identity: '$userNameStr'")

        // Click handler to launch dynamic budget modifier bottom sheets
        btnSetBudget.setOnClickListener {
            Log.d(TAG, "btnSetBudget clicked. Opening SetBudgetBottomSheet container instance.")
            SetBudgetBottomSheet { newBudget ->
                Log.d(TAG, "SetBudgetBottomSheet callback invoked. Updating target budget goal values to: $newBudget")
                thread {
                    AppDatabase.getDatabase(this).budgetDao()
                        .insertBudget(Budget(monthlyBudget = newBudget))
                    runOnUiThread {
                        Log.d(TAG, "Budget persistence sequence finalized. Refreshing dashboard data displays.")
                        loadData()
                    }
                }
            }.show(supportFragmentManager, "SetBudgetSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Reactivating activity scope focus. Refreshing transactional models.")
        loadData()
    }

    /**
     * Spawns an asynchronous tracking thread to pull records from SQLite storage,
     * processing math values to populate metric summaries on the UI main thread loop.
     */
    private fun loadData() {
        thread {
            Log.d(TAG, "loadData: Initiating asynchronous worker dataset load queries from Room database.")
            val db           = AppDatabase.getDatabase(this)
            val transactions = db.transactionDao().getAllTransactions()
            val budget       = db.budgetDao().getLatestBudget()

            Log.d(TAG, "Database extraction completed. Retrieved ${transactions.size} transactions logs and active budget metadata.")

            var totalIncome  = 0.0
            var totalExpense = 0.0
            val categoryCount = mutableMapOf<String, Int>()
            var expenseCount  = 0

            // Consolidate values inside processing arrays split by classification types
            for (tx in transactions) {
                when (tx.transactionType.lowercase()) {
                    "income"  -> totalIncome  += tx.amount
                    "expense" -> { totalExpense += tx.amount; expenseCount++ }
                }
                if (tx.category.isNotEmpty()) {
                    categoryCount[tx.category] = (categoryCount[tx.category] ?: 0) + 1
                }
            }

            // Calculate aggregated metrics formulas
            val totalBalance = totalIncome - totalExpense
            val topCategory  = categoryCount.maxByOrNull { it.value }?.key ?: "None"
            val dailyAvg     = if (expenseCount > 0) totalExpense / expenseCount else 0.0
            val budgetAmount = budget?.monthlyBudget ?: 0.0
            val spentPct     = if (budgetAmount > 0)
                ((totalExpense / budgetAmount) * 100).toInt().coerceIn(0, 100) else 0
            val remaining    = budgetAmount - totalExpense

            // Extract the most recent 10 transactions
            val recent = transactions.take(10)

            // Route execution back to Main UI Thread to update widgets safely
            runOnUiThread {
                etMonthlyGoal.text      = if (budgetAmount > 0) "R %.2f".format(budgetAmount) else "R 0.00"
                tvBudgetPercentage.text = "$spentPct%"
                etTotalBalance.text     = "R %.2f".format(totalBalance)
                etTotalIncome.text      = "R %.2f".format(totalIncome)
                etTotalExp.text         = "R %.2f".format(totalExpense)
                tvTopCategory.text      = topCategory
                tvDailyAvg.text         = if (dailyAvg > 0) "R %.2f".format(dailyAvg) else "R 0.00"
                progressBudget.progress = spentPct

                // Build context-specific text warning components matching active utilization margins
                tvBudgetStatus.text = when {
                    budgetAmount <= 0  -> "Tap 'Set' to create your monthly budget goal"
                    remaining < 0      -> {
                        Log.w(TAG, "Financial Alert: Current budget limits EXCEEDED by ${-remaining}")
                        "Over budget by R %.2f!".format(-remaining)
                    }
                    spentPct >= 90     -> "Almost at limit — R %.2f left".format(remaining)
                    else               -> "R %.2f remaining ($spentPct%% spent)".format(remaining)
                }

                renderRecentTransactions(recent)
            }
        }
    }

    /**
     * Programmatically inflates linear rows mapped to transaction data elements.
     */
    private fun renderRecentTransactions(recent: List<com.example.opsc_poe.db.entities.Transaction>) {
        llTransactionList.removeAllViews()

        if (recent.isEmpty()) {
            Log.d(TAG, "renderRecentTransactions: No transaction records to display.")
            val tv = TextView(this)
            tv.text     = "No transactions yet — add your first one!"
            tv.textSize = 13f
            tv.setTextColor(Color.parseColor("#BCC8CC"))
            tv.setPadding(0, 8, 0, 8)
            llTransactionList.addView(tv)
            return
        }

        Log.d(TAG, "renderRecentTransactions: Populating ${recent.size} structural item list items.")

        recent.forEach { tx ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity     = Gravity.CENTER_VERTICAL
            row.setPadding(0, 10, 0, 10)

            // Rating emoji verification badge layout setup
            val tvRating = TextView(this)
            tvRating.text = when (tx.rating) {
                "THUMBS_UP"   -> "👍"
                "THUMBS_DOWN" -> "👎"
                else          -> "➖"
            }
            tvRating.textSize = 18f
            tvRating.layoutParams = LinearLayout.LayoutParams(
                48.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.addView(tvRating)

            // Details and category grouping tracking text view configurations
            val tvInfo = TextView(this)
            val titleText = if (!tx.title.isNullOrEmpty()) tx.title else tx.category
            tvInfo.text    = "$titleText  •  ${tx.category.replaceFirstChar { it.uppercase() }}"
            tvInfo.textSize = 13f
            tvInfo.setTextColor(Color.parseColor("#15174D"))
            tvInfo.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            row.addView(tvInfo)

            // Amount tracking label configurations matching directional logic colors
            val tvAmount = TextView(this)
            tvAmount.text = "R %.2f".format(tx.amount)
            tvAmount.textSize = 13f
            tvAmount.setTypeface(null, android.graphics.Typeface.BOLD)
            tvAmount.setTextColor(
                if (tx.transactionType.lowercase() == "income")
                    Color.parseColor("#4CAF50")
                else Color.parseColor("#ED4B00")
            )
            row.addView(tvAmount)

            llTransactionList.addView(row)

            // Instantiation element representing dynamic structural horizontal layout line breaks
            val div = android.view.View(this)
            div.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            div.setBackgroundColor(Color.parseColor("#DDDDCC"))
            llTransactionList.addView(div)
        }
    }

    // Helper conversion extension translating scaling structural DP values into physical resolution metrics
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}