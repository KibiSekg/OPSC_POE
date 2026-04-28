package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
//import this , put them at the top of the code
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread
import android.widget.Toast
//import them at the top of the code
import org.json.JSONArray
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread



class Profile : AppCompatActivity(), View.OnClickListener {

    lateinit var btnHome : Button
    lateinit var btnExpInc : Button
    lateinit var btnProfile : Button
    lateinit var btnSetMonthlyBudg: Button
    lateinit var btnThisWeek: Button
    lateinit var btnThisMonth: Button
    lateinit var btnLastMonth: Button

    lateinit var tvProfileId: TextView
    lateinit var tvMonthlyBalance: TextView
    lateinit var etProfLossStatus: TextView
    lateinit var tvNumOfInc: TextView
    lateinit var tvNumOfExp: TextView
    lateinit var etMonthlyBudget: EditText
    lateinit var llResultsContainer: LinearLayout

    // Store monthly budget to use in balance calculation
    var monthlyBudget = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnHome = findViewById(R.id.btnHome)
        btnExpInc = findViewById(R.id.btnExpInc)
        btnProfile = findViewById(R.id.btnProfile)
        btnSetMonthlyBudg = findViewById(R.id.btnSetMonthlyBudg)
        btnThisWeek = findViewById(R.id.btnThisWeek)
        btnThisMonth = findViewById(R.id.btnThisMonth)
        btnLastMonth = findViewById(R.id.btnLastMonth)

