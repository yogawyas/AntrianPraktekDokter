package com.example.antrianpraktekdokter.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
//import java.text.StringFormat
import java.util.Calendar
import android.app.TimePickerDialog

class AturJadwalFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var switchStatusPraktik: Switch
    private lateinit var etJamBuka: EditText
    private lateinit var etJamTutup: EditText
    private lateinit var btnSimpanJadwal: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_atur_jadwal, container, false)

        db = FirebaseFirestore.getInstance()
        switchStatusPraktik = view.findViewById(R.id.switchStatusPraktik)
        etJamBuka = view.findViewById(R.id.etJamBuka)
        etJamTutup = view.findViewById(R.id.etJamTutup)
        btnSimpanJadwal = view.findViewById(R.id.btnSimpanJadwal)

        // Load existing jadwal
        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                switchStatusPraktik.isChecked = doc.getBoolean("isOpen") ?: false
                etJamBuka.setText(doc.getString("bukaJam") ?: "")
                etJamTutup.setText(doc.getString("tutupJam") ?: "")
            }

        etJamBuka.setOnClickListener { showTimePicker(etJamBuka) }
        etJamTutup.setOnClickListener { showTimePicker(etJamTutup) }

        switchStatusPraktik.setOnCheckedChangeListener { _, isChecked ->
            db.collection("config").document("status_praktik")
                .set(hashMapOf("isOpen" to isChecked), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, if (isChecked) "Praktek dibuka" else "Praktek ditutup", Toast.LENGTH_SHORT).show()
                }
        }

        btnSimpanJadwal.setOnClickListener {
            val buka = etJamBuka.text.toString()
            val tutup = etJamTutup.text.toString()
            if (buka.isEmpty() || tutup.isEmpty()) {
                Toast.makeText(context, "Isi jam buka dan tutup!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("config").document("status_praktik")
                .set(hashMapOf("bukaJam" to buka, "tutupJam" to tutup), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Jadwal disimpan", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }

    private fun showTimePicker(et: EditText) {
        val c = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m ->
            et.setText(String.format("%02d:%02d", h, m))
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }
}