package com.thefiresonthebird.freedomweather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val LAST_LOCATION_NAME = stringPreferencesKey("last_location_name")
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        val LAST_TEMP_C = doublePreferencesKey("last_temp_c")
        val LAST_TEMP_F = doublePreferencesKey("last_temp_f")
        val LAST_MIN_TEMP = doublePreferencesKey("last_min_temp")
        val LAST_MAX_TEMP = doublePreferencesKey("last_max_temp")
        val LAST_CONDITION_ICON = stringPreferencesKey("last_condition_icon")
        val LAST_CONDITION_TEXT = stringPreferencesKey("last_condition_text")
        val LAST_UPDATED = longPreferencesKey("last_updated")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                lastLocationName = preferences[LAST_LOCATION_NAME] ?: "Unknown",
                lastLatitude = preferences[LAST_LATITUDE] ?: 0.0,
                lastLongitude = preferences[LAST_LONGITUDE] ?: 0.0,
                lastTempC = preferences[LAST_TEMP_C] ?: 20.0,
                lastTempF = preferences[LAST_TEMP_F] ?: 68.0,
                lastMinTemp = preferences[LAST_MIN_TEMP] ?: 15.0,
                lastMaxTemp = preferences[LAST_MAX_TEMP] ?: 25.0,
                lastConditionIcon = preferences[LAST_CONDITION_ICON] ?: "01d",
                lastConditionText = preferences[LAST_CONDITION_TEXT] ?: "Unknown",
                lastUpdated = preferences[LAST_UPDATED] ?: 0L
            )
        }

    suspend fun saveWeather(
        locationName: String,
        latitude: Double,
        longitude: Double,
        tempC: Double,
        tempF: Double,
        minTemp: Double,
        maxTemp: Double,
        conditionIcon: String,
        conditionText: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOCATION_NAME] = locationName
            preferences[LAST_LATITUDE] = latitude
            preferences[LAST_LONGITUDE] = longitude
            preferences[LAST_TEMP_C] = tempC
            preferences[LAST_TEMP_F] = tempF
            preferences[LAST_MIN_TEMP] = minTemp
            preferences[LAST_MAX_TEMP] = maxTemp
            preferences[LAST_CONDITION_ICON] = conditionIcon
            preferences[LAST_CONDITION_TEXT] = conditionText
            preferences[LAST_UPDATED] = System.currentTimeMillis()
        }
    }
}

data class UserPreferences(
    val lastLocationName: String,
    val lastLatitude: Double,
    val lastLongitude: Double,
    val lastTempC: Double,
    val lastTempF: Double,
    val lastMinTemp: Double,
    val lastMaxTemp: Double,
    val lastConditionIcon: String,
    val lastConditionText: String,
    val lastUpdated: Long
)
