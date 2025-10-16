package com.example.antrianpraktekdokter.patient.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.fragment.app.Fragment
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import android.util.Base64

class ProfileFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var edtName: EditText
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private var encodedImage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        imgProfile = view.findViewById(R.id.imgProfile)
        edtName = view.findViewById(R.id.edtName)
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)

        loadProfile()

        btnChangePhoto.setOnClickListener {
            openImagePicker()
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            imgProfile.setImageURI(imageUri)
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            encodedImage = encodeImage(bitmap)
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
                    // Skip photo decode for now
                }
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