package com.example.antrianpraktekdokter.patient

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class JanjiTemuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var etNama: EditText
    private lateinit var etUsia: EditText
    private lateinit var etTanggal: EditText
    private lateinit var etJam: EditText
    private lateinit var etKeluhan: EditText
    private lateinit var tvDokter: TextView
    private lateinit var cbAlergi: CheckBox
    private lateinit var cbPenyakitBawaan: CheckBox
    private lateinit var containerExtraFields: LinearLayout
    private lateinit var btnDaftar: Button
    private lateinit var btnRiwayat: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_janji_temu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etNama = findViewById(R.id.etNama)
        etUsia = findViewById(R.id.etUsia)
        etTanggal = findViewById(R.id.etTanggal)
        etJam = findViewById(R.id.etJam)
        etKeluhan = findViewById(R.id.etKeluhan)
        tvDokter = findViewById(R.id.tvDokter)
        cbAlergi = findViewById(R.id.cbAlergi)
        cbPenyakitBawaan = findViewById(R.id.cbPenyakitBawaan)
        containerExtraFields = findViewById(R.id.containerExtraFields)
        btnDaftar = findViewById(R.id.btnDaftar)

        // Load nama dari Firestore
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    Log.d("JanjiTemuActivity", "User data fetched: ${doc.data}")
                    etNama.setText(doc.getString("nama") ?: user.email)
                }
                .addOnFailureListener { e ->
                    Log.e("JanjiTemuActivity", "Fetch user failed: ${e.message}", e)
                    Toast.makeText(this, "Gagal fetch nama: ${e.message}", Toast.LENGTH_SHORT).show()
                    etNama.setText(user.email) // Fallback ke email jika gagal
                }
        }

        // Load jadwal praktek
        db.collection("config").document("status_praktik")
            .get()
            .addOnSuccessListener { doc ->
                Log.d("JanjiTemuActivity", "Config fetched: ${doc.data}")
                val buka = doc.getString("bukaJam") ?: "08:00"
                val tutup = doc.getString("tutupJam") ?: "17:00"
                val isOpen = doc.getBoolean("isOpen") ?: false
                tvDokter.text = "Dr. Alexander (Umum)\nJam Praktek: $buka - $tutup\nStatus: ${if (isOpen) "Buka" else "Tutup"}"
            }
            .addOnFailureListener { e ->
                Log.e("JanjiTemuActivity", "Config fetch failed: ${e.message}", e)
                Toast.makeText(this, "Gagal fetch jadwal: ${e.message}", Toast.LENGTH_SHORT).show()
                tvDokter.text = "Dr. Alexander (Umum)\nJam Praktek: -\nStatus: -"
            }

        // Date picker dibatasi hanya hari ini
        val c = Calendar.getInstance()
        val today = Calendar.getInstance()
        etTanggal.setText(String.format("%02d/%02d/%d", today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH) + 1, today.get(Calendar.YEAR)))
        etTanggal.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                if (selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    etTanggal.setText(String.format("%02d/%02d/%d", day, month + 1, year))
                } else {
                    Toast.makeText(this, "Hanya bisa membuat janji untuk hari ini!", Toast.LENGTH_SHORT).show()
                }
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = today.timeInMillis
                datePicker.minDate = today.timeInMillis
            }.show()
        }

        // Time picker dibatasi 00:00 - 21:00 dan tidak boleh jam terlewat
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        etJam.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                if (h < currentHour || h > 21) {
                    Toast.makeText(this, "Jam harus antara ${String.format("%02d:00", currentHour)} - 21:00!", Toast.LENGTH_SHORT).show()
                } else {
                    etJam.setText(String.format("%02d:%02d", h, m))
                }
            }, currentHour, 0, true).show()
        }

        // Dynamic fields for alergi/penyakit bawaan
        cbAlergi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val etAlergi = EditText(this).apply {
                    hint = "Tulis alergi obat"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                    backgroundTintList = etNama.backgroundTintList
                }
                containerExtraFields.addView(etAlergi)
            } else {
                containerExtraFields.removeAllViews()
                if (cbPenyakitBawaan.isChecked) {
                    val etPenyakit = EditText(this).apply {
                        hint = "Tulis penyakit bawaan"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                        backgroundTintList = etNama.backgroundTintList
                    }
                    containerExtraFields.addView(etPenyakit)
                }
            }
        }

        cbPenyakitBawaan.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val etPenyakit = EditText(this).apply {
                    hint = "Tulis penyakit bawaan"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                    backgroundTintList = etNama.backgroundTintList
                }
                containerExtraFields.addView(etPenyakit)
            } else {
                containerExtraFields.removeAllViews()
                if (cbAlergi.isChecked) {
                    val etAlergi = EditText(this).apply {
                        hint = "Tulis alergi obat"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                        backgroundTintList = etNama.backgroundTintList
                    }
                    containerExtraFields.addView(etAlergi)
                }
            }
        }

        btnDaftar.setOnClickListener {
            simpanData()
        }

        // Tambah tombol Riwayat
