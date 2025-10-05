package com.example.antrianpraktekdokter

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class JanjiTemuActivity : AppCompatActivity() {

    // 🔹 Helper SharedPreferences
    class PrefsHelper(context: android.content.Context) {
        private val prefs = context.getSharedPreferences("AntrianPrefs", MODE_PRIVATE)

        fun saveList(list: List<Map<String, String>>) {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("nama", item["nama"])
                obj.put("jam", item["jam"])
                obj.put("dokter", item["dokter"])
                jsonArray.put(obj)
            }
            prefs.edit().putString("listAntrian", jsonArray.toString()).apply()
        }

        fun loadList(): MutableList<Map<String, String>> {
            val jsonStr = prefs.getString("listAntrian", null) ?: return mutableListOf()
            val jsonArray = JSONArray(jsonStr)
            val result = mutableListOf<Map<String, String>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    mapOf(
                        "nama" to obj.getString("nama"),
                        "jam" to obj.getString("jam"),
                        "dokter" to obj.getString("dokter")
                    )
                )
            }
            return result
        }
    }

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
        val spKeluhan = findViewById<Spinner>(R.id.spKeluhan)
        val spDokter = findViewById<Spinner>(R.id.spDokter)
        val btnDaftar = findViewById<Button>(R.id.btnDaftar)

        val keluhanList = listOf("Demam", "Sakit Gigi", "Mata Perih", "Batuk", "Sakit Perut")
        val dokterMap = mapOf(
            "Demam" to listOf("Dr. Andi (Umum)", "Dr. Budi (Umum)"),
            "Sakit Gigi" to listOf("Drg. Citra (Gigi)", "Drg. Danu (Gigi)"),
            "Mata Perih" to listOf("Dr. Eka (Mata)", "Dr. Fajar (Mata)"),
            "Batuk" to listOf("Dr. Gita (Umum)", "Dr. Hadi (Umum)"),
            "Sakit Perut" to listOf("Dr. Indah (Penyakit Dalam)", "Dr. Joko (Penyakit Dalam)")
        )

        val keluhanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, keluhanList)
        keluhanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spKeluhan.adapter = keluhanAdapter

        spKeluhan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                val selectedKeluhan = keluhanList[pos]
                val dokterList = dokterMap[selectedKeluhan] ?: listOf("Tidak ada dokter")
                val dokterAdapter = ArrayAdapter(this@JanjiTemuActivity, android.R.layout.simple_spinner_item, dokterList)
                dokterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spDokter.adapter = dokterAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        etTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> etTanggal.setText("$d/${m + 1}/$y") },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            val tpd = TimePickerDialog(this, { _, h, m ->
                if (h < 8 || h > 20) {
                    Toast.makeText(this, "Jam praktek 08:00 - 20:00", Toast.LENGTH_SHORT).show()
                } else {
                    etJam.setText(String.format("%02d:%02d", h, m))
                }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true)
            tpd.show()
        }

        btnDaftar.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val usia = etUsia.text.toString().trim()
            val tanggal = etTanggal.text.toString().trim()
            val jam = etJam.text.toString().trim()
            val dokter = spDokter.selectedItem.toString()

            if (nama.isEmpty() || usia.isEmpty() || tanggal.isEmpty() || jam.isEmpty()) {
                Toast.makeText(this, "Harap isi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = PrefsHelper(this)
            val list = prefs.loadList()

            list.add(
                mapOf(
                    "nama" to "Nama: $nama",
                    "jam" to "Jam: $jam",
                    "dokter" to dokter
                )
            )

            prefs.saveList(list) // simpan permanen

            val intent = Intent(this, ListAntrianActivity::class.java)
            intent.putExtra("dokter", dokter)
            startActivity(intent)
        }
    }
}
