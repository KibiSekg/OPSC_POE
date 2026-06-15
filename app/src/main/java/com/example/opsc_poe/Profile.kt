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

class Profile : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var ivEditPhoto: ImageView
    private lateinit var tvStreak: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var llBadges: LinearLayout
    private lateinit var btnLogout: MaterialButton

    // Camera / gallery
    private var photoUri: Uri? = null

    // ── Activity Result Launchers ─────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { saveAndShowAvatar(it) }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) photoUri?.let { saveAndShowAvatar(it) }
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else checkGalleryPermissionAndOpen()
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            executeGalleryIntent()
        } else {
            Toast.makeText(this, "Gallery permission is required to select photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setupNavigation(this, R.id.btnProfile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvUserName     = findViewById(R.id.tvUserName)
        tvUserEmail    = findViewById(R.id.tvUserEmail)
        ivAvatar       = findViewById(R.id.ivAvatar)
        ivEditPhoto    = findViewById(R.id.ivEditPhoto)
        tvStreak       = findViewById(R.id.tvStreak)
        tvExpenseCount = findViewById(R.id.tvExpenseCount)
        llBadges       = findViewById(R.id.llBadges)
        btnLogout      = findViewById(R.id.btnLogout)

        // Load session
        val session = getSharedPreferences("user_session", MODE_PRIVATE)
        tvUserName.text  = session.getString("user_name", "User") ?: "User"
        tvUserEmail.text = session.getString("user_email", "") ?: ""

        // Load saved profile picture
        val savedPath = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("profile_pic_path", null)
        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists()) {
                Glide.with(this).load(file).circleCrop().into(ivAvatar)
            }
        }

        // Photo picker listeners
        ivAvatar.setOnClickListener   { showPhotoPickerDialog() }
        ivEditPhoto.setOnClickListener { showPhotoPickerDialog() }

        btnLogout.setOnClickListener {
            getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshGamificationMetrics()
    }

    private fun refreshGamificationMetrics() {
        tvStreak.text = "🔥 ${GamificationManager.getStreak(this)}-day login streak"

        thread {
            val totalDbCount = AppDatabase.getDatabase(this).transactionDao().getAllTransactions().size
            val savedCount = GamificationManager.getExpenseCount(this)

            if (totalDbCount > savedCount) {
                val diff = totalDbCount - savedCount
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

    private fun showPhotoPickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(arrayOf("📷 Take Photo", "🖼️ Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> checkGalleryPermissionAndOpen()
                }
            }
            .show()
    }

    private fun checkGalleryPermissionAndOpen() {
        // Evaluate dynamic SDK string to safely circumvent media permission rule updates
        val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permissionNeeded) == PackageManager.PERMISSION_GRANTED) {
            executeGalleryIntent()
        } else {
            galleryPermissionLauncher.launch(permissionNeeded)
        }
    }

    private fun executeGalleryIntent() {
        galleryLauncher.launch("image/*")
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            photoUri?.let { safeUri -> cameraLauncher.launch(safeUri) }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun saveAndShowAvatar(uri: Uri) {
        try {
            val dest = File(filesDir, "profile_picture.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                .putString("profile_pic_path", dest.absolutePath)
                .apply()
            Glide.with(this).load(dest).circleCrop().into(ivAvatar)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Badges ────────────────────────────────────────────────────

    private fun renderBadges() {
        llBadges.removeAllViews()
        val unlocked = GamificationManager.getUnlockedBadges(this)
        Badge.values().forEach { badge ->
            val isUnlocked = unlocked.contains(badge)
            val tv = TextView(this)
            tv.text    = "${badge.emoji} ${badge.title} — ${badge.description}"
            tv.textSize = 14f
            tv.alpha   = if (isUnlocked) 1f else 0.35f
            tv.setPadding(0, 6, 0, 6)
            llBadges.addView(tv)
        }
    }
}