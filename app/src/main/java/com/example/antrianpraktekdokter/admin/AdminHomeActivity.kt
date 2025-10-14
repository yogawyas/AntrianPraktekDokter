package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
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
import com.example.antrianpraktekdokter.patient.HomeActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.antrianpraktekdokter.auth.LoginActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Setup Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Hamburger toggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Button logout
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Method untuk dialog tambah antrian (dipanggil dari fragment)
        setupFragmentListeners()
    }

    private fun setupFragmentListeners() {
        // Listener untuk fragment komunikasi dengan activity
        // Akan digunakan oleh TambahAntrianFragment untuk panggil showTambahAntrianDialog()
    }

    // Method public untuk fragment panggil dialog
    fun showTambahAntrianDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_antrian, null)
        val etNama = dialogView.findViewById<EditText>(R.id.etNama)
        val etJam = dialogView.findViewById<EditText>(R.id.etJam)
        val etKeluhan = dialogView.findViewById<EditText>(R.id.etKeluhan)

        etJam.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                etJam.setText(String.format("%02d:%02d", h, m))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Antrian Manual")
            .setView(dialogView)
            .setPositiveButton("Tambah") { _, _ ->
                val nama = etNama.text.toString()
                val jam = etJam.text.toString()
                val keluhan = etKeluhan.text.toString()
                if (nama.isEmpty() || jam.isEmpty() || keluhan.isEmpty()) {
                    Toast.makeText(this, "Isi semua field!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = sdf.format(Date())

                // Validasi batas harian
                FirebaseFirestore.getInstance().collection("antrian")
                    .whereEqualTo("tanggal_simpan", today)
                    .whereEqualTo("dihapus", false)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.size() >= 20) {
                            Toast.makeText(this, "Antrian penuh hari ini!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Validasi batas jam
                        FirebaseFirestore.getInstance().collection("antrian")
                            .whereEqualTo("tanggal_simpan", today)
                            .whereEqualTo("jam", jam)
                            .whereEqualTo("dihapus", false)
                            .get()
                            .addOnSuccessListener { jamSnapshot ->
                                if (jamSnapshot.size() >= 2) {
                                    Toast.makeText(this, "Jam $jam sudah penuh!", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                // Hitung nomor antrian
                                FirebaseFirestore.getInstance().runTransaction { transaction ->
                                    val countRef = FirebaseFirestore.getInstance().collection("config").document("antrian_count_$today")
                                    val snapshot = transaction.get(countRef)
                                    val currentCount = snapshot.getLong("count") ?: 0
                                    val newCount = currentCount + 1
                                    transaction.set(countRef, hashMapOf("count" to newCount))
                                    newCount
                                }.addOnSuccessListener { newNomor ->
                                    val newAntrian = hashMapOf(
                                        "nama_pasien" to nama,
                                        "jam" to jam,
                                        "keluhan" to keluhan,
                                        "tanggal_simpan" to today,
                                        "selesai" to false,
                                        "dipanggil" to 0,
                                        "dihapus" to false,
                                        "nomor_antrian" to newNomor,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    FirebaseFirestore.getInstance().collection("antrian").add(newAntrian)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Antrian ditambahkan! Nomor: $newNomor", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}