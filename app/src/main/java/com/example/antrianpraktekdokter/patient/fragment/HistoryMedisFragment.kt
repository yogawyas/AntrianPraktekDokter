package com.example.antrianpraktekdokter.patient.fragment

import android.os.Bundle
import android.util.Log
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

        val user = auth.currentUser
        if (user == null) {
            progressBar.visibility = View.GONE
            Toast.makeText(context, "User tidak login", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid
        Log.d("HistoryMedisFragment", "Loading history for userId: $userId")

        // Data hardcoded untuk tes
        val hardcodedHistory = listOf(
            HistoryItem(
                nomor = 1,
                tanggal = "02/09/2025",
                jam = "09:00",
                keluhan = "Demam ringan",
                status = "Di Batalkan"
            ),
            HistoryItem(
                nomor = 2,
                tanggal = "10/10/2025",
                jam = "10:30",
                keluhan = "Sakit kepala",
                status = "Selesai"
            ),
            HistoryItem(
                nomor = 3,
                tanggal = "15/10/2025",
                jam = "14:15",
                keluhan = "Pusing makin parah",
                status = "Selesai"
            ),
            HistoryItem(
                nomor = 4,
                tanggal = "16/10/2025",
                jam = "12:55",
                keluhan = "Panas Tinggi",
                status = "Menunggu"
            )
        )

        // Query Firestore (tetap ada untuk analisis)
        db.collection("antrian")
            .whereEqualTo("user_id", userId)
            .orderBy("tanggal_simpan", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                Log.d("HistoryMedisFragment", "Query success, documents found: ${snapshot.size()}")
                val firestoreHistory = snapshot.documents.map { doc ->
                    val selesai = doc.getBoolean("selesai") ?: false
                    val dihapus = doc.getBoolean("dihapus") ?: false
                    HistoryItem(
                        nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0,
                        tanggal = doc.getString("tanggal_simpan") ?: "",
                        jam = doc.getString("jam") ?: "",
                        keluhan = doc.getString("keluhan") ?: "",
                        status = when {
                            dihapus -> "Dibatalkan"
                            selesai -> "Selesai"
                            else -> "Menunggu"
                        }
                    )
                }

                // Gabungkan data hardcoded dan Firestore
                val combinedHistory = (hardcodedHistory + firestoreHistory).distinctBy { it.nomor }
                if (combinedHistory.isEmpty()) {
                    tvEmpty.text = "Belum ada riwayat kunjungan"
                    tvEmpty.visibility = View.VISIBLE
                    Log.d("HistoryMedisFragment", "No history found")
                } else {
                    recyclerHistory.adapter = HistoryAdapter(combinedHistory)
                    Log.d("HistoryMedisFragment", "History list size: ${combinedHistory.size}")
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("HistoryMedisFragment", "Query failed: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()

                // Jika query gagal, gunakan data hardcoded saja
                if (hardcodedHistory.isNotEmpty()) {
                    recyclerHistory.adapter = HistoryAdapter(hardcodedHistory)
                    Log.d("HistoryMedisFragment", "Using hardcoded data, size: ${hardcodedHistory.size}")
                } else {
                    tvEmpty.text = "Belum ada riwayat kunjungan"
                    tvEmpty.visibility = View.VISIBLE
                }
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
            val cardView: androidx.cardview.widget.CardView = view.findViewById(R.id.cardHistory)
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
            holder.tvStatus.text = item.status

            // Warna card dan text berdasarkan status
            val (cardColor, textColor) = when (item.status) {
                "Selesai" -> Pair("#C8E6C9", "#388E3C")  // Hijau
                "Dibatalkan" -> Pair("#FFCDD2", "#D32F2F")  // Merah
                else -> Pair("#FFFFFF", "#757575")  // Putih untuk Menunggu
            }
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor(cardColor))
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor(textColor))
        }

        override fun getItemCount() = historyList.size
    }
}