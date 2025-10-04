package com.example.antrianpraktekdokter

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        val spDokter = findViewById<Spinner>(R.id.spDokter)
        val spKeluhan = findViewById<Spinner>(R.id.spKeluhan)
        val btnDaftar = findViewById<Button>(R.id.btnDaftar)

        // --- Data Keluhan dan Dokter ---
        val keluhanList = listOf("Demam", "Sakit Gigi", "Mata Perih", "Batuk", "Sakit Perut")

        val dokterMap = mapOf(
            "Demam" to listOf("Dr. Andi (Umum)", "Dr. Budi (Umum)"),
            "Sakit Gigi" to listOf("Drg. Citra (Dokter Gigi)", "Drg. Danu (Dokter Gigi)"),
            "Mata Perih" to listOf("Dr. Eka (Spesialis Mata)", "Dr. Fajar (Spesialis Mata)"),
            "Batuk" to listOf("Dr. Gita (Umum)", "Dr. Hadi (Umum)"),
            "Sakit Perut" to listOf("Dr. Indah (Spesialis Penyakit Dalam)", "Dr. Joko (Spesialis Penyakit Dalam)")
        )

        // --- Spinner Keluhan ---
        val keluhanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, keluhanList)
        keluhanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spKeluhan.adapter = keluhanAdapter

        // --- Spinner Dokter dinamis sesuai keluhan ---
        spKeluhan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedKeluhan = keluhanList[position]
                val dokterList = dokterMap[selectedKeluhan] ?: listOf("Tidak ada dokter")
                val dokterAdapter = ArrayAdapter(this@JanjiTemuActivity, android.R.layout.simple_spinner_item, dokterList)
                dokterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spDokter.adapter = dokterAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Date Picker ---
        etTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            val dpd = DatePickerDialog(this, { _, y, m, d ->
                etTanggal.setText("$d/${m + 1}/$y")
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            dpd.show()
        }

        // --- Time Picker dengan validasi jam 08:00 – 20:00 ---
        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this, { _, h, m ->
                if (h < 8 || h > 20) {
                    Toast.makeText(this, "Jam praktek hanya 08:00 - 20:00", Toast.LENGTH_SHORT).show()
                } else {
                    etJam.setText(String.format("%02d:%02d", h, m))
                }
            }, hour, minute, true)
            tpd.show()
        }

        // --- Tombol Daftar ---
        btnDaftar.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val usiaStr = etUsia.text.toString().trim()
            val tanggal = etTanggal.text.toString().trim()
            val jam = etJam.text.toString().trim()
            val keluhan = spKeluhan.selectedItem.toString()
            val dokter = spDokter.selectedItem.toString()

            // Validasi input
            if (nama.isEmpty() || usiaStr.isEmpty() || tanggal.isEmpty() || jam.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi usia harus angka > 0
            val usia = usiaStr.toIntOrNull()
            if (usia == null || usia <= 0) {
                Toast.makeText(this, "Usia tidak valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Menampilkan hasil (sementara pakai Toast)
            val hasil = """
                Nama: $nama
                Usia: $usia tahun
                Tanggal: $tanggal
                Jam: $jam
                Keluhan: $keluhan
                Dokter: $dokter
            """.trimIndent()

            Toast.makeText(this, hasil, Toast.LENGTH_LONG).show()
        }
    }
}
