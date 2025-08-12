package com.thefiresonthebird.freedomweather.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.thefiresonthebird.freedomweather.services.LocationService
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

private const val TAG = "MainActivity"

// Enum to track which temperature is currently selected for editing
enum class SelectedTemperature {
    NONE, CELSIUS, FAHRENHEIT
}

class MainActivity : ComponentActivity() {
    private var currentSelectedTemp: SelectedTemperature = SelectedTemperature.NONE
    private var currentCelsiusTemp: Double = 20.0
    private var currentFahrenheitTemp: Double = 68.0
    
    // State holder to trigger UI updates
    private var stateUpdateTrigger by mutableStateOf(0)
    
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
                getCurrentLocation()
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
        
        // Check location permissions and get location
        checkLocationPermissions()

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
                selectedTemp = currentSelectedTemp,
                currentLocation = currentLocation,
                onTemperatureChanged = { celsius, fahrenheit ->
                    currentCelsiusTemp = celsius
                    currentFahrenheitTemp = fahrenheit
                    Log.d(TAG, "MainActivity: Temperature updated - C: $celsius, F: $fahrenheit")
                    stateUpdateTrigger++ // Force UI recomposition
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed, checking for rotary input")
    }
    
    // Method to handle rotary input from the crown/dial
    fun handleRotaryInput(scrollAmount: Float) {
        Log.d(TAG, "handleRotaryInput: Rotary input received, scrollAmount: $scrollAmount, selected: $currentSelectedTemp")
        
        when (currentSelectedTemp) {
            SelectedTemperature.CELSIUS -> {
                val newTemp = currentCelsiusTemp - scrollAmount
                if (newTemp in -50.0..50.0) {
                    currentCelsiusTemp = newTemp
                    currentFahrenheitTemp = (newTemp * 9/5) + 32
                    Log.d(TAG, "handleRotaryInput: Updated Celsius to $newTemp, Fahrenheit to $currentFahrenheitTemp")
                    // Trigger UI recomposition
                    stateUpdateTrigger++
                }
            }
            SelectedTemperature.FAHRENHEIT -> {
                val newTemp = currentFahrenheitTemp - scrollAmount
                if (newTemp in -58.0..122.0) {
                    currentFahrenheitTemp = newTemp
                    currentCelsiusTemp = (newTemp - 32) * 5/9
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
                getCurrentLocation()
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
        
        locationService.getCurrentLocation(
            onLocationReceived = { location ->
                currentLocation = location
                Log.d(TAG, "getCurrentLocation: Location received: $location")
                stateUpdateTrigger++
            },
            onError = { error ->
                currentLocation = error
                Log.w(TAG, "getCurrentLocation: Error: $error")
                stateUpdateTrigger++
            }
        )
    }
    

    
    // Method to refresh location (can be called from UI if needed)
    fun refreshLocation() {
        Log.d(TAG, "refreshLocation: Refreshing location")
        currentLocation = "Getting location..."
        stateUpdateTrigger++
        getCurrentLocation()
    }
    
    // Method to check if location services are available
    fun isLocationServicesAvailable(): Boolean {
        return locationService.isLocationServicesAvailable()
    }

}

@Composable
fun WearApp(
    onTemperatureSelected: (SelectedTemperature) -> Unit,
    onTemperatureDeselected: () -> Unit,
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    selectedTemp: SelectedTemperature,
    currentLocation: String,
    onTemperatureChanged: (Double, Double) -> Unit
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
                context = LocalContext.current
            )
        }
    }
}

