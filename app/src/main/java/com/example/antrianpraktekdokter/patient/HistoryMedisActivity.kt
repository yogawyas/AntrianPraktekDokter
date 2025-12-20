package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryMedisActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history_medis)

        // FIX: id harus sesuai dengan XML (id/main)
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerHistory = findViewById(R.id.recyclerHistory)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        recyclerHistory.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        val user = auth.currentUser ?: return
        db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE
                if (e != null) {
                    Log.e("FirestoreError", e.message.toString())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    // Filter hanya yang selesai atau dihapus/cancel
                    val historyList = snapshot.documents.filter {
                        it.getBoolean("selesai") == true || it.getBoolean("dihapus") == true
                    }.map { doc ->
                        HistoryItem(
                            nama = doc.getString("nama_pasien") ?: "",
                            tanggal = doc.getString("tanggal_simpan") ?: "",
                            jam = doc.getString("jam") ?: "",
                            keluhan = doc.getString("keluhan") ?: "",
                            resep = doc.getString("resep_obat") ?: "No prescription",
                            saran = doc.getString("saran_dokter") ?: "No advice",
                            isSelesai = doc.getBoolean("selesai") ?: false,
                            isDibatalkan = doc.getBoolean("dihapus") ?: false
                        )
                    }

                    if (historyList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        tvEmpty.visibility = View.GONE
                        // Memasukkan lambda click listener ke constructor adapter
                        recyclerHistory.adapter = HistoryAdapter(historyList) { item ->
                            if (item.isSelesai) {
                                showDetailPopup(item)
                            } else {
                                Toast.makeText(this, "Cancelled records have no detail", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
    }

    private fun showDetailPopup(item: HistoryItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detail_history, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.detNama).text = item.nama
        dialogView.findViewById<TextView>(R.id.detSymptom).text = item.keluhan
        dialogView.findViewById<TextView>(R.id.detPrescription).text = item.resep
        dialogView.findViewById<TextView>(R.id.detAdvice).text = item.saran

        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}

// FIX: Data Class dengan atribut yang lengkap
data class HistoryItem(
    val nama: String,
    val tanggal: String,
    val jam: String,
    val keluhan: String,
    val resep: String,
    val saran: String,
    val isSelesai: Boolean,
    val isDibatalkan: Boolean
)

// FIX: Adapter dengan listener untuk handle click card
class HistoryAdapter(
    private val historyList: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewStatusBar: View = view.findViewById(R.id.viewStatusBar)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSymptomPreview: TextView = view.findViewById(R.id.tvSymptomPreview)
        val tvStatusLabel: TextView = view.findViewById(R.id.tvStatusLabel)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvDate.text = item.tanggal
        holder.tvTime.text = item.jam
        holder.tvSymptomPreview.text = item.keluhan

        if (item.isDibatalkan) {
            holder.tvStatusLabel.text = "CANCELLED"
            holder.tvStatusLabel.setTextColor(Color.RED)
            holder.viewStatusBar.setBackgroundColor(Color.RED)
        } else {
            holder.tvStatusLabel.text = "FINISHED"
            holder.tvStatusLabel.setTextColor(Color.parseColor("#4CAF50"))
            holder.viewStatusBar.setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = historyList.size
}