package com.example.antrianpraktekdokter.patient

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

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

    private fun renderAntrian() {
        val container = findViewById<LinearLayout>(R.id.containerAntrian)
        container.removeAllViews()

        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val dataString = prefs.getString("dataAntrian", "[]")
        val jsonArray = JSONArray(dataString)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        val filteredArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val tanggalSimpan = obj.optString("tanggal_simpan", today)
            if (tanggalSimpan == today) filteredArray.put(obj)
        }

        // 🔔 popup notifikasi kalau dokter hapus pasien
        for (i in 0 until filteredArray.length()) {
            val obj = filteredArray.getJSONObject(i)
            val dihapus = obj.optBoolean("dihapus", false)
            if (dihapus) {
                AlertDialog.Builder(this)
                    .setTitle("Pemberitahuan Dokter")
                    .setMessage("Dokter menghapus antrianmu karena kamu tidak hadir/terlambat.")
                    .setPositiveButton("OK", null)
                    .show()
                break
            }
        }

        if (filteredArray.length() == 0) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada antrian hari ini"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(dpToPx(8), dpToPx(16), dpToPx(8), 0)
            }
            container.addView(tvEmpty)
            return
        }

        for (i in 0 until filteredArray.length()) {
            val obj = filteredArray.getJSONObject(i)
            val nama = obj.getString("nama_pasien")
            val jam = obj.getString("jam")
            val keluhan = obj.getString("keluhan")
            val selesai = obj.optBoolean("selesai", false)
            val dipanggil = obj.optInt("dipanggil", 0)
            val dihapus = obj.optBoolean("dihapus", false)
            if (dihapus) continue

            val card = CardView(this).apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                layoutParams = lp
                radius = dpToPx(10).toFloat()
                cardElevation = dpToPx(6).toFloat()
                setCardBackgroundColor(
                    when {
                        selesai -> Color.parseColor("#C8E6C9")
                        dipanggil > 0 -> Color.parseColor("#FFF9C4")
                        else -> Color.WHITE
                    }
                )
                useCompatPadding = true
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val tvNama = TextView(this).apply {
                text = "Nama: $nama"
                textSize = 16f
                setTextColor(Color.BLACK)
            }

            val tvJam = TextView(this).apply {
                text = "Jam: $jam"
                textSize = 14f
                setTextColor(Color.parseColor("#2196F3"))
            }

            val tvKeluhan = TextView(this).apply {
                text = "Keluhan: $keluhan"
                textSize = 14f
                setTextColor(Color.DKGRAY)
            }

            val tvStatus = TextView(this).apply {
                text = when {
                    selesai -> "✅ Pemeriksaan selesai"
                    dipanggil > 0 -> "📣 Dipanggil (${dipanggil}x)"
                    else -> "⏳ Menunggu giliran"
                }
                textSize = 14f
                setTextColor(
                    when {
                        selesai -> Color.parseColor("#388E3C")
                        dipanggil > 0 -> Color.parseColor("#F57C00")
                        else -> Color.DKGRAY
                    }
                )
            }

            inner.addView(tvNama)
            inner.addView(tvJam)
            inner.addView(tvKeluhan)
            inner.addView(tvStatus)
            card.addView(inner)
            container.addView(card)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
