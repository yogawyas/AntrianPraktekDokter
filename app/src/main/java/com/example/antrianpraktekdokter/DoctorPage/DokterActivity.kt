package com.example.antrianpraktekdokter.DoctorPage

import android.app.AlertDialog
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
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
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        renderPasien()
    }

    private fun renderPasien() {
        containerPasien.removeAllViews()

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

                if (snapshot == null) {
                    Toast.makeText(this, "Data antrian tidak tersedia", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                var countSelesai = 0
                var countSisa = 0
                containerPasien.removeAllViews()

                for (doc in snapshot.documents) {
                    val nama = doc.getString("nama_pasien") ?: "Nama Tidak Diketahui"
                    val jam = doc.getString("jam") ?: "Jam Tidak Diketahui"
                    val keluhan = doc.getString("keluhan") ?: "Keluhan Tidak Diketahui"
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
                        setCardBackgroundColor(if (selesai) Color.parseColor("#C8E6C9") else Color.WHITE)
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

                    val btnCancel = Button(this).apply {
                        text = "BATAL"
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
                                    this@DokterActivity,
                                    if (isChecked) "Pasien $nama selesai diperiksa." else "Status selesai dibatalkan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@DokterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    btnPanggil.setOnClickListener {
                        val newCount = dipanggil + 1
                        doc.reference.update("dipanggil", newCount)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@DokterActivity,
                                    "Hai, $nama! Sekarang waktunya pemeriksaan dengan dokter.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Placeholder FCM notifikasi (post-UTS)
                                // sendFcmNotification(docUserId, "Antrian Anda nomor $nomor hampir tiba!")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@DokterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    btnCancel.setOnClickListener {
                        AlertDialog.Builder(this@DokterActivity)
                            .setTitle("Batalkan Pasien?")
                            .setMessage("Yakin ingin menghapus pasien ini dari antrian?")
                            .setPositiveButton("Ya") { _, _ ->
                                doc.reference.update("dihapus", true)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@DokterActivity, "Antrian $nama dibatalkan", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@DokterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("Tidak", null)
                            .show()
                    }

                    btnPindahAkhir.setOnClickListener {
                        AlertDialog.Builder(this@DokterActivity)
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
                                    null
                                }.addOnSuccessListener {
                                    Toast.makeText(this@DokterActivity, "Antrian $nama dipindah ke akhir", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener { e ->
                                    Toast.makeText(this@DokterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    btnLayout.addView(btnCancel)
                    btnLayout.addView(space3)
                    btnLayout.addView(btnPindahAkhir)

                    inner.addView(tvNomor)
                    inner.addView(tvNama)
                    inner.addView(tvJam)
                    inner.addView(tvKeluhan)
                    inner.addView(btnLayout)
                    card.addView(inner)
                    containerPasien.addView(card)
                }

                tvSelesai.text = "Pasien selesai: $countSelesai"
                tvSisa.text = "Sisa pasien: $countSisa"
            }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Kode asli temanmu (SharedPreferences) - dikomentari
    /*
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dokter)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)

        tvSelesai = findViewById(R.id.tvSelesai)
        tvSisa = findViewById(R.id.tvSisa)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        renderPasien()
    }

    private fun renderPasien() {
        val container = findViewById<LinearLayout>(R.id.containerPasien)
        container.removeAllViews()

        val dataString = prefs.getString("dataAntrian", "[]")
        val jsonArray = JSONArray(dataString)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        var countSelesai = 0
        var countSisa = 0

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val tanggalSimpan = obj.optString("tanggal_simpan", today)
            val dihapus = obj.optBoolean("dihapus", false)
            if (tanggalSimpan != today || dihapus) continue

            val nama = obj.getString("nama_pasien")
            val jam = obj.getString("jam")
            val keluhan = obj.getString("keluhan")
            val selesai = obj.optBoolean("selesai", false)
            val dipanggil = obj.optInt("dipanggil", 0)

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
                setCardBackgroundColor(if (selesai) Color.parseColor("#C8E6C9") else Color.WHITE)
                useCompatPadding = true
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
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

            val btnCancel = Button(this).apply {
                text = "BATAL"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                background = resources.getDrawable(R.drawable.bg_button_red, null)
            }

            cbSelesai.setOnCheckedChangeListener { _, isChecked ->
                obj.put("selesai", isChecked)
                prefs.edit().putString("dataAntrian", jsonArray.toString()).apply()
                Toast.makeText(
                    this,
                    if (isChecked) "Pasien $nama selesai diperiksa." else "Status selesai dibatalkan.",
                    Toast.LENGTH_SHORT
                ).show()
                renderPasien()
            }

            btnPanggil.setOnClickListener {
                val newCount = dipanggil + 1
                obj.put("dipanggil", newCount)
                prefs.edit().putString("dataAntrian", jsonArray.toString()).apply()
                Toast.makeText(
                    this,
                    "Hai, $nama! Sekarang waktunya pemeriksaan dengan dokter.",
                    Toast.LENGTH_SHORT
                ).show()

                if (newCount >= 3) {
                    AlertDialog.Builder(this)
                        .setTitle("Pasien tidak hadir")
                        .setMessage("Pasien sudah dipanggil 3 kali. Batalkan pasien ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            obj.put("dihapus", true)
                            prefs.edit().putString("dataAntrian", jsonArray.toString()).apply()
                            Toast.makeText(
                                this,
                                "Antrian $nama dihapus karena tidak hadir/terlambat.",
                                Toast.LENGTH_LONG
                            ).show()
                            renderPasien()
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            }

            btnCancel.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Batalkan Pasien?")
                    .setMessage("Yakin ingin menghapus pasien ini dari antrian?")
                    .setPositiveButton("Ya") { _, _ ->
                        obj.put("dihapus", true)
                        prefs.edit().putString("dataAntrian", jsonArray.toString()).apply()
                        renderPasien()
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

            btnLayout.addView(cbSelesai)
            btnLayout.addView(space1)
            btnLayout.addView(btnPanggil)
            btnLayout.addView(space2)
            btnLayout.addView(btnCancel)

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
    */
}