package com.thefiresonthebird.freedomweather.presentation

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.thefiresonthebird.freedomweather.services.LocationService
import com.thefiresonthebird.freedomweather.data.WeatherRepository
import com.thefiresonthebird.freedomweather.data.UserPreferencesRepository
import com.thefiresonthebird.freedomweather.BuildConfig
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.thefiresonthebird.freedomweather.presentation.theme.FreedomWeatherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainActivity"

// Enum to track which temperature is currently selected for editing
enum class SelectedTemperature {
    NONE, CELSIUS, FAHRENHEIT
}

class MainActivity : ComponentActivity() {
    private var currentSelectedTemp: SelectedTemperature = SelectedTemperature.NONE
    private var currentCelsiusTemp: Double = 20.0
    private var currentFahrenheitTemp: Double = 68.0
    private var currentMinTemp: Double = 15.0
    private var currentMaxTemp: Double = 25.0
    private var currentConditionIcon: String = "01d"
    private var currentConditionText: String = "Unknown"
    private var currentLastUpdated: Long = 0L
    
    // Weather repository
    private lateinit var weatherRepository: WeatherRepository
    
    // User preferences repository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    
    // State holder to trigger UI updates
    private var stateUpdateTrigger by mutableIntStateOf(0)
    
    // Loading state
    private var isLoading by mutableStateOf(false)
    
    // Location services
    private lateinit var locationService: LocationService
    private var currentLocation: String = "Getting location..."
    
