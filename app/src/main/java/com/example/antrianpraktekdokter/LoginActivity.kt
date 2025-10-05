package com.example.antrianpraktekdokter

import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail: EditText = findViewById(R.id.etEmail)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val tvRegisterLink: TextView = findViewById(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val url = "http://10.0.2.2/api_antrian/login.php" // FIX: hapus spasi

            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    try {
                        val obj = JSONObject(response)

                        // Gunakan optBoolean dan optString agar tidak crash
                        if (obj.optBoolean("success", false)) {
                            val nama = obj.optString("nama", "Pengguna")
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.putExtra("nama", nama)
                            startActivity(intent)
                            finish() // supaya tidak bisa kembali ke login
                        } else {
                            Toast.makeText(this, obj.optString("message", "Login gagal"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Response tidak valid", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): MutableMap<String, String> {
                    return hashMapOf(
                        "email" to email,
                        "password" to password
                    )
                }
            }

            Volley.newRequestQueue(this).add(request)
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
