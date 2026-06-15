package com.example.opsc_poe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log // Imported Android Log utility
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.opsc_poe.db.AppDatabase
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Activity presenting user account data metrics, game design assets,
 * current continuous streak counters, and avatar picture update pipelines.
 */
class Profile : AppCompatActivity() {

    companion object {
        private const val TAG = "ProfileActivity"
    }

    // View Components
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var ivEditPhoto: ImageView
    private lateinit var tvStreak: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var llBadges: LinearLayout
    private lateinit var btnLogout: MaterialButton

    // Internal Camera URI target pointer
    private var photoUri: Uri? = null

    // ── Activity Result Launchers ─────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Log.d(TAG, "galleryLauncher: Avatar image selected from storage file picker. URI: $uri")
            saveAndShowAvatar(uri)
        } else {
            Log.d(TAG, "galleryLauncher: Selection dropped by user.")
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { safeUri ->
                Log.d(TAG, "cameraLauncher: Image capture success. Persisting target URI: $safeUri")
                saveAndShowAvatar(safeUri)
            }
        } else {
            Log.e(TAG, "cameraLauncher: Hard copy snapshot capturing task broken or timed out.")
        }
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "cameraPermission: Hardware access initialization authorization GRANTED.")
            launchCamera()
        } else {
            Log.w(TAG, "cameraPermission: Camera access DENIED. Falling back to storage permission prompt.")
            checkGalleryPermissionAndOpen()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d(TAG, "galleryPermissionLauncher: Storage access verification checks GRANTED.")
            executeGalleryIntent()
        } else {
            Log.e(TAG, "galleryPermissionLauncher: Storage layer authorization explicitly DENIED.")
            Toast.makeText(this, "Gallery permission is required to select photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing user account profile layouts.")
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setupNavigation(this, R.id.btnProfile)

        // Setup WindowInsets layout window margins safely avoiding display cutouts
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind layouts elements
        tvUserName     = findViewById(R.id.tvUserName)
        tvUserEmail    = findViewById(R.id.tvUserEmail)
        ivAvatar       = findViewById(R.id.ivAvatar)
        ivEditPhoto    = findViewById(R.id.ivEditPhoto)
        tvStreak       = findViewById(R.id.tvStreak)
        tvExpenseCount = findViewById(R.id.tvExpenseCount)
        llBadges       = findViewById(R.id.llBadges)
        btnLogout      = findViewById(R.id.btnLogout)

        // Pull credential details matching active user session parameters
        val session = getSharedPreferences("user_session", MODE_PRIVATE)
        tvUserName.text  = session.getString("user_name", "User") ?: "User"
        tvUserEmail.text = session.getString("user_email", "") ?: ""

        // Fetch local disk references containing existing custom avatars
        val savedPath = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("profile_pic_path", null)

        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists()) {
                Log.d(TAG, "Avatar found inside application storage space. Path: ${file.absolutePath}")
                Glide.with(this).load(file).circleCrop().into(ivAvatar)
            } else {
                Log.w(TAG, "Avatar configuration mismatch: Path reference points to a non-existent file.")
            }
        }

        // Attach listeners onto image display elements to prompt profile updates
        ivAvatar.setOnClickListener   { showPhotoPickerDialog() }
        ivEditPhoto.setOnClickListener { showPhotoPickerDialog() }

        btnLogout.setOnClickListener {
            Log.i(TAG, "Logout sequence triggered. Flushing session preferences strings.")
            getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Profile view gains structural focus. Executing data sync cycles.")
        refreshGamificationMetrics()
    }

    /**
     * Re-allocates totals matching SQLite entry tables to verify synchronization metrics
     * against stored key values inside local SharedPreferences.
     */
    private fun refreshGamificationMetrics() {
        tvStreak.text = "🔥 ${GamificationManager.getStreak(this)}-day login streak"

        thread {
            Log.d(TAG, "refreshGamificationMetrics: Validating tracking statistics asynchronously against DB counts.")
            val totalDbCount = AppDatabase.getDatabase(this).transactionDao().getAllTransactions().size
            val savedCount = GamificationManager.getExpenseCount(this)

            // Dynamic tracking data alignment repair segment
            if (totalDbCount > savedCount) {
                val diff = totalDbCount - savedCount
                Log.w(TAG, "refreshGamificationMetrics: Statistical drift located. DB rows: $totalDbCount, Stored Pref counts: $savedCount. Resolving gap of $diff records.")

                val prefs = getSharedPreferences("spend_smart_gamification", Context.MODE_PRIVATE)
                prefs.edit().putInt("total_expense_count", totalDbCount).apply()

                for (i in 0 until diff) {
                    GamificationManager.recordExpense(this)
                }
            }

            runOnUiThread {
                tvExpenseCount.text = "📊 $totalDbCount transactions logged"
                renderBadges()
            }
        }
    }

    // ── Photo handling ────────────────────────────────────────────

    /**
     * Renders selection prompts separating camera attachments from media galleries.
     */
    private fun showPhotoPickerDialog() {
        Log.d(TAG, "showPhotoPickerDialog: Displaying asset ingestion options dialog.")
        AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(arrayOf("📷 Take Photo", "🖼️ Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> {
                        Log.d(TAG, "User opted to launch device system camera.")
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            Log.d(TAG, "Requesting runtime authorization permissions for Manifest.permission.CAMERA")
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        Log.d(TAG, "User opted to launch internal media pickers.")
                        checkGalleryPermissionAndOpen()
                    }
                }
            }
            .show()
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
            Log.d(TAG, "Requesting runtime storage reading authorizations ($permissionNeeded).")
            galleryPermissionLauncher.launch(permissionNeeded)
        }
    }

    private fun executeGalleryIntent() {
        Log.d(TAG, "Invoking system storage provider routing filters.")
        galleryLauncher.launch("image/*")
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            Log.d(TAG, "launchCamera: Target internal media caching file initialized: ${file.absolutePath}")
            photoUri?.let { safeUri -> cameraLauncher.launch(safeUri) }
        } catch (e: Exception) {
            Log.e(TAG, "Exception encountered trying to spin up system camera processing components.", e)
            Toast.makeText(this, "Failed to initialize camera cache destination.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("PROFILE_${timestamp}_", ".jpg", storageDir)
    }

    /**
     * Clips stream blocks targeting external image allocations and duplicates them into
     * private application subdirectories before registering location keys into shared references.
     */
    private fun saveAndShowAvatar(uri: Uri) {
        try {
            Log.d(TAG, "saveAndShowAvatar: Writing media resource streams into private app workspace.")
            val dest = File(filesDir, "profile_picture.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }

            Log.d(TAG, "Avatar image processing file operation successful. Absolute destination: ${dest.absolutePath}")
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                .putString("profile_pic_path", dest.absolutePath)
                .apply()

            Glide.with(this).load(dest).circleCrop().into(ivAvatar)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal data parsing error attempting to transfer designated profile source image content", e)
            Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Badges ────────────────────────────────────────────────────

    /**
     * Loops through system achievement enums and constructs programmatic display views,
     * applying opacity changes to visually flag locked awards.
     */
    private fun renderBadges() {
        llBadges.removeAllViews()
        val unlocked = GamificationManager.getUnlockedBadges(this)
        Log.d(TAG, "renderBadges: Populating user medals. Unlocked count: ${unlocked.size}/${Badge.values().size}")

        Badge.values().forEach { badge ->
            val isUnlocked = unlocked.contains(badge)
            val tv = TextView(this)
            tv.text    = "${badge.emoji} ${badge.title} — ${badge.description}"
            tv.textSize = 14f

            // Apply visual transparency filters to accurately signal unlocking progress metrics
            tv.alpha   = if (isUnlocked) 1f else 0.35f
            tv.setPadding(0, 6, 0, 6)
            llBadges.addView(tv)
        }
    }
}