package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AntrianAdapter(private val antrianList: List<DocumentSnapshot>) :
    RecyclerView.Adapter<AntrianAdapter.AntrianViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class AntrianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomor: TextView = itemView.findViewById(R.id.tvNomor)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvJam: TextView = itemView.findViewById(R.id.tvJam)
        val tvKeluhan: TextView = itemView.findViewById(R.id.tvKeluhan)
        val cardView: CardView = itemView.findViewById(R.id.cardAntrian)
        val btnPanggil: Button = itemView.findViewById(R.id.btnPanggil)
        val btnHapus: Button = itemView.findViewById(R.id.btnCancel)
        val btnLayout: ViewGroup = itemView.findViewById(R.id.btnLayout)
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

        // Set text
        holder.tvNomor.text = "No. Antrian: $nomor"
        holder.tvNama.text = "Nama Pasien: $nama"
        holder.tvJam.text = "Jam Janji: $jam"
        holder.tvKeluhan.text = "Keluhan: $keluhan"

        // Set card background based on status
        holder.cardView.setCardBackgroundColor(
            when {
                selesai -> ContextCompat.getColor(holder.itemView.context, R.color.green_light)
                dipanggil > 0 -> ContextCompat.getColor(holder.itemView.context, R.color.yellow_light)
                else -> ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            }
        )

        // Enable/disable buttons based on status
        holder.btnPanggil.isEnabled = !selesai && dipanggil < 3
        holder.btnHapus.isEnabled = !selesai

        // Panggil button logic
        holder.btnPanggil.setOnClickListener {
            val newCount = dipanggil + 1
            doc.reference.update("dipanggil", newCount)
                .addOnSuccessListener {
                    Toast.makeText(holder.itemView.context, "Hai, $nama! Waktunya pemeriksaan ($newCount).", Toast.LENGTH_SHORT).show()
                }
        }

        // Hapus button logic
        holder.btnHapus.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Hapus Antrian?")
                .setMessage("Yakin hapus pasien $nama?")
                .setPositiveButton("Ya") { _, _ ->
                    doc.reference.update("dihapus", true)
                        .addOnSuccessListener {
                            Toast.makeText(holder.itemView.context, "Antrian $nama dihapus.", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    override fun getItemCount(): Int = antrianList.size
}