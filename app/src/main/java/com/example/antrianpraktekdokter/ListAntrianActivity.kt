package com.example.antrianpraktekdokter

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ListAntrianActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_antrian)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        renderAntrian()
    }

    override fun onResume() {
        super.onResume()
        renderAntrian()
    }

    private fun renderAntrian() {
        val container = findViewById<LinearLayout>(R.id.containerAntrian)
        container.removeAllViews()

        val dokterDipilih = intent.getStringExtra("dokter")
        val list = JanjiTemuActivity.listAntrian

        val filtered = list.filter { it["dokter"] == dokterDipilih }

        if (filtered.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada antrian untuk $dokterDipilih"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }
            container.addView(tvEmpty)
            return
        }

        for ((index, item) in filtered.withIndex()) {
            val card = CardView(this).apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                layoutParams = lp
                radius = dpToPx(10).toFloat()
                cardElevation = dpToPx(6).toFloat()
                setCardBackgroundColor(Color.WHITE)
                useCompatPadding = true
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val tvNomor = TextView(this).apply {
                text = "No: ${index + 1}" // Nomor antrian per dokter
                textSize = 18f
                setTextColor(Color.parseColor("#2196F3"))
            }

            val tvNama = TextView(this).apply {
                text = item["nama"] ?: "Nama: -"
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(0, dpToPx(6), 0, 0)
            }

            val tvJam = TextView(this).apply {
                text = item["jam"] ?: "Jam: -"
                textSize = 14f
                setTextColor(Color.parseColor("#4CAF50"))
                setPadding(0, dpToPx(4), 0, 0)
            }

            val tvDokter = TextView(this).apply {
                text = "Dokter: $dokterDipilih"
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(0, dpToPx(4), 0, 0)
            }

            inner.addView(tvNomor)
            inner.addView(tvNama)
            inner.addView(tvJam)
            inner.addView(tvDokter)
            card.addView(inner)
            container.addView(card)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
