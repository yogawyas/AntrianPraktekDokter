package com.example.antrianpraktekdokter.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.admin.viewmodel.ListAntrianViewModel
import com.example.antrianpraktekdokter.model.Antrian

class ListAntrianFragment : Fragment() {

    private lateinit var viewModel: ListAntrianViewModel
    private lateinit var adapter: AntrianAdapter

    // Views
    private lateinit var recyclerAntrian: RecyclerView
    private lateinit var tvSelesai: TextView
    private lateinit var tvSisa: TextView
    private lateinit var switchShowCompleted: Switch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list_antrian, container, false)

        // Init ViewModel
        viewModel = ViewModelProvider(this)[ListAntrianViewModel::class.java]

        // Init Views
        recyclerAntrian = view.findViewById(R.id.recyclerAntrian)
        tvSelesai = view.findViewById(R.id.tvSelesai)
        tvSisa = view.findViewById(R.id.tvSisa)
        switchShowCompleted = view.findViewById(R.id.switchShowCompleted)

        recyclerAntrian.layoutManager = LinearLayoutManager(context)

        // Setup Adapter
        adapter = AntrianAdapter(emptyList(), viewModel)
        recyclerAntrian.adapter = adapter


        viewModel.antrianList.observe(viewLifecycleOwner) { list ->

            adapter.updateData(list)
            viewModel.calculateStats(list)
        }

        viewModel.stats.observe(viewLifecycleOwner) { (selesai, sisa) ->
            tvSelesai.text = "Pasien selesai: $selesai"
            tvSisa.text = "Sisa pasien: $sisa"
        }

        // --- LISTENERS ---
        switchShowCompleted.setOnCheckedChangeListener { _, isChecked ->
            viewModel.loadAntrian(isChecked)
        }


        viewModel.loadAntrian(false)

        return view
    }

    // --- INNER CLASS ADAPTER mvvm
    inner class AntrianAdapter(
        private var list: List<Antrian>,
        private val vm: ListAntrianViewModel
    ) : RecyclerView.Adapter<AntrianAdapter.ViewHolder>() {

        fun updateData(newList: List<Antrian>) {
            this.list = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNomor: TextView = itemView.findViewById(R.id.tvNomor)
            val tvNama: TextView = itemView.findViewById(R.id.tvNama)
            val tvJam: TextView = itemView.findViewById(R.id.tvJam)
            val tvKeluhan: TextView = itemView.findViewById(R.id.tvKeluhan)
            val cbSelesai: CheckBox = itemView.findViewById(R.id.cbSelesai)
            val btnPanggil: Button = itemView.findViewById(R.id.btnPanggil)
            val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
            val cardView: CardView = itemView.findViewById(R.id.cardAntrian)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_antrian, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]

            holder.tvNomor.text = "No. ${item.nomor_antrian}"
            holder.tvNama.text = "Nama: ${item.nama_pasien}"
            holder.tvJam.text = "Jam: ${item.jam}"
            holder.tvKeluhan.text = "Keluhan: ${item.keluhan}"


            holder.cbSelesai.setOnCheckedChangeListener(null)
            holder.cbSelesai.isChecked = item.selesai
            holder.cbSelesai.text = if (item.selesai) "Selesai " else "Tandai Selesai"

            val greenColor = ContextCompat.getColor(requireContext(), R.color.green_light)
            val whiteColor = ContextCompat.getColor(requireContext(), R.color.white) // Pastikan ada color resource ini atau gunakan android.R.color.white


            holder.cardView.setCardBackgroundColor(if (item.selesai) 0xFFC8E6C9.toInt() else 0xFFFFFFFF.toInt())


            holder.cbSelesai.setOnCheckedChangeListener { _, isChecked ->
                vm.updateStatusSelesai(item, isChecked)
            }

            holder.btnPanggil.setOnClickListener {
                val newCount = dipanggil + 1
                doc.reference.update("dipanggil", newCount)
                    .addOnSuccessListener {
                        // Notifikasi Panggil dari Admin
                        val notifData = hashMapOf(
                            "user_id" to userId,
                            "nomor_antrian" to nomor,
                            "nama_pasien" to nama,
                            "message" to "No. Antrian $nomor ($nama), silakan masuk ke ruang periksa sekarang!",
                            "type" to "Called", // Agar muncul warna hijau di card
                            "timestamp" to FieldValue.serverTimestamp(),
                            "isRead" to false
                        )
                        db.collection("notifikasi").add(notifData)
                        Toast.makeText(context, "Panggilan dikirim ke $nama", Toast.LENGTH_SHORT).show()
                    }
            }

            holder.btnCancel.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Batalkan Pasien?")
                    .setMessage("Yakin ingin menghapus pasien ini?")
                    .setPositiveButton("Ya") { _, _ ->
                        doc.reference.update("dihapus", true)
                            .addOnSuccessListener {
                                // Notifikasi Batal dari Admin
                                val notifData = hashMapOf(
                                    "user_id" to userId,
                                    "nomor_antrian" to nomor,
                                    "nama_pasien" to nama,
                                    "message" to "Your appointment was canceled by Admin.",
                                    "type" to "Canceled", // Agar muncul warna merah di card
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "isRead" to false
                                )
                                db.collection("notifikasi").add(notifData)
                                Toast.makeText(context, "Antrian $nama dibatalkan", Toast.LENGTH_SHORT).show()
                            }
                    }.setNegativeButton("Tidak", null).show()
            }
        }

        override fun getItemCount() = list.size
    }
}