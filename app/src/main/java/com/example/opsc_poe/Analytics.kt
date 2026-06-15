package com.example.opsc_poe

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Transaction
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class Analytics : AppCompatActivity() {

    private lateinit var tvPeriodLabel:       TextView
    private lateinit var tvTotalIncome:       TextView
    private lateinit var tvTotalExpense:      TextView
    private lateinit var tvNetBalance:        TextView
    private lateinit var llCategoryChart:     LinearLayout
    private lateinit var llTransactionList:  LinearLayout
    private lateinit var llGraphContainer:   LinearLayout

    private lateinit var btnThisWeek:   MaterialButton
    private lateinit var btnThisMonth:  MaterialButton
    private lateinit var btnLastMonth:  MaterialButton
    private lateinit var pieChartView: PieChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analytics)

        setupNavigation(this, R.id.btnAnalytics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvPeriodLabel      = findViewById(R.id.tvPeriodLabel)
        tvTotalIncome      = findViewById(R.id.tvTotalIncome)
        tvTotalExpense     = findViewById(R.id.tvTotalExpense)
        tvNetBalance       = findViewById(R.id.tvNetBalance)
        llCategoryChart    = findViewById(R.id.llCategoryChart)
        llTransactionList  = findViewById(R.id.llTransactionList)
        llGraphContainer   = findViewById(R.id.llAnalyticsGraphContainer)
        pieChartView       = findViewById(R.id.pieChartView)

        btnThisWeek   = findViewById(R.id.btnThisWeek)
        btnThisMonth  = findViewById(R.id.btnThisMonth)
        btnLastMonth  = findViewById(R.id.btnLastMonth)

        btnThisWeek.setOnClickListener  { loadPeriodData("week") }
        btnThisMonth.setOnClickListener { loadPeriodData("month") }
        btnLastMonth.setOnClickListener { loadPeriodData("last_month") }

        loadPeriodData("month")
    }

    override fun onResume() {
        super.onResume()
        loadPeriodData("month")
    }

    private fun loadPeriodData(period: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        var startDate = ""

        when (period) {
            "week" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                startDate = sdf.format(cal.time)
                tvPeriodLabel.text = "This Week"
                updateButtonStyles(btnThisWeek)
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startDate = sdf.format(cal.time)
                tvPeriodLabel.text = "This Month"
                updateButtonStyles(btnThisMonth)
            }
            "last_month" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startDate = sdf.format(cal.time)

                val endCal = Calendar.getInstance()
                endCal.add(Calendar.MONTH, -1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                tvPeriodLabel.text = "Last Month"
                updateButtonStyles(btnLastMonth)
                fetchAndRender(startDate, sdf.format(endCal.time))
                return
            }
        }
        fetchAndRender(startDate, endDate)
    }

    private fun fetchAndRender(start: String, end: String) {
        thread {
            val db = AppDatabase.getDatabase(this)
            val transactions = db.transactionDao().getTransactionsByDateRange(start, end)

            var totalIncome = 0.0
            var totalExpense = 0.0
            val categoryTotals = mutableMapOf<String, Double>()

            for (tx in transactions) {
                val type = tx.transactionType.lowercase(Locale.getDefault()).trim()
                val categoryName = if (tx.category.isNullOrEmpty()) "other" else tx.category.lowercase(Locale.getDefault()).trim()

                if (type == "income") {
                    totalIncome += tx.amount
                } else {
                    totalExpense += tx.amount
                    categoryTotals[categoryName] = (categoryTotals[categoryName] ?: 0.0) + tx.amount
                }
            }

            runOnUiThread {
                tvTotalIncome.text  = "R %.2f".format(totalIncome)
                tvTotalExpense.text = "R %.2f".format(totalExpense)

                val net = totalIncome - totalExpense
                tvNetBalance.text   = "R %.2f".format(net)
                tvNetBalance.setTextColor(if (net >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#ED4B00"))

                renderGraphBars(totalIncome, totalExpense)
                renderCategoryBreakdown(categoryTotals, totalExpense)
                renderTransactionsList(transactions)
            }
        }
    }

    private fun renderGraphBars(income: Double, expense: Double) {
        llGraphContainer.removeAllViews()
        val maxVal = maxOf(income, expense, 1.0)

        val incomeHeight  = ((income / maxVal) * 100).toInt().coerceAtLeast(8).dpToPx()
        val expenseHeight = ((expense / maxVal) * 100).toInt().coerceAtLeast(8).dpToPx()

        llGraphContainer.addView(createBarStack(incomeHeight, "R %.0f".format(income), "Income", "#4CAF50"))
        llGraphContainer.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 1) })
        llGraphContainer.addView(createBarStack(expenseHeight, "R %.0f".format(expense), "Expenses", "#ED4B00"))
    }

    private fun createBarStack(barHeightPx: Int, amountStr: String, labelText: String, hexColor: String): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(80.dpToPx(), LinearLayout.LayoutParams.MATCH_PARENT)
        }

        column.addView(TextView(this).apply {
            text = amountStr
            textSize = 11f
            setTextColor(Color.parseColor("#15174D"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 4)
        })

        column.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(36.dpToPx(), barHeightPx)
            setBackgroundColor(Color.parseColor(hexColor))
        })

        column.addView(TextView(this).apply {
            text = labelText
            textSize = 12f
            setTextColor(Color.parseColor("#BCC8CC"))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        })

        return column
    }

    private fun renderCategoryBreakdown(categories: Map<String, Double>, totalExpense: Double) {
        llCategoryChart.removeAllViews()
        pieChartView.setData(categories)

        if (categories.isEmpty()) {
            pieChartView.setData(emptyMap())
            llCategoryChart.addView(TextView(this).apply {
                text = "No expenses recorded for this period."
                textSize = 13f
                setTextColor(Color.parseColor("#BCC8CC"))
            })
            return
        }

        categories.forEach { (category, amount) ->
            val pct = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }

            row.addView(TextView(this).apply {
                text = "${category.replaceFirstChar { it.uppercase() }} (${pct.toInt()}%)"
                textSize = 13f
                setTextColor(Color.parseColor("#15174D"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            row.addView(TextView(this).apply {
                text = "R %.2f".format(amount)
                textSize = 13f
                setTextColor(Color.parseColor("#ED4B00"))
            })

            llCategoryChart.addView(row)
        }
    }

    private fun renderTransactionsList(list: List<Transaction>) {
        llTransactionList.removeAllViews()
        if (list.isEmpty()) {
            llTransactionList.addView(TextView(this).apply {
                text = "No transactions found for this period."
                textSize = 13f
                setTextColor(Color.parseColor("#BCC8CC"))
            })
            return
        }

        list.forEach { tx ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 10.dpToPx())
                }

                setPadding(16.dpToPx(), 14.dpToPx(), 16.dpToPx(), 14.dpToPx())
                isClickable = true
                isFocusable = true

                val shapeDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    setColor(Color.WHITE)
                    cornerRadius = 10.dpToPx().toFloat()
                    setStroke(1.dpToPx(), Color.parseColor("#E0E0D8"))
                }

                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                val rippleDrawable = ContextCompat.getDrawable(this@Analytics, outValue.resourceId)

                background = android.graphics.drawable.LayerDrawable(arrayOf(shapeDrawable, rippleDrawable))
            }

            val ivIndicator = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(10.dpToPx(), 10.dpToPx()).apply {
                    marginEnd = 14.dpToPx()
                }
                setImageResource(android.R.drawable.presence_online)
                val isIncome = tx.transactionType.lowercase(Locale.getDefault()) == "income"
                setColorFilter(Color.parseColor(if (isIncome) "#4CAF50" else "#ED4B00"))
            }
            row.addView(ivIndicator)

            row.addView(TextView(this).apply {
                text = if (!tx.title.isNullOrEmpty()) tx.title else tx.category.replaceFirstChar { it.uppercase() }
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#15174D"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            row.addView(TextView(this).apply {
                text = "R %.2f".format(tx.amount)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(if (tx.transactionType.lowercase(Locale.getDefault()) == "income") "#4CAF50" else "#ED4B00"))
                setPadding(0, 0, 8.dpToPx(), 0)
            })

            val ivChevron = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx())
                setImageResource(android.R.drawable.ic_media_next)
                setColorFilter(Color.parseColor("#BCC8CC"))
            }
            row.addView(ivChevron)

            row.setOnClickListener {
                showTransactionDetailBottomSheet(tx)
            }

            llTransactionList.addView(row)
        }
    }

    private fun showTransactionDetailBottomSheet(transaction: Transaction) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_transaction_detail, null)
        bottomSheetDialog.setContentView(sheetView)

        val bsType          = sheetView.findViewById<TextView>(R.id.bsType)
        val bsAmount        = sheetView.findViewById<TextView>(R.id.bsAmount)
        val bsCategory      = sheetView.findViewById<TextView>(R.id.bsCategory)
        val bsDate          = sheetView.findViewById<TextView>(R.id.bsDate)
        val bsImage         = sheetView.findViewById<ImageView>(R.id.bsImage)
        val bsNoPhotoLabel  = sheetView.findViewById<TextView>(R.id.bsNoPhotoLabel)

        val isIncome = transaction.transactionType.lowercase(Locale.getDefault()) == "income"
        bsType.text = if (isIncome) "Income" else "Expense"
        bsType.setTextColor(Color.WHITE)
        bsType.setBackgroundColor(Color.parseColor(if (isIncome) "#4CAF50" else "#ED4B00"))

        val displayTitle = if (!transaction.title.isNullOrEmpty()) " (${transaction.title})" else ""
        bsAmount.text = "R %.2f%s".format(transaction.amount, displayTitle)
        bsCategory.text = "Category: ${transaction.category.replaceFirstChar { it.uppercase() }}"

        val ratingTag = when(transaction.rating) {
            "THUMBS_UP"   -> " | 👍 Satisfied"
            "THUMBS_DOWN" -> " | 👎 Unsatisfied"
            else          -> ""
        }
        bsDate.text = "Logged: ${transaction.date}$ratingTag"

        if (!transaction.imagePath.isNullOrEmpty() && File(transaction.imagePath).exists()) {
            bsImage.visibility = View.VISIBLE
            bsNoPhotoLabel.visibility = View.GONE
            Glide.with(this).load(File(transaction.imagePath)).into(bsImage)
        } else {
            bsImage.visibility = View.GONE
            bsNoPhotoLabel.visibility = View.VISIBLE
        }

        bottomSheetDialog.show()
    }

    private fun updateButtonStyles(selected: MaterialButton) {
        val selectedBgColor   = ColorStateList.valueOf(Color.parseColor("#15174D"))
        val unselectedBgColor = ColorStateList.valueOf(Color.TRANSPARENT)

        val selectedTextColor   = Color.parseColor("#F4F1EC")
        val unselectedTextColor = Color.parseColor("#15174D")

        listOf(btnThisWeek, btnThisMonth, btnLastMonth).forEach { btn ->
            if (btn == selected) {
                btn.backgroundTintList = selectedBgColor
                btn.setTextColor(selectedTextColor)
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = unselectedBgColor
                btn.setTextColor(unselectedTextColor)
                btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#15174D")))
                btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}