package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
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

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun loginUser(view: View){
        var email : EditText = findViewById(R.id.etEmail)
        var password : EditText = findViewById(R.id.etPassword)

        thread {
            val data = getRows("user_ef3f2aac_poeUsers")

            runOnUiThread {
                if (data == null || data.length() == 0) {
                    Toast.makeText(this, "Error connecting to server", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                var userFound = false

                // Loop through all users and check for a match
                for (i in 0 until data.length()) {
                    val row = data.getJSONObject(i)

                    val dbEmail = row.getString("email")
                    val dbPassword = row.getString("password")

                    if (dbEmail == email.text.toString() && dbPassword == password.text.toString()) {
                        userFound = true
                        break
                    }
                }

                if (userFound) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_LONG).show()
                    val navigate = Intent(this@Login, Home::class.java) // replace with your home activity
                    startActivity(navigate)
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
                }
            }
        }


    }//end of loginUser function

    fun navigateToRegister(view: View) {

        //navigating to register page using intent
        //creating object for intent called navigate
        val navigate = Intent(this@Login, Register :: class.java)//can just say 'this' instead of 'this@Login'

        //start the next page
        startActivity(navigate)
    }// end navigateToRegister Function

    // FUNCTION: Get all rows from table
    fun getRows(tableName: String): JSONArray? {

        val url = URL("https://studyplugtools.cloud/you_connect.php/$tableName/get")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            // Send empty payload (same as PHP example)
            connection.outputStream.use { os ->
                os.write("{}".toByteArray(Charsets.UTF_8))
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

            JSONArray(response.toString())

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection.disconnect()
        }
    }



}