package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ListAntrianFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerAntrian: RecyclerView
    private lateinit var tvSelesai: TextView
    private lateinit var tvSisa: TextView
    private lateinit var switchShowCompleted: Switch
    private var showCompleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list_antrian, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerAntrian = view.findViewById(R.id.recyclerAntrian)
        tvSelesai = view.findViewById(R.id.tvSelesai)
        tvSisa = view.findViewById(R.id.tvSisa)
        switchShowCompleted = view.findViewById(R.id.switchShowCompleted)

        recyclerAntrian.layoutManager = LinearLayoutManager(context)

        switchShowCompleted.setOnCheckedChangeListener { _, isChecked ->
            showCompleted = isChecked
            loadAntrian()
        }

        loadAntrian()
        return view
    }

    private fun loadAntrian() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        var query = db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)

        if (!showCompleted) {
            query = query.whereEqualTo("selesai", false)
        }

        // Snapshot listener bersifat real-time, otomatis update UI saat data Firestore berubah
        query.orderBy("jam", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    context?.let { Toast.makeText(it, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var countSelesai = 0
                    var countSisa = 0
                    val antrianList = snapshot.documents

                    // Cukup pasang adapter sekali atau update datanya
                    recyclerAntrian.adapter = AntrianAdapter(antrianList)

                    for (doc in antrianList) {
                        if (doc.getBoolean("selesai") == true) countSelesai++ else countSisa++
                    }

                    tvSelesai.text = "Pasien selesai: $countSelesai"
                    tvSisa.text = "Sisa pasien: $countSisa"
                }
            }
    }

    inner class AntrianAdapter(private val antrianList: List<com.google.firebase.firestore.DocumentSnapshot>) :
        RecyclerView.Adapter<AntrianAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNomor: TextView = itemView.findViewById(R.id.tvNomor)
            val tvNama: TextView = itemView.findViewById(R.id.tvNama)
            val tvJam: TextView = itemView.findViewById(R.id.tvJam)
            val tvKeluhan: TextView = itemView.findViewById(R.id.tvKeluhan)
            val cbSelesai: CheckBox = itemView.findViewById(R.id.cbSelesai)
            val btnPanggil: Button = itemView.findViewById(R.id.btnPanggil)
            val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
            val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardAntrian)
            val tvRisk: TextView = itemView.findViewById(R.id.tvRiskStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_antrian, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val doc = antrianList[position]
            val nama = doc.getString("nama_pasien") ?: "N/A"
            val jam = doc.getString("jam") ?: "N/A"
            val keluhan = doc.getString("keluhan") ?: "N/A"
            val selesai = doc.getBoolean("selesai") ?: false
            val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
            val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
            val userId = doc.getString("user_id") ?: ""


            val mlScore = doc.getDouble("prediction_score") ?: 0.5
            val hourInterval = doc.getDouble("hour_interval") ?: 10.0
            val usia = doc.getString("usia")?.toIntOrNull() ?: 30
            val finalStatus: String
            val statusColor: Int

            holder.tvNomor.text = "No. $nomor"
            holder.tvNama.text = "Nama: $nama"
            holder.tvJam.text = "Jam: $jam"
            holder.tvKeluhan.text = "Keluhan: $keluhan"

            // Logika Checkbox Selesai
            holder.cbSelesai.setOnCheckedChangeListener(null)
            holder.cbSelesai.isChecked = selesai
            holder.cbSelesai.text = if (selesai) "Selesai ✅" else "Tandai Selesai"

            when {

                (hourInterval in 0.0..2.0) || (mlScore > 0.75) -> {
                    finalStatus = "Likely to Attend"
                    statusColor = Color.parseColor("#04AA78") // Hijau Sukses
                }

                (hourInterval < -0.5) || (mlScore < 0.3) || (usia in 18..35 && hourInterval > 5.0) -> {
                    finalStatus = "High No-Show Risk"
                    statusColor = Color.RED
                }

                else -> {
                    finalStatus = "Normal Risk"
                    statusColor = Color.parseColor("#2196F3")
                }
            }

            holder.tvRisk.text = finalStatus
            holder.tvRisk.backgroundTintList = ColorStateList.valueOf(statusColor)

            holder.cardView.setCardBackgroundColor(if (selesai) Color.parseColor("#C8E6C9") else Color.WHITE)

            holder.cbSelesai.setOnCheckedChangeListener { _, isChecked ->
                doc.reference.update("selesai", isChecked)
            }

            holder.btnPanggil.setOnClickListener {
                doc.reference.update("dipanggil", dipanggil + 1).addOnSuccessListener {
                    val notifData = hashMapOf(
                        "user_id" to userId,
                        "nomor_antrian" to nomor,
                        "nama_pasien" to nama,
                        "message" to "No. Antrian $nomor ($nama), silakan masuk ke ruang periksa sekarang!",
                        "type" to "Called",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "isRead" to false
                    )
                    db.collection("notifikasi").add(notifData)
                    Toast.makeText(holder.itemView.context, "Panggilan dikirim ke $nama", Toast.LENGTH_SHORT).show()
                }
            }

            holder.btnCancel.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Batalkan Antrian?")
                    .setMessage("Hapus $nama dari list?")
                    .setPositiveButton("Ya") { _, _ ->
                        doc.reference.update("dihapus", true).addOnSuccessListener {
                            val notifData = hashMapOf(
                                "user_id" to userId,
                                "nomor_antrian" to nomor,
                                "nama_pasien" to nama,
                                "message" to "Maaf, antrian Anda telah dibatalkan oleh Admin.",
                                "type" to "Canceled",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "isRead" to false
                            )
                            db.collection("notifikasi").add(notifData)
                        }
                    }.setNegativeButton("Tidak", null).show()
            }
        }

        override fun getItemCount() = antrianList.size
    }
}