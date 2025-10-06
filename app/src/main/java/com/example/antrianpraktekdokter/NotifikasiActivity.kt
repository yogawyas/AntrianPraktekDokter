package com.example.antrianpraktekdokter

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
import org.json.JSONObject

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var rvHealthNews: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private val newsList = mutableListOf<HealthNews>()
    private lateinit var adapter: NewsAdapter

    private val TAG = "NotifikasiActivity"
    private val API_KEY = "b29df8fc2ca3f2bccb2e02b83b11983b"  // Ganti dengan key dari gnews.io

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifikasi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvHealthNews = findViewById(R.id.rvHealthNews)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvHealthNews.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(newsList)
        rvHealthNews.adapter = adapter

        tvEmpty.setOnClickListener { fetchHealthNews() }

        fetchHealthNews()
    }

    private fun fetchHealthNews() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        // Endpoint GNews: Kategori health, max 10 item, negara Indonesia
        val url = "https://gnews.io/api/v4/top-headlines?category=health&lang=en&country=id&max=10&apikey=$API_KEY"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                progressBar.visibility = View.GONE
                Log.d(TAG, "Response: $response")
                parseResponse(response)
            },
            { error ->
                progressBar.visibility = View.GONE
                tvEmpty.text = "Koneksi gagal. Coba lagi?"
                tvEmpty.visibility = View.VISIBLE
                Log.e(TAG, "Error: ${error.message}")
                Toast.makeText(this, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        request.retryPolicy = DefaultRetryPolicy(15000, 2, 1.0f)
        Volley.newRequestQueue(this).add(request)
    }

    private fun parseResponse(response: JSONObject) {
        try {
            val articles = response.optJSONArray("articles")
            if (articles == null || articles.length() == 0) {
                tvEmpty.text = "Tidak ada berita kesehatan tersedia."
                tvEmpty.visibility = View.VISIBLE
                return
            }

            newsList.clear()
            for (i in 0 until articles.length()) {
                val item = articles.getJSONObject(i)
                val title = item.optString("title", "No Title")
                val description = item.optString("description", "No Description")
                val urlToImage = item.optString("image", null)
                val url = item.optString("url", "")

                newsList.add(HealthNews(title, description, urlToImage, url))
                Log.d(TAG, "Added: $title")
            }

            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Loaded ${newsList.size} berita!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}", e)
            tvEmpty.text = "Error: ${e.message}"
            tvEmpty.visibility = View.VISIBLE
        }
    }

    inner class NewsAdapter(private val list: List<HealthNews>) :
        RecyclerView.Adapter<NotifikasiActivity.NewsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_health_news, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val news = list[position]
            holder.tvTitle.text = news.title
            holder.tvDescription.text = news.description

            if (news.urlToImage != null && news.urlToImage.isNotEmpty()) {
                holder.ivThumbnail.visibility = View.VISIBLE
                Glide.with(this@NotifikasiActivity).load(news.urlToImage).into(holder.ivThumbnail)
            } else {
                holder.ivThumbnail.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                if (news.url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                    startActivity(intent)
                } else {
                    Toast.makeText(this@NotifikasiActivity, "Link tidak ada", Toast.LENGTH_SHORT).show()
                }
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
        val urlToImage: String?,
        val url: String
    )
}