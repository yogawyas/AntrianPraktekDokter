package com.example.antrianpraktekdokter.auth

import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.patient.HomeActivity
import com.example.antrianpraktekdokter.DoctorPage.DokterActivity
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val tvRegisterLink: TextView = findViewById(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi email dan password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🩺 Cek dulu: apakah login dokter Alexander?
            if (email.equals("alexander@dokter.com", true) && password == "12345") {
                Toast.makeText(this, "Login sebagai Dokter Alexander", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DokterActivity::class.java)
                startActivity(intent)
                finish()
                return@setOnClickListener
            }

            // 🔑 Jika bukan dokter → lanjut login Firebase (pasien)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        db.collection("users").document(user!!.uid).get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val nama = document.getString("nama") ?: "User"
                                    val intent = Intent(this, HomeActivity::class.java)
                                    intent.putExtra("nama", nama)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal fetch data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
