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
import android.widget.TextView


class LihatLaporanFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var spinnerPeriode: Spinner
    private lateinit var recyclerLaporan: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lihat_laporan, container, false)

        db = FirebaseFirestore.getInstance()
        spinnerPeriode = view.findViewById(R.id.spinnerPeriode)
        recyclerLaporan = view.findViewById(R.id.recyclerLaporan)

        spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val periode = parent?.getItemAtPosition(position).toString()
                loadLaporan(periode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    private fun loadLaporan(periode: String) {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val startDate: String
        val endDate = sdf.format(Date())

        when (periode) {
            "Per Hari" -> {
                // DatePicker for hari
                val c = Calendar.getInstance()
                DatePickerDialog(requireContext(), { _, year, month, day ->
                    val selectedDate =
                        sdf.format(Calendar.getInstance().apply { set(year, month, day) }.time)
                    fetchLaporan(selectedDate, selectedDate)
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
            "Per Minggu" -> {
                // Spinner for minggu 1-4
                val weeks = arrayOf("Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4")
                val weekSpinner = Spinner(requireContext())
                weekSpinner.adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weeks)
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Minggu")
                    .setView(weekSpinner)
                    .setPositiveButton("OK") { _, _ ->
                        val selectedWeek = weekSpinner.selectedItem.toString()
                        // Hitung start/end minggu (contoh: Minggu 1 = tgl 1-7 bulan ini)
                        val month = cal.get(Calendar.MONTH)
                        val year = cal.get(Calendar.YEAR)
                        val weekNum = selectedWeek.replace("Minggu ", "").toInt()
                        val startDay = (weekNum - 1) * 7 + 1
                        val endDay = startDay + 6
                        val start = sdf.format(Calendar.getInstance().apply { set(year, month, startDay) }.time)
                        val end = sdf.format(Calendar.getInstance().apply { set(year, month, endDay) }.time)
                        fetchLaporan(start, end)
                    }
                    .show()
            }
            "Per Bulan" -> {
                // Spinner for bulan
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
                val laporanList = snapshot.documents.map { doc ->
                    // Hitung per doc atau agregat (contoh: list tanggal, total, selesai)
                    LaporanItem(doc.getString("tanggal_simpan") ?: "", snapshot.size(), snapshot.documents.count { it.getBoolean("selesai") == true })
                }
                recyclerLaporan.adapter = LaporanAdapter(laporanList)
            }
    }
}

// LaporanItem (data class)
data class LaporanItem(val tanggal: String, val total: Int, val selesai: Int)

// LaporanAdapter (mirip AntrianAdapter, gunakan TableLayout jika mau tabel, tapi RecyclerView lebih flexible)
class LaporanAdapter(private val laporanList: List<LaporanItem>) : RecyclerView.Adapter<LaporanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvSelesai: TextView = view.findViewById(R.id.tvSelesai)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = laporanList[position]
        holder.tvTanggal.text = item.tanggal
        holder.tvTotal.text = "Total: ${item.total}"
        holder.tvSelesai.text = "Selesai: ${item.selesai}"
    }

    override fun getItemCount() = laporanList.size
}