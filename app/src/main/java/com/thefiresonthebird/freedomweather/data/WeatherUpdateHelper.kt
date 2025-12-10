package com.thefiresonthebird.freedomweather.data

import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import com.thefiresonthebird.freedomweather.tile.WeatherTileService
import kotlinx.coroutines.flow.first

class WeatherUpdateHelper(private val context: Context) {

    private val TAG = "WeatherUpdateHelper"
    private val weatherRepository = WeatherRepository()
    private val userPreferencesRepository = UserPreferencesRepository(context)

    suspend fun updateWeather(
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ): Boolean {
        Log.i(TAG, "updateWeather: Starting weather update")

        return try {
            val finalLatitude: Double
            val finalLongitude: Double
            val finalLocationName: String

            if (latitude != null && longitude != null && locationName != null) {
                // Use provided location
                finalLatitude = latitude
                finalLongitude = longitude
                finalLocationName = locationName
                Log.d(TAG, "updateWeather: Using provided location: $finalLocationName ($finalLatitude, $finalLongitude)")
            } else {
                // Get last known location from preferences
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                finalLatitude = preferences.lastLatitude
                finalLongitude = preferences.lastLongitude
                finalLocationName = preferences.lastLocationName
                Log.d(TAG, "updateWeather: Using saved location: $finalLocationName ($finalLatitude, $finalLongitude)")
            }

            if (finalLatitude == 0.0 && finalLongitude == 0.0) {
                Log.w(TAG, "updateWeather: No location saved, cannot update weather")
                return false
            }

            Log.i(TAG, "updateWeather: Fetching weather for $finalLocationName ($finalLatitude, $finalLongitude)")
            val weather = weatherRepository.getCurrentWeather(finalLatitude, finalLongitude)

            if (weather != null) {
                Log.i(TAG, "updateWeather: Weather fetched successfully: ${weather.main.temp}°C")
                
                userPreferencesRepository.saveWeather(
                    locationName = finalLocationName,
                    latitude = finalLatitude,
                    longitude = finalLongitude,
                    tempC = weather.main.temp,
                    tempF = (weather.main.temp * 9/5) + 32,
                    minTemp = weather.main.tempMin,
                    maxTemp = weather.main.tempMax,
                    conditionIcon = weather.weather.firstOrNull()?.icon ?: "01d",
                    conditionText = weather.weather.firstOrNull()?.main ?: "Unknown"
                )
                
                // Request tile update
                TileService.getUpdater(context)
                    .requestUpdate(WeatherTileService::class.java)

                Log.d(TAG, "Location: $finalLocationName")
                Log.d(TAG, "Condition icon code: ${weather.weather.firstOrNull()?.icon ?: "01d"}")
                Log.d(TAG, "Condition text: ${weather.weather.firstOrNull()?.main ?: "Unknown"}")
                Log.d(TAG, "Temperature C: ${weather.main.temp}")
                Log.d(TAG, "Temperature F: ${(weather.main.temp * 9/5) + 32}")

                
                Log.i(TAG, "updateWeather: Weather saved and tile update requested")
                true
            } else {
                Log.e(TAG, "updateWeather: Failed to fetch weather")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateWeather: Error updating weather", e)
            false
        }
    }
}
