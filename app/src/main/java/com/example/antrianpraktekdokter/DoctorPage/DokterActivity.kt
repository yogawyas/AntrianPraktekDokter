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

                        if (selesai) countSelesai++ else countSisa++

                        val itemView = LayoutInflater.from(this).inflate(R.layout.item_pasien_dokter, containerPasien, false)

                        itemView.findViewById<TextView>(R.id.tvNomorAntrian).text = "No. Antrian: $nomor"
                        itemView.findViewById<TextView>(R.id.tvNamaPasien).text = "Nama: $nama"
                        itemView.findViewById<TextView>(R.id.tvJamJanji).text = "Jam: $jam"
                        itemView.findViewById<TextView>(R.id.tvKeluhanSingkat).text = "Keluhan: $keluhan"

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
        db.collection("antrian").document(id).update("dipanggil", dipanggil + 1)
            .addOnSuccessListener {
                kirimNotifikasi(userId, "Called", "You were called by Dr. Alexander. Please enter the room.", "Called", nomor, nama)
                Toast.makeText(this, "Panggilan dikirim ke $nama", Toast.LENGTH_SHORT).show()
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