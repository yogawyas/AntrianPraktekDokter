package com.example.antrianpraktekdokter.patient

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.model.MLClient // Sesuaikan package path MLClient Anda
import com.example.antrianpraktekdokter.model.PredictionRequest
import com.example.antrianpraktekdokter.model.PredictionResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
        etTanggal.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(today.time))

        etTanggal.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                etTanggal.setText(String.format("%02d/%02d/%d", day, month + 1, year))
                etJam.setText("")
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
                if (selectedTime < jamBuka || selectedTime > jamTutup) {
                    Toast.makeText(this, "Dokter hanya praktek pukul $jamBuka - $jamTutup", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                if (h < nowHour || (h == nowHour && m <= nowMinute)) {
                    Toast.makeText(this, "Waktu sudah terlewat!", Toast.LENGTH_SHORT).show()
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
        val jam = etJam.text.toString().trim()
        val keluhan = etKeluhan.text.toString().trim()

        if (nama.isEmpty() || birthdate.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        btnJanjiTemu.isEnabled = false
        processMLAndSave(nama, usia, jam, keluhan)
    }

    private fun processMLAndSave(nama: String, usia: String, jam: String, keluhan: String) {
        val currentTime = Calendar.getInstance()
        val appointmentTime = Calendar.getInstance()

        try {
            val timeParts = jam.split(":")
            appointmentTime.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            appointmentTime.set(Calendar.MINUTE, timeParts[1].toInt())

            val diffInMillis = appointmentTime.timeInMillis - currentTime.timeInMillis
            val diffInHours = diffInMillis.toDouble() / (1000 * 60 * 60)

            // Konversi usia ke Int untuk logika
            val usiaInt = usia.toIntOrNull() ?: 0

            // Buat request input ML
            val inputML = PredictionRequest(
                gender = if (usiaInt % 2 == 0) 1 else 0,
                age = usiaInt,
                neighbourhood = (30..80).random(),
                scholarship = 0,
                hipertension = if (usiaInt > 50) 1 else 0,
                diabetes = 0,
                alcoholism = 0,
                handcap = 0,
                sms_received = if (diffInHours > 4) 1 else 0, // Gunakan diffInHours yang sudah dihitung
                date_diff = 0
            )

            MLClient.instance.getPrediction(inputML).enqueue(object : Callback<PredictionResponse> {
                override fun onResponse(call: Call<PredictionResponse>, response: Response<PredictionResponse>) {
                    val score = if (response.isSuccessful) response.body()?.probability ?: 0.5 else 0.5
                    saveFinalData(nama, usia, jam, keluhan, score, diffInHours)
                }
                override fun onFailure(call: Call<PredictionResponse>, t: Throwable) {
                    saveFinalData(nama, usia, jam, keluhan, 0.5, diffInHours)
                }
            })
        } catch (e: Exception) {
            btnJanjiTemu.isEnabled = true
            Toast.makeText(this, "Kesalahan format waktu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFinalData(nama: String, usia: String, jam: String, keluhan: String, mlScore: Double, hourInterval: Double) {
        val todayString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val user = auth.currentUser ?: return

        db.runTransaction { transaction ->
            val countRef = db.collection("config").document("antrian_count_$todayString")
            val snapshot = transaction.get(countRef)
            val newCount = (snapshot.getLong("count") ?: 0) + 1
            transaction.set(countRef, hashMapOf("count" to newCount))
            newCount
        }.addOnSuccessListener { newNomor ->
            val newItem = hashMapOf(
                "nama_pasien" to nama,
                "usia" to usia,
                "prediction_score" to mlScore,
                "hour_interval" to hourInterval,
                "jam" to jam,
                "tanggal_simpan" to todayString,
                "selesai" to false,
                "dihapus" to false,
                "nomor_antrian" to newNomor,
                "user_id" to user.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("antrian").add(newItem).addOnSuccessListener {
                Toast.makeText(this, "Janji Temu Berhasil!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            btnJanjiTemu.isEnabled = true
            Toast.makeText(this, "Gagal menyimpan antrian", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            etNama.setText(doc.getString("nama") ?: "")
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