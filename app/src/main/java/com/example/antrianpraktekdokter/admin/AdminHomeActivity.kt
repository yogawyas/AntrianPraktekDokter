package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.patient.HomeActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvSelesai: TextView
    private lateinit var tvSisa: TextView
    private lateinit var switchStatus: Switch
    private lateinit var btnTambahAntrian: Button
    private lateinit var btnLaporan: Button
    private lateinit var btnAturJadwal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        tvSelesai = findViewById(R.id.tvSelesai)
        tvSisa = findViewById(R.id.tvSisa)
        switchStatus = findViewById(R.id.switchStatusPraktik)
        btnTambahAntrian = findViewById(R.id.btnTambahAntrian)
        btnLaporan = findViewById(R.id.btnLihatLaporan)
        btnAturJadwal = findViewById(R.id.btnAturJadwal)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        loadStatusPraktik()
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            updateStatusPraktik(isChecked)
        }

        btnTambahAntrian.setOnClickListener {
            showTambahAntrianDialog()
        }

        btnLaporan.setOnClickListener {
            showLaporanDialog()
        }

        btnAturJadwal.setOnClickListener {
            showAturJadwalDialog()
        }

        renderPasien()
    }

    private fun renderPasien() {
        val container = findViewById<LinearLayout>(R.id.containerPasien)
        container.removeAllViews()

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)
            .orderBy("jam", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                var countSelesai = 0
                var countSisa = 0
                container.removeAllViews()

                for (doc in snapshot.documents) {
                    val nama = doc.getString("nama_pasien") ?: ""
                    val jam = doc.getString("jam") ?: ""
                    val keluhan = doc.getString("keluhan") ?: ""
                    val selesai = doc.getBoolean("selesai") ?: false
                    val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
                    val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0

                    if (selesai) countSelesai++ else countSisa++

                    val card = CardView(this).apply {
                        val lp = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        lp.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                        layoutParams = lp
                        radius = dpToPx(10).toFloat()
                        cardElevation = dpToPx(6).toFloat()
                        setCardBackgroundColor(
                            when {
                                selesai -> Color.parseColor("#C8E6C9")
                                dipanggil > 0 -> Color.parseColor("#FFF9C4")
                                else -> Color.WHITE
                            }
                        )
                        useCompatPadding = true
                    }

                    val inner = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                    }

                    val tvNomor = TextView(this).apply {
                        text = "No. Antrian: $nomor"
                        textSize = 14f
                        setTextColor(Color.parseColor("#2196F3"))
                    }

                    val tvNama = TextView(this).apply {
                        text = "Nama Pasien: $nama"
                        textSize = 16f
                        setTextColor(Color.BLACK)
                    }

                    val tvJam = TextView(this).apply {
                        text = "Jam Janji: $jam"
                        textSize = 14f
                        setTextColor(Color.parseColor("#2196F3"))
                    }

                    val tvKeluhan = TextView(this).apply {
                        text = "Keluhan: $keluhan"
                        textSize = 14f
                        setTextColor(Color.DKGRAY)
                    }

                    val cbSelesai = CheckBox(this).apply {
                        text = if (selesai) "Pasien selesai ✅" else "Tandai pasien selesai"
                        isChecked = selesai
                        setTextColor(if (selesai) Color.parseColor("#388E3C") else Color.BLACK)
                    }

                    val btnPanggil = Button(this).apply {
                        text = "PANGGIL"
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                        background = resources.getDrawable(R.drawable.bg_button_green, null)
                    }

                    val btnHapus = Button(this).apply {
                        text = "HAPUS"
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                        background = resources.getDrawable(R.drawable.bg_button_red, null)
                    }

                    val btnPindahAkhir = Button(this).apply {
                        text = "PINDAH KE AKHIR"
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                        background = resources.getDrawable(R.drawable.bg_button_orange, null)
                        isEnabled = dipanggil >= 3 && !selesai
                    }

                    cbSelesai.setOnCheckedChangeListener { _, isChecked ->
                        doc.reference.update("selesai", isChecked)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@AdminHomeActivity,
                                    if (isChecked) "Pasien $nama selesai." else "Status dibatalkan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                    btnPanggil.setOnClickListener {
                        val newCount = dipanggil + 1
                        doc.reference.update("dipanggil", newCount)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@AdminHomeActivity,
                                    "Hai, $nama! Waktunya pemeriksaan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Placeholder FCM notifikasi (post-UTS)
                                // sendFcmNotification(docUserId, "Antrian Anda nomor $nomor hampir tiba!")
                            }
                    }

                    btnHapus.setOnClickListener {
                        AlertDialog.Builder(this@AdminHomeActivity)
                            .setTitle("Hapus Antrian?")
                            .setMessage("Yakin hapus pasien $nama?")
                            .setPositiveButton("Ya") { _, _ ->
                                doc.reference.update("dihapus", true)
                            }
                            .setNegativeButton("Tidak", null)
                            .show()
                    }

                    btnPindahAkhir.setOnClickListener {
                        AlertDialog.Builder(this@AdminHomeActivity)
                            .setTitle("Pindah ke Akhir?")
                            .setMessage("Pindahkan pasien $nama ke antrian terakhir?")
                            .setPositiveButton("Ya") { _, _ ->
                                db.runTransaction { transaction ->
                                    val countRef = db.collection("config").document("antrian_count_$today")
                                    val snapshot = transaction.get(countRef)
                                    val currentCount = snapshot.getLong("count") ?: 0
                                    val newCount = currentCount + 1
                                    transaction.set(countRef, hashMapOf("count" to newCount))
                                    transaction.update(doc.reference, mapOf(
                                        "nomor_antrian" to newCount,
                                        "dipanggil" to 0,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    ))
                                }.addOnSuccessListener {
                                    Toast.makeText(this@AdminHomeActivity, "Antrian $nama dipindah ke akhir", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Tidak", null)
                            .show()
                    }

                    val btnLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        val params = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, dpToPx(8), 0, dpToPx(8))
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                        layoutParams = params
                    }

                    val space1 = Space(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(12), 0) }
                    val space2 = Space(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(12), 0) }
                    val space3 = Space(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(12), 0) }

                    btnLayout.addView(cbSelesai)
                    btnLayout.addView(space1)
                    btnLayout.addView(btnPanggil)
                    btnLayout.addView(space2)
                    btnLayout.addView(btnHapus)
                    btnLayout.addView(space3)
                    btnLayout.addView(btnPindahAkhir)

                    inner.addView(tvNomor)
                    inner.addView(tvNama)
                    inner.addView(tvJam)
                    inner.addView(tvKeluhan)
                    inner.addView(btnLayout)
                    card.addView(inner)
                    container.addView(card)
                }

                tvSelesai.text = "Pasien selesai: $countSelesai"
                tvSisa.text = "Sisa pasien: $countSisa"
            }
    }

    private fun loadStatusPraktik() {
        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                switchStatus.isChecked = doc.getBoolean("isOpen") ?: false
                switchStatus.text = "Status Praktik: ${if (doc.getBoolean("isOpen") == true) "Buka" else "Tutup"}"
            }
    }

    private fun updateStatusPraktik(isOpen: Boolean) {
        db.collection("config").document("status_praktik")
            .set(hashMapOf("isOpen" to isOpen), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, if (isOpen) "Praktik dibuka" else "Praktik ditutup", Toast.LENGTH_SHORT).show()
                switchStatus.text = "Status Praktik: ${if (isOpen) "Buka" else "Tutup"}"
            }
    }

    private fun showTambahAntrianDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_antrian, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etJam = dialogView.findViewById<EditText>(R.id.etJam)
        val etKeluhan = dialogView.findViewById<EditText>(R.id.etKeluhan)

        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                etJam.setText(String.format("%02d:%02d", h, m))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Antrian Manual")
            .setView(dialogView)
            .setPositiveButton("Tambah") { _, _ ->
                val nama = etNama.text.toString()
                val jam = etJam.text.toString()
                val keluhan = etKeluhan.text.toString()
                if (nama.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
                    Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = sdf.format(Date())
                db.collection("antrian")
                    .whereEqualTo("tanggal_simpan", today)
                    .whereEqualTo("dihapus", false)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.size() >= 20) {
                            Toast.makeText(this, "Antrian penuh hari ini!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        db.collection("antrian")
                            .whereEqualTo("tanggal_simpan", today)
                            .whereEqualTo("jam", jam)
                            .whereEqualTo("dihapus", false)
                            .get()
                            .addOnSuccessListener { jamSnapshot ->
                                if (jamSnapshot.size() >= 2) {
                                    Toast.makeText(
                                        this,
                                        "Jam $jam sudah penuh! Pilih jam lain.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnSuccessListener
                                }

                                db.runTransaction { transaction ->
                                    val countRef = db.collection("config").document("antrian_count_$today")
                                    val snapshot = transaction.get(countRef)
                                    val currentCount = snapshot.getLong("count") ?: 0
                                    val newCount = currentCount + 1
                                    transaction.set(countRef, hashMapOf("count" to newCount))
                                    newCount
                                }.addOnSuccessListener { newNomor ->
                                    val newAntrian = hashMapOf(
                                        "nama_pasien" to nama,
                                        "jam" to jam,
                                        "keluhan" to keluhan,
                                        "tanggal_simpan" to today,
                                        "selesai" to false,
                                        "dipanggil" to 0,
                                        "dihapus" to false,
                                        "nomor_antrian" to newNomor,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("antrian").add(newAntrian)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "Antrian ditambahkan! Nomor: $newNomor",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLaporanDialog() {
        val periods = arrayOf("Hari ini", "Minggu ini", "Bulan ini")
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)

        AlertDialog.Builder(this)
            .setTitle("Lihat Laporan")
            .setView(spinner)
            .setPositiveButton("Tampilkan") { _, _ ->
                val selected = spinner.selectedItem.toString()
                fetchLaporan(selected)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun fetchLaporan(periode: String) {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val startDate: String
        val endDate = sdf.format(Date())

        when (periode) {
            "Hari ini" -> startDate = endDate
            "Minggu ini" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                startDate = sdf.format(cal.time)
            }
            "Bulan ini" -> {
                cal.add(Calendar.MONTH, -1)
                startDate = sdf.format(cal.time)
            }
            else -> return
        }

        db.collection("antrian")
            .whereGreaterThanOrEqualTo("tanggal_simpan", startDate)
            .whereLessThanOrEqualTo("tanggal_simpan", endDate)
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.size()
                val selesai = snapshot.documents.count { it.getBoolean("selesai") == true }
                AlertDialog.Builder(this)
                    .setTitle("Laporan $periode")
                    .setMessage("Total pasien: $total\nSelesai: $selesai\nSisa: ${total - selesai}")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    private fun showAturJadwalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_atur_jadwal, null)
        val etBuka = dialogView.findViewById<EditText>(R.id.etJamBuka)
        val etTutup = dialogView.findViewById<EditText>(R.id.etJamTutup)

        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                etBuka.setText(doc.getString("bukaJam") ?: "")
                etTutup.setText(doc.getString("tutupJam") ?: "")
            }

        etBuka.setOnClickListener { showTimePicker(etBuka) }
        etTutup.setOnClickListener { showTimePicker(etTutup) }

        AlertDialog.Builder(this)
            .setTitle("Atur Jadwal Praktek")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val buka = etBuka.text.toString()
                val tutup = etTutup.text.toString()
                if (buka.isEmpty() || tutup.isEmpty()) {
                    Toast.makeText(this, "Isi jam!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val jadwal = hashMapOf(
                    "bukaJam" to buka,
                    "tutupJam" to tutup
                )
                db.collection("config").document("status_praktik")
                    .set(jadwal, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Jadwal diupdate", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showTimePicker(et: EditText) {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            et.setText(String.format("%02d:%02d", h, m))
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}