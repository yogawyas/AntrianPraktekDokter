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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class JanjiTemuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Elements
    private lateinit var etNama: EditText
    private lateinit var etBirthdate: EditText
    private lateinit var etUsia: EditText
    private lateinit var etTanggal: EditText
    private lateinit var etJam: EditText
    private lateinit var etKeluhan: EditText
    private lateinit var tvDokter: TextView
    private lateinit var cbAlergi: CheckBox
    private lateinit var cbPenyakitBawaan: CheckBox

    // Pastikan ID ini ada di XML Anda (Lihat catatan di bawah)
    private lateinit var containerExtraFields: LinearLayout

    // Ganti btnDaftar (Button) menjadi btnJanjiTemu (ImageButton)
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var btnBack: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_janji_temu)

        // Init Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Bind Views
        btnBack = findViewById(R.id.btnBack)
        etNama = findViewById(R.id.etNama)
        etBirthdate = findViewById(R.id.etBirthdate)
        etUsia = findViewById(R.id.etUsia)
        etTanggal = findViewById(R.id.etTanggal)
        etJam = findViewById(R.id.etJam)
        etKeluhan = findViewById(R.id.etKeluhan)
        tvDokter = findViewById(R.id.tvDokter)
        cbAlergi = findViewById(R.id.cbAlergi)
        cbPenyakitBawaan = findViewById(R.id.cbPenyakitBawaan)

        // PENTING: Tambahkan LinearLayout dengan ID ini di XML Anda agar tidak crash!
        containerExtraFields = findViewById(R.id.containerExtraFields)

        // Ubah binding ke tombol ImageButton Anda
        btnJanjiTemu = findViewById(R.id.btnJanjiTemu)

        // Tombol Back
        btnBack.setOnClickListener { finish() }

        // Logic Birthdate & Auto-Age
        etBirthdate.setOnClickListener {
            showBirthdatePicker()
        }

        // Logic Appointment Date
        setupAppointmentDatePicker()

        // Logic Time Picker
        setupTimePicker()

        // Load Data User & Config
        loadUserData()
        loadConfigPraktek()

        // Checkbox Listener
        setupCheckboxes()

        // Button Daftar (Sekarang menggunakan btnJanjiTemu)
        btnJanjiTemu.setOnClickListener {
            simpanData()
        }
    }

    private fun showBirthdatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val birthDateString = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            etBirthdate.setText(birthDateString)
            val age = calculateAge(selectedYear, selectedMonth, selectedDay)
            etUsia.setText(age.toString())
        }, year, month, day)

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()
        dob.set(year, month, day)
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return if (age < 0) 0 else age
    }

    private fun setupAppointmentDatePicker() {
        val c = Calendar.getInstance()
        val today = Calendar.getInstance()
        etTanggal.setText(String.format("%02d/%02d/%d", today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH) + 1, today.get(Calendar.YEAR)))

        etTanggal.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                if (selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    etTanggal.setText(String.format("%02d/%02d/%d", day, month + 1, year))
                } else {
                    Toast.makeText(this, "Hanya bisa membuat janji untuk hari ini!", Toast.LENGTH_SHORT).show()
                }
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = today.timeInMillis
                datePicker.maxDate = today.timeInMillis
            }.show()
        }
    }

    private fun setupTimePicker() {
        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                val selectedHour = h
                if (selectedHour >= 0 && selectedHour <= 21) {
                    etJam.setText(String.format("%02d:%02d", h, m))
                } else {
                    Toast.makeText(this, "Jam antrian hanya 00:00 - 21:00!", Toast.LENGTH_SHORT).show()
                }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    etNama.setText(doc.getString("nama") ?: user.email)
                }
        }
    }

    private fun loadConfigPraktek() {
        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                val buka = doc.getString("bukaJam") ?: "08:00"
                val tutup = doc.getString("tutupJam") ?: "17:00"
                val isOpen = doc.getBoolean("isOpen") ?: false
                tvDokter.text = "Dr. Alexander (Umum)\nJam Praktek: $buka - $tutup\nStatus: ${if (isOpen) "Buka" else "Tutup"}"
            }
    }

    private fun setupCheckboxes() {
        cbAlergi.setOnCheckedChangeListener { _, isChecked -> updateExtraFields() }
        cbPenyakitBawaan.setOnCheckedChangeListener { _, isChecked -> updateExtraFields() }
    }

    private fun updateExtraFields() {
        containerExtraFields.removeAllViews()
        if (cbAlergi.isChecked) {
            addEditText("Tulis alergi obat")
        }
        if (cbPenyakitBawaan.isChecked) {
            addEditText("Tulis penyakit bawaan")
        }
    }

    private fun addEditText(hintText: String) {
        val editText = EditText(this).apply {
            hint = hintText
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(8)) }
            backgroundTintList = etNama.backgroundTintList
        }
        containerExtraFields.addView(editText)
    }

    private fun simpanData() {
        val nama = etNama.text.toString().trim()
        val usia = etUsia.text.toString().trim()
        val tanggal = etTanggal.text.toString().trim()
        val jam = etJam.text.toString().trim()
        val keluhan = etKeluhan.text.toString().trim()

        var alergi = ""
        var penyakitBawaan = ""

        val childCount = containerExtraFields.childCount
        if (cbAlergi.isChecked && childCount > 0) {
            alergi = (containerExtraFields.getChildAt(0) as? EditText)?.text.toString()
        }
        if (cbPenyakitBawaan.isChecked) {
            val index = if (cbAlergi.isChecked) 1 else 0
            if (childCount > index) {
                penyakitBawaan = (containerExtraFields.getChildAt(index) as? EditText)?.text.toString()
            }
        }

        if (nama.isEmpty() || usia.isEmpty() || tanggal.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi data", Toast.LENGTH_SHORT).show()
            return
        }

        processSavingToFirebase(nama, usia, tanggal, jam, keluhan, alergi, penyakitBawaan)
    }

    private fun processSavingToFirebase(nama: String, usia: String, tanggal: String, jam: String, keluhan: String, alergi: String, penyakit: String) {
        val user = auth.currentUser ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = sdf.format(Date())

        if (tanggal != todayString) {
            Toast.makeText(this, "Hanya bisa membuat janji untuk hari ini!", Toast.LENGTH_SHORT).show()
            return
        }

        db.runTransaction { transaction ->
            val countRef = db.collection("config").document("antrian_count_$tanggal")
            val snapshot = transaction.get(countRef)
            val newCount = (snapshot.getLong("count") ?: 0) + 1
            transaction.set(countRef, hashMapOf("count" to newCount))
            newCount
        }.addOnSuccessListener { newNomor ->
            val newItem = hashMapOf(
                "nama_pasien" to nama,
                "usia" to usia,
                "tanggal_lahir" to etBirthdate.text.toString(),
                "tanggal_simpan" to tanggal,
                "jam" to jam,
                "keluhan" to keluhan,
                "alergi" to alergi,
                "penyakit_bawaan" to penyakit,
                "selesai" to false,
                "dihapus" to false,
                "nomor_antrian" to newNomor,
                "createdAt" to FieldValue.serverTimestamp(),
                "user_id" to user.uid
            )

            db.collection("antrian").add(newItem).addOnSuccessListener {
                Toast.makeText(this, "Berhasil! Nomor Antrian: $newNomor", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}