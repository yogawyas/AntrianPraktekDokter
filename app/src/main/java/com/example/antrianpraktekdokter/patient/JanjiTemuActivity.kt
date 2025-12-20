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
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class JanjiTemuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etNama: EditText
    private lateinit var etBirthdate: EditText
    private lateinit var etUsia: EditText
    private lateinit var etTanggal: EditText
    private lateinit var etJam: EditText
    private lateinit var etKeluhan: EditText
    private lateinit var tvDokter: TextView
    private lateinit var cbAlergi: CheckBox
    private lateinit var cbPenyakitBawaan: CheckBox
    private lateinit var containerExtraFields: LinearLayout
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var btnBack: com.google.android.material.button.MaterialButton

    // Variabel untuk menyimpan jam praktek dari firebase
    private var jamBuka: String = "09:00"
    private var jamTutup: String = "21:00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_janji_temu)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

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
        containerExtraFields = findViewById(R.id.containerExtraFields)
        btnJanjiTemu = findViewById(R.id.btnJanjiTemu)

        // 1. Age view tidak bisa di edit manual
        etUsia.isEnabled = false

        btnBack.setOnClickListener { finish() }
        etBirthdate.setOnClickListener { showBirthdatePicker() }
        setupAppointmentDatePicker()
        setupTimePicker()
        loadUserData()
        loadConfigPraktek()
        setupCheckboxes()

        btnJanjiTemu.setOnClickListener {
            simpanData()
        }
    }

    private fun showBirthdatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
            val birthDateString = String.format("%02d/%02d/%d", day, month + 1, year)
            etBirthdate.setText(birthDateString)
            val age = calculateAge(year, month, day)
            etUsia.setText(age.toString())
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

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
        val today = Calendar.getInstance()
        etTanggal.setText(String.format("%02d/%02d/%d", today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH) + 1, today.get(Calendar.YEAR)))

        etTanggal.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                etTanggal.setText(String.format("%02d/%02d/%d", day, month + 1, year))
                etJam.setText("") // Reset jam jika tanggal berubah
            }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = today.timeInMillis
                datePicker.maxDate = today.timeInMillis
            }.show()
        }
    }

    private fun setupTimePicker() {
        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            val nowHour = c.get(Calendar.HOUR_OF_DAY)
            val nowMinute = c.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, m ->
                val selectedTime = String.format("%02d:%02d", h, m)

                // Validasi 1: Cek terhadap jam praktek (Admin)
                if (selectedTime < jamBuka || selectedTime > jamTutup) {
                    Toast.makeText(this, "Dokter hanya praktek pukul $jamBuka - $jamTutup", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                // Validasi 2: Cek apakah waktu sudah terlewat (Real-time)
                if (h < nowHour || (h == nowHour && m <= nowMinute)) {
                    Toast.makeText(this, "Waktu sudah terlewat, pilih jam lain!", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                etJam.setText(selectedTime)
            }, nowHour, nowMinute, true).show()
        }
    }

    private fun loadConfigPraktek() {
        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                jamBuka = doc.getString("bukaJam") ?: "09:00"
                jamTutup = doc.getString("tutupJam") ?: "21:00"
                val isOpen = doc.getBoolean("isOpen") ?: false
                tvDokter.text = "Dr. Alexander (Umum)\nJam Praktek: $jamBuka - $jamTutup\nStatus: ${if (isOpen) "Buka" else "Tutup"}"
            }
    }

    private fun simpanData() {
        val nama = etNama.text.toString().trim()
        val birthdate = etBirthdate.text.toString().trim()
        val usia = etUsia.text.toString().trim()
        val tanggal = etTanggal.text.toString().trim()
        val jam = etJam.text.toString().trim()
        val keluhan = etKeluhan.text.toString().trim()

        // Validasi Field Wajib
        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (birthdate.isEmpty()) {
            Toast.makeText(this, "Tanggal lahir wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (jam.isEmpty()) {
            Toast.makeText(this, "Waktu janji temu wajib dipilih!", Toast.LENGTH_SHORT).show()
            return
        }
        if (keluhan.isEmpty()) {
            Toast.makeText(this, "Symptoms/Keluhan wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        processSavingToFirebase(nama, usia, tanggal, jam, keluhan)
    }

    private fun processSavingToFirebase(nama: String, usia: String, tanggal: String, jam: String, keluhan: String) {
        // Ambil data alergi/penyakit jika ada
        val alergi = if (cbAlergi.isChecked && containerExtraFields.childCount > 0)
            (containerExtraFields.getChildAt(0) as? EditText)?.text.toString() else ""
        val penyakit = if (cbPenyakitBawaan.isChecked)
            (containerExtraFields.getChildAt(if (cbAlergi.isChecked) 1 else 0) as? EditText)?.text.toString() else ""

        val user = auth.currentUser ?: return
        val todayString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

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

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    etNama.setText(doc.getString("nama") ?: user.email)
                }
        }
    }

    private fun setupCheckboxes() {
        cbAlergi.setOnCheckedChangeListener { _, _ -> updateExtraFields() }
        cbPenyakitBawaan.setOnCheckedChangeListener { _, _ -> updateExtraFields() }
    }

    private fun updateExtraFields() {
        containerExtraFields.removeAllViews()
        if (cbAlergi.isChecked) addEditText("Tulis alergi obat")
        if (cbPenyakitBawaan.isChecked) addEditText("Tulis penyakit bawaan")
    }

    private fun addEditText(hintText: String) {
        val editText = EditText(this).apply {
            hint = hintText
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dpToPx(8)) }
        }
        containerExtraFields.addView(editText)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}