package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class AntrianAdapter(private val antrianList: List<DocumentSnapshot>) :
    RecyclerView.Adapter<AntrianAdapter.AntrianViewHolder>() {

    // 1. Inisialisasi Firestore di tingkat class
    private val db = FirebaseFirestore.getInstance()

    class AntrianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomor: TextView = itemView.findViewById(R.id.tvNomor)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvJam: TextView = itemView.findViewById(R.id.tvJam)
        val tvKeluhan: TextView = itemView.findViewById(R.id.tvKeluhan)
        val cardView: CardView = itemView.findViewById(R.id.cardAntrian)
        val btnPanggil: Button = itemView.findViewById(R.id.btnPanggil)
        val btnHapus: Button = itemView.findViewById(R.id.btnCancel)
        val tvRisk: TextView = itemView.findViewById(R.id.tvRiskStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntrianViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_antrian, parent, false)
        return AntrianViewHolder(view)
    }

    override fun onBindViewHolder(holder: AntrianViewHolder, position: Int) {
        val doc = antrianList[position]
        val nama = doc.getString("nama_pasien") ?: ""
        val jam = doc.getString("jam") ?: ""
        val keluhan = doc.getString("keluhan") ?: ""
        val selesai = doc.getBoolean("selesai") ?: false
        val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
        val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
        val mlScore = doc.getDouble("prediction_score") ?: 0.0
        val hourInterval = doc.getDouble("hour_interval") ?: 10.0
        val userId = doc.getString("user_id") ?: "" // 2. Pastikan userId diambil di sini

        holder.tvNomor.text = "No. Antrian: $nomor"
        holder.tvNama.text = "Nama Pasien: $nama"
        holder.tvJam.text = "Jam Janji: $jam"
        holder.tvKeluhan.text = "Keluhan: $keluhan"

        // Logika warna label risiko Machine Learning
        when {
            (mlScore >= 0.5 && hourInterval <= 2.0) || mlScore > 0.8 -> {
                holder.tvRisk.text = "Likely to Attend"
                holder.tvRisk.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#04AA78"))
            }
            mlScore < 0.4 && hourInterval > 4.0 -> {
                holder.tvRisk.text = "High No-Show Risk"
                holder.tvRisk.backgroundTintList = ColorStateList.valueOf(Color.RED)
            }
            else -> {
                holder.tvRisk.text = "Normal Risk"
                holder.tvRisk.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
            }
        }

        holder.cardView.setCardBackgroundColor(
            if (selesai) Color.parseColor("#C8E6C9") else Color.WHITE
        )

        holder.btnPanggil.isEnabled = !selesai && dipanggil < 3
        holder.btnHapus.isEnabled = !selesai

        holder.btnPanggil.setOnClickListener {
            val nextCallCount = dipanggil + 1
            val todayString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            // Update status panggil di data antrian
            doc.reference.update("dipanggil", nextCallCount)
                .addOnSuccessListener {
                    // Update nomor sekarang di config agar HP Pasien terupdate otomatis
                    val configRef = db.collection("config").document("status_antrian_$todayString")

                    val updateData = hashMapOf(
                        "nomor_sekarang" to nomor,
                        "last_call_timestamp" to FieldValue.serverTimestamp()
                    )

                    configRef.set(updateData, SetOptions.merge())
                        .addOnSuccessListener {
                            // Kirim notifikasi panggilan
                            val notifData = hashMapOf(
                                "user_id" to userId,
                                "nomor_antrian" to nomor,
                                "nama_pasien" to nama,
                                "message" to "No. Antrian $nomor ($nama), silakan masuk ke ruang periksa!",
                                "type" to "Called",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "isRead" to false
                            )
                            db.collection("notifikasi").add(notifData)

                            Toast.makeText(holder.itemView.context, "Memanggil No. $nomor", Toast.LENGTH_SHORT).show()
                        }
                }
        }

        holder.btnHapus.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Hapus Antrian?")
                .setMessage("Yakin hapus pasien $nama?")
                .setPositiveButton("Ya") { _, _ ->
                    doc.reference.update("dihapus", true)
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    override fun getItemCount(): Int = antrianList.size
}