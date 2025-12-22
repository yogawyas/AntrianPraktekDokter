package com.example.antrianpraktekdokter.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.antrianpraktekdokter.model.JadwalConfig
import com.example.antrianpraktekdokter.repository.AdminRepository

class JadwalViewModel : ViewModel() {
    private val repository = AdminRepository()

    // Data Jadwal yang akan diamati oleh Fragment
    private val _jadwalConfig = MutableLiveData<JadwalConfig>()
    val jadwalConfig: LiveData<JadwalConfig> get() = _jadwalConfig

    // Pesan notifikasi (Toast) untuk Fragment
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    // Fungsi 1: Load data saat layar dibuka
    fun loadJadwal() {
        repository.getJadwal { config ->
            _jadwalConfig.value = config
        }
    }

    // Fungsi 2: Saat Switch ditekan
    fun updateStatus(isOpen: Boolean) {
        repository.updateStatusPraktik(isOpen) { success ->
            if (success) {
                _message.value = if (isOpen) "Praktek DIBUKA" else "Praktek DITUTUP"
            } else {
                _message.value = "Gagal update status"
                // Kembalikan switch ke posisi semula (reload data)
                loadJadwal()
            }
        }
    }

    // Fungsi 3: Saat tombol Simpan ditekan
    fun saveJadwal(buka: String, tutup: String) {
        if (buka.isEmpty() || tutup.isEmpty()) {
            _message.value = "Jam buka dan tutup harus diisi!"
            return
        }
        repository.updateJamPraktik(buka, tutup) { success ->
            if (success) _message.value = "Jadwal berhasil disimpan"
            else _message.value = "Gagal menyimpan jadwal"
        }
    }
}