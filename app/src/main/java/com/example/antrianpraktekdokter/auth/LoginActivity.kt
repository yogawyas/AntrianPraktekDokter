package com.example.antrianpraktekdokter.auth

import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.patient.HomeActivity
import com.example.antrianpraktekdokter.R
// import com.android.volley.Request  // Comment out Volley imports
// import com.android.volley.toolbox.StringRequest
// import com.android.volley.toolbox.Volley
// import org.json.JSONObject
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

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login sukses, fetch nama dari Firestore
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

            // Code lama: Comment out Volley request
            // val url = " http://192.168.1.8/api_antrian/login.php "
            //
            // val request = object : StringRequest(
            //     Request.Method.POST, url,
            //     { response ->
            //         try {
            //             val obj = JSONObject(response)
            //             if (obj.getBoolean("success")) {
            //                 val intent = Intent(this, HomeActivity::class.java)
            //                 intent.putExtra("nama", obj.getString("nama"))
            //                 startActivity(intent)
            //             } else {
            //                 Toast.makeText(this, obj.getString("message"), Toast.LENGTH_SHORT).show()
            //             }
            //         } catch (e: Exception) {
            //             e.printStackTrace()
            //         }
            //     },
            //     { error ->
            //         Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            //     }
            // ) {
            //     override fun getParams(): MutableMap<String, String> {
            //         return hashMapOf(
            //             "email" to email,
            //             "password" to password
            //         )
            //     }
            // }
            //
            // Volley.newRequestQueue(this).add(request)
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}