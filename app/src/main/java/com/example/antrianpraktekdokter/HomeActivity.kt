package com.example.antrianpraktekdokter


import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.antrianpraktekdokter.R

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val nama = intent.getStringExtra("nama")
        tvWelcome.text = "Selamat datang, $nama!"
    }
}
