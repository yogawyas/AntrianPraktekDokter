package com.example.antrianpraktekdokter.patient

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.auth.LoginActivity // Import LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    // UI Components
    private lateinit var imgProfile: ImageView
    private lateinit var edtName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var btnChangePhoto: ImageButton
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var btnLogout: MaterialButton // Variabel Logout

    // Navigasi
    private lateinit var bottomNav: BottomNavigationView

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Variables for Camera/Gallery
    private var currentPhotoPath: String? = null
    private var encodedImage: String? = null

    // Permission & Activity Launchers
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startCamera() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && currentPhotoPath != null) {
            val file = File(currentPhotoPath!!)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgProfile.setImageBitmap(bitmap)
            encodedImage = encodeImage(bitmap)
        }
    }

    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                imgProfile.setImageURI(imageUri)
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    encodedImage = encodeImage(bitmap)
                } catch (e: IOException) { e.printStackTrace() }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Init Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // --- INISIALISASI VIEW (FIXED) ---
        imgProfile = findViewById(R.id.imgProfile)

        try { tilName = findViewById(R.id.tilName) } catch (e: Exception) { }

        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnBack = findViewById(R.id.btnBack)
        bottomNav = findViewById(R.id.bottomNavigation)

        // PENTING: Inisialisasi tombol Logout
        btnLogout = findViewById(R.id.btnLogout)

        // 1. Load Data
        loadProfile()

        // 2. Logic Tombol Back
        btnBack.setOnClickListener { kembaliKeHome() }

        // 3. Logic Edit Nama
        if (::tilName.isInitialized) {
            tilName.setEndIconOnClickListener {
                edtName.isEnabled = true
                edtName.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edtName, InputMethodManager.SHOW_IMPLICIT)
                Toast.makeText(this, "You can now edit your name", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Logic Ganti Foto
        btnChangePhoto.setOnClickListener { showImagePickerOptions() }

        // 5. Logic Simpan Profil
        btnSaveProfile.setOnClickListener {
            val name = edtName.text.toString().trim()
            if (name.isEmpty()) {
                edtName.error = "Name required"
                return@setOnClickListener
            }
            saveProfile(name, encodedImage)
        }

        // 6. Logic LOGOUT
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // 7. Setup Bottom Navigation
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> {
                    kembaliKeHome()
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun kembaliKeHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        kembaliKeHome()
    }

    // ... (Fungsi showImagePicker, startCamera, encodeImage, loadProfile, saveProfile SAMA SEPERTI SEBELUMNYA) ...
    // Pastikan fungsi-fungsi helper tersebut tetap ada di bawah sini

    private fun showImagePickerOptions() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Profile Photo")
        builder.setItems(options) { _, which ->
            if (which == 0) checkCameraPermissionAndOpen() else openGallery()
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", it)
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPath = absolutePath }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickGalleryLauncher.launch(intent)
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 400
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        edtEmail.setText(user.email)
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                edtName.setText(doc.getString("nama"))
                val photoBase64 = doc.getString("photo")
                if (!photoBase64.isNullOrEmpty()) {
                    try {
                        val decodedString = Base64.decode(photoBase64, Base64.DEFAULT)
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        imgProfile.setImageBitmap(decodedByte)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    private fun saveProfile(name: String, photoBase64: String?) {
        val user = auth.currentUser ?: return
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."
        val profile = hashMapOf<String, Any>("nama" to name, "email" to (user.email ?: ""))
        if (photoBase64 != null) profile["photo"] = photoBase64
        db.collection("users").document(user.uid).set(profile, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Profile"
                edtName.isEnabled = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Profile"
            }
    }
}