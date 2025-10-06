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
import com.example.antrianpraktekdokter.R
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

            val url = " http://10.0.2.2/api_antrian/login.php "


            val request = object : StringRequest(
                Request.Method.POST, url,
                { response ->
                    try {
                        val obj = JSONObject(response)
                        if (obj.getBoolean("success")) {
                            val nama = obj.getString("nama")
                            val email = obj.getString("email")

                            val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("email", email)
                                putString("nama", nama)
                                apply()
                            }

                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
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
