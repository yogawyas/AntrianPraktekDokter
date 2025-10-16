package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
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

        val query = db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)

        val finalQuery = if (showCompleted) {
            query
        } else {
            query.whereEqualTo("selesai", false)
        }.orderBy("jam", Query.Direction.ASCENDING)

        finalQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            var countSelesai = 0
            var countSisa = 0
            val antrianList = snapshot.documents

            recyclerAntrian.adapter = AntrianAdapter(antrianList)

            for (doc in antrianList) {
                val selesai = doc.getBoolean("selesai") ?: false
                if (selesai) countSelesai++ else countSisa++
            }

            tvSelesai.text = "Pasien selesai: $countSelesai"
            tvSisa.text = "Sisa pasien: $countSisa"
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
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_antrian, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val doc = antrianList[position]
            val nama = doc.getString("nama_pasien") ?: "Nama Tidak Diketahui"
            val jam = doc.getString("jam") ?: "Jam Tidak Diketahui"
            val keluhan = doc.getString("keluhan") ?: "Keluhan Tidak Diketahui"
            val selesai = doc.getBoolean("selesai") ?: false
            val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
            val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
            val userId = doc.getString("user_id") ?: ""

            holder.tvNomor.text = "No. $nomor"
            holder.tvNama.text = "Nama: $nama"
            holder.tvJam.text = "Jam: $jam"
            holder.tvKeluhan.text = "Keluhan: $keluhan"
            holder.cbSelesai.isChecked = selesai
            holder.cbSelesai.text = if (selesai) "Selesai ✅" else "Tandai Selesai"
            holder.cbSelesai.setTextColor(if (selesai) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.BLACK)

            // Ubah warna card menjadi hijau saat selesai
            holder.cardView.setCardBackgroundColor(
                if (selesai) android.graphics.Color.parseColor("#C8E6C9") else android.graphics.Color.WHITE
            )

            holder.cbSelesai.setOnCheckedChangeListener { _, isChecked ->
                doc.reference.update("selesai", isChecked)
                    .addOnSuccessListener {
                        Toast.makeText(context, if (isChecked) "Pasien $nama selesai diperiksa." else "Status selesai dibatalkan.", Toast.LENGTH_SHORT).show()
                        loadAntrian() // Refresh untuk update warna dan hitungan
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            holder.btnPanggil.setOnClickListener {
                val newCount = dipanggil + 1
                doc.reference.update("dipanggil", newCount)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Hai, $nama! Sekarang waktunya pemeriksaan dengan dokter.", Toast.LENGTH_SHORT).show()

                        // Tampilkan pop-up notifikasi untuk pasien
                        AlertDialog.Builder(requireContext())
                            .setTitle("Panggilan Antrian")
                            .setMessage("Pasien $nama (No. $nomor), silakan masuk untuk pemeriksaan!")
                            .setPositiveButton("OK", null)
                            .show()

                        // Simpan notifikasi ke Firestore
                        val notifData = hashMapOf(
                            "user_id" to userId,
                            "nomor_antrian" to nomor,
                            "nama_pasien" to nama,
                            "jam" to jam,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                        db.collection("notifikasi").add(notifData)
                            .addOnSuccessListener {
                                Log.d("ListAntrianFragment", "Notifikasi disimpan untuk user: $userId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ListAntrianFragment", "Gagal simpan notifikasi: ${e.message}", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            holder.btnCancel.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Batalkan Pasien?")
                    .setMessage("Yakin ingin menghapus pasien ini dari antrian?")
                    .setPositiveButton("Ya") { _, _ ->
                        doc.reference.update("dihapus", true)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Antrian $nama dibatalkan", Toast.LENGTH_SHORT).show()
                                loadAntrian() // Refresh setelah pembatalan
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        }

        override fun getItemCount() = antrianList.size
    }
}