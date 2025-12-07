package com.example.antrianpraktekdokter.patient.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotifikasiFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerNotifikasi: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private val notifikasiList = mutableListOf<NotifikasiItem>()

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifikasi, container, false)

        recyclerNotifikasi = view.findViewById(R.id.recyclerNotifikasi)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        recyclerNotifikasi.layoutManager = LinearLayoutManager(context)
        recyclerNotifikasi.adapter = NotifikasiAdapter(notifikasiList) // Inisialisasi awal

        loadNotifikasi()

        return view
    }

    private fun loadNotifikasi() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        val user = auth.currentUser
        if (user == null) {
            progressBar.visibility = View.GONE
            Toast.makeText(context, "User tidak login", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = user.uid
        Log.d("NotifikasiFragment", "Loading notifikasi for userId: $userId")

        // Ambil notifikasi dari Firestore
        db.collection("notifikasi")
            .whereEqualTo("user_id", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                notifikasiList.clear()
                for (doc in snapshot.documents) {
                    val nomor = doc.getLong("nomor_antrian")?.toInt() ?: 0
                    val nama = doc.getString("nama_pasien") ?: ""
                    val jam = doc.getString("jam") ?: ""
                    notifikasiList.add(NotifikasiItem(nomor, nama, jam))
                }
                updateRecyclerView()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("NotifikasiFragment", "Query failed: ${e.message}", e)
                Toast.makeText(context, "Error fetching notifikasi: ${e.message}", Toast.LENGTH_SHORT).show()
                // Fallback ke data hardcoded
                notifikasiList.clear()
                notifikasiList.addAll(getHardcodedNotifikasi())
                updateRecyclerView()
            }
    }

    private fun updateRecyclerView() {
        if (notifikasiList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerNotifikasi.adapter = null
        } else {
            tvEmpty.visibility = View.GONE
            recyclerNotifikasi.adapter = NotifikasiAdapter(notifikasiList)
        }
    }

    private fun getHardcodedNotifikasi(): List<NotifikasiItem> {
        return listOf(
            NotifikasiItem(4, "Yoga", "12:55"), // Sesuai data yang kamu berikan
            NotifikasiItem(3, "Yoga", "14:15"),
            NotifikasiItem(2, "Yoga", "10:30"),
            NotifikasiItem(1, "Yoga", "09:00")
        )
    }

    data class NotifikasiItem(
        val nomor: Int,
        val nama: String,
        val jam: String
    )

    class NotifikasiAdapter(private val notifikasiList: List<NotifikasiItem>) : RecyclerView.Adapter<NotifikasiAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNomor: TextView = view.findViewById(R.id.tvNomorNotif)
            val tvNama: TextView = view.findViewById(R.id.tvNamaNotif)
            val tvJam: TextView = view.findViewById(R.id.tvJamNotif)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notifikasi, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = notifikasiList[position]
            holder.tvNomor.text = "No. ${item.nomor}"
            holder.tvNama.text = "Nama: ${item.nama}"
            holder.tvJam.text = "Jam: ${item.jam}"
        }

        override fun getItemCount() = notifikasiList.size
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NotifikasiFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}