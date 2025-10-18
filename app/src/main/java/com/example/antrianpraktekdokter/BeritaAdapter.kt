package com.example.antrianpraktekdokter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.antrianpraktekdokter.model.NotificationItem

class BeritaAdapter(
    private val items: MutableList<NotificationItem>
) : RecyclerView.Adapter<BeritaAdapter.ViewHolder>() {

    // ViewHolder = represents each row in the RecyclerView
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    // Inflates (creates) the layout for each row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    // Binds data to the views inside each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvMessage.text = item.message
    }

    override fun getItemCount(): Int = items.size

    // Removes a notification when swiped
    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItems(): List<NotificationItem> = items
}
