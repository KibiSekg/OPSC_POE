package com.example.opsc_poe

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe.db.AppDatabase
import com.example.opsc_poe.db.entities.Category
import com.example.opsc_poe.db.entities.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class Expense : AppCompatActivity() {

    // Form fields
    private lateinit var etTitle:          TextInputEditText
    private lateinit var etAmnt:           TextInputEditText
    private lateinit var etDate:           TextInputEditText
    private lateinit var spinnerTransType: AutoCompleteTextView
    private lateinit var spinnerCategory:  AutoCompleteTextView

    // TextInputLayout wrappers for inline validation errors
    private lateinit var tilTitle:    TextInputLayout
    private lateinit var tilAmount:   TextInputLayout
    private lateinit var tilType:     TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var tilDate:     TextInputLayout

    // Buttons
    private lateinit var btnAddCategory: MaterialButton
    private lateinit var btnTakePhoto:   MaterialButton
    private lateinit var btnPickPhoto:   MaterialButton
    private lateinit var btnThumbsUp:    MaterialButton
    private lateinit var btnThumbsDown:  MaterialButton

    // Photo preview
    private lateinit var flPhotoPreview: FrameLayout
    private lateinit var ivExpensePhoto: ImageView
    private lateinit var ivRemovePhoto:  ImageView

    // State
    private var selectedPhotoUri: Uri? = null
    private lateinit var cameraPhotoUri: Uri
    private var currentRating = "NONE"   // "THUMBS_UP" | "THUMBS_DOWN" | "NONE"

    // Default categories + extras loaded from DB
    private val categories = mutableListOf(
        "Food", "Transport", "Housing", "Utilities",
        "Entertainment", "Health", "Education", "Clothing", "Savings", "Other"
    )
    private lateinit var categoryAdapter: ArrayAdapter<String>

    // ── Activity result launchers ─────────────────────────────────

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ::cameraPhotoUri.isInitialized) {
            selectedPhotoUri = cameraPhotoUri
            showPhotoPreview(selectedPhotoUri!!)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            showPhotoPreview(uri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense)
        setupNavigation(this, R.id.btnExpInc)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Bind form fields
        etTitle          = findViewById(R.id.etTransactionTitle)
        etAmnt           = findViewById(R.id.etAmnt)
        etDate           = findViewById(R.id.etTransactionDate)
        spinnerTransType = findViewById(R.id.spinnerTransType)
        spinnerCategory  = findViewById(R.id.spinnerCategory)

        // Resolve TextInputLayout parents (EditText → FrameLayout → TextInputLayout)
        tilTitle    = etTitle.parent.parent          as TextInputLayout
        tilAmount   = etAmnt.parent.parent           as TextInputLayout
        tilType     = spinnerTransType.parent.parent as TextInputLayout
        tilCategory = spinnerCategory.parent.parent  as TextInputLayout
        tilDate     = etDate.parent.parent           as TextInputLayout

        // Bind buttons
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnTakePhoto   = findViewById(R.id.btnTakePhoto)
        btnPickPhoto   = findViewById(R.id.btnPickPhoto)
        btnThumbsUp    = findViewById(R.id.btnThumbsUp)
        btnThumbsDown  = findViewById(R.id.btnThumbsDown)

        // Bind photo preview
        flPhotoPreview = findViewById(R.id.flPhotoPreview)
        ivExpensePhoto = findViewById(R.id.ivExpensePhoto)
        ivRemovePhoto  = findViewById(R.id.ivRemovePhoto)

        setupTypeDropdown()
        setupCategoryDropdown()
        setupDatePicker()
        setupPhotoButtons()
        setupRatingButtons()
        loadCustomCategoriesFromDb()
    }

    // ── Transaction type dropdown ─────────────────────────────────

    private fun setupTypeDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            listOf("Income", "Expense")
        )
        spinnerTransType.setAdapter(adapter)
        spinnerTransType.inputType = 0   // block soft keyboard — dropdown only
    }

    // ── Category dropdown ─────────────────────────────────────────

    private fun setupCategoryDropdown() {
        categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        spinnerCategory.setAdapter(categoryAdapter)
        spinnerCategory.inputType = 0   // dropdown only

        btnAddCategory.setOnClickListener {
            AddCategoryBottomSheet { newName ->
                if (!categories.contains(newName)) {
                    categories.add(newName)
                    categoryAdapter.notifyDataSetChanged()
                    // Persist to Room so it survives restarts
                    thread {
                        AppDatabase.getDatabase(this)
                            .categoryDao()
                            .insertCategory(Category(name = newName))
                    }
                }
                spinnerCategory.setText(newName, false)
            }.show(supportFragmentManager, "AddCategorySheet")
        }
    }

    private fun loadCustomCategoriesFromDb() {
        thread {
            val dbCats = AppDatabase.getDatabase(this).categoryDao().getAllCategories()
            runOnUiThread {
                dbCats.forEach { cat ->
                    if (!categories.contains(cat.name)) {
                        categories.add(cat.name)
                    }
                }
                categoryAdapter.notifyDataSetChanged()
            }
        }
    }

    // ── Date picker ───────────────────────────────────────────────

    private fun setupDatePicker() {
        etDate.isFocusable = false
        etDate.setOnClickListener { openDatePicker() }
        tilDate.setEndIconOnClickListener { openDatePicker() }
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                etDate.setText("%04d-%02d-%02d".format(year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ── Photo buttons ─────────────────────────────────────────────

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnPickPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        ivRemovePhoto.setOnClickListener {
            selectedPhotoUri = null
            flPhotoPreview.visibility = View.GONE
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "expense_${System.currentTimeMillis()}",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        cameraPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(cameraPhotoUri)
    }

    private fun showPhotoPreview(uri: Uri) {
        flPhotoPreview.visibility = View.VISIBLE
        ivExpensePhoto.setImageURI(uri)
    }

    // ── Rating buttons ────────────────────────────────────────────

    private val colorGreen = 0xFF4CAF50.toInt()
    private val colorRed   = 0xFFF44336.toInt()
    private val colorBlue  = 0xFF216999.toInt()
    private val colorWhite = 0xFFFFFFFF.toInt()

    private fun setupRatingButtons() {
        btnThumbsUp.setOnClickListener   { selectRating("THUMBS_UP") }
        btnThumbsDown.setOnClickListener { selectRating("THUMBS_DOWN") }
    }

    private fun selectRating(rating: String) {
        currentRating = rating

        btnThumbsUp.backgroundTintList   =
            if (rating == "THUMBS_UP") android.content.res.ColorStateList.valueOf(colorGreen) else null
        btnThumbsDown.backgroundTintList =
            if (rating == "THUMBS_DOWN") android.content.res.ColorStateList.valueOf(colorRed) else null

        btnThumbsUp.setTextColor(if (rating == "THUMBS_UP") colorWhite else colorBlue)
        btnThumbsDown.setTextColor(if (rating == "THUMBS_DOWN") colorWhite else colorBlue)
    }

    // ── Save ──────────────────────────────────────────────────────

    fun saveTransaction(view: View) {
        val title    = etTitle.text.toString().trim()
        val amount   = etAmnt.text.toString().trim()
        val type     = spinnerTransType.text.toString().trim()
        val category = spinnerCategory.text.toString().trim()
        val date     = etDate.text.toString().trim()

        tilTitle.error    = null
        tilAmount.error   = null
        tilType.error     = null
        tilCategory.error = null
        tilDate.error     = null

        var hasError = false

        if (title.isEmpty()) {
            tilTitle.error = "Title is required"
            hasError = true
        }
        if (amount.isEmpty()) {
            tilAmount.error = "Amount is required"
            hasError = true
        } else if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            tilAmount.error = "Enter a valid amount greater than 0"
            hasError = true
        }
        if (type.isEmpty()) {
            tilType.error = "Please select Income or Expense"
            hasError = true
        }
        if (category.isEmpty()) {
            tilCategory.error = "Please select a category"
            hasError = true
        }
        if (date.isEmpty()) {
            tilDate.error = "Please pick a date"
            hasError = true
        }

        if (hasError) return

        thread {
            val last = AppDatabase.getDatabase(this)
                .transactionDao()
                .getLastTransactionByTitle(title)

            runOnUiThread {
                if (last != null && last.rating == "THUMBS_DOWN") {
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Previous Dislike")
                        .setMessage(
                            "Last time you added \"$title\" you marked it as 👎 Unsatisfied.\n\nDo you still want to add it?"
                        )
                        .setPositiveButton("Yes, add it") { _, _ ->
                            persistTransaction(title, amount, type, category, date)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    persistTransaction(title, amount, type, category, date)
                }
            }
        }
    }

    private fun persistTransaction(
        title: String, amount: String, type: String, category: String, date: String
    ) {
        var finalSavedPath: String? = null

        selectedPhotoUri?.let { uri ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val targetFile = File(filesDir, "RECEIPT_${timestamp}.jpg")

                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                finalSavedPath = targetFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val tx = Transaction(
            title           = title,
            amount          = amount.toDouble(),
            transactionType = type.lowercase(),
            category        = category.lowercase(),
            date            = date,
            imagePath       = finalSavedPath,
            rating          = currentRating
        )

        thread {
            AppDatabase.getDatabase(this).transactionDao().insertTransaction(tx)
            val newBadges = GamificationManager.recordExpense(this)

            runOnUiThread {
                Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()
                newBadges.forEach { badge ->
                    Toast.makeText(
                        this, "${badge.emoji} Badge unlocked: ${badge.title}!", Toast.LENGTH_LONG
                    ).show()
                }
                clearForm()
            }
        }
    }

    private fun clearForm() {
        etTitle.text?.clear()
        etAmnt.text?.clear()
        etDate.text?.clear()
        spinnerTransType.text.clear()
        spinnerCategory.text.clear()
        selectedPhotoUri = null
        flPhotoPreview.visibility = View.GONE
        currentRating = "NONE"
        btnThumbsUp.backgroundTintList   = null
        btnThumbsUp.setTextColor(colorBlue)
        btnThumbsDown.backgroundTintList = null
        btnThumbsDown.setTextColor(colorBlue)
        tilTitle.error    = null
        tilAmount.error   = null
        tilType.error     = null
        tilCategory.error = null
        tilDate.error     = null
    }
}