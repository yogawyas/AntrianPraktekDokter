package com.example.antrianpraktekdokter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import com.example.antrianpraktekdokter.model.NotificationItem


class NotifikasiActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifikasi)

        recyclerView = findViewById(R.id.recyclerNotifikasi)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadNotifications()
        adapter = NotificationAdapter(notifList)
        recyclerView.adapter = adapter

        enableSwipeToDelete()
    }

    private fun loadNotifications() {
        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("notifList", null) ?: return
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            notifList.add(NotificationItem(jsonArray.getString(i)))
        }
    }

    private fun enableSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                adapter.removeItem(position)
                saveNotifications()
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun saveNotifications() {
        val prefs = getSharedPreferences("AntrianPrefs", MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in adapter.getItems()) {
            jsonArray.put(item.message)
        }
        prefs.edit().putString("notifList", jsonArray.toString()).apply()
    }
}
