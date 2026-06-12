package com.example.opsc_poe

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Transaction
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class Analytics : AppCompatActivity() {

    private lateinit var btnThisWeek: MaterialButton
    private lateinit var btnThisMonth: MaterialButton
    private lateinit var btnLastMonth: MaterialButton
    private lateinit var tvPeriodLabel: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var llCategoryChart: LinearLayout
    private lateinit var llTransactionList: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analytics)
        setupNavigation(this, R.id.btnAnalytics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnThisWeek      = findViewById(R.id.btnThisWeek)
        btnThisMonth     = findViewById(R.id.btnThisMonth)
        btnLastMonth     = findViewById(R.id.btnLastMonth)
        tvPeriodLabel    = findViewById(R.id.tvPeriodLabel)
        tvTotalIncome    = findViewById(R.id.tvTotalIncome)
        tvTotalExpense   = findViewById(R.id.tvTotalExpense)
        tvNetBalance     = findViewById(R.id.tvNetBalance)
        llCategoryChart  = findViewById(R.id.llCategoryChart)
        llTransactionList = findViewById(R.id.llTransactionList)

        btnThisWeek.setOnClickListener  { loadAnalytics("thisWeek") }
        btnThisMonth.setOnClickListener { loadAnalytics("thisMonth") }
        btnLastMonth.setOnClickListener { loadAnalytics("lastMonth") }

        // Default highlight selection target state
        loadAnalytics("thisMonth")
    }

    private fun loadAnalytics(period: String) {
        val calendar = Calendar.getInstance()
        val startDate: String
        val endDate: String

        // Update button highlight layout states instantly
        updateButtonHighlightStates(period)

        when (period) {
            "thisWeek" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                startDate = dateFormat.format(calendar.time)
                endDate   = dateFormat.format(Calendar.getInstance().time)
                tvPeriodLabel.text = "This Week"
            }
            "thisMonth" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startDate = dateFormat.format(calendar.time)
                endDate   = dateFormat.format(Calendar.getInstance().time)
                tvPeriodLabel.text = "This Month"
            }
            "lastMonth" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startDate = dateFormat.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endDate   = dateFormat.format(calendar.time)
                tvPeriodLabel.text = "Last Month"
            }
            else -> return
        }

        thread {
            val data = AppDatabase.getDatabase(this)
                .transactionDao()
                .getTransactionsByDateRange(startDate, endDate)

            runOnUiThread { renderAnalytics(data) }
        }
    }

    private fun updateButtonHighlightStates(activePeriod: String) {
        val primaryColor = getColor(R.color.purple_500)
        val whiteColor = Color.WHITE

        fun setButtonStyle(button: MaterialButton, isActive: Boolean) {
            if (isActive) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                button.setTextColor(whiteColor)
                button.strokeWidth = 0
            } else {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(whiteColor)
                button.setTextColor(primaryColor)
                button.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
                button.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            }
        }

        setButtonStyle(btnThisWeek, activePeriod == "thisWeek")
        setButtonStyle(btnThisMonth, activePeriod == "thisMonth")
        setButtonStyle(btnLastMonth, activePeriod == "lastMonth")
    }

    private fun renderAnalytics(data: List<Transaction>) {
        var totalIncome  = 0.0
        var totalExpense = 0.0
        val categorySpend = mutableMapOf<String, Double>()

        for (tx in data) {
            when (tx.transactionType.lowercase()) {
                "income"  -> totalIncome  += tx.amount
                "expense" -> {
                    totalExpense += tx.amount
                    categorySpend[tx.category] = (categorySpend[tx.category] ?: 0.0) + tx.amount
                }
            }
        }

        val net = totalIncome - totalExpense

        tvTotalIncome.text  = "R %.2f".format(totalIncome)
        tvTotalExpense.text = "R %.2f".format(totalExpense)
        tvNetBalance.text   = "R %.2f".format(net)
        tvNetBalance.setTextColor(if (net >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

        // ── Category bar chart ──
        llCategoryChart.removeAllViews()

        if (categorySpend.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No expense data for this period"
            tv.setPadding(0, 8, 0, 8)
            tv.setTextColor(Color.GRAY)
            llCategoryChart.addView(tv)
        } else {
            val maxSpend = categorySpend.values.maxOrNull() ?: 1.0
            val barColors = listOf("#6200EE", "#9C27B0", "#AB47BC", "#CE93D8", "#E1BEE7")
            var colorIdx = 0

            categorySpend.entries
                .sortedByDescending { it.value }
                .forEach { (cat, amount) ->
                    val pct = ((amount / totalExpense) * 100).toInt()
                    val barWidth = ((amount / maxSpend) * 100).toInt()

                    val rowTop = LinearLayout(this)
                    rowTop.orientation = LinearLayout.HORIZONTAL
                    rowTop.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    val tvCat = TextView(this)
                    tvCat.text = cat.replaceFirstChar { it.uppercase() }
                    tvCat.textSize = 14f
                    tvCat.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    rowTop.addView(tvCat)

                    val tvAmt = TextView(this)
                    tvAmt.text = "R %.2f ($pct%%)".format(amount)
                    tvAmt.textSize = 13f
                    tvAmt.setTextColor(Color.GRAY)
                    rowTop.addView(tvAmt)

                    llCategoryChart.addView(rowTop)

                    val barContainer = LinearLayout(this)
                    barContainer.orientation = LinearLayout.HORIZONTAL
                    barContainer.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 4, 0, 12) }

                    val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
                    bar.max = 100
                    bar.progress = barWidth
                    bar.progressTintList = android.content.res.ColorStateList.valueOf(
                        Color.parseColor(barColors[colorIdx % barColors.size])
                    )
                    bar.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 24
                    )
                    barContainer.addView(bar)
                    llCategoryChart.addView(barContainer)
                    colorIdx++
                }
        }

        // ── Transaction list ──
        llTransactionList.removeAllViews()

        if (data.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No transactions for this period"
            tv.setPadding(0, 8, 0, 8)
            tv.setTextColor(Color.GRAY)
            llTransactionList.addView(tv)
        } else {
            val header = LinearLayout(this)
            header.orientation = LinearLayout.HORIZONTAL
            header.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }

            listOf("Date", "Category", "Type", "Amount").forEachIndexed { i, title ->
                val tv = TextView(this)
                tv.text = title
                tv.textSize = 12f
                tv.setTextColor(Color.GRAY)
                tv.setTypeface(null, android.graphics.Typeface.BOLD)
                tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                header.addView(tv)
            }
            llTransactionList.addView(header)

            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 0, 0, 12) }
            divider.setBackgroundColor(Color.LTGRAY)
            llTransactionList.addView(divider)

            // Dp formatting structural metrics
            val rowMarginBottomPx = (14 * resources.displayMetrics.density).toInt()
            val rowVerticalPaddingPx = (10 * resources.displayMetrics.density).toInt()

            data.sortedByDescending { it.date }.forEach { tx ->
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, rowMarginBottomPx) }

                row.setPadding(0, rowVerticalPaddingPx, 0, rowVerticalPaddingPx)

                val isIncome = tx.transactionType.lowercase() == "income"

                fun cell(text: String, color: Int = Color.BLACK): TextView {
                    val tv = TextView(this)
                    tv.text = text
                    tv.textSize = 13f
                    tv.setTextColor(color)
                    tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    return tv
                }

                row.addView(cell(tx.date))
                row.addView(cell(tx.category.replaceFirstChar { it.uppercase() }))
                row.addView(cell(
                    tx.transactionType.replaceFirstChar { it.uppercase() },
                    if (isIncome) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                ))
                row.addView(cell(
                    "R %.2f".format(tx.amount),
                    if (isIncome) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                ))

                row.setOnClickListener {
                    showTransactionBottomSheet(tx)
                }

                llTransactionList.addView(row)
            }
        }
    }

    private fun showTransactionBottomSheet(transaction: Transaction) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_transaction_detail, null)

        val bsType = sheetView.findViewById<TextView>(R.id.bsType)
        val bsAmount = sheetView.findViewById<TextView>(R.id.bsAmount)
        val bsCategory = sheetView.findViewById<TextView>(R.id.bsCategory)
        val bsDate = sheetView.findViewById<TextView>(R.id.bsDate)
        val bsImage = sheetView.findViewById<ImageView>(R.id.bsImage)

        bsType.text = transaction.transactionType.uppercase()
        if (transaction.transactionType.lowercase() == "income") {
            bsType.setTextColor(Color.parseColor("#A5D6A7"))
        } else {
            bsType.setTextColor(Color.parseColor("#EF9A9A"))
        }

        bsAmount.text = String.format("R %.2f", transaction.amount)
        bsCategory.text = "Category: ${transaction.category}"
        bsDate.text = "Date Logged: ${transaction.date}"

        if (!transaction.imagePath.isNullOrEmpty()) {
            try {
                bsImage.setImageURI(android.net.Uri.parse(transaction.imagePath))
                bsImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                bsImage.visibility = View.GONE
            }
        } else {
            bsImage.visibility = View.GONE
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }
}