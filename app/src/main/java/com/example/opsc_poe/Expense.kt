package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import java.util.Locale


class Expense : AppCompatActivity(), View.OnClickListener {

    lateinit var btnHome : Button
    lateinit var btnExpInc : Button
    lateinit var btnProfile : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense)
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
    }
    fun saveTransaction(view: View){

        var amount : EditText = findViewById(R.id.etAmnt)
        var transType : EditText = findViewById(R.id.etTransactionType)
        var category: EditText = findViewById(R.id.etCategory)


        // -------------------------------
// HOW TO Insert, put this in a function or in the main under the override fun Oncreate
// -------------------------------
        thread {
            val rowData = mapOf(
                "amount" to amount.text.toString(),
                "transactionType" to transType.text.toString().lowercase(),
                "Category" to category.text.toString().lowercase() ,
            )

            val response = insertRow(
                tableName = "user_ef3f2aac_transactions",
                data = rowData
            )

            // Update UI on main thread
            runOnUiThread {
                println(response)
                // or show Toast / TextView
                Toast.makeText(this,response.toString(),Toast.LENGTH_LONG).show()
            }
        }

    }
    override fun onClick(v: View?){

        when(v?.id){

            //when button home is pressed
            R.id.btnHome -> {
                //navigate to home using intent
                //creating object for intent called navigate
                val navigate = Intent(this@Expense, Home :: class.java)//can just say 'this' instead of 'this@Home'

                //start the page
                startActivity(navigate)
            }

            //when button Exp/Inc is pressed
            R.id.btnExpInc -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Expense, Expense :: class.java)

                //start the page
                startActivity(navigate)
            }

            //when button profile is pressed
            R.id.btnProfile -> {
                //creating object for intent called navigate
                val navigate = Intent(this@Expense, Profile :: class.java)

                //start the page
                startActivity(navigate)
            }

        }

    }

    fun insertRow(
        tableName: String,
        data: Map< String, Any?>
    ): String? {
        val url = URL("https://studyplugtools.cloud/you_connect.php/$tableName/insert")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            // Build JSON payload (include nulls as empty strings if needed)
            val payload = JSONObject()
            for ((key, value) in data) {
                if (key != "id") {
                    payload.put(key, value ?: "")
                }
            }

            // Send JSON payload
            connection.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            // Read response
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
    }

}