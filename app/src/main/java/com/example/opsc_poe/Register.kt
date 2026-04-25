package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun registerUser(view: View){

        var name : EditText = findViewById(R.id.etName)
        var email : EditText = findViewById(R.id.etEmail)
        var password : EditText = findViewById(R.id.etPassword)

        // -------------------------------
// HOW TO Insert, put this in a function or in the main under the override fun Oncreate
// -------------------------------
        thread {
            val rowData = mapOf(
                "name" to name.text.toString(),
                "email" to email.text.toString(),
                "password" to password.text.toString(),
            )

            val response = insertRow(
                tableName = "user_ef3f2aac_poeUsers",
                data = rowData
            )

            // Update UI on main thread
            runOnUiThread {
                println(response)
                // or show Toast / TextView
                Toast.makeText(this,response.toString(),Toast.LENGTH_LONG).show()
            }
        }

    }//end registerUserFunction


    //Function to navigate to Login page when Login button is pressed
    fun navigateToLogin(view: View) {
        //navigating to Login page using intent
        //creating object for intent called navigate
        val navigate = Intent(this@Register, Login :: class.java)//can just say 'this' instead of 'this@Register'

        //start the next page
        startActivity(navigate)
    }//end of navigateToLogin function

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
    }// end insertRow Function
}