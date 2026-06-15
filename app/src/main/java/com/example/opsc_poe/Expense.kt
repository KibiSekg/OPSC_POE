package com.example.opsc_poe

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
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

    companion object {
        private const val TAG = "ExpenseActivity"
    }

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

    // State management variables
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
            Log.d(TAG, "cameraLauncher: Image captured successfully. URI: $cameraPhotoUri")
            selectedPhotoUri = cameraPhotoUri
            showPhotoPreview(selectedPhotoUri!!)
        } else {
            Log.e(TAG, "cameraLauncher: Camera capture failed or photo URI not properly initialized.")
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Log.d(TAG, "galleryLauncher: Selected image URI picked from storage: $uri")
            selectedPhotoUri = uri
            showPhotoPreview(uri)
        } else {
            Log.d(TAG, "galleryLauncher: User cancelled picking an image from storage.")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "cameraPermissionLauncher: Camera access GRANTED by user.")
            launchCamera()
        } else {
            Log.e(TAG, "cameraPermissionLauncher: Camera access DENIED by user.")
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "galleryPermissionLauncher: Storage access GRANTED by user.")
            executeGalleryIntent()
        } else {
            Log.e(TAG, "galleryPermissionLauncher: Storage access DENIED by user.")
            Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Expense activity forms")
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
            Log.d(TAG, "btnAddCategory clicked: Displaying sheet dialog to append custom types")
            AddCategoryBottomSheet { newName ->
                if (!categories.contains(newName)) {
                    Log.d(TAG, "New custom category provided: '$newName'. Adding to current adapter cache.")
                    categories.add(newName)
                    categoryAdapter.notifyDataSetChanged()

                    // Persist to Room so it survives restarts
                    thread {
                        Log.d(TAG, "Saving '$newName' custom entry into SQLite Database context.")
                        AppDatabase.getDatabase(this)
                            .categoryDao()
                            .insertCategory(Category(name = newName))
                    }
                } else {
                    Log.d(TAG, "Custom category fallback triggered: '$newName' already exists in list.")
                }
                spinnerCategory.setText(newName, false)
            }.show(supportFragmentManager, "AddCategorySheet")
        }
    }

    private fun loadCustomCategoriesFromDb() {
        thread {
            Log.d(TAG, "loadCustomCategoriesFromDb: Pulling extended tables info from DB asynchronously.")
            val dbCats = AppDatabase.getDatabase(this).categoryDao().getAllCategories()
            runOnUiThread {
                var addedCount = 0
                dbCats.forEach { cat ->
                    if (!categories.contains(cat.name)) {
                        categories.add(cat.name)
                        addedCount++
                    }
                }
                Log.d(TAG, "Successfully injected $addedCount custom external rows into dropdown array adapter.")
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
                val formattedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                Log.d(TAG, "Date picked from native UI prompt: $formattedDate")
                etDate.setText(formattedDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ── Photo buttons ─────────────────────────────────────────────

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener {
            Log.d(TAG, "btnTakePhoto clicked. Evaluating hardware camera access profiles.")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                Log.d(TAG, "Camera authorization required. Initializing runtime prompt handshake.")
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnPickPhoto.setOnClickListener {
            Log.d(TAG, "btnPickPhoto clicked. Evaluating storage read permission profiles.")
            checkGalleryPermissionAndOpen()
        }

        ivRemovePhoto.setOnClickListener {
            Log.d(TAG, "ivRemovePhoto clicked: Discarding active attachment URI pointer selection.")
            selectedPhotoUri = null
            flPhotoPreview.visibility = View.GONE
        }
    }

    private fun checkGalleryPermissionAndOpen() {
        val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permissionNeeded) == PackageManager.PERMISSION_GRANTED) {
            executeGalleryIntent()
        } else {
            Log.d(TAG, "Storage authorization required ($permissionNeeded). Launching execution interceptor.")
            galleryPermissionLauncher.launch(permissionNeeded)
        }
    }

    private fun executeGalleryIntent() {
        Log.d(TAG, "Launching system image picker pipeline intent filter.")
        galleryLauncher.launch("image/*")
    }

    private fun launchCamera() {
        try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs()
            }

            val photoFile = File.createTempFile(
                "expense_${System.currentTimeMillis()}",
                ".jpg",
                storageDir
            )

            cameraPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            Log.d(TAG, "Temporary camera target file created: ${photoFile.absolutePath}")
            cameraLauncher.launch(cameraPhotoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error building cache storage file descriptors for native camera application allocation layer", e)
            Toast.makeText(this, "Failed to initialize camera cache destination.", Toast.LENGTH_SHORT).show()
        }
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
        Log.d(TAG, "Satisfaction flag tracking selection changed: '$rating'")
        currentRating = rating

        btnThumbsUp.backgroundTintList =
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

        Log.d(TAG, "saveTransaction workflow initialized. Evaluating entry values validations.")

        // Reset text field wrappers
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

        if (hasError) {
            Log.e(TAG, "Validation failed: Transaction creation aborted due to missing or invalid fields.")
            return
        }

        thread {
            Log.d(TAG, "Querying Room DB for historical metrics linked to title: '$title'")
            val last = AppDatabase.getDatabase(this)
                .transactionDao()
                .getLastTransactionByTitle(title)

            runOnUiThread {
                // Intercept execution loop if user previously flagged an item as unsatisfactory
                if (last != null && last.rating == "THUMBS_DOWN") {
                    Log.d(TAG, "Matching negative confirmation loop found for name: '$title'. Intercepting with Alert verification.")
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Previous Dislike")
                        .setMessage(
                            "Last time you added \"$title\" you marked it as 👎 Unsatisfied.\n\nDo you still want to add it?"
                        )
                        .setPositiveButton("Yes, add it") { _, _ ->
                            Log.d(TAG, "User bypassed alert dialog constraints manually.")
                            persistTransaction(title, amount, type, category, date)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            Log.d(TAG, "Transaction creation canceled by user after warning dialogue.")
                        }
                        .show()
                } else {
                    persistTransaction(title, amount, type, category, date)
                }
            }
        }
    }

    /**
     * Handles local image cloning execution into protected internal files storage slots,
     * builds data transfer tokens, and saves records into SQLite.
     */
    private fun persistTransaction(
        title: String, amount: String, type: String, category: String, date: String
    ) {
        var finalSavedPath: String? = null

        selectedPhotoUri?.let { uri ->
            try {
                Log.d(TAG, "Active image attachment located. Exporting content wrapper data stream to app storage workspace.")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val targetFile = File(filesDir, "RECEIPT_${timestamp}.jpg")

                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                finalSavedPath = targetFile.absolutePath
                Log.d(TAG, "Receipt local storage copy successfully processed: $finalSavedPath")
            } catch (e: Exception) {
                Log.e(TAG, "IOException caught trying to replicate targeted metadata attachment data streams", e)
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
            Log.d(TAG, "Writing entry structures directly to app database layer logs.")
            AppDatabase.getDatabase(this).transactionDao().insertTransaction(tx)

            Log.d(TAG, "Invoking Gamification evaluation routines based on user updates.")
            val newBadges = GamificationManager.recordExpense(this)

            runOnUiThread {
                Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()
                newBadges.forEach { badge ->
                    Log.d(TAG, "Gamification Alert -> New award unlocked: ${badge.title}")
                    Toast.makeText(
                        this, "${badge.emoji} Badge unlocked: ${badge.title}!", Toast.LENGTH_LONG
                    ).show()
                }
                clearForm()
            }
        }
    }

    /**
     * Flushes active string modifications and returns form inputs and rating selections back to initial defaults.
     */
    private fun clearForm() {
        Log.d(TAG, "Flushing form changes. Re-indexing inputs to default empty states.")
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