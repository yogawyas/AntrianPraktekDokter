package com.example.antrianpraktekdokter


import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.JanjiTemuActivity



import com.google.firebase.auth.FirebaseAuth
class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check jika user belum login, redirect ke Login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val nama = prefs.getString("nama", "") ?: ""
        tvWelcome.text = "Selamat datang, $nama!"
        val btnJanjiTemu: MaterialButton = findViewById(R.id.btnJanjiTemu)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)

        btnJanjiTemu.setOnClickListener {
            val intent = Intent(this, JanjiTemuActivity::class.java)
            startActivity(intent)
        }



        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_history_medis -> {
                    startActivity(Intent(this, HistoryMedisActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_list_antrian -> {
                    startActivity(Intent(this, ListAntrianActivity::class.java))
                    true
                }
                R.id.nav_notifikasi -> {
                    startActivity(Intent(this, NotifikasiActivity::class.java))
                    true
                }
                else -> false
            }
        }

    }
}
