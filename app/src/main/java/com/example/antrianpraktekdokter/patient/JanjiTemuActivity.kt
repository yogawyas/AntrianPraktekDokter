package com.example.antrianpraktekdokter.patient

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class JanjiTemuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_janji_temu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNama = findViewById<EditText>(R.id.etNama)
        val etUsia = findViewById<EditText>(R.id.etUsia)
        val etTanggal = findViewById<EditText>(R.id.etTanggal)
        val etJam = findViewById<EditText>(R.id.etJam)
        val etKeluhan = findViewById<EditText>(R.id.etKeluhan)
        val cbAlergi = findViewById<CheckBox>(R.id.cbAlergi)
        val cbPenyakit = findViewById<CheckBox>(R.id.cbPenyakitBawaan)
        val btnDaftar = findViewById<Button>(R.id.btnDaftar)
        val container = findViewById<LinearLayout>(R.id.containerExtraFields)

        val dokter = "Dr. Alexander (Umum)"

        // ====== TANGGAL OTOMATIS HARI INI ======
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = sdf.format(Date())
        etTanggal.setText(todayString)
        etTanggal.isEnabled = false

        // ====== FIELD DINAMIS (ALERGI & PENYAKIT) ======
        val etDetailAlergi = EditText(this).apply {
            hint = "Sebutkan alergi Anda"
            backgroundTintList = getColorStateList(android.R.color.holo_blue_light)
            visibility = View.GONE
        }

        val etDetailPenyakit = EditText(this).apply {
            hint = "Sebutkan penyakit bawaan Anda"
            backgroundTintList = getColorStateList(android.R.color.holo_blue_light)
            visibility = View.GONE
        }

        container.addView(etDetailAlergi)
        container.addView(etDetailPenyakit)

        cbAlergi.setOnCheckedChangeListener { _, isChecked ->
            etDetailAlergi.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        cbPenyakit.setOnCheckedChangeListener { _, isChecked ->
            etDetailPenyakit.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // ====== JAM PRAKTEK 07:00 - 20:00 ======
        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            val tpd = TimePickerDialog(this, { _, h, m ->
                if (h < 7 || h > 20) {
                    Toast.makeText(this, "Jam praktek 07:00 - 20:00", Toast.LENGTH_SHORT).show()
                } else {
                    etJam.setText(String.format("%02d:%02d", h, m))
                }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)
            tpd.show()
        }

        // ====== TOMBOL DAFTAR ======
        btnDaftar.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val usia = etUsia.text.toString().trim()
            val tanggal = etTanggal.text.toString().trim()
            val jam = etJam.text.toString().trim()
            val keluhan = etKeluhan.text.toString().trim()
            val alergiChecked = cbAlergi.isChecked
            val penyakitChecked = cbPenyakit.isChecked
            val detailAlergi = etDetailAlergi.text.toString().trim()
            val detailPenyakit = etDetailPenyakit.text.toString().trim()

            if (nama.isEmpty() || usia.isEmpty() || tanggal.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (alergiChecked && detailAlergi.isEmpty()) {
                Toast.makeText(this, "Harap isi jenis alergi Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (penyakitChecked && detailPenyakit.isEmpty()) {
                Toast.makeText(this, "Harap isi penyakit bawaan Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ====== KONFIRMASI SEDERHANA ======
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi Janji Temu")
                .setMessage("Anda yakin ingin membuat janji temu dengan $dokter hari ini pada jam $jam?")
                .setPositiveButton("Ya") { _, _ ->
                    simpanData(
                        nama,
                        usia,
                        tanggal,
                        jam,
                        keluhan,
                        alergiChecked,
                        penyakitChecked,
                        detailAlergi,
                        detailPenyakit,
                        todayString,
                        dokter
                    )
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun simpanData(
        nama: String,
        usia: String,
        tanggal: String,
        jam: String,
        keluhan: String,
        alergiChecked: Boolean,
        penyakitChecked: Boolean,
        detailAlergi: String,
        detailPenyakit: String,
        todayString: String,
        dokter: String
    ) {
        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val dataString = prefs.getString("dataAntrian", "[]")
        val jsonArray = JSONArray(dataString)

        val newItem = JSONObject().apply {
            put("nama_pasien", nama)
            put("usia", usia)
            put("tanggal", tanggal)
            put("jam", jam)
            put("dokter", dokter)
            put("keluhan", keluhan)
            put("alergi", if (alergiChecked) "Ya: $detailAlergi" else "Tidak")
            put("penyakit_bawaan", if (penyakitChecked) "Ya: $detailPenyakit" else "Tidak")
            put("tanggal_simpan", todayString)
        }

        jsonArray.put(newItem)
        prefs.edit().putString("dataAntrian", jsonArray.toString()).apply()

        Toast.makeText(this, "Janji temu berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, ListAntrianActivity::class.java))
        finish()
    }
}
