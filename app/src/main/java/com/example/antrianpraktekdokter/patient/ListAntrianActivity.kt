package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_antrian)

        // 1. DEKLARASI mainLayout agar tidak 'Unresolved reference'
        // Sesuai dengan android:id="@+id/main" di XML ConstraintLayout Anda
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

        // 2. Set Padding untuk System Bars (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 3. Inisialisasi ProgressBar secara programmatik (atau lewat XML)
        progressBar = ProgressBar(this).apply {
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // Menempatkan ProgressBar di tengah ConstraintLayout
                bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            }
            visibility = View.GONE
        }
        mainLayout.addView(progressBar)

        // 4. Tombol Back (Gunakan finish agar kembali ke Home tanpa tumpukan activity)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        renderAntrian()
    }

    private fun renderAntrian() {
        val container = findViewById<LinearLayout>(R.id.containerAntrian)
        container.removeAllViews()
        progressBar.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)
            .orderBy("jam", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    progressBar.visibility = View.GONE
                    return@addSnapshotListener
                }

                container.removeAllViews()

                // Tampilkan teks jika kosong (ID dari XML sebelumnya)
                val tvEmpty = findViewById<TextView>(R.id.tvEmptyQueue)
                if (snapshot?.isEmpty == true) {
                    tvEmpty?.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    return@addSnapshotListener
                } else {
                    tvEmpty?.visibility = View.GONE
                }

                val userId = auth.currentUser?.uid
                var position = 1

                for (doc in snapshot!!.documents) {
                    // Inflate layout dari XML item_antrian_patient
                    val itemView = layoutInflater.inflate(R.layout.item_antrian_patient, container, false)

                    // Binding View dari XML
                    val tvNumberLabel = itemView.findViewById<TextView>(R.id.tvNumberLabel)
                    val tvNameValue = itemView.findViewById<TextView>(R.id.tvNameValue)
                    val tvTimeValue = itemView.findViewById<TextView>(R.id.tvTimeValue)
                    val tvProblemsValue = itemView.findViewById<TextView>(R.id.tvProblemsValue)
                    val tvStatusText = itemView.findViewById<TextView>(R.id.tvStatusText)
                    val viewStatusIndicator = itemView.findViewById<View>(R.id.viewStatusIndicator)
                    val btnCancel = itemView.findViewById<Button>(R.id.btnCancelAppointment)

                    val selesai = doc.getBoolean("selesai") ?: false
                    val dipanggil = doc.getLong("dipanggil")?.toInt() ?: 0
                    val docUserId = doc.getString("user_id")

                    // Isi Data
                    tvNumberLabel.text = "Number ${position}"
                    tvNameValue.text = doc.getString("nama_pasien")
                    tvTimeValue.text = doc.getString("jam")
                    tvProblemsValue.text = doc.getString("keluhan")

                    // Logika Status
                    when {
                        selesai -> {
                            tvStatusText.text = "Finished"
                            tvStatusText.setTextColor(Color.parseColor("#4CAF50"))
                            viewStatusIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
                            btnCancel.visibility = View.GONE
                        }
                        dipanggil > 0 -> {
                            tvStatusText.text = "Calling"
                            tvStatusText.setTextColor(Color.parseColor("#FF9800"))
                            viewStatusIndicator.setBackgroundColor(Color.parseColor("#FF9800"))
                            btnCancel.visibility = View.GONE
                        }
                        else -> {
                            tvStatusText.text = "Waiting"
                            tvStatusText.setTextColor(Color.parseColor("#FFEB3B")) // Kuning sesuai gambar
                            viewStatusIndicator.setBackgroundColor(Color.parseColor("#FFEB3B"))

                            // Hanya tampilkan tombol cancel jika ini milik user yang login
                            if (docUserId == userId) {
                                btnCancel.visibility = View.VISIBLE
                                btnCancel.setOnClickListener {
                                    showCancelDialog(doc.reference)
                                }
                            }
                        }
                    }

                    container.addView(itemView)
                    if (!selesai) position++
                }
                progressBar.visibility = View.GONE
            }
    }
    private fun showCancelDialog(docRef: com.google.firebase.firestore.DocumentReference) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_queue, null)
        val builder = AlertDialog.Builder(this).setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            docRef.update("dihapus", true).addOnSuccessListener {
                Toast.makeText(this, "Antrian dibatalkan", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
