package com.example.antrianpraktekdokter.DoctorPage

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.auth.MainActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DokterActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvSelesai: TextView
    private lateinit var tvSisa: TextView
    private lateinit var containerPasien: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dokter)

        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        tvSelesai = findViewById(R.id.tvSelesai)
        tvSisa = findViewById(R.id.tvSisa)
        containerPasien = findViewById(R.id.containerPasien)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ ->
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .setNegativeButton("Tidak", null)
                .show()
        }

        renderPasien()
    }

    private fun renderPasien() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)
            .orderBy("jam", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DokterActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var countSelesai = 0
                    var countSisa = 0
                    containerPasien.removeAllViews()

                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val nama = data["nama_pasien"]?.toString() ?: "N/A"
                        val jam = data["jam"]?.toString() ?: "N/A"
                        val keluhan = data["keluhan"]?.toString() ?: "N/A"
                        val selesai = data["selesai"] as? Boolean ?: false
                        val dipanggil = (data["dipanggil"] as? Long)?.toInt() ?: 0
                        val nomor = (data["nomor_antrian"] as? Long)?.toInt() ?: 0
                        val userId = data["user_id"]?.toString() ?: ""
                        val finalStatus: String
                        val statusColor: Int
                        val mlScore = doc.getDouble("prediction_score") ?: 0.5
                        val hourInterval = doc.getDouble("hour_interval") ?: 10.0
                        val usia = doc.getString("usia")?.toIntOrNull() ?: 30

                        if (selesai) countSelesai++ else countSisa++

                        val itemView = LayoutInflater.from(this).inflate(R.layout.item_pasien_dokter, containerPasien, false)

                        // Bind Basic Data
                        itemView.findViewById<TextView>(R.id.tvNomorAntrian).text = "No. Antrian: $nomor"
                        itemView.findViewById<TextView>(R.id.tvNamaPasien).text = nama
                        itemView.findViewById<TextView>(R.id.tvJamJanji).text = jam
                        itemView.findViewById<TextView>(R.id.tvKeluhanSingkat).text = "Keluhan: $keluhan"

                        // Logika Label Risiko Machine Learning (AI Badge)
                        val tvRisk = itemView.findViewById<TextView>(R.id.tvRiskStatus)
                        when {
                            // KONDISI 1: SANGAT PASTI HADIR (Likely to Attend - Hijau)
                            // Logika: Interval jam dekat (< 2 jam) ATAU AI Score sangat tinggi
                            (hourInterval in 0.0..2.0) || (mlScore > 0.75) -> {
                                finalStatus = "Likely to Attend"
                                statusColor = Color.parseColor("#04AA78") // Hijau Sukses
                            }

                            // KONDISI 2: RISIKO TINGGI BOLOS (High No-Show Risk - Merah)
                            // Logika: Waktu sudah terlewat (interval minus) ATAU usia produktif dengan keluhan ringan
                            (hourInterval < -0.5) || (mlScore < 0.3) || (usia in 18..35 && hourInterval > 5.0) -> {
                                finalStatus = "High No-Show Risk"
                                statusColor = Color.RED
                            }

                            // KONDISI 3: NORMAL/MODERATE (Biru)
                            // Default jika tidak masuk kategori ekstrem
                            else -> {
                                finalStatus = "Normal Risk"
                                statusColor = Color.parseColor("#2196F3") // Biru
                            }
                        }

                        tvRisk.text = finalStatus
                        tvRisk.backgroundTintList = ColorStateList.valueOf(statusColor)

                        val btnFinish = itemView.findViewById<Button>(R.id.btnFinish)
                        val btnPanggil = itemView.findViewById<Button>(R.id.btnPanggil)

                        if (selesai) {
                            itemView.findViewById<View>(R.id.viewStatusColor).setBackgroundColor(Color.GRAY)
                            btnFinish.text = "COMPLETED"
                            btnFinish.isEnabled = false
                            btnPanggil.isEnabled = false
                        }

                        itemView.findViewById<Button>(R.id.btnDetail).setOnClickListener {
                            showDetailPopup(nama, jam, keluhan, nomor, userId)
                        }

                        btnPanggil.setOnClickListener {
                            panggilLogika(doc.id, nama, nomor, dipanggil, userId)
                        }

                        btnFinish.setOnClickListener {
                            showResepPopup(doc.id, nama, userId, nomor)
                        }

                        itemView.findViewById<ImageButton>(R.id.btnMore).setOnClickListener { view ->
                            val popup = PopupMenu(this, view)
                            popup.menu.add("Batalkan Antrian")
                            val itemPindah = popup.menu.add("Pindah ke Akhir")
                            itemPindah.isEnabled = dipanggil >= 3 && !selesai

                            popup.setOnMenuItemClickListener { menu ->
                                when (menu.title) {
                                    "Batalkan Antrian" -> batalkanAntrian(doc.id, nama, userId, nomor)
                                    "Pindah ke Akhir" -> pindahLogika(doc.id, nama, today)
                                }
                                true
                            }
                            popup.show()
                        }
                        containerPasien.addView(itemView)
                    }
                    tvSelesai.text = countSelesai.toString()
                    tvSisa.text = countSisa.toString()
                }
            }
    }

    private fun showDetailPopup(nama: String, jam: String, keluhan: String, nomor: Int, uid: String) {
        AlertDialog.Builder(this)
            .setTitle("Data Diri Pasien")
            .setMessage("Nomor Antrian: $nomor\nNama: $nama\nJam Janji: $jam\nKeluhan: $keluhan\nUser ID: $uid")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showResepPopup(docId: String, nama: String, userId: String, nomor: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_isi_resep, null)
        val etPrescription = dialogView.findViewById<EditText>(R.id.etPrescription)
        val etAdvice = dialogView.findViewById<EditText>(R.id.etAdvice)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialogView.findViewById<Button>(R.id.btnSaveRecord).setOnClickListener {
            val resep = etPrescription.text.toString().trim()
            val saran = etAdvice.text.toString().trim()

            if (resep.isEmpty() || saran.isEmpty()) {
                Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            } else {
                db.collection("antrian").document(docId).update(
                    "selesai", true,
                    "resep_obat", resep,
                    "saran_dokter", saran,
                    "status", "Selesai"
                ).addOnSuccessListener {
                    kirimNotifikasi(userId, "Treatment Completed", "Your appointment is finished. Get well soon!", "Called", nomor, nama)
                    Toast.makeText(this, "Pemeriksaan $nama Selesai!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun panggilLogika(id: String, nama: String, nomor: Int, dipanggil: Int, userId: String) {
        val todayString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // 1. Update data pasien tersebut (seperti yang sudah ada)
        db.collection("antrian").document(id).update("dipanggil", dipanggil + 1)
            .addOnSuccessListener {
                // 2. UPDATE NOMOR ANTREAN SEKARANG DI DOKUMEN PUSAT
                val configRef = db.collection("config").document("status_antrian_$todayString")
                configRef.set(hashMapOf("nomor_sekarang" to nomor), com.google.firebase.firestore.SetOptions.merge())

                kirimNotifikasi(userId, "Called", "No. $nomor, silakan masuk!", "Called", nomor, nama)
                Toast.makeText(this, "Memanggil No. $nomor", Toast.LENGTH_SHORT).show()
            }
    }

    private fun batalkanAntrian(id: String, nama: String, userId: String, nomor: Int) {
        AlertDialog.Builder(this)
            .setTitle("Batal")
            .setMessage("Hapus $nama dari list?")
            .setPositiveButton("Ya") { _, _ ->
                db.collection("antrian").document(id).update("dihapus", true)
                    .addOnSuccessListener {
                        kirimNotifikasi(userId, "Canceled", "Your appointment was canceled by Dr. Alexander.", "Canceled", nomor, nama)
                    }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun kirimNotifikasi(userId: String, title: String, msg: String, type: String, nomor: Int, nama: String) {
        val notifData = hashMapOf(
            "user_id" to userId,
            "nomor_antrian" to nomor,
            "nama_pasien" to nama,
            "message" to msg,
            "type" to type,
            "isRead" to false,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("notifikasi").add(notifData)
    }

    private fun pindahLogika(id: String, nama: String, today: String) {
        db.runTransaction { transaction ->
            val countRef = db.collection("config").document("antrian_count_$today")
            val snapshot = transaction.get(countRef)
            val currentCount = snapshot.getLong("count") ?: 0
            val newCount = currentCount + 1

            transaction.set(countRef, hashMapOf("count" to newCount))
            transaction.update(db.collection("antrian").document(id), mapOf(
                "nomor_antrian" to newCount,
                "dipanggil" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            null
        }.addOnSuccessListener {
            Toast.makeText(this, "$nama dipindah ke urutan terakhir", Toast.LENGTH_SHORT).show()
        }
    }
}