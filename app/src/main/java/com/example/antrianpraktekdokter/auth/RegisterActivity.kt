package com.example.antrianpraktekdokter.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.R
// import com.android.volley.Request  // Comment out Volley imports
// import com.android.volley.toolbox.StringRequest
// import com.android.volley.toolbox.Volley
// import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth  // Tambahkan import Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore  // Tambahkan import Firestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // Tambahkan instance Auth
    private lateinit var db: FirebaseFirestore  // Tambahkan instance Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etNama: EditText = findViewById(R.id.etNama)
        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnRegister: Button = findViewById(R.id.btnRegister)
        val tvLoginLink: TextView = findViewById(R.id.tvLoginLink)
        val btnBack: Button = findViewById(R.id.btn_back)


        btnRegister.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val url = " http://10.0.2.2/api_antrian/register.php"

            // Code baru: Register dengan Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User berhasil dibuat, sekarang simpan nama ke Firestore
                        val user = auth.currentUser
                        val userData = hashMapOf(
                            "nama" to nama,
                            "email" to email,
                            "role" to "patient"
                        )
                        db.collection("users").document(user!!.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            // Code lama: Comment out Volley request
            // val url = " http://192.168.1.8/api_antrian/register.php"
            //
            // val request = object : StringRequest(
            //     Request.Method.POST, url,
            //     { response ->
            //         try {
            //             val obj = JSONObject(response)
            //             if (obj.getBoolean("success")) {
            //                 Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
            //                 startActivity(Intent(this, LoginActivity::class.java))
            //                 finish()
            //             } else {
            //                 Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
            //             }
            //         } catch (e: Exception) {
            //             e.printStackTrace()
            //             Toast.makeText(this, "Response error", Toast.LENGTH_SHORT).show()
            //         }
            //     },
            //     { error ->
            //         Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            //     }
            // ) {
            //     override fun getParams(): MutableMap<String, String> {
            //         return hashMapOf(
            //             "nama" to nama,
            //             "email" to email,
            //             "password" to password
            //         )
            //     }
            // }
            //
            // Volley.newRequestQueue(this).add(request)
        }
        btnBack.setOnClickListener {
            val intent = Intent(this, com.example.antrianpraktekdokter.auth.MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}