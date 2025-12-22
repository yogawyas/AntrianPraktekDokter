package com.example.antrianpraktekdokter.patient

import java.util.Date

data class NotifModel(
    var id: String = "",
    val user_id: String = "",
    val nama_pasien: String = "",
    val message: String = "",
    val timestamp: Date? = null,
    val type: String = "Called",
    @field:JvmField val isRead: Boolean = false
)