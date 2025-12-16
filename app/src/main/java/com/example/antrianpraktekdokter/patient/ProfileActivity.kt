package com.example.antrianpraktekdokter.patient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.antrianpraktekdokter.R
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

    private lateinit var imgProfile: ImageView
    private lateinit var edtName: EditText
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSaveProfile: Button

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Variables for Camera/Gallery
    private var currentPhotoPath: String? = null
    private var encodedImage: String? = null

    // Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Result Launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && currentPhotoPath != null) {
            val file = File(currentPhotoPath!!)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            // Tampilkan ke ImageView
            imgProfile.setImageBitmap(bitmap)
            // Encode ke Base64 untuk disimpan
            encodedImage = encodeImage(bitmap)
        }
    }

    // Gallery Result Launcher
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
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Init Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Init Views
        imgProfile = findViewById(R.id.imgProfile)
        edtName = findViewById(R.id.edtName)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        loadProfile()

        // Button Change Photo Click
        btnChangePhoto.setOnClickListener {
            showImagePickerOptions()
        }

        // Button Save Click
        btnSaveProfile.setOnClickListener {
            val name = edtName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveProfile(name, encodedImage)
        }
    }

    // Menampilkan Dialog Pilihan (Kamera / Galeri)
    private fun showImagePickerOptions() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Profile Photo")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen() // Camera
                1 -> openGallery() // Gallery
            }
        }
        builder.show()
    }

    // 1. Logic Kamera
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        // Membuat file temporary untuk menyimpan hasil foto [cite: 371]
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            // Menggunakan FileProvider untuk mendapatkan URI yang aman [cite: 393]
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                it
            )
            // Meluncurkan kamera [cite: 584]
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // [cite: 404]
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // 2. Logic Galeri
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickGalleryLauncher.launch(intent)
    }

    // Helper: Convert Bitmap to Base64 String
    private fun encodeImage(bitmap: Bitmap): String {
        // Resize bitmap to avoid Firestore limit (1MB)
        val previewWidth = 400
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)

        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos) // Compress quality 70%
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    // Load Data from Firestore
    private fun loadProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtName.setText(doc.getString("nama"))

                    // Load Profile Image if exists
                    val photoBase64 = doc.getString("photo")
                    if (!photoBase64.isNullOrEmpty()) {
                        try {
                            val decodedString = Base64.decode(photoBase64, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            imgProfile.setImageBitmap(decodedByte)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    // Save Data to Firestore
    private fun saveProfile(name: String, photoBase64: String?) {
        val user = auth.currentUser ?: return

        // Disable button to prevent double click
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        val profile = hashMapOf<String, Any>(
            "nama" to name,
            "email" to (user.email ?: "")
        )
        if (photoBase64 != null) {
            profile["photo"] = photoBase64
        }

        db.collection("users").document(user.uid)
            .set(profile, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "SAVE PROFILE"

                // Optional: Kembali ke halaman sebelumnya
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "SAVE PROFILE"
            }
    }
}