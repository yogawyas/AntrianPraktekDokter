package com.example.antrianpraktekdokter.patient

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var tvCurrentQueueNumber: TextView
    private lateinit var tvYourQueueNumber: TextView
    private lateinit var btnNotification: MaterialButton
    private lateinit var bottomNav: BottomNavigationView
    private var currentQueueListener: ListenerRegistration? = null
    private var myQueueListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private val CHANNEL_ID = "ANTRIAN_NOTIF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContentView(R.layout.activity_home)

        // 3. Inisialisasi Views
        btnNotification = findViewById(R.id.btn_notification)
        tvCurrentQueueNumber = findViewById(R.id.tvCurrentQueueNumber)
        tvYourQueueNumber = findViewById(R.id.tvYourQueueNumber)
        bottomNav = findViewById(R.id.bottomNavigation)
        btnJanjiTemu = findViewById(R.id.btnJanjiTemu)
        val tvWelcomeName: TextView = findViewById(R.id.tvWelcomeName)

        setupUserDisplayName(tvWelcomeName, currentUser.uid)
        setupQueueMonitoring()
        listenForNotifications()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupUserDisplayName(view: TextView, uid: String) {
        val namaDariIntent = intent.getStringExtra("nama")
        if (namaDariIntent != null) {
            view.text = "Hello, $namaDariIntent!"
        } else {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        view.text = "Hello, ${document.getString("nama")}!"
                    }
                }
        }
    }

    private fun setupClickListeners() {

        btnJanjiTemu.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    startActivity(Intent(this, JanjiTemuActivity::class.java))
                }
                MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
            }
            true
        }

        findViewById<ImageButton>(R.id.queueNum).setOnClickListener {
            startActivity(Intent(this, ListAntrianActivity::class.java))
        }

        findViewById<ImageButton>(R.id.newsButton).setOnClickListener {
            startActivity(Intent(this, BeritaActivity::class.java))
        }

        findViewById<ImageButton>(R.id.historyButton).setOnClickListener {
            startActivity(Intent(this, HistoryMedisActivity::class.java))
        }

        btnNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun listenForNotifications() {
        val userId = auth.currentUser?.uid ?: return

        notificationListener = db.collection("notifikasi")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {

                    btnNotification.setIconResource(R.drawable.ic_notification_dot)


                    for (change in snapshot.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val msg = change.document.getString("message") ?: "Ada panggilan baru!"
                            val type = change.document.getString("type") ?: "Update Antrian"
                            showAndroidNotification(type, msg)
                        }
                    }
                } else {

                    btnNotification.setIconResource(R.drawable.ic_notification)
                }
            }
    }

    private fun showAndroidNotification(title: String, message: String) {
        val intent = Intent(this, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Antrian"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Menerima panggilan dokter"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Pastikan icon ini ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                if (ActivityCompat.checkSelfPermission(this@HomeActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupQueueMonitoring() {
        val user = auth.currentUser ?: return
        val todayString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())


        currentQueueListener = db.collection("config")
            .document("status_antrian_$todayString")
            .addSnapshotListener { snapshot, _ ->
                val currentNumber = snapshot?.getLong("nomor_sekarang") ?: 0
                tvCurrentQueueNumber.text = String.format("%03d", currentNumber)
            }


        myQueueListener = db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .whereEqualTo("tanggal_simpan", todayString)
            .whereEqualTo("dihapus", false)
            .whereEqualTo("selesai", false)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val myNumber = snapshots.documents[0].getLong("nomor_antrian") ?: 0
                    tvYourQueueNumber.text = String.format("%03d", myNumber)
                } else {
                    tvYourQueueNumber.text = "-"
                }
            }
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    btnJanjiTemu.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).withEndAction {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }.start()
                    false
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_home


        btnJanjiTemu.scaleX = 1f
        btnJanjiTemu.scaleY = 1f
        btnJanjiTemu.alpha = 1f
    }

    override fun onDestroy() {
        super.onDestroy()
        currentQueueListener?.remove()
        myQueueListener?.remove()
        notificationListener?.remove()
    }
}