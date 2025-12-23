package com.example.antrianpraktekdokter.patient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.antrianpraktekdokter.R
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class BeritaActivity : AppCompatActivity() {

    private lateinit var rvHealthNews: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: MaterialButton

    private val newsList = mutableListOf<HealthNews>()
    private lateinit var adapter: BeritaAdapter

    private val TAG = "BeritaActivity"

    private val API_KEY = "b29df8fc2ca3f2bccb2e02b83b11983b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_berita)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        btnBack = findViewById(R.id.btnBack)
        rvHealthNews = findViewById(R.id.rvHealthNews)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvHealthNews.layoutManager = LinearLayoutManager(this)
        adapter = BeritaAdapter(newsList)
        rvHealthNews.adapter = adapter

        btnBack.setOnClickListener { finish() }
        tvEmpty.setOnClickListener { fetchHealthNews() }

        fetchHealthNews()
    }

    private fun fetchHealthNews() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        val url = "https://gnews.io/api/v4/top-headlines?category=health&lang=en&apikey=$API_KEY"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                progressBar.visibility = View.GONE
                parseResponse(response)
            },
            { error ->
                progressBar.visibility = View.GONE
                tvEmpty.text = "Failed to load news. Tap to try again."
                tvEmpty.visibility = View.VISIBLE
                Log.e(TAG, "Volley Error: ${error.message}")
            }
        )

        request.retryPolicy = DefaultRetryPolicy(10000, 1, 1.0f)
        Volley.newRequestQueue(this).add(request)
    }

    private fun parseResponse(response: JSONObject) {
        try {
            val articles = response.optJSONArray("articles")
            if (articles == null || articles.length() == 0) {
                tvEmpty.visibility = View.VISIBLE
                return
            }

            newsList.clear()
            for (i in 0 until articles.length()) {
                val item = articles.getJSONObject(i)
                newsList.add(HealthNews(
                    title = item.optString("title", "No Title"),
                    description = item.optString("description", "No Description"),
                    image = item.optString("image", ""),
                    url = item.optString("url", "")
                ))
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Parsing Error: ${e.message}")
            tvEmpty.visibility = View.VISIBLE
        }
    }

    inner class BeritaAdapter(private val list: List<HealthNews>) :
        RecyclerView.Adapter<BeritaAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_health_news, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val news = list[position]
            holder.tvTitle.text = news.title
            holder.tvDescription.text = news.description

            if (news.image.isNotEmpty()) {
                Glide.with(this@BeritaActivity)
                    .load(news.image)
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.ivThumbnail)
            }

            holder.itemView.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                startActivity(browserIntent)
            }
        }

        override fun getItemCount(): Int = list.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        }
    }

    data class HealthNews(
        val title: String,
        val description: String,
        val image: String,
        val url: String
    )
}