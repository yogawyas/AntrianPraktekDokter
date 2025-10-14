package com.example.antrianpraktekdokter.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ListAntrianFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerAntrian: RecyclerView
    private lateinit var tvSelesai: TextView
    private lateinit var tvSisa: TextView
    private lateinit var switchShowCompleted: Switch
    private var showCompleted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list_antrian, container, false)

        db = FirebaseFirestore.getInstance()
        recyclerAntrian = view.findViewById(R.id.recyclerAntrian)
        tvSelesai = view.findViewById(R.id.tvSelesai)
        tvSisa = view.findViewById(R.id.tvSisa)
        switchShowCompleted = view.findViewById(R.id.switchShowCompleted)

        recyclerAntrian.layoutManager = LinearLayoutManager(context)

        switchShowCompleted.setOnCheckedChangeListener { _, isChecked ->
            showCompleted = isChecked
            loadAntrian()
        }

        loadAntrian()

        return view
    }

    private fun loadAntrian() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        val query = db.collection("antrian")
            .whereEqualTo("tanggal_simpan", today)
            .whereEqualTo("dihapus", false)

        val finalQuery = if (showCompleted) {
            query
        } else {
            query.whereEqualTo("selesai", false)
        }.orderBy("jam", Query.Direction.ASCENDING)

        finalQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot == null) return@addSnapshotListener

            var countSelesai = 0
            var countSisa = 0
            val antrianList = snapshot.documents

            recyclerAntrian.adapter = AntrianAdapter(antrianList)

            for (doc in antrianList) {
                val selesai = doc.getBoolean("selesai") ?: false
                if (selesai) countSelesai++ else countSisa++
            }

            tvSelesai.text = "Pasien selesai: $countSelesai"
            tvSisa.text = "Sisa pasien: $countSisa"
        }
    }
}