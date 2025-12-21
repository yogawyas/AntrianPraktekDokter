package com.example.antrianpraktekdokter.repository

import androidx.lifecycle.MutableLiveData
import com.example.antrianpraktekdokter.model.Antrian
import com.example.antrianpraktekdokter.model.JadwalConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    // --- BAGIAN 1: JADWAL PRAKTIK ---

    // Ambil data jadwal (Jam buka/tutup)
    fun getJadwal(onResult: (JadwalConfig) -> Unit) {
        db.collection("config").document("status_praktik").get()
            .addOnSuccessListener { doc ->
                // Ubah data Firestore jadi object JadwalConfig
                val config = doc.toObject(JadwalConfig::class.java) ?: JadwalConfig()
                onResult(config)
            }
            .addOnFailureListener {
                onResult(JadwalConfig()) // Kembalikan default jika error
            }
    }

    // Update status buka/tutup (Switch)
    fun updateStatusPraktik(isOpen: Boolean, onComplete: (Boolean) -> Unit) {
        db.collection("config").document("status_praktik")
            .set(hashMapOf("isOpen" to isOpen), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Update jam buka dan tutup
    fun updateJamPraktik(buka: String, tutup: String, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf("bukaJam" to buka, "tutupJam" to tutup)
        db.collection("config").document("status_praktik")
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // --- BAGIAN 2: ANTRIAN (REALTIME) ---

    // Mendengarkan perubahan data antrian secara langsung (Live)
    fun listenToAntrian(date: String, showCompleted: Boolean, liveData: MutableLiveData<List<Antrian>>) {
        val query = db.collection("antrian")
            .whereEqualTo("tanggal_simpan", date)
            .whereEqualTo("dihapus", false)

        // Filter tambahan: kalau showCompleted false, cuma ambil yang belum selesai
        val finalQuery = if (showCompleted) {
            query
        } else {
            query.whereEqualTo("selesai", false)
        }

        // Urutkan berdasarkan jam
        finalQuery.orderBy("jam", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.map { doc ->
                    val antrian = doc.toObject(Antrian::class.java)!!
                    antrian.id = doc.id // Penting: Simpan ID dokumen biar bisa diedit nanti
                    antrian
                }
                liveData.value = list // Update data ke ViewModel
            }
    }

    // Update status pasien (Selesai, Dipanggil, atau Dihapus)
    fun updateAntrianStatus(docId: String, field: String, value: Any, onComplete: (Boolean) -> Unit) {
        db.collection("antrian").document(docId).update(field, value)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}