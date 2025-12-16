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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.auth.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var navListAntrian: ImageButton
    private lateinit var btnNews: ImageButton
    private lateinit var tvCurrentQueueNumber: TextView
    private lateinit var tvYourQueueNumber: TextView
    private var currentQueueListener: ListenerRegistration? = null
    private var myQueueListener: ListenerRegistration? = null

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
        tvCurrentQueueNumber = findViewById(R.id.tvCurrentQueueNumber)
        tvYourQueueNumber = findViewById(R.id.tvYourQueueNumber)

        setupQueueMonitoring()


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
    private fun setupQueueMonitoring() {
        val user = auth.currentUser ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = sdf.format(Date()) // Contoh: "12/12/2025"

        // -------------------------------------------------------------
        // 1. LOGIKA "CURRENT LINE NUMBER" (Nomor yang sedang dipanggil)
        // -------------------------------------------------------------
        // Kita ambil dari koleksi 'config', dokumen 'status_harian' (atau nama lain yg disepakati)
        // Dokumen ini harus di-update oleh Admin saat menekan tombol "Next Patient"

        val statusRef = db.collection("config").document("status_antrian_$todayString")

        currentQueueListener = statusRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Error atau dokumen belum ada
                tvCurrentQueueNumber.text = "-"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Ambil field 'nomor_sekarang' (integer/string)
                val currentNumber = snapshot.getLong("nomor_sekarang") ?: 0
                // Format jadi 3 digit (contoh: 005)
                tvCurrentQueueNumber.text = String.format("%03d", currentNumber)
            } else {
                // Belum ada antrian dimulai hari ini
                tvCurrentQueueNumber.text = "000"
            }
        }
        val myQueueQuery = db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .whereEqualTo("tanggal_simpan", todayString)
            .whereEqualTo("dihapus", false) // Hanya yang tidak dibatalkan
            .whereEqualTo("selesai", false) // Hanya yang belum selesai (opsional, tergantung keinginan)

        myQueueListener = myQueueQuery.addSnapshotListener { snapshots, e ->
            if (e != null) {
                tvYourQueueNumber.text = "-"
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                // User punya janji temu hari ini
                val doc = snapshots.documents[0] // Ambil yang pertama ditemukan
                val myNumber = doc.getLong("nomor_antrian") ?: 0

                // Tampilkan nomor user (Format 3 digit)
                tvYourQueueNumber.text = String.format("%03d", myNumber)
            } else {
                // Tidak ada janji temu hari ini
                tvYourQueueNumber.text = "-"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hentikan pemantauan database saat aplikasi ditutup agar hemat baterai/kuota
        currentQueueListener?.remove()
        myQueueListener?.remove()
    }
}