@Composable
fun WeatherInterface(
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    selectedTemp: SelectedTemperature,
    currentLocation: String,
    onCelsiusClick: () -> Unit,
    onFahrenheitClick: () -> Unit,
    onTemperatureDeselected: () -> Unit,
    onTemperatureChanged: (Double, Double) -> Unit,
    context: android.content.Context
) {
    Log.d(TAG, "WeatherInterface: Setting up weather interface")
    
    // State for min/max temperatures
    var minTemp by remember { mutableStateOf(15.0) }
    var maxTemp by remember { mutableStateOf(25.0) }
    
    // Temperature conversion functions
    fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9/5) + 32
    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5/9
    
    // Handle temperature changes
    fun updateCelsius(newCelsius: Double) {
        Log.d(TAG, "WeatherInterface: Updating Celsius temperature to: $newCelsius")
        onTemperatureChanged(newCelsius, celsiusToFahrenheit(newCelsius))
        // Update min/max to maintain relative relationship
        val tempDiff = newCelsius - 20.0 // difference from base temp
        minTemp = 15.0 + tempDiff
        maxTemp = 25.0 + tempDiff
        // Trigger UI update
    }
    
    fun updateFahrenheit(newFahrenheit: Double) {
        Log.d(TAG, "WeatherInterface: Updating Fahrenheit temperature to: $newFahrenheit")
        onTemperatureChanged(fahrenheitToCelsius(newFahrenheit), newFahrenheit)
        // Update min/max to maintain relative relationship
        val tempDiff = newFahrenheit - 68.0 // difference from base temp
        minTemp = 59.0 + tempDiff
        maxTemp = 77.0 + tempDiff
        // Trigger UI update
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
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Top row: Location
        LocationRow(location = currentLocation)
        
        // Center row: Current temperature in Celsius and Fahrenheit
        TemperatureRow(
            celsiusTemp = celsiusTemp,
            fahrenheitTemp = fahrenheitTemp,
            selectedTemp = selectedTemp,
            onCelsiusClick = onCelsiusClick,
            onFahrenheitClick = onFahrenheitClick
        )
        
        // Bottom row: Min/Max temperatures in Fahrenheit
        MinMaxRow(
            minTemp = minTemp,
            maxTemp = maxTemp
        )

    }
}

@Composable
fun LocationRow(location: String) {
    Log.d(TAG, "LocationRow: Displaying location: $location")
    
    Text(
        text = location,
        fontSize = 16.sp,
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TemperatureRow(
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    selectedTemp: SelectedTemperature,
    onCelsiusClick: () -> Unit,
    onFahrenheitClick: () -> Unit
) {
    Log.d(TAG, "TemperatureRow: Displaying temperatures - C: $celsiusTemp, F: $fahrenheitTemp, Selected: $selectedTemp")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Celsius temperature (left side)
        TemperatureDisplay(
            value = celsiusTemp,
            unit = "°C",
            isSelected = selectedTemp == SelectedTemperature.CELSIUS,
            onClick = onCelsiusClick,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Fahrenheit temperature (right side)
        TemperatureDisplay(
            value = fahrenheitTemp,
            unit = "°F",
            isSelected = selectedTemp == SelectedTemperature.FAHRENHEIT,
            onClick = onFahrenheitClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TemperatureDisplay(
    value: Double,
    unit: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "TemperatureDisplay: Displaying $value$unit, Selected: $isSelected")
    
    val borderColor = if (isSelected) MaterialTheme.colors.primary else Color.Transparent
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    
    Box(
        modifier = modifier
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%.1f".format(value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MinMaxRow(
    minTemp: Double,
    maxTemp: Double
) {
    Log.d(TAG, "MinMaxRow: Displaying min: $minTemp, max: $maxTemp")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Min temperature
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MIN",
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${minTemp.toInt()}°F",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
        }
        
        // Max temperature
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MAX",
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${maxTemp.toInt()}°F",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
        }
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
        selectedTemp = SelectedTemperature.NONE, // Pass the default value for preview
        currentLocation = "San Francisco, CA", // Preview location
        onTemperatureChanged = { celsius, fahrenheit ->
            Log.d(TAG, "DefaultPreview: Temperature updated - C: $celsius, F: $fahrenheit")
        }
    )
}