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
//import them at the top of the code
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class Home : AppCompatActivity(), View.OnClickListener {

    lateinit var btnHome : Button
    lateinit var btnExpInc : Button
    lateinit var btnProfile : Button
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnHome = findViewById(R.id.btnHome)
        btnExpInc = findViewById(R.id.btnExpInc)
        btnProfile = findViewById(R.id.btnProfile)

        btnHome.setOnClickListener(this)
        btnExpInc.setOnClickListener(this)
        btnProfile.setOnClickListener(this)

        etMonthlyGoal = findViewById(R.id.etMonthlyGoal)
        etTotalBalance = findViewById(R.id.etTotalBalance)
        etTotalIncome = findViewById(R.id.etTotalIncome)
        etTotalExp = findViewById(R.id.etTotalExp)
        tvTopCategory = findViewById(R.id.tvTopCategory)
        tvDailyAvg = findViewById(R.id.tvDailyAvg)

        loadTransactions()
        loadMonthlyBudget()

    }//end of onCreate function

    fun loadMonthlyBudget() {
        thread {
            val data = getRows("user_ef3f2aac_budget")

            runOnUiThread {
                if (data == null || data.length() == 0) {
                    // No budget set yet, keep default text
                    etMonthlyGoal.text = "R 0.00 - No Budget Set"
                    return@runOnUiThread
                }// end of if

                // Get the last row which is the most recently set budget
                val lastRow = data.getJSONObject(data.length() - 1)
                val budget = lastRow.optString("monthlyBudget", "0")

                // Display the monthly budget on the home screen
                etMonthlyGoal.text = "R %.2f".format(budget.toDoubleOrNull() ?: 0.0)

            }// end of runOnUiThread

        }// end of thread

    }// end of loadMonthlyBudget

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
                val categoryCount = mutableMapOf<String, Int>()

                // Loop through all rows
                for (i in 0 until data.length()) {
                    val row = data.getJSONObject(i)

                    val amount = row.optString("amount", "0").toDoubleOrNull() ?: 0.0
                    val transactionType = row.optString("transactionType", "").lowercase()
                    val category = row.optString("Category", "").lowercase()

                    // ---------------------------------
                    // TOTAL INCOME & TOTAL EXPENSE
                    // If transactionType is "income", add to totalIncome
                    // If transactionType is "expense", add to totalExpense
                    // ---------------------------------
                    if (transactionType == "income") {
                        totalIncome += amount
                    } else if (transactionType == "expense") {
                        totalExpense += amount
                    }

                    // ---------------------------------
                    // TOP CATEGORY
                    // Count how many times each category appears
                    // The one with the highest count is the top category
                    // ---------------------------------
                    if (category.isNotEmpty()) {
                        categoryCount[category] = (categoryCount[category] ?: 0) + 1
                    }

                }// end of for loop

                // ---------------------------------
                // TOTAL BALANCE
                // Balance = Total Income - Total Expense
                // ---------------------------------
                val totalBalance = totalIncome - totalExpense

                // ---------------------------------
                // TOP CATEGORY
                // Get the category with the highest count from the map
                // ---------------------------------
                val topCategory = categoryCount.maxByOrNull { it.value }?.key ?: "No Category"

                // ---------------------------------
                // DAILY AVERAGE
                // Count expense rows, then divide total expense by that count
                // ---------------------------------
                val expenseCount = (1..data.length())
                    .map { data.getJSONObject(it - 1) }
                    .count { it.optString("transactionType", "").lowercase() == "expense" }
                val dailyAvg = if (expenseCount > 0) totalExpense / expenseCount else 0.0

                // ---------------------------------
                // UPDATE UI - Display all calculated values on screen
                // ---------------------------------

                // Shows total income on the home screen
                etTotalIncome.text = "R %.2f".format(totalIncome)

                // Shows total expense on the home screen
                etTotalExp.text = "R %.2f".format(totalExpense)

                // Shows total balance (income - expense) on the home screen
                etTotalBalance.text = "R %.2f".format(totalBalance)

                // Shows the most used category on the home screen
                tvTopCategory.text = topCategory

                // Shows the daily average expense on the home screen
                tvDailyAvg.text = if (dailyAvg > 0) "R %.2f".format(dailyAvg) else "R 0.00 - No Expenses"

            }// end of runOnUiThread

        }// end of thread

    }// end of loadTransactions

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

    override fun onClick(v: View?){

        when(v?.id){

            //when button home is pressed
            R.id.btnHome -> {
                //navigate to home using intent
                //creating object for intent called navigate
                val navigate = Intent(this@Home, Home :: class.java)//can just say 'this' instead of 'this@Home'

                //start the page
                startActivity(navigate)
            }

            //when button Exp/Inc is pressed
            R.id.btnExpInc -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Home, Expense :: class.java)

                //start the page
                startActivity(navigate)
            }

            //when button profile is pressed
            R.id.btnProfile -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Home, Profile :: class.java)

                //start the page
                startActivity(navigate)
            }// end of onClick function

        }

    }
}