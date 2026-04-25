package com.example.opsc_poe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
}