        tvMonthlyBalance = findViewById(R.id.tvMonthlyBalance)
        etProfLossStatus = findViewById(R.id.etProfLossStatus)
        tvNumOfInc = findViewById(R.id.tvNumOfInc)
        tvNumOfExp = findViewById(R.id.tvNumOfExp)
        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)
        llResultsContainer = findViewById(R.id.llResultsContainer)


        btnHome.setOnClickListener(this)
        btnExpInc.setOnClickListener(this)
        btnProfile.setOnClickListener(this)

        // When set budget button is pressed
        btnSetMonthlyBudg.setOnClickListener {
            val input = etMonthlyBudget.text.toString()

            // Check if the input is not empty
            if (input.isNotEmpty()) {
                monthlyBudget = input.toDoubleOrNull() ?: 0.0

                // Save to DB and recalculate balance
                saveMonthlyBudget(monthlyBudget)
                loadTransactions()
                Toast.makeText(this, "Monthly budget set to R %.2f".format(monthlyBudget), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            }// end of if

        }// end of btnSetMonthlyBudg click listener

        // Period filter button listeners
        btnThisWeek.setOnClickListener {
            filterByPeriod("thisWeek")
        }// end of btnThisWeek

        btnThisMonth.setOnClickListener {
            filterByPeriod("thisMonth")
        }// end of btnThisMonth

        btnLastMonth.setOnClickListener {
            filterByPeriod("lastMonth")
        }// end of btnLastMonth

        // Load transactions when page opens
        loadTransactions()

    }//end OnCreate

    fun filterByPeriod(period: String) {
        thread {
            val data = getRows("user_ef3f2aac_transactions")

            runOnUiThread {

                // Clear previous results
                llResultsContainer.removeAllViews()

                if (data == null || data.length() == 0) {
                    val tvEmpty = TextView(this)
                    tvEmpty.text = "No transactions found"
                    tvEmpty.setPadding(8, 8, 8, 8)
                    llResultsContainer.addView(tvEmpty)
                    return@runOnUiThread
                }// end of if

                // Get date range based on selected period
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Calculate start and end dates for the selected period
                val startDate: String
                val endDate: String

                when (period) {

                    "thisWeek" -> {
                        // Start = Monday of this week, End = today
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        startDate = dateFormat.format(calendar.time)
                        endDate = dateFormat.format(Calendar.getInstance().time)
                    }// end of thisWeek

                    "thisMonth" -> {
                        // Start = 1st of this month, End = today
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        startDate = dateFormat.format(calendar.time)
                        endDate = dateFormat.format(Calendar.getInstance().time)
                    }// end of thisMonth

                    "lastMonth" -> {
                        // Start = 1st of last month, End = last day of last month
                        calendar.add(Calendar.MONTH, -1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        startDate = dateFormat.format(calendar.time)
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        endDate = dateFormat.format(calendar.time)
                    }// end of lastMonth

                    else -> return@runOnUiThread

                }// end of when

                // Collect filtered transactions
                val filteredEntries = mutableListOf<JSONObject>()
                val categorySet = mutableSetOf<String>()

                for (i in 0 until data.length()) {
                    val row = data.getJSONObject(i)
                    val date = row.optString("date", "")

                    // Check if date falls within range
                    if (date >= startDate && date <= endDate) {
                        filteredEntries.add(row)
                        val category = row.optString("Category", "").lowercase()
                        if (category.isNotEmpty()) {
                            categorySet.add(category)
                        }// end of if
                    }// end of if

                }// end of for loop

                // ---------------------------------
                // DISPLAY CATEGORIES FOR PERIOD
                // ---------------------------------
                val tvCatHeader = TextView(this)
                tvCatHeader.text = "── Categories ──"
                tvCatHeader.textSize = 17f
                tvCatHeader.setPadding(8, 16, 8, 8)
                llResultsContainer.addView(tvCatHeader)

                if (categorySet.isEmpty()) {
                    val tvNoCat = TextView(this)
                    tvNoCat.text = "No categories found for this period"
                    tvNoCat.setPadding(8, 4, 8, 4)
                    llResultsContainer.addView(tvNoCat)
                } else {
                    for (category in categorySet) {
                        val tvCat = TextView(this)
                        tvCat.text = "• $category"
                        tvCat.textSize = 15f
                        tvCat.setPadding(8, 4, 8, 4)
                        llResultsContainer.addView(tvCat)
                    }// end of for loop
                }// end of if

                // ---------------------------------
                // DISPLAY ENTRIES FOR PERIOD
                // ---------------------------------
                val tvEntryHeader = TextView(this)
                tvEntryHeader.text = "── Entries ──"
                tvEntryHeader.textSize = 17f
                tvEntryHeader.setPadding(8, 16, 8, 8)
                llResultsContainer.addView(tvEntryHeader)

                if (filteredEntries.isEmpty()) {
                    val tvNoEntry = TextView(this)
                    tvNoEntry.text = "No entries found for this period"
                    tvNoEntry.setPadding(8, 4, 8, 4)
                    llResultsContainer.addView(tvNoEntry)
                } else {
                    for (entry in filteredEntries) {
                        val amount = entry.optString("amount", "0")
                        val type = entry.optString("transactionType", "")
                        val category = entry.optString("Category", "")
                        val date = entry.optString("date", "")

                        // Create a row for each entry
                        val rowLayout = LinearLayout(this)
                        rowLayout.orientation = LinearLayout.HORIZONTAL
                        rowLayout.setPadding(8, 6, 8, 6)

                        // Date
                        val tvDate = TextView(this)
                        tvDate.text = date
                        tvDate.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        rowLayout.addView(tvDate)

                        // Category
                        val tvCat = TextView(this)
                        tvCat.text = category
                        tvCat.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        rowLayout.addView(tvCat)

                        // Type
                        val tvType = TextView(this)
                        tvType.text = type
                        tvType.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        rowLayout.addView(tvType)

                        // Amount
                        val tvAmt = TextView(this)
                        tvAmt.text = "R %.2f".format(amount.toDoubleOrNull() ?: 0.0)
                        tvAmt.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        rowLayout.addView(tvAmt)

                        // Add row to container
                        llResultsContainer.addView(rowLayout)

                    }// end of for loop
                }// end of if

            }// end of runOnUiThread

        }// end of thread

    }// end of filterByPeriod

    fun getRows(tableName: String): JSONArray? {
        val url = URL("https://studyplugtools.cloud/you_connect.php/$tableName/get")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write("{}".toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            JSONArray(response.toString())

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection.disconnect()
        }

    }// end of getRows

    fun insertRow(tableName: String, data: Map<String, Any?>): String? {
        val url = URL("https://studyplugtools.cloud/you_connect.php/$tableName/insert")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val payload = JSONObject()
            for ((key, value) in data) {
                if (key != "id") {
                    payload.put(key, value ?: "")
                }
            }

            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            response.toString()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection.disconnect()
        }

    }// end of insertRow


    override fun onClick(v: View?){

        when(v?.id){

            //when button home is pressed
            R.id.btnHome -> {
                //navigate to home using intent
                //creating object for intent called navigate
                val navigate = Intent(this@Profile, Home :: class.java)//can just say 'this' instead of 'this@Home'

                //start the page
                startActivity(navigate)
            }

            //when button Exp/Inc is pressed
            R.id.btnExpInc -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Profile, Expense :: class.java)

                //start the page
                startActivity(navigate)
            }

            //when button profile is pressed
            R.id.btnProfile -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Profile, Profile :: class.java)

                //start the page
                startActivity(navigate)
            }

        }

    }

    fun loadTransactions() {
        thread {
            val data = getRows("user_ef3f2aac_transactions")

            runOnUiThread {
                if (data == null || data.length() == 0) {
                    println("No data found")
                    return@runOnUiThread
                }

                var totalIncome = 0.0
                var totalExpense = 0.0
                var incomeCount = 0
                var expenseCount = 0

                // Loop through all transactions
                for (i in 0 until data.length()) {
                    val row = data.getJSONObject(i)

                    val amount = row.optString("amount", "0").toDoubleOrNull() ?: 0.0
                    val transactionType = row.optString("transactionType", "").lowercase()

                    // Count and tally income transactions
                    if (transactionType == "income") {
                        totalIncome += amount
                        incomeCount++
                    }

                    // Count and tally expense transactions
                    if (transactionType == "expense") {
                        totalExpense += amount
                        expenseCount++
                    }

                }// end of for loop

                // ---------------------------------
                // MONTHLY BALANCE
                // Formula: Monthly Budget - (Income - Expense)
                // ---------------------------------
                val monthlyBalance = monthlyBudget - (totalIncome - totalExpense)

                // ---------------------------------
                // PROFIT / LOSS STATUS
                // Based on the result of monthly balance
                // Negative = under budget (profit), Positive = over budget (deficit)
                // ---------------------------------
                val status = when {
                    monthlyBalance < 0 -> "You are in deficit! "
                    monthlyBalance > 0 -> "You are in profit!"
                    else -> "Exactly on budget!"
                }

                // ---------------------------------
                // NUMBER OF INCOME TRANSACTIONS
                // ---------------------------------
                tvNumOfInc.text = incomeCount.toString()

                // ---------------------------------
                // NUMBER OF EXPENSE TRANSACTIONS
                // ---------------------------------
                tvNumOfExp.text = expenseCount.toString()

                // ---------------------------------
                // DISPLAY MONTHLY BALANCE
                // ---------------------------------
                tvMonthlyBalance.text = "R %.2f".format(monthlyBalance)

                // ---------------------------------
                // DISPLAY PROFIT / LOSS STATUS
                // ---------------------------------
                etProfLossStatus.text = status

            }// end of runOnUiThread

        }// end of thread

    }// end of loadTransactions

    fun setMonthlyBudget(view: View) {
        val input = etMonthlyBudget.text.toString()

        // Check if input is not empty before saving
        if (input.isNotEmpty()) {
            monthlyBudget = input.toDoubleOrNull() ?: 0.0
            saveMonthlyBudget(monthlyBudget)
            loadTransactions()
            Toast.makeText(this, "Monthly budget set to R %.2f".format(monthlyBudget), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
        }// end of if

    }// end of setMonthlyBudget

    fun saveMonthlyBudget(budget: Double) {
        thread {
            val rowData = mapOf(
                "monthlyBudget" to budget.toString()
            )

            val response = insertRow("user_ef3f2aac_budget", rowData)

            runOnUiThread {
                println(response)
            }// end of runOnUiThread

        }// end of thread

    }// end of saveMonthlyBudget
}