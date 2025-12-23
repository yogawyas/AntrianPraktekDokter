package com.example.antrianpraktekdokter.patient

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvNotif: RecyclerView
    private lateinit var tvEmpty: TextView
    private val listNotif = mutableListOf<NotifModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvNotif = findViewById(R.id.rvNotification)
        tvEmpty = findViewById(R.id.tvEmpty)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        val btnReadAll = findViewById<TextView>(R.id.btnClearAll)
        btnReadAll.text = "Read All"

        rvNotif.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }
        btnReadAll.setOnClickListener { readAllNotifications() }

        loadNotifications()
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("notifikasi")
            .whereEqualTo("user_id", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                listNotif.clear()
                for (doc in snapshot.documents) {
                    val item = doc.toObject(NotifModel::class.java)
                    if (item != null) {
                        item.id = doc.id
                        listNotif.add(item)
                    }
                }

                if (listNotif.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvNotif.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvNotif.visibility = View.VISIBLE
                    rvNotif.adapter = NotificationAdapter(listNotif)
                }
            }
    }

    private fun readAllNotifications() {
        val uid = auth.currentUser?.uid ?: return
        // Cari notifikasi yang isRead-nya masih false
        db.collection("notifikasi")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "All notifications already read", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in snapshot) {
                    batch.update(doc.reference, "isRead", true)
                }

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Marked all as read", Toast.LENGTH_SHORT).show()
                }
            }
    }
}