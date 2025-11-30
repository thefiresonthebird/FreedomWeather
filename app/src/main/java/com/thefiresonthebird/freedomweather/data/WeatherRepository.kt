package com.thefiresonthebird.freedomweather.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    private val apiService: WeatherApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(WeatherApiService::class.java)
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse? {
        return try {
            val apiKey = com.thefiresonthebird.freedomweather.BuildConfig.WEATHER_API_KEY
            apiService.getCurrentWeather(lat, lon, apiKey)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather", e)
            null
        }
    }
}
