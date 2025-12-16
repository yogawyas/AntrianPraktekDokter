package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.auth.LoginActivity
//import com.example.antrianpraktekdokter.adapter.NewsAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    // Variabel ini yang error karena belum di-set di onCreate
    private lateinit var bottomNav: BottomNavigationView

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

        // Binding View
        val tvWelcomeName: TextView = findViewById(R.id.tvWelcomeName)
        val tvDetails: TextView = findViewById(R.id.tvDetails)
        val tvSeeUsOnGMaps: TextView = findViewById(R.id.tvSeeUsOnGMaps)

        // --- PERBAIKAN DI SINI ---
        // HAPUS "var" agar mengacu pada variabel class, bukan membuat variabel lokal baru
        bottomNav = findViewById(R.id.bottomNavigation)

        val namaDariIntent = intent.getStringExtra("nama")
        tvCurrentQueueNumber = findViewById(R.id.tvCurrentQueueNumber)
        tvYourQueueNumber = findViewById(R.id.tvYourQueueNumber)

        setupQueueMonitoring()

        // Logic Nama
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

        // Logic Animasi Sentuh Tombol Janji Temu
        btnJanjiTemu.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    val intent = Intent(this, JanjiTemuActivity::class.java)
                    startActivity(intent)
                    // Penting: return false atau jalankan performClick agar warning accessibility hilang,
                    // tapi logic Anda di sini sudah handle startActivity jadi aman.
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true
        }

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

        // Setup Bottom Nav
        bottomNav.selectedItemId = R.id.nav_home

        // Animasi Bottom Nav
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    // 1. ANIMASI KELUAR (Mengecil & Hilang)
                    btnJanjiTemu.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(200) // Durasi cepat
                        .withEndAction {
                            // 2. PINDAH ACTIVITY SETELAH ANIMASI SELESAI
                            val intent = Intent(this, ProfileActivity::class.java)
                            startActivity(intent)

                            // 3. ANIMASI TRANSISI LAYAR (Slide)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                        .start()
                    false // Return false agar seleksi menu ditangani activity tujuan
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Set menu aktif ke Home
        // KARENA SUDAH DI-INIT DI ONCREATE, KODE INI TIDAK AKAN ERROR LAGI
        bottomNav.selectedItemId = R.id.nav_home

        // Reset kondisi awal: Kecil dan Transparan
        btnJanjiTemu.scaleX = 0f
        btnJanjiTemu.scaleY = 0f
        btnJanjiTemu.alpha = 0f

        // Animasi Membesar (Pop Up)
        btnJanjiTemu.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(100)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    private fun setupQueueMonitoring() {
        val user = auth.currentUser ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = sdf.format(Date())

        // 1. LOGIKA "CURRENT LINE NUMBER"
        val statusRef = db.collection("config").document("status_antrian_$todayString")

        currentQueueListener = statusRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                tvCurrentQueueNumber.text = "-"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val currentNumber = snapshot.getLong("nomor_sekarang") ?: 0
                tvCurrentQueueNumber.text = String.format("%03d", currentNumber)
            } else {
                tvCurrentQueueNumber.text = "000"
            }
        }

        // 2. LOGIKA "YOUR LINE NUMBER"
        val myQueueQuery = db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .whereEqualTo("tanggal_simpan", todayString)
            .whereEqualTo("dihapus", false)
            .whereEqualTo("selesai", false)

        myQueueListener = myQueueQuery.addSnapshotListener { snapshots, e ->
            if (e != null) {
                tvYourQueueNumber.text = "-"
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val doc = snapshots.documents[0]
                val myNumber = doc.getLong("nomor_antrian") ?: 0
                tvYourQueueNumber.text = String.format("%03d", myNumber)
            } else {
                tvYourQueueNumber.text = "-"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentQueueListener?.remove()
        myQueueListener?.remove()
    }
}