package com.example.antrianpraktekdokter.model

import com.google.firebase.Timestamp

data class Antrian(
    var id: String = "",
    val nama_pasien: String = "",
    val jam: String = "",
    val keluhan: String = "",
    val tanggal_simpan: String = "",
    val selesai: Boolean = false,
    val dipanggil: Int = 0,
    val dihapus: Boolean = false,
    val nomor_antrian: Int = 0,
    val user_id: String? = null,
    val createdAt: Timestamp? = null
)
{
    constructor() : this("", "", "", "", "", false, 0, false, 0, null, null)
}