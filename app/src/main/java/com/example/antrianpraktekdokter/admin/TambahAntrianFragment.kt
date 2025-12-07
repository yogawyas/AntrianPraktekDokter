package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.Query
class TambahAntrianFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerAntrian: RecyclerView
    private lateinit var fabTambah: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tambah_antrian, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerAntrian = view.findViewById(R.id.recyclerAntrian)
        fabTambah = view.findViewById(R.id.fabTambah)

        recyclerAntrian.layoutManager = LinearLayoutManager(context)

        fabTambah.setOnClickListener {
            showTambahAntrianDialog()
        }

        loadAntrian()

        return view
    }

    private fun loadAntrian() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)
            .orderBy("jam", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val antrianList = snapshot?.documents ?: emptyList()
                recyclerAntrian.adapter = AntrianAdapter(antrianList)
            }
    }

    private fun showTambahAntrianDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tambah_antrian, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etJam = dialogView.findViewById<EditText>(R.id.etJam)
        val etKeluhan = dialogView.findViewById<EditText>(R.id.etKeluhan)

        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                etJam.setText(String.format("%02d:%02d", h, m))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Antrian Manual")
            .setView(dialogView)
            .setPositiveButton("Tambah") { _, _ ->
                val nama = etNama.text.toString()
                val jam = etJam.text.toString()
                val keluhan = etKeluhan.text.toString()
                if (nama.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
                    Toast.makeText(context, "Isi semua field!", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Antrian penuh hari ini!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        db.collection("antrian")
                            .whereEqualTo("tanggal_simpan", today)
                            .whereEqualTo("jam", jam)
                            .whereEqualTo("dihapus", false)
                            .get()
                            .addOnSuccessListener { jamSnapshot ->
                                if (jamSnapshot.size() >= 2) {
                                    Toast.makeText(context, "Jam $jam sudah penuh!", Toast.LENGTH_SHORT).show()
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
                                            Toast.makeText(context, "Antrian ditambahkan! Nomor: $newNomor", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}