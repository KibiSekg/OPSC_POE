package com.example.opsc_poe

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class Expense : AppCompatActivity() {

    private lateinit var etAmnt: TextInputEditText
    private lateinit var spinnerTransType: AutoCompleteTextView
    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var etTransactionDate: TextInputEditText

    // Photo preview bindings
    private lateinit var flPhotoPreview: FrameLayout
    private lateinit var ivExpensePhoto: ImageView
    private lateinit var ivRemovePhoto: ImageView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnPickPhoto: MaterialButton

    // To hold selected photo location. To fix type mismatch,
    // it remains a Uri here, but gets converted to String during saving.
    private var selectedImageUri: Uri? = null
    private val calendar = Calendar.getInstance()

    // Gallery Picker Contract
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                // Resolve type mismatch at runtime: ImageView accepts a direct Uri object safely
                ivExpensePhoto.setImageURI(uri)
                flPhotoPreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)
        setupNavigation(this, R.id.btnExpInc)

        // Initialize Views
        etAmnt = findViewById(R.id.etAmnt)
        spinnerTransType = findViewById(R.id.spinnerTransType)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etTransactionDate = findViewById(R.id.etTransactionDate)

        flPhotoPreview = findViewById(R.id.flPhotoPreview)
        ivExpensePhoto = findViewById(R.id.ivExpensePhoto)
        ivRemovePhoto = findViewById(R.id.ivRemovePhoto)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnPickPhoto = findViewById(R.id.btnPickPhoto)

        // Setup Dropdowns
        val types = arrayOf("income", "expense")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        spinnerTransType.setAdapter(typeAdapter)

        val categories = arrayOf("Salary", "Food", "Transport", "Rent", "Groceries", "Entertainment")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(catAdapter)

        // Photo Click Listeners
        btnPickPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        ivRemovePhoto.setOnClickListener {
            selectedImageUri = null
            ivExpensePhoto.setImageURI(null)
            flPhotoPreview.visibility = View.GONE
        }
    }

    fun showDatePickerFromXml(view: View) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etTransactionDate.setText(sdf.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Called from XML android:onClick="saveTransaction"
    fun saveTransaction(view: View) {
        val amountStr = etAmnt.text.toString().trim()
        val type = spinnerTransType.text.toString()
        val category = spinnerCategory.text.toString()
        val date = etTransactionDate.text.toString()

        if (amountStr.isEmpty() || type.isEmpty() || category.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill out all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0

        // CRITICAL FIX: To prevent an Argument Type Mismatch, convert the Uri format
        // to a String representation using safe navigation (?.) or pass null if nothing is attached.
        val imagePathString: String? = selectedImageUri?.toString()

        val transaction = Transaction(
            amount = amount,
            transactionType = type,
            category = category,
            date = date,
            imagePath = imagePathString // Successfully matches data contract schema string parameter
        )

        thread {
            AppDatabase.getDatabase(applicationContext).transactionDao().insertTransaction(transaction)
            runOnUiThread {
                Toast.makeText(this, "Transaction logged safely!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}