package com.example.antrianpraktekdokter.patient.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
//import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.antrianpraktekdokter.patient.ListAntrianActivity  // Pastikan import ini ditambahkan
import androidx.navigation.fragment.findNavController
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.patient.BeritaActivity
import com.example.antrianpraktekdokter.patient.JanjiTemuActivity
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnJanjiTemu: ImageButton
    private lateinit var navListAntrian: ImageButton
    private lateinit var btnNews: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()

        //val tvWelcome: TextView = view.findViewById(R.id.tvWelcome)
        val prefs = requireContext().getSharedPreferences("AntrianPrefs", Context.MODE_PRIVATE)
        val nama = prefs.getString("nama", "") ?: ""
        //tvWelcome.text = "ini Home Fragment, $nama!"
        btnJanjiTemu = view.findViewById(R.id.btnJanjiTemu)
        navListAntrian = view.findViewById(R.id.nav_list_antrian)
        btnNews = view.findViewById(R.id.btnNews)

        // Handle klik button Janji Temu
        btnJanjiTemu.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    val intent = Intent(requireContext(), JanjiTemuActivity::class.java)
                    startActivity(intent)

                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true
        }

        // Handle klik button nav_list_antrian
        navListAntrian.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    findNavController().navigate(R.id.action_nav_home_to_nav_list_antrian)
                    val intent = Intent(requireContext(), ListAntrianActivity::class.java)
                    startActivity(intent)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true
        }

        // Handle klik button btnNews
        btnNews.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                    //findNavController().navigate(R.id.nav_notifikasi)
                    val intent = Intent(requireContext(), BeritaActivity::class.java)
                    startActivity(intent)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start()
                }
            }
            true
        }

        return view
    }

}