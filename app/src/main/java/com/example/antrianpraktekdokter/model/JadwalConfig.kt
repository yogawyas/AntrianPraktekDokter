package com.example.antrianpraktekdokter.model

data class JadwalConfig(
    val isOpen: Boolean = false,
    val bukaJam: String = "",
    val tutupJam: String = ""
) {
    // Constructor kosong Firebase
    constructor() : this(false, "", "")
}