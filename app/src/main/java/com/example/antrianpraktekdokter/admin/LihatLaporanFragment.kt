package com.example.antrianpraktekdokter.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.NonDisposableHandle.parent

class LihatLaporanFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var btnPilihPeriode: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerLaporan: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lihat_laporan, container, false)

        db = FirebaseFirestore.getInstance()
        btnPilihPeriode = view.findViewById(R.id.btnPilihPeriode)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerLaporan = view.findViewById(R.id.recyclerLaporan)

        recyclerLaporan.layoutManager = LinearLayoutManager(context)
        recyclerLaporan.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        btnPilihPeriode.setOnClickListener {
            showPeriodeDialog()
        }

        return view
    }

    private fun showPeriodeDialog() {
        val periods = arrayOf("Per Hari", "Per Minggu", "Per Bulan")
        val spinner = Spinner(requireContext())
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Periode")
            .setView(spinner)
            .setPositiveButton("OK") { _, _ ->
                val periode = spinner.selectedItem.toString()
                loadLaporan(periode)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadLaporan(periode: String) {
        progressBar.visibility = View.VISIBLE

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        when (periode) {
            "Per Hari" -> {
                val c = Calendar.getInstance()
                DatePickerDialog(requireContext(), { _, year, month, day ->
                    val selectedDate = sdf.format(Calendar.getInstance().apply { set(year, month, day) }.time)
                    fetchLaporan(selectedDate, selectedDate)
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
            "Per Minggu" -> {
                val weeks = arrayOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
                val weekSpinner = Spinner(requireContext())
                weekSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weeks)
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Minggu")
                    .setView(weekSpinner)
                    .setPositiveButton("OK") { _, _ ->
                        val selectedWeek = weekSpinner.selectedItem.toString()
                        val month = cal.get(Calendar.MONTH)
                        val year = cal.get(Calendar.YEAR)
                        val weekNum = selectedWeek.replace("Minggu ", "").toInt()
                        val startDay = (weekNum - 1) * 7 + 1
                        val endDay = startDay + 6
                        val start = sdf.format(Calendar.getInstance().apply { set(year, month, startDay) }.time)
                        val end = sdf.format(Calendar.getInstance().apply { set(year, month, endDay) }.time)
                        fetchLaporan(start, end)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            "Per Bulan" -> {
                val months = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                val monthSpinner = Spinner(requireContext())
                monthSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Bulan")
                    .setView(monthSpinner)
                    .setPositiveButton("OK") { _, _ ->
                        val selectedMonth = monthSpinner.selectedItemPosition
                        cal.set(Calendar.MONTH, selectedMonth)
                        val start = sdf.format(cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.time)
                        val end = sdf.format(cal.apply { set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)) }.time)
                        fetchLaporan(start, end)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    private fun fetchLaporan(startDate: String, endDate: String) {
        db.collection("antrian")
            .whereGreaterThanOrEqualTo("tanggal_simpan", startDate)
            .whereLessThanOrEqualTo("tanggal_simpan", endDate)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                val laporanList = snapshot.documents.map { doc ->
                    LaporanItem(
                        nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0,
                        namaPasien = doc.getString("nama_pasien") ?: "",
                        usia = doc.getString("usia") ?: "N/A",
                        keluhan = doc.getString("keluhan") ?: "N/A",
                        selesai = doc.getBoolean("selesai") ?: false,
                        tanggal = doc.getString("tanggal_simpan") ?: ""
                    )
                }
                recyclerLaporan.adapter = LaporanAdapter(laporanList)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class LaporanItem(
    val nomor: Int,
    val namaPasien: String,
    val usia: String,
    val keluhan: String,
    val selesai: Boolean,
    val tanggal: String
)

class LaporanAdapter(private val laporanList: List<LaporanItem>) : RecyclerView.Adapter<LaporanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomor: TextView = view.findViewById(R.id.tvNomor)
        val tvNamaPasien: TextView = view.findViewById(R.id.tvNamaPasien)
        val tvUsia: TextView = view.findViewById(R.id.tvUsia)
        val tvKeluhan: TextView = view.findViewById(R.id.tvKeluhan)
        val tvSelesai: TextView = view.findViewById(R.id.tvSelesai)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = laporanList[position]
        holder.tvNomor.text = item.nomor.toString()
        holder.tvNamaPasien.text = item.namaPasien
        holder.tvUsia.text = item.usia
        holder.tvKeluhan.text = item.keluhan
        holder.tvSelesai.text = if (item.selesai) "Ya" else "Tidak"
        holder.tvTanggal.text = item.tanggal

        // Warna baris selesai hijau
        holder.itemView.setBackgroundColor(
            if (item.selesai) ContextCompat.getColor(holder.itemView.context, R.color.green_light)
            else ContextCompat.getColor(holder.itemView.context, android.R.color.white)
        )
    }

    override fun getItemCount() = laporanList.size
}