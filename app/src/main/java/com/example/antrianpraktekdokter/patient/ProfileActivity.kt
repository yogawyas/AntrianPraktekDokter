package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import android.graphics.Bitmap
import android.util.Base64
import com.example.antrianpraktekdokter.R
import java.io.ByteArrayOutputStream
import org.json.JSONObject

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var edtName: EditText
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSaveProfile: Button

    private val PICK_IMAGE_REQUEST = 100
    private var imageUri: Uri? = null
    private var email: String? = null   // from login
    private var encodedImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imgProfile = findViewById(R.id.imgProfile)
        edtName = findViewById(R.id.edtName)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        email = intent.getStringExtra("email")

        btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        btnSaveProfile.setOnClickListener {
            val name = edtName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveProfile(name, encodedImage)
        }
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
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos) // compress for smaller size
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun saveProfile(name: String, photoBase64: String?) {
        val url = "http://10.0.2.2/api_antrian/update_profile.php"

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getBoolean("success")) {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email ?: ""
                params["nama"] = name
                if (photoBase64 != null) {
                    params["photo"] = photoBase64
                }
                return params
            }
        }
        Volley.newRequestQueue(this).add(request)
    }
}
