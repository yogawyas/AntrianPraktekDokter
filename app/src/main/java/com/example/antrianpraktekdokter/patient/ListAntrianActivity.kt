package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ListAntrianActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_antrian)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.btnKembaliHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        renderAntrian()
    }

    private fun renderAntrian() {
        val container = findViewById<LinearLayout>(R.id.containerAntrian)
        container.removeAllViews()

        val db = FirebaseFirestore.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        // Simplifikasi: Hilangkan orderBy("createdAt") untuk hindari index kompleks sementara
        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)
            .orderBy("jam", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                container.removeAllViews()
                if (snapshot?.isEmpty == true) {
                    val tvEmpty = TextView(this).apply {
                        text = "Belum ada antrian hari ini"
                        textSize = 16f
                        setTextColor(Color.DKGRAY)
                        setPadding(dpToPx(8), dpToPx(16), dpToPx(8), 0)
                    }
                    container.addView(tvEmpty)
                    return@addSnapshotListener
                }

                var position = 1
                var userPosition = -1
                val userId = auth.currentUser?.uid

                for (doc in snapshot!!.documents) {
                    val nama = doc.getString("nama_pasien") ?: ""
                    val jam = doc.getString("jam") ?: ""
                    val keluhan = doc.getString("keluhan") ?: ""
                    val selesai = doc.getBoolean("selesai") ?: false
                    val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
                    val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
                    val docUserId = doc.getString("user_id")

                    val estimasi = if (selesai || dipanggil > 0) "Sedang/Selesai" else "${position * 15} menit"
                    if (docUserId == userId && !selesai && dipanggil == 0) {
                        userPosition = position
                    }

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

                    val tvNomor = TextView(this).apply {
                        text = "No. Antrian: $nomor"
                        textSize = 14f
                        setTextColor(Color.parseColor("#2196F3"))
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
                            selesai -> "Selesai ✅"
                            dipanggil > 0 -> "Sedang dipanggil ($dipanggil)"
                            else -> "Menunggu"
                        }
                        textSize = 14f
                        setTextColor(
                            when {
                                selesai -> Color.parseColor("#388E3C")
                                dipanggil > 0 -> Color.parseColor("#FF6F00")
                                else -> Color.DKGRAY
                            }
                        )
                    }

                    val tvEstimasi = TextView(this).apply {
                        text = "Estimasi: $estimasi"
                        textSize = 14f
                        setTextColor(Color.DKGRAY)
                    }

                    // Tombol batalkan (hanya untuk antrian user sendiri)
                    if (docUserId == userId && !selesai && dipanggil == 0) {
                        val btnCancel = Button(this).apply {
                            text = "Batalkan Antrian"
                            setTextColor(Color.WHITE)
                            textSize = 14f
                            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                            background = resources.getDrawable(R.drawable.bg_button_red, null)
                        }
                        btnCancel.setOnClickListener {
                            AlertDialog.Builder(this@ListAntrianActivity)
                                .setTitle("Batalkan Antrian?")
                                .setMessage("Yakin ingin membatalkan antrian Anda?")
                                .setPositiveButton("Ya") { _, _ ->
                                    doc.reference.update("dihapus", true)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@ListAntrianActivity, "Antrian dibatalkan", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .setNegativeButton("Tidak", null)
                                .show()
                        }
                        inner.addView(btnCancel)
                    }

                    inner.addView(tvNomor)
                    inner.addView(tvNama)
                    inner.addView(tvJam)
                    inner.addView(tvKeluhan)
                    inner.addView(tvStatus)
                    inner.addView(tvEstimasi)
                    card.addView(inner)
                    container.addView(card)

                    if (!selesai && dipanggil == 0) position++
                }

                // Tampilkan posisi antrian user
                if (userPosition > 0) {
                    Toast.makeText(this, "Antrian Anda saat ini: Nomor $userPosition", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}