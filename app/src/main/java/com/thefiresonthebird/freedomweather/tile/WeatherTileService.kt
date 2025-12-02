package com.thefiresonthebird.freedomweather.tile

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.thefiresonthebird.freedomweather.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeatherTileService : TileService() {
    private val TAG = "WeatherTileService"
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: Service created")
        userPreferencesRepository = UserPreferencesRepository(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        Log.i(TAG, "onTileRequest: Request received")
        return serviceScope.future {
            try {
                Log.d(TAG, "onTileRequest: Fetching preferences")
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                Log.d(TAG, "onTileRequest: Preferences fetched: ${preferences.lastLocationName}")
                
                val location = preferences.lastLocationName
                val tempC = preferences.lastTempC
                val tempF = preferences.lastTempF
                val conditionIcon = preferences.lastConditionIcon
                val conditionText = preferences.lastConditionText
                val lastUpdated = preferences.lastUpdated

                // Check if data is stale (older than 5 minutes)
                // If stale, trigger update and show loading state
                val isStale = System.currentTimeMillis() - lastUpdated > 300000
                
                Log.d(TAG, "onTileRequest: isStale: $isStale")
                Log.d(TAG, "onTileRequest: Condition icon code: $conditionIcon")
                Log.d(TAG, "onTileRequest: Location: $location")
                Log.d(TAG, "onTileRequest: Temperature C: $tempC")
                Log.d(TAG, "onTileRequest: Temperature F: $tempF")
                Log.d(TAG, "onTileRequest: Condition text: $conditionText")
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastUpdated))
                Log.d(TAG, "onTileRequest: Last updated: $time")
                
                if (isStale) {
                    Log.i(TAG, "onTileRequest: Data is stale, triggering background weather update")
                    serviceScope.launch {
                    try {
                            val weatherUpdateHelper = com.thefiresonthebird.freedomweather.data.WeatherUpdateHelper(this@WeatherTileService)
                            weatherUpdateHelper.updateWeather()
                        } catch (e: Exception) {
                            Log.e(TAG, "onTileRequest: Error triggering background weather update", e)
                        }
                    }
                } else {
                    Log.i(TAG, "onTileRequest: Data is fresh, no update requested")
                }

                val deviceParams = DeviceParametersBuilders.DeviceParameters.Builder()
                    .setScreenDensity(requestParams.deviceConfiguration.screenDensity)
                    .setScreenWidthDp(requestParams.deviceConfiguration.screenWidthDp)
                    .setScreenHeightDp(requestParams.deviceConfiguration.screenHeightDp)
                    .setScreenShape(requestParams.deviceConfiguration.screenShape)
                    .build()

                Log.i(TAG, "onTileRequest: Building tile")
                val tile = TileBuilders.Tile.Builder()
                    .setResourcesVersion("2")
                    .setFreshnessIntervalMillis(15 * 60 * 1000L) // tile will refresh every 15 minutes
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(
                                                layout(this@WeatherTileService, deviceParams, location, tempC, tempF, conditionIcon, conditionText, lastUpdated)
                                            )
                                            .build()
                            )
                            .build()
                    )
                    .build()
                )
                .build()
                Log.i(TAG, "onTileRequest: Returning tile")
                tile
            } catch (e: Exception) {
                Log.e(TAG, "onTileRequest: Error building tile", e)
                // Return a basic error tile
                TileBuilders.Tile.Builder()
                    .setResourcesVersion("1")
                    .setTileTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(
                                                Text.Builder(this@WeatherTileService, "Error loading tile")
                                                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                                                    .setColor(ColorBuilders.argb(0xFFFF0000.toInt()))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }
        }
    }

    private val ALL_ICONS = listOf(
        "01d", "01n",
        "02d", "02n", "03d", "03n", "04d", "04n",
        "09d", "09n", "10d", "10n", "11d", "11n",
        "13d", "13n",
        "50d", "50n"
    )

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<androidx.wear.protolayout.ResourceBuilders.Resources> {
        Log.i(TAG, "onTileResourcesRequest: Request received")
        return serviceScope.future {
            val resourcesBuilder = androidx.wear.protolayout.ResourceBuilders.Resources.Builder()
                .setVersion("2")
            Log.i(TAG, "onTileResourcesRequest: Loading condition icons")
            ALL_ICONS.forEach { iconCode ->
                Log.d(TAG, "onTileResourcesRequest: Loading icon $iconCode")
                resourcesBuilder.addIdToImageMapping(iconCode, androidx.wear.protolayout.ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(getIconResource(iconCode))
                            .build()
                    )
                    .build()
                )
            }

            resourcesBuilder.build()
        }
    }

    private fun getIconResource(iconCode: String): Int {
        return when (iconCode) {
            "01d" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_clear
            "01n" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_clear_night
            "02d", "02n", "03d", "03n", "04d", "04n" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_cloudy
            "09d", "09n", "10d", "10n", "11d", "11n" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_rain
            "13d", "13n" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_snow
            "50d", "50n" -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_mist
            else -> com.thefiresonthebird.freedomweather.R.drawable.ic_weather_clear
        }
    }
}

