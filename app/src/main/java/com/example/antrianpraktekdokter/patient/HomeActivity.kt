package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.auth.LoginActivity
import com.example.antrianpraktekdokter.adapter.NewsAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User


class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var navListAntrian: ImageButton
    private lateinit var btnNews: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check jika user belum login, redirect ke Login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)

        //val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val tvWelcomeName: TextView = findViewById(R.id.tvWelcomeName)
        //val tvLocation: TextView = findViewById(R.id.tvLocation)
        val tvDetails: TextView = findViewById(R.id.tvDetails)
        val tvSeeUsOnGMaps: TextView = findViewById(R.id.tvSeeUsOnGMaps)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        val namaDariIntent = intent.getStringExtra("nama")




        //segala macam function


        if (namaDariIntent != null) {
            tvWelcomeName.text = "Hello, $namaDariIntent!"
        } else {
            tvWelcomeName.text = "Loading..."
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val namaDiDb = document.getString("nama")
                        tvWelcomeName.text = "${namaDiDb ?: "User"}!"
                    }
                }
                .addOnFailureListener {
                    tvWelcomeName.text = "User!"
                }
        }

        btnJanjiTemu = findViewById(R.id.btnJanjiTemu)
        navListAntrian = findViewById(R.id.queueNum)
        btnNews = findViewById(R.id.newsButton)
        btnJanjiTemu.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    val intent = Intent(this, JanjiTemuActivity::class.java)
                    startActivity(intent)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true
        }
//        btnJanjiTemu.setOnClickListener {
//            val intent = Intent(this, JanjiTemuActivity::class.java)
//            startActivity(intent)
//        }
        val queueButton = findViewById<ImageButton>(R.id.queueNum)
        queueButton.setOnClickListener {
            startActivity(Intent(this, ListAntrianActivity::class.java))
        }

        val newsButton = findViewById<ImageButton>(R.id.newsButton)
        newsButton.setOnClickListener {
            startActivity(Intent(this, BeritaActivity::class.java))
        }

        val historyButton = findViewById<ImageButton>(R.id.historyButton)
        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryMedisActivity::class.java))
        }

        // animasu Bottom Nav
        bottomNav.setOnItemSelectedListener { item ->
            val menuView = bottomNav.findViewById<android.view.View>(item.itemId)

            if (menuView != null) {
                menuView.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction {
                        menuView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                    .start()
            }
            when(item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}