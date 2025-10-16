package com.example.antrianpraktekdokter.patient.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryMedisFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_medis, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerHistory = view.findViewById(R.id.recyclerHistory)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        recyclerHistory.layoutManager = LinearLayoutManager(context)

        loadHistory()

        return view
    }

    private fun loadHistory() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        val user = auth.currentUser ?: return
        db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .whereIn("status", listOf("selesai", "dibatalkan"))
            .orderBy("tanggal_simpan", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                if (snapshot.isEmpty) {
                    tvEmpty.text = "Belum ada riwayat kunjungan"
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    val historyList = snapshot.documents.map { doc ->
                        HistoryItem(
                            nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0,
                            tanggal = doc.getString("tanggal_simpan") ?: "",
                            jam = doc.getString("jam") ?: "",
                            keluhan = doc.getString("keluhan") ?: "",
                            status = doc.getString("status") ?: ""
                        )
                    }
                    recyclerHistory.adapter = HistoryAdapter(historyList)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Data class untuk item history
    data class HistoryItem(
        val nomor: Int,
        val tanggal: String,
        val jam: String,
        val keluhan: String,
        val status: String
    )

    // Adapter untuk RecyclerView
    class HistoryAdapter(private val historyList: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNomor: TextView = view.findViewById(R.id.tvNomor)
            val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
            val tvJam: TextView = view.findViewById(R.id.tvJam)
            val tvKeluhan: TextView = view.findViewById(R.id.tvKeluhan)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = historyList[position]
            holder.tvNomor.text = "No. ${item.nomor}"
            holder.tvTanggal.text = "Tanggal: ${item.tanggal}"
            holder.tvJam.text = "Jam: ${item.jam}"
            holder.tvKeluhan.text = "Keluhan: ${item.keluhan}"
            holder.tvStatus.text = "Status: ${item.status}"
        }

        override fun getItemCount() = historyList.size
    }
}