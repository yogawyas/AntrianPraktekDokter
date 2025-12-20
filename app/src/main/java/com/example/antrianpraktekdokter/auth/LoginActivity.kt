package com.example.antrianpraktekdokter.auth

import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.patient.HomeActivity
import com.example.antrianpraktekdokter.DoctorPage.DokterActivity
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

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
        //val tvRegisterLink: TextView = findViewById(R.id.tvRegisterLink)
        val tvForgotPassword: TextView = findViewById(R.id.tv_forgot_password)
        val btnBack: Button = findViewById(R.id.btn_back)



        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi email dan password!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔑 SEMUA LOGIN (Admin, Dokter, Pasien) sekarang melewati Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        // Cek Role di Firestore setelah login berhasil
                        db.collection("users").document(user!!.uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val nama = document.getString("nama") ?: "User"
                                    val role = document.getString("role") ?: "patient"

                                    Toast.makeText(this, "Welcome, $nama!", Toast.LENGTH_SHORT).show()

                                    // Navigasi berdasarkan Role
                                    val intent = when (role) {
                                        "admin" -> Intent(this, com.example.antrianpraktekdokter.admin.AdminHomeActivity::class.java)
                                        "doctor" -> Intent(this, DokterActivity::class.java)
                                        else -> Intent(this, com.example.antrianpraktekdokter.patient.HomeActivity::class.java)
                                    }

                                    intent.putExtra("nama", nama)
                                    intent.putExtra("role", role)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Jika dokumen user belum ada di Firestore (misal akun baru dibuat di Auth Console)
                                    // Kita arahkan ke default sesuai email (untuk kemudahan Anda)
                                    handleDefaultNavigation(email)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("LoginError", e.message.toString())
                                handleDefaultNavigation(email)
                            }
                    } else {
                        Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, com.example.antrianpraktekdokter.auth.MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        tvForgotPassword.setOnClickListener {
            showResetPasswordDialog()
        }

//        tvRegisterLink.setOnClickListener {
//            startActivity(Intent(this, RegisterActivity::class.java))
//        }
    }
    private fun handleDefaultNavigation(email: String) {
        if (email.contains("admin")) {
            startActivity(Intent(this, com.example.antrianpraktekdokter.admin.AdminHomeActivity::class.java))
        } else if (email.contains("dokter")) {
            startActivity(Intent(this, DokterActivity::class.java))
        } else {
            startActivity(Intent(this, com.example.antrianpraktekdokter.patient.HomeActivity::class.java))
        }
        finish()
    }
    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        // Setup input EditText untuk email
        val input = EditText(this)
        input.hint = "Masukkan Email Anda"
        input.setPadding(50, 50, 50, 50)

        // Tambahkan EditText ke dalam LinearLayout agar padding lebih rapi
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(45, 10, 45, 10) // Padding kiri, atas, kanan, bawah
        container.addView(input)

        builder.setView(container)

        // Tombol Kirim Reset
        builder.setPositiveButton("Kirim") { dialog, which ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Batal
        builder.setNegativeButton("Batal") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Mengirim email reset password menggunakan Firebase Auth.
     */
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Email reset password telah dikirim ke $email. Cek folder spam Anda juga.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Gagal mengirim email reset: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
