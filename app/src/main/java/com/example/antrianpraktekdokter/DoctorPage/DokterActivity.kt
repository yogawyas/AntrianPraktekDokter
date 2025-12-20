package com.example.antrianpraktekdokter.DoctorPage

import android.app.AlertDialog
import android.content.Intent
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

        // Setup Padding untuk EdgeToEdge
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

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Apakah Anda yakin ingin keluar?")
            builder.setPositiveButton("Ya") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            builder.setNegativeButton("Tidak", null)
            builder.show()
        }

        renderPasien()
    }

    private fun renderPasien() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        // Mengambil data realtime dari Firestore
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

                        if (selesai) countSelesai++ else countSisa++

                        // Memasukkan Item Layout dari XML
                        val itemView = LayoutInflater.from(this).inflate(R.layout.item_pasien_dokter, containerPasien, false)

                        val tvNomor = itemView.findViewById<TextView>(R.id.tvNomorAntrian)
                        val tvNama = itemView.findViewById<TextView>(R.id.tvNamaPasien)
                        val tvJam = itemView.findViewById<TextView>(R.id.tvJamJanji)
                        val tvKeluhan = itemView.findViewById<TextView>(R.id.tvKeluhanSingkat)
                        val viewStatus = itemView.findViewById<View>(R.id.viewStatusColor)

                        val btnPanggil = itemView.findViewById<Button>(R.id.btnPanggil)
                        val btnFinish = itemView.findViewById<Button>(R.id.btnFinish)
                        val btnDetail = itemView.findViewById<Button>(R.id.btnDetail)
                        val btnMore = itemView.findViewById<ImageButton>(R.id.btnMore)

                        // Bind Data ke UI
                        tvNomor.text = "No. Antrian: $nomor"
                        tvNama.text = "Nama: $nama"
                        tvJam.text = "Jam: $jam"
                        tvKeluhan.text = "Keluhan: $keluhan"

                        if (selesai) {
                            viewStatus.setBackgroundColor(Color.GRAY)
                            btnFinish.text = "COMPLETED"
                            btnFinish.isEnabled = false
                            btnPanggil.isEnabled = false
                        }

                        // --- FITUR 1: LIHAT DETAIL DATA PASIEN (POPUP) ---
                        btnDetail.setOnClickListener {
                            showDetailPopup(nama, jam, keluhan, nomor, userId)
                        }

                        // --- FITUR 2: PANGGIL PASIEN ---
                        btnPanggil.setOnClickListener {
                            panggilLogika(doc.id, nama, nomor, dipanggil, userId, jam)
                        }

                        // --- FITUR 3: SELESAI PERIKSA (ISI RESEP POPUP) ---
                        btnFinish.setOnClickListener {
                            showResepPopup(doc.id, nama)
                        }

                        // --- FITUR 4: MENU LAINNYA (CANCEL & PINDAH AKHIR) ---
                        btnMore.setOnClickListener {
                            val popup = PopupMenu(this, it)
                            popup.menu.add("Batalkan Antrian")
                            val itemPindah = popup.menu.add("Pindah ke Akhir")

                            // Logika Anda: Hanya aktif jika dipanggil >= 3
                            itemPindah.isEnabled = dipanggil >= 3 && !selesai

                            popup.setOnMenuItemClickListener { menu ->
                                when (menu.title) {
                                    "Batalkan Antrian" -> batalkanAntrian(doc.id, nama)
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

    private fun showResepPopup(docId: String, nama: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_isi_resep, null)
        val etPrescription = dialogView.findViewById<EditText>(R.id.etPrescription)
        val etAdvice = dialogView.findViewById<EditText>(R.id.etAdvice)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveRecord)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        btnSave.setOnClickListener {
            val resep = etPrescription.text.toString().trim()
            val saran = etAdvice.text.toString().trim()

            if (resep.isEmpty() || saran.isEmpty()) {
                Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            } else {
                // Update dokumen yang sama di koleksi 'antrian'
                db.collection("antrian").document(docId).update(
                    "selesai", true,
                    "resep_obat", resep,
                    "saran_dokter", saran,
                    "status", "Selesai"
                ).addOnSuccessListener {
                    Toast.makeText(this, "Pemeriksaan $nama Selesai!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener { e ->
                    Log.e("FIRESTORE_ERROR", "Gagal update: ${e.message}")
                    // Jika masih error PERMISSION_DENIED di sini, berarti SHA belum terdaftar dengan benar
                }
            }
        }
        dialog.show()
    }

    private fun panggilLogika(id: String, nama: String, nomor: Int, dipanggil: Int, userId: String, jam: String) {
        val newCount = dipanggil + 1
        db.collection("antrian").document(id).update("dipanggil", newCount)
            .addOnSuccessListener {
                Toast.makeText(this, "Panggilan dikirim ke $nama", Toast.LENGTH_SHORT).show()

                // Fitur Notifikasi Anda
                val notifData = hashMapOf(
                    "user_id" to userId,
                    "nomor_antrian" to nomor,
                    "nama_pasien" to nama,
                    "message" to "Silakan masuk ke ruang periksa sekarang!",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("notifikasi").add(notifData)
            }
    }

    private fun batalkanAntrian(id: String, nama: String) {
        AlertDialog.Builder(this)
            .setTitle("Batal")
            .setMessage("Hapus $nama dari list?")
            .setPositiveButton("Ya") { _, _ ->
                db.collection("antrian").document(id).update("dihapus", true)
            }
            .setNegativeButton("Tidak", null)
            .show()
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