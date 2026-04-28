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
import kotlin.concurrent.thread


class Profile : AppCompatActivity(), View.OnClickListener {

    lateinit var btnHome : Button
    lateinit var btnExpInc : Button
    lateinit var btnProfile : Button
    lateinit var btnSetMonthlyBudg: Button

    lateinit var tvProfileId: TextView
    lateinit var tvMonthlyBalance: TextView
    lateinit var etProfLossStatus: TextView
    lateinit var tvNumOfInc: TextView
    lateinit var tvNumOfExp: TextView
    lateinit var etMonthlyBudget: EditText

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

        tvProfileId = findViewById(R.id.tvProfileId)
        tvMonthlyBalance = findViewById(R.id.tvMonthlyBalance)
        etProfLossStatus = findViewById(R.id.etProfLossStatus)
        tvNumOfInc = findViewById(R.id.tvNumOfInc)
        tvNumOfExp = findViewById(R.id.tvNumOfExp)
        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)

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

        // Load transactions when page opens
        loadTransactions()

    }//end OnCreate

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
                "monthlyBudget" to budget.toString(),
                "month" to java.util.Calendar.getInstance()
                    .getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()),
                "year" to java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            )

            val response = insertRow("user_ef3f2aac_budget", rowData)

            runOnUiThread {
                println(response)
            }// end of runOnUiThread

        }// end of thread

    }// end of saveMonthlyBudget
}