//        btnRiwayat = Button(this).apply {
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply { setMargins(0, dpToPx(8), 0, 0) }
//            text = "Lihat Riwayat Kunjungan"
//            setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")))
//            setTextColor(android.graphics.Color.WHITE)
//        }
//        val parentLayout = findViewById<LinearLayout>(R.id.parentLayout)
//        parentLayout?.addView(btnRiwayat)
//
//        btnRiwayat.setOnClickListener {
//            showRiwayatKunjungan()
//        }
    }

    private fun simpanData() {
        val nama = etNama.text.toString().trim()
        val usia = etUsia.text.toString().trim()
        val tanggal = etTanggal.text.toString().trim()
        val jam = etJam.text.toString().trim()
        val keluhan = etKeluhan.text.toString().trim()
        val alergi = if (cbAlergi.isChecked) (containerExtraFields.getChildAt(0) as? EditText)?.text.toString() else ""
        val penyakitBawaan = if (cbPenyakitBawaan.isChecked) (containerExtraFields.getChildAt(if (cbAlergi.isChecked) 1 else 0) as? EditText)?.text.toString() else ""

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = sdf.format(Date())

        // Validasi tanggal hanya hari ini
        if (tanggal != todayString) {
            Toast.makeText(this, "Hanya bisa membuat janji untuk hari ini!", Toast.LENGTH_SHORT).show()
            return
        }

        if (nama.isEmpty() || usia.isEmpty() || tanggal.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
            Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            Toast.makeText(this, "User tidak login", Toast.LENGTH_SHORT).show()
            return
        }

        // Cek apakah user sudah punya janji aktif hari ini
        db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .whereEqualTo("tanggal_simpan", tanggal)
            .whereEqualTo("dihapus", false)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("JanjiTemuActivity", "Existing appointments found: ${snapshot.size()}")
                if (snapshot.isEmpty) {
                    checkDailyAndHourlyLimits(tanggal, jam, nama, usia, keluhan, alergi, penyakitBawaan)
                } else {
                    val existingAntrian = snapshot.documents.first()
                    val existingStatus = existingAntrian.getBoolean("dihapus") ?: false
                    if (existingStatus) {
                        showRescheduleDialog(existingAntrian, tanggal, jam, nama, usia, keluhan, alergi, penyakitBawaan)
                    } else {
                        Toast.makeText(this, "Anda sudah punya janji hari ini! Batalkan dulu untuk reschedule.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("JanjiTemuActivity", "Check existing appointment failed: ${e.message}", e)
                Toast.makeText(this, "Gagal cek janji: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkDailyAndHourlyLimits(tanggal: String, jam: String, nama: String, usia: String, keluhan: String, alergi: String, penyakitBawaan: String) {
        // Cek batas harian (max 20)
        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", tanggal)
            .whereEqualTo("dihapus", false)
            .get()
            .addOnSuccessListener { dailySnapshot ->
                if (dailySnapshot.size() >= 20) {
                    Toast.makeText(this, "Antrian penuh untuk tanggal $tanggal!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Cek batas jam (max 2 per jam)
                db.collection("antrian")
                    .whereEqualTo("tanggal_simpan", tanggal)
                    .whereEqualTo("jam", jam)
                    .whereEqualTo("dihapus", false)
                    .get()
                    .addOnSuccessListener { jamSnapshot ->
                        if (jamSnapshot.size() >= 2) {
                            Toast.makeText(this, "Jam $jam sudah penuh! Pilih jam lain.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Validasi jam (00:00 - 21:00)
                        val selectedHour = jam.substring(0, 2).toIntOrNull() ?: 0
                        if (selectedHour < 0 || selectedHour > 21) {
                            Toast.makeText(this, "Jam antrian hanya 00:00 - 21:00!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Hitung nomor antrian
                        db.runTransaction { transaction ->
                            val countRef = db.collection("config").document("antrian_count_$tanggal")
                            val snapshot = transaction.get(countRef)
                            val currentCount = snapshot.getLong("count") ?: 0
                            val newCount = currentCount + 1
                            transaction.set(countRef, hashMapOf("count" to newCount))
                            newCount
                        }.addOnSuccessListener { newNomor ->
                            val estimasiMenit = (newNomor * 15).toInt() // 15 menit/pasien
                            val newItem = hashMapOf(
                                "nama_pasien" to nama,
                                "usia" to usia,
                                "tanggal_simpan" to tanggal,
                                "jam" to jam,
                                "keluhan" to keluhan,
                                "alergi" to alergi,
                                "penyakit_bawaan" to penyakitBawaan,
                                "selesai" to false,
                                "dipanggil" to 0,
                                "dihapus" to false,
                                "nomor_antrian" to newNomor,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "user_id" to auth.currentUser?.uid
                            )

                            // Dialog konfirmasi
                            AlertDialog.Builder(this)
                                .setTitle("Konfirmasi Antrian")
                                .setMessage("Nomor antrian Anda: $newNomor\nEstimasi tunggu: $estimasiMenit menit\nLanjutkan?")
                                .setPositiveButton("Daftar") { _, _ ->
                                    db.collection("antrian").add(newItem)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Antrian berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, ListAntrianActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("JanjiTemuActivity", "Save appointment failed: ${e.message}", e)
                                            Toast.makeText(this, "Gagal simpan antrian: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .setNegativeButton("Batal", null)
                                .show()
                        }.addOnFailureListener { e ->
                            Log.e("JanjiTemuActivity", "Transaction failed: ${e.message}", e)
                            Toast.makeText(this, "Gagal hitung nomor: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("JanjiTemuActivity", "Check hourly limit failed: ${e.message}", e)
                        Toast.makeText(this, "Gagal cek jam: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("JanjiTemuActivity", "Check daily limit failed: ${e.message}", e)
                Toast.makeText(this, "Gagal cek batas harian: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRescheduleDialog(existingAntrian: DocumentSnapshot, tanggal: String, jam: String, nama: String, usia: String, keluhan: String, alergi: String, penyakitBawaan: String) {
        AlertDialog.Builder(this)
            .setTitle("Reschedule Antrian")
            .setMessage("Anda sudah punya antrian hari ini. Reschedule jam dan keluhan?")
            .setPositiveButton("Reschedule") { _, _ ->
                val updatedItem: Map<String, Any?> = hashMapOf(
                    "jam" to jam,
                    "keluhan" to keluhan,
                    "alergi" to alergi,
                    "penyakit_bawaan" to penyakitBawaan
                )
                existingAntrian.reference.update(updatedItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Antrian berhasil direschedule!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ListAntrianActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("JanjiTemuActivity", "Reschedule failed: ${e.message}", e)
                        Toast.makeText(this, "Gagal reschedule: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showRiwayatKunjungan() {
        val user = auth.currentUser ?: return
        db.collection("antrian")
            .whereEqualTo("user_id", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val riwayat = StringBuilder()
                for (doc in snapshot.documents) {
                    val tanggal = doc.getString("tanggal_simpan") ?: "-"
                    val jam = doc.getString("jam") ?: "-"
                    val keluhan = doc.getString("keluhan") ?: "-"
                    val selesai = doc.getBoolean("selesai") ?: false
                    val dihapus = doc.getBoolean("dihapus") ?: false
                    val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
                    riwayat.append("No: $nomor | $tanggal, $jam | Keluhan: $keluhan | Status: ${if (dihapus) "Dibatalkan" else if (selesai) "Selesai" else "Menunggu"}\n\n")
                }
                AlertDialog.Builder(this)
                    .setTitle("Riwayat Kunjungan")
                    .setMessage(if (riwayat.isEmpty()) "Belum ada riwayat kunjungan" else riwayat.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e("JanjiTemuActivity", "Fetch history failed: ${e.message}", e)
                Toast.makeText(this, "Gagal load riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}