    // Permission request launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d(TAG, "Location permission granted")
                // Location will be fetched in onResume
            }
            else -> {
                Log.w(TAG, "Location permission denied")
                currentLocation = "Location permission denied"
                stateUpdateTrigger++
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Starting MainActivity")
        
        try {
            // Install splash screen for better UX
            installSplashScreen()
            Log.d(TAG, "onCreate: Splash screen installed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to install splash screen", e)
        }

        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: MainActivity created successfully")

        // Set WearOS theme
        setTheme(android.R.style.Theme_DeviceDefault)
        
        // Initialize location services
        locationService = LocationService(this)
        
        // Initialize weather repository
        weatherRepository = WeatherRepository()
        
        // Initialize user preferences repository
        userPreferencesRepository = UserPreferencesRepository(this)
        
        // Initialize weather update helper
        val weatherUpdateHelper = com.thefiresonthebird.freedomweather.data.WeatherUpdateHelper(this)
        
        // Load saved preferences
        lifecycleScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                    currentLocation = preferences.lastLocationName
                    currentCelsiusTemp = preferences.lastTempC
                    currentFahrenheitTemp = preferences.lastTempF
                    currentMinTemp = preferences.lastMinTemp
                    currentMaxTemp = preferences.lastMaxTemp
                    currentConditionIcon = preferences.lastConditionIcon
                    currentConditionText = preferences.lastConditionText
                    currentLastUpdated = preferences.lastUpdated
                    Log.d(TAG, "onCreate: Loaded cached preferences - Loc: ${preferences.lastLocationName}, Temp: ${preferences.lastTempC}")
                    stateUpdateTrigger++
            }
        }
        
        // Check location permissions and get location
        checkLocationPermissions()
        
        // Schedule background updates
        //scheduleWeatherUpdates()

        setContent {
            Log.d(TAG, "onCreate: Setting up Compose content")
            
            // Use the stateUpdateTrigger to force recomposition
            val currentState = stateUpdateTrigger
            
            WearApp(
                onTemperatureSelected = { temp -> 
                    currentSelectedTemp = temp
                    Log.d(TAG, "MainActivity: Temperature selected: $temp")
                    stateUpdateTrigger++ // Force UI recomposition
                },
                onTemperatureDeselected = {
                    currentSelectedTemp = SelectedTemperature.NONE
                    Log.d(TAG, "MainActivity: Temperature deselected")
                    stateUpdateTrigger++ // Force UI recomposition
                },
                celsiusTemp = currentCelsiusTemp,
                fahrenheitTemp = currentFahrenheitTemp,
                minTemp = currentMinTemp,
                maxTemp = currentMaxTemp,
                conditionIcon = currentConditionIcon,
                conditionText = currentConditionText,
                lastUpdated = currentLastUpdated,
                isLoading = isLoading,
                selectedTemp = currentSelectedTemp,
                currentLocation = currentLocation,
                onTemperatureChanged = { celsius, fahrenheit ->
                    val diff = celsius - currentCelsiusTemp
                    currentCelsiusTemp = celsius
                    currentFahrenheitTemp = fahrenheit
                    currentMinTemp += diff
                    currentMaxTemp += diff
                    Log.d(TAG, "MainActivity: Temperature updated - C: $celsius, F: $fahrenheit")
                    Log.d(TAG, "MainActivity: Temperature updated - C: $celsius, F: $fahrenheit")
                    stateUpdateTrigger++ // Force UI recomposition
                },
                onRefresh = {
                    Log.d(TAG, "MainActivity: Refresh requested from UI")
                    refreshLocation()
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed, checking for rotary input and refreshing weather")
        
        // Refresh weather data if permissions are granted
        if (::locationService.isInitialized && locationService.hasLocationPermissions()) {
            isLoading = true
            stateUpdateTrigger++
            getCurrentLocation()
        }
    }
    
    // Method to handle rotary input from the crown/dial
    fun handleRotaryInput(scrollAmount: Float) {
        Log.d(TAG, "handleRotaryInput: Rotary input received, scrollAmount: $scrollAmount, selected: $currentSelectedTemp")
        
        when (currentSelectedTemp) {
            SelectedTemperature.CELSIUS -> {
                val newTemp = currentCelsiusTemp - scrollAmount
                if (newTemp in -100.0..100.0) {
                    currentFahrenheitTemp = (newTemp * 9/5) + 32
                    val diff = newTemp - currentCelsiusTemp
                    currentMinTemp += diff
                    currentMaxTemp += diff
                    currentCelsiusTemp = newTemp
                    Log.d(TAG, "handleRotaryInput: Updated Celsius to $newTemp, Fahrenheit to $currentFahrenheitTemp")
                    // Trigger UI recomposition
                    stateUpdateTrigger++
                }
            }
            SelectedTemperature.FAHRENHEIT -> {
                val newTemp = currentFahrenheitTemp - scrollAmount
                if (newTemp in -148.0..212.0) {
                    currentFahrenheitTemp = newTemp
                    currentCelsiusTemp = (newTemp - 32) * 5/9
                    val diffC = (newTemp - 32) * 5/9 - (currentFahrenheitTemp - 32) * 5/9
                    currentMinTemp += diffC
                    currentMaxTemp += diffC
                    currentFahrenheitTemp = newTemp
                    Log.d(TAG, "handleRotaryInput: Updated Fahrenheit to $newTemp, Celsius to $currentCelsiusTemp")
                    // Trigger UI recomposition
                    stateUpdateTrigger++
                }
            }
            SelectedTemperature.NONE -> {
                Log.d(TAG, "handleRotaryInput: No temperature selected, ignoring rotary input")
            }
        }
    }
    
    // Handle actual rotary input events from the crown/dial
    override fun onGenericMotionEvent(event: android.view.MotionEvent?): Boolean {
        event?.let { motionEvent ->
            Log.d(TAG, "onGenericMotionEvent: Event received - action: ${motionEvent.action}, source: ${motionEvent.source}")
            
            // Check for rotary input events
            if (motionEvent.action == android.view.MotionEvent.ACTION_SCROLL) {
                val scrollAmount = motionEvent.getAxisValue(android.view.MotionEvent.AXIS_SCROLL)
                Log.d(TAG, "onGenericMotionEvent: Rotary scroll detected, amount: $scrollAmount")
                
                // handle the rotary input
                handleRotaryInput(scrollAmount)
                
                return true // Event handled
            }
            
            // Log other motion events for debugging
            if (motionEvent.action != android.view.MotionEvent.ACTION_MOVE) {
                Log.d(TAG, "onGenericMotionEvent: Other motion event - action: ${motionEvent.action}")
            }
        }
        return super.onGenericMotionEvent(event)
    }
    
    // Check if location permissions are granted
    private fun checkLocationPermissions() {
        when {
            locationService.hasLocationPermissions() -> {
                Log.d(TAG, "Location permissions already granted")
                // Location will be fetched in onResume
            }
            else -> {
                Log.d(TAG, "Requesting location permissions")
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    // Get current location using GPS or network
    private fun getCurrentLocation() {
        Log.d(TAG, "getCurrentLocation: Attempting to get current location")
        
        locationService.getCurrentLocationData(
            onLocationReceived = { location, address ->
                currentLocation = address
                Log.d(TAG, "getCurrentLocation: Location received: $address")
                stateUpdateTrigger++
                
                // Fetch weather data
                lifecycleScope.launch {
                    val weatherUpdateHelper = com.thefiresonthebird.freedomweather.data.WeatherUpdateHelper(this@MainActivity)
                    val success = weatherUpdateHelper.updateWeather(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        locationName = address
                    )
                    
                    if (success) {
                        Log.d(TAG, "getCurrentLocation: Weather update successful")
                    } else {
                        Log.w(TAG, "getCurrentLocation: Weather update failed")
                    }
                    isLoading = false
                    stateUpdateTrigger++
                }
            },
            onError = { error ->
                currentLocation = error
                Log.w(TAG, "getCurrentLocation: Error: $error")
                isLoading = false
                stateUpdateTrigger++
            }
        )
    }
    
    // Method to refresh location (can be called from UI if needed)
    fun refreshLocation() {
        Log.d(TAG, "refreshLocation: Refreshing location")
        currentLocation = "Getting location..."
        isLoading = true
        stateUpdateTrigger++
        getCurrentLocation()
    }
    
    // Method to check if location services are available
    fun isLocationServicesAvailable(): Boolean {
        return locationService.isLocationServicesAvailable()
    }

//    private fun scheduleWeatherUpdates() {
//        Log.d(TAG, "scheduleWeatherUpdates: Scheduling periodic weather updates")
//        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.thefiresonthebird.freedomweather.workers.WeatherWorker>(
//            15, java.util.concurrent.TimeUnit.MINUTES
//        )
//            .setConstraints(
//                androidx.work.Constraints.Builder()
//                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
//                    .build()
//            )
//            .build()
//
//        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "WeatherUpdateWork",
//            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
//            workRequest
//        )
//    }
}

@Composable
fun WearApp(
    onTemperatureSelected: (SelectedTemperature) -> Unit,
    onTemperatureDeselected: () -> Unit,
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    minTemp: Double,
    maxTemp: Double,
    conditionIcon: String,
    conditionText: String,
    lastUpdated: Long,
    isLoading: Boolean,
    selectedTemp: SelectedTemperature,
    currentLocation: String,
    onTemperatureChanged: (Double, Double) -> Unit,
    onRefresh: () -> Unit
) {
    Log.d(TAG, "WearApp: Composable function called with temperature: $celsiusTemp")

    FreedomWeatherTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // Display current time (WearOS standard)
            TimeText()
            
            // Display weather interface
            WeatherInterface(
                celsiusTemp = celsiusTemp,
                fahrenheitTemp = fahrenheitTemp,
                minTemp = minTemp,
                maxTemp = maxTemp,
                conditionIcon = conditionIcon,
                conditionText = conditionText,
                lastUpdated = lastUpdated,
                isLoading = isLoading,
                selectedTemp = selectedTemp,
                currentLocation = currentLocation,
                onCelsiusClick = { 
                    Log.d(TAG, "WearApp: Celsius temperature selected")
                    onTemperatureSelected(SelectedTemperature.CELSIUS)
                },
                onFahrenheitClick = { 
                    Log.d(TAG, "WearApp: Fahrenheit temperature selected")
                    onTemperatureSelected(SelectedTemperature.FAHRENHEIT)
                },
                onTemperatureDeselected = onTemperatureDeselected,
                onTemperatureChanged = onTemperatureChanged,
                onRefresh = onRefresh,
                context = LocalContext.current
            )
        }
    }
}

@Composable
fun WeatherInterface(
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    minTemp: Double,
    maxTemp: Double,
    conditionIcon: String,
    conditionText: String,
    lastUpdated: Long,
    isLoading: Boolean,
    selectedTemp: SelectedTemperature,
    currentLocation: String,
    onCelsiusClick: () -> Unit,
    onFahrenheitClick: () -> Unit,
    onTemperatureDeselected: () -> Unit,
    onTemperatureChanged: (Double, Double) -> Unit,
    onRefresh: () -> Unit,
    context: android.content.Context
) {
    Log.d(TAG, "WeatherInterface: Setting up weather interface")
    
    // Temperature conversion functions
    fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9/5) + 32
    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5/9
    
    // Handle temperature changes
    fun updateCelsius(newCelsius: Double) {
        Log.d(TAG, "WeatherInterface: Updating Celsius temperature to: $newCelsius")
        onTemperatureChanged(newCelsius, celsiusToFahrenheit(newCelsius))
    }

    fun updateFahrenheit(newFahrenheit: Double) {
        Log.d(TAG, "WeatherInterface: Updating Fahrenheit temperature to: $newFahrenheit")
        onTemperatureChanged(fahrenheitToCelsius(newFahrenheit), newFahrenheit)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable { 
                // Deselect temperature when tapping elsewhere
                if (selectedTemp != SelectedTemperature.NONE) {
                    Log.d(TAG, "WeatherInterface: Deselecting temperature by tapping elsewhere")
                    onTemperatureDeselected() 
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Location
        LocationRow(location = currentLocation)
        
        Spacer(modifier = Modifier.height(20.dp)) // Add spacing between location and temperature
        
        // Current temperature in Celsius, Icon, and Fahrenheit
        TemperatureRow(
            celsiusTemp = celsiusTemp,
            fahrenheitTemp = fahrenheitTemp,
            conditionIcon = conditionIcon,
            conditionText = conditionText,
            selectedTemp = selectedTemp,
            onCelsiusClick = onCelsiusClick,
            onFahrenheitClick = onFahrenheitClick,
            onRefresh = onRefresh
        )
        
        Spacer(modifier = Modifier.height(20.dp)) // Add spacing between temperature and last updated
        
        // Last updated time
        LastUpdatedRow(lastUpdated = lastUpdated, isLoading = isLoading)
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Version number
        VersionRow()
    }
}

@Composable
fun VersionRow() {
    Text(
        text = "v${BuildConfig.VERSION_NAME}",
        fontSize = 10.sp,
        color = Color(0xFF888888),
        textAlign = TextAlign.Center
    )
}

@Composable
fun LocationRow(location: String) {
    Log.d(TAG, "LocationRow: Displaying location: $location")
    
    Text(
        text = location,
        fontSize = 14.sp,
        color = Color(0xFFBB86FC), // Matching Tile color
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TemperatureRow(
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    conditionIcon: String,
    conditionText: String,
    selectedTemp: SelectedTemperature,
    onCelsiusClick: () -> Unit,
    onFahrenheitClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Log.d(TAG, "TemperatureRow: Displaying temperatures - C: $celsiusTemp, F: $fahrenheitTemp, Selected: $selectedTemp")
    
    val celsiusText = "%.0f°C".format(celsiusTemp)
    val fahrenheitText = "%.0f°F".format(fahrenheitTemp)
    
    // Adjust font size for 3-digit temperatures (length > 4 including degree symbol and unit)
    // Standard size is 30.sp, reduce to 24.sp for longer strings to prevent overflow
    val fontSize = if (celsiusText.length > 4 || fahrenheitText.length > 4) 24.sp else 30.sp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Celsius temperature (left side)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = celsiusText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTemp == SelectedTemperature.CELSIUS) MaterialTheme.colors.primary else Color.White,
                    modifier = Modifier.clickable { onCelsiusClick() }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Weather Icon
            Image(
                painter = painterResource(id = getIconResource(conditionIcon)),
                contentDescription = "Weather Icon",
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRefresh() }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Fahrenheit temperature (right side)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = fahrenheitText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTemp == SelectedTemperature.FAHRENHEIT) MaterialTheme.colors.primary else Color.White,
                    modifier = Modifier.clickable { onFahrenheitClick() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Condition Text
        Text(
            text = conditionText.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            fontSize = 14.sp,
            color = Color(0xFFEEEEEE),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LastUpdatedRow(lastUpdated: Long, isLoading: Boolean) {
    val lastUpdatedText = if (isLoading) {
        "Loading..."
    } else if (lastUpdated > 0) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdated))
        "Updated: $time"
    } else {
        "Update pending"
    }
    
    Text(
        text = lastUpdatedText,
        fontSize = 12.sp,
        color = Color(0xFFAAAAAA), // Matching Tile color
        textAlign = TextAlign.Center
    )
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

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    Log.d(TAG, "DefaultPreview: Preview composable called")
    WearApp(
        onTemperatureSelected = { temp -> 
            Log.d(TAG, "DefaultPreview: Temperature selected: $temp")
        },
        onTemperatureDeselected = {
            Log.d(TAG, "DefaultPreview: Temperature deselected")
        },
        celsiusTemp = 20.0,
        fahrenheitTemp = 68.0,
        minTemp = 15.0,
        maxTemp = 25.0,
        conditionIcon = "01d",
        conditionText = "Clear Sky",
        lastUpdated = System.currentTimeMillis(),
        isLoading = false,
        selectedTemp = SelectedTemperature.NONE, // Pass the default value for preview
        currentLocation = "San Francisco, CA", // Preview location
        onTemperatureChanged = { celsius, fahrenheit ->
            Log.d(TAG, "DefaultPreview: Temperature updated - C: $celsius, F: $fahrenheit")
        },
        onRefresh = {
            Log.d(TAG, "DefaultPreview: Refresh requested")
        }
    )
}