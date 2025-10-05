package com.example.antrianpraktekdokter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
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

        val btnKembali = findViewById<Button>(R.id.btnKembaliHome)
        btnKembali.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        renderAntrian()
    }

    private fun renderAntrian() {
        val container = findViewById<LinearLayout>(R.id.containerAntrian)
        container.removeAllViews()

        val dokterDipilih = intent.getStringExtra("dokter")
        val prefs = JanjiTemuActivity.PrefsHelper(this)
        val list = prefs.loadList()

        // 🔹 Kalau dokter null → tampilkan semua antrian
        val filtered = if (dokterDipilih.isNullOrEmpty()) list else list.filter { it["dokter"] == dokterDipilih }

        if (filtered.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = if (dokterDipilih.isNullOrEmpty()) {
                    "Belum ada antrian"
                } else {
                    "Belum ada antrian untuk $dokterDipilih"
                }
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
                text = "No: ${index + 1}"
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
                text = "Dokter: ${item["dokter"] ?: "-"}"
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
