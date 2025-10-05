package com.example.antrianpraktekdokter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.StringRequest
import com.android.volley.Request

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

        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val email = prefs.getString("email", "") ?: ""
        val url = "http://10.0.2.2/api_antrian/get_antrian.php?email=$email"

        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(Request.Method.GET, url,
            { response ->
                val jsonArray = JSONArray(response)
                if (jsonArray.length() == 0) {
                    val tvEmpty = TextView(this).apply {
                        text = "Belum ada antrian"
                        textSize = 16f
                        setTextColor(Color.DKGRAY)
                    }
                    container.addView(tvEmpty)
                    return@StringRequest
                }

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val nama = obj.getString("nama_pasien")
                    val jam = obj.getString("jam")
                    val dokter = obj.getString("dokter")

                    val card = CardView(this).apply {
                        val lp = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT )
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
                        text = "No: ${i + 1}"
                        textSize = 18f
                        setTextColor(Color.parseColor("#2196F3"))
                    }
                    val tvNama = TextView(this).apply {
                        text = "Nama: $nama"
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        setPadding(0, dpToPx(6), 0, 0)
                    }
                    val tvJam = TextView(this).apply {
                        text = "Jam: $jam"
                        textSize = 14f
                        setTextColor(Color.parseColor("#4CAF50"))
                        setPadding(0, dpToPx(4), 0, 0)
                    }
                    val tvDokter = TextView(this).apply {
                        text = "Dokter: $dokter"
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
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
