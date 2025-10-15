package com.example.antrianpraktekdokter.patient


import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.MotionEvent
import com.example.antrianpraktekdokter.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.antrianpraktekdokter.auth.LoginActivity
import android.widget.ImageButton


import com.google.firebase.auth.FirebaseAuth
class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnJanjiTemu: ImageButton


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
        btnJanjiTemu = findViewById(R.id.btnJanjiTemu)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)

//        btnJanjiTemu.setOnTouchListener { v, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
//                }
//                MotionEvent.
//            }
//            false
//        }

        btnJanjiTemu.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()

                    // ✅ Handle the click after release
                    val intent = Intent(this, JanjiTemuActivity::class.java)
                    startActivity(intent)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true  // consume the touch so it doesn't trigger twice
        }

//        btnJanjiTemu.setOnClickListener {
//            val intent = Intent(this, JanjiTemuActivity::class.java)
//            startActivity(intent)
//        }



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
                    startActivity(Intent(this, BeritaActivity::class.java))
                    true
                }
                else -> false
            }
        }

    }
}
