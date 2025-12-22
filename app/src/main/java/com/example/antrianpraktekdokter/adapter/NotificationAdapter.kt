package com.example.antrianpraktekdokter.patient

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter(private val items: List<NotifModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val status: TextView = v.findViewById(R.id.tvStatus)
        val msg: TextView = v.findViewById(R.id.tvNotifMessage)
        val dateTime: TextView = v.findViewById(R.id.tvDateTimeDetail)
        val timeAgo: TextView = v.findViewById(R.id.tvTimeAgo)
        val dot: View = v.findViewById(R.id.viewUnreadDot)
        val btnMenu: ImageButton = v.findViewById(R.id.btnMenuNotif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val db = FirebaseFirestore.getInstance()

        holder.msg.text = item.message
        holder.status.text = item.type
        holder.status.setTextColor(if(item.type == "Called") 0xFF04AA78.toInt() else 0xFFFF3737.toInt())

        val sdf = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault())
        holder.dateTime.text = item.timestamp?.let { sdf.format(it) } ?: ""
        holder.timeAgo.text = item.timestamp?.let { calculateTime(it) }

        // Indikator titik merah
        holder.dot.visibility = if (item.isRead) View.GONE else View.VISIBLE

        // Klik Card juga menandai sudah baca
        holder.itemView.setOnClickListener {
            if (!item.isRead) {
                FirebaseFirestore.getInstance().collection("notifikasi")
                    .document(item.id)
                    .update("isRead", true)
            }
        }

        // Popup Menu untuk masing-masing notifikasi
        holder.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)

            // Tambahkan menu Mark as Read jika belum dibaca
            if (!item.isRead) {
                popup.menu.add("Mark as Read")
            }
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Mark as Read" -> {
                        db.collection("notifikasi").document(item.id).update("isRead", true)
                    }
                    "Delete" -> {
                        db.collection("notifikasi").document(item.id).delete()
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun calculateTime(date: Date): String {
        val diff = Date().time - date.time
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    override fun getItemCount() = items.size
}