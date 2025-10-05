package com.example.antrianpraktekdokter

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.w3c.dom.Element
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // === Welcome Text ===
        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val nama = intent.getStringExtra("nama") ?: "Pengguna"
        tvWelcome.text = "Selamat datang, $nama!"

        // === Tombol Janji Temu Dokter ===
        val btnJanjiTemu: MaterialButton = findViewById(R.id.btnJanjiTemu)
        btnJanjiTemu.setOnClickListener {
            val intent = Intent(this, JanjiTemuActivity::class.java)
            startActivity(intent)
        }

        // === Bottom Navigation ===
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_history_medis -> {
                    startActivity(Intent(this, HistoryMedisActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_list_antrian -> {
                    startActivity(Intent(this, ListAntrianActivity::class.java))
                    true
                }
                R.id.nav_notifikasi -> {
                    startActivity(Intent(this, NotifikasiActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // === RecyclerView untuk berita kesehatan ===
        val rvHealthNews: RecyclerView = findViewById(R.id.rvHealthNews)
        rvHealthNews.layoutManager = LinearLayoutManager(this)

        // Fetch WHO News RSS dalam background thread
        Thread {
            try {
                val url = URL("https://www.who.int/rss-feeds/news-english.xml")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream = connection.inputStream
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
                val nodeList = doc.getElementsByTagName("item")

                val newsList = mutableListOf<NewsItem>()
                for (i in 0 until nodeList.length) {
                    val element = nodeList.item(i) as Element
                    val title = element.getElementsByTagName("title").item(0).textContent
                    val description = element.getElementsByTagName("description").item(0).textContent
                    val link = element.getElementsByTagName("link").item(0).textContent

                    val mediaList = element.getElementsByTagName("media:content")
                    val imageUrl = if (mediaList.length > 0) {
                        mediaList.item(0).attributes.getNamedItem("url").textContent
                    } else {
                        ""
                    }

                    newsList.add(NewsItem(title, description, imageUrl, link))
                }

                runOnUiThread {
                    rvHealthNews.adapter = NewsAdapter(newsList)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
