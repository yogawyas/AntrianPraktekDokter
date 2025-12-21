package com.example.antrianpraktekdokter.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.antrianpraktekdokter.model.Antrian
import com.example.antrianpraktekdokter.repository.AdminRepository
import java.text.SimpleDateFormat
import java.util.*

class ListAntrianViewModel : ViewModel() {
    private val repository = AdminRepository()

    // List Antrian untuk RecyclerView
    private val _antrianList = MutableLiveData<List<Antrian>>()
    val antrianList: LiveData<List<Antrian>> get() = _antrianList

    // Statistik (Jumlah Selesai & Sisa)
    private val _stats = MutableLiveData<Pair<Int, Int>>() // Pair(Selesai, Sisa)
    val stats: LiveData<Pair<Int, Int>> get() = _stats

    // Fungsi 1: Load antrian hari ini
    fun loadAntrian(showCompleted: Boolean) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        // Panggil repository dan masukkan hasilnya ke _antrianList
        repository.listenToAntrian(today, showCompleted, _antrianList)
    }

    // Fungsi 2: Hitung statistik (dipanggil di Fragment setiap list berubah)
    fun calculateStats(list: List<Antrian>) {
        val selesai = list.count { it.selesai }
        val sisa = list.count { !it.selesai }
        _stats.value = Pair(selesai, sisa)
    }

    // Fungsi 3: Update Checklist Selesai
    fun updateStatusSelesai(antrian: Antrian, isChecked: Boolean) {
        repository.updateAntrianStatus(antrian.id, "selesai", isChecked) { }
    }

    // Fungsi 4: Panggil Pasien
    fun panggilPasien(antrian: Antrian) {
        val newCount = antrian.dipanggil + 1
        repository.updateAntrianStatus(antrian.id, "dipanggil", newCount) { }
    }

    // Fungsi 5: Hapus Pasien (Cancel)
    fun hapusPasien(antrian: Antrian) {
        repository.updateAntrianStatus(antrian.id, "dihapus", true) { }
    }
}