private fun layout(
    context: Context,
    deviceParameters: DeviceParametersBuilders.DeviceParameters,
    location: String,
    tempC: Double,
    tempF: Double,
    conditionIcon: String,
    conditionText: String,
    lastUpdated: Long
): LayoutElementBuilders.LayoutElement {

    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastUpdated))
    val lastUpdatedText = "Updated: $time"

    val launchActivity = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName("com.thefiresonthebird.freedomweather.presentation.MainActivity")
                .setPackageName("com.thefiresonthebird.freedomweather")
                .build()
        )
        .build()
        
    val celsiusText = "%.0f°C".format(tempC)
    val fahrenheitText = "%.0f°F".format(tempF)
    
    // Adaptive font size: 30sp default, 24sp for longer text
    val fontSizeSp = if (celsiusText.length > 4 || fahrenheitText.length > 4) 24f else 30f

    // Use a custom Column layout instead of PrimaryLayout to have full control over vertical stacking
    // and avoid layout overflow issues when adding extra elements like lastUpdated text.
    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(
            LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand()) // Ensure Column takes full width
                .addContent(
                    Text.Builder(context, location)
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(0xFFBB86FC.toInt()))
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(20f)) // Add spacing between location and temperature
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Row.Builder()
                        .setWidth(DimensionBuilders.expand()) // Row takes full width
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER) // Align items vertically
                        .addContent(
                            // Celsius (Left) - Weighted Box
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.weight(1f))
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_END)
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setText(celsiusText)
                                        .setFontStyle(
                                            LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(DimensionBuilders.sp(fontSizeSp))
                                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setWidth(DimensionBuilders.dp(8f)) 
                                .build()
                        )
                        .addContent(
                            // Icon (Center)
                            LayoutElementBuilders.Image.Builder()
                                .setResourceId(conditionIcon)
                                .setWidth(DimensionBuilders.dp(40f))
                                .setHeight(DimensionBuilders.dp(40f))
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setWidth(DimensionBuilders.dp(8f))
                                .build()
                        )
                        .addContent(
                            // Fahrenheit (Right) - Weighted Box
                            LayoutElementBuilders.Box.Builder()
                                .setWidth(DimensionBuilders.weight(1f))
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
                                .addContent(
                                    LayoutElementBuilders.Text.Builder()
                                        .setText(fahrenheitText)
                                        .setFontStyle(
                                            LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(DimensionBuilders.sp(fontSizeSp))
                                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                                .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(4f)) // Add spacing between temperature and condition
                        .build()
                )
                .addContent(
                    Text.Builder(context, conditionText)
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(0xFFEEEEEE.toInt()))
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(20f)) // Add spacing between condition and last updated
                        .build()
                )
                .addContent(
                    Text.Builder(context, lastUpdatedText)
                        .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                        .setColor(ColorBuilders.argb(0xFFAAAAAA.toInt()))
                        .build()
                )
                .build()
        )
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(
                    ModifiersBuilders.Clickable.Builder()
                        .setId("open_app")
                        .setOnClick(launchActivity)
                        .build()
                )
                .build()
        )
        .build()
}