package com.example.antrianpraktekdokter.patient.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.appcompat.app.AlertDialog

class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var imageUri: Uri? = null
    private var encodedImage: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            imageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                imgProfile.setImageBitmap(bitmap)
                encodedImage = encodeImage(bitmap)
            }
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            imgProfile.setImageBitmap(imageBitmap)
            encodedImage = encodeImage(imageBitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        imgProfile = view.findViewById(R.id.imgProfile)
        edtName = view.findViewById(R.id.edtName)
        edtEmail = view.findViewById(R.id.edtEmail) // Tambah referensi ke edtEmail
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)

        loadProfile()

        btnChangePhoto.setOnClickListener {
            showImageSourceDialog()
        }

        btnSaveProfile.setOnClickListener {
            val name = edtName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveProfile(name, encodedImage)
        }

        return view
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(arrayOf("Galeri", "Kamera")) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            takePhoto.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtName.setText(doc.getString("nama"))
                    edtEmail.setText(user.email) // Isi email dari Firebase Auth
                    val photoBase64 = doc.getString("photo")
                    if (!photoBase64.isNullOrEmpty()) {
                        val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        imgProfile.setImageBitmap(bitmap)
                        encodedImage = photoBase64
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile(name: String, photoBase64: String?) {
        val user = auth.currentUser ?: return
        val profile = hashMapOf(
            "nama" to name,
            "email" to user.email
        )
        if (photoBase64 != null) {
            profile["photo"] = photoBase64
        }

        db.collection("users").document(user.uid)
            .set(profile, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}