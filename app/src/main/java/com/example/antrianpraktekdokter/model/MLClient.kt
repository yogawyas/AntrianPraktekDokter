package com.example.antrianpraktekdokter.model
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Data yang diterima dari API Python
data class PredictionResponse(
    val probability: Double,
    val status: String
)

data class PredictionRequest(
    val gender: Int,
    val age: Int,
    val neighbourhood: Int,
    val scholarship: Int,
    val hipertension: Int,
    val diabetes: Int,
    val alcoholism: Int,
    val handcap: Int,
    val sms_received: Int,
    val date_diff: Int
)

interface MLApiService {
    @POST("predict")
    // Ganti Map dengan data class PredictionRequest
    fun getPrediction(@Body input: PredictionRequest): Call<PredictionResponse>
}

object MLClient {
    private const val BASE_URL = "https://tarekh001.pythonanywhere.com/" // GANTI DENGAN URL ANDA

    val instance: MLApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MLApiService::class.java)
    }
}
