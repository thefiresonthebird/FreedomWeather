/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.thefiresonthebird.freedomweather.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

        setContent {
            Log.d(TAG, "onCreate: Setting up Compose content")
            WearApp("Android")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    Log.d(TAG, "WearApp: Composable function called with greeting: $greetingName")

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
            WeatherInterface()
        }
    }
}

@Composable
fun WeatherInterface() {
    Log.d(TAG, "WeatherInterface: Setting up weather interface")
    
    // State for temperature values and selection
    var celsiusTemp by remember { mutableStateOf(20.0) }
    var fahrenheitTemp by remember { mutableStateOf(68.0) }
    var minTemp by remember { mutableStateOf(15.0) }
    var maxTemp by remember { mutableStateOf(25.0) }
    var selectedTemp by remember { mutableStateOf(SelectedTemperature.NONE) }
    
    // Placeholder location
    val location = "San Francisco, CA"
    
    // Temperature conversion functions
    fun celsiusToFahrenheit(celsius: Double): Double = (celsius * 9/5) + 32
    fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5/9
    
    // Handle temperature changes
    fun updateCelsius(newCelsius: Double) {
        Log.d(TAG, "WeatherInterface: Updating Celsius temperature to: $newCelsius")
        celsiusTemp = newCelsius
        fahrenheitTemp = celsiusToFahrenheit(newCelsius)
        // Update min/max to maintain relative relationship
        val tempDiff = newCelsius - 20.0 // difference from base temp
        minTemp = 15.0 + tempDiff
        maxTemp = 25.0 + tempDiff
    }
    
    fun updateFahrenheit(newFahrenheit: Double) {
        Log.d(TAG, "WeatherInterface: Updating Fahrenheit temperature to: $newFahrenheit")
        fahrenheitTemp = newFahrenheit
        celsiusTemp = fahrenheitToCelsius(newFahrenheit)
        // Update min/max to maintain relative relationship
        val tempDiff = newFahrenheit - 68.0 // difference from base temp
        minTemp = 59.0 + tempDiff
        maxTemp = 77.0 + tempDiff
    }
    
    // Handle watch dial input simulation (for testing)
    fun handleDialInput(increment: Boolean) {
        when (selectedTemp) {
            SelectedTemperature.CELSIUS -> {
                val newTemp = if (increment) celsiusTemp + 1 else celsiusTemp - 1
                if (newTemp in -50.0..50.0) { // reasonable temperature range
                    updateCelsius(newTemp)
                }
            }
            SelectedTemperature.FAHRENHEIT -> {
                val newTemp = if (increment) fahrenheitTemp + 1 else fahrenheitTemp - 1
                if (newTemp in -58.0..122.0) { // reasonable temperature range
                    updateFahrenheit(newTemp)
                }
            }
            SelectedTemperature.NONE -> {
                // No temperature selected, do nothing
                Log.d(TAG, "WeatherInterface: No temperature selected for dial input")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable { 
                // Deselect temperature when tapping elsewhere
                if (selectedTemp != SelectedTemperature.NONE) {
                    Log.d(TAG, "WeatherInterface: Deselecting temperature by tapping elsewhere")
                    selectedTemp = SelectedTemperature.NONE 
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Top row: Location
        LocationRow(location = location)
        
        // Status indicator showing which temperature is selected
        if (selectedTemp != SelectedTemperature.NONE) {
            Text(
                text = "Editing: ${if (selectedTemp == SelectedTemperature.CELSIUS) "Celsius" else "Fahrenheit"}",
                fontSize = 12.sp,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Center row: Current temperature in Celsius and Fahrenheit
        TemperatureRow(
            celsiusTemp = celsiusTemp,
            fahrenheitTemp = fahrenheitTemp,
            selectedTemp = selectedTemp,
            onCelsiusClick = { 
                Log.d(TAG, "WeatherInterface: Celsius temperature selected")
                selectedTemp = SelectedTemperature.CELSIUS 
            },
            onFahrenheitClick = { 
                Log.d(TAG, "WeatherInterface: Fahrenheit temperature selected")
                selectedTemp = SelectedTemperature.FAHRENHEIT 
            }
        )
        
        // Bottom row: Min/Max temperatures in Fahrenheit
        MinMaxRow(
            minTemp = minTemp,
            maxTemp = maxTemp
        )
        
        // Simple test buttons for simulating watch dial input
        if (selectedTemp != SelectedTemperature.NONE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "↻ -1",
                    modifier = Modifier
                        .clickable { handleDialInput(false) }
                        .padding(8.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = "↻ +1",
                    modifier = Modifier
                        .clickable { handleDialInput(true) }
                        .padding(8.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary
                )
            }
        }
        
        // Handle watch dial input for temperature editing
        LaunchedEffect(selectedTemp) {
            // TODO: Implement actual watch dial input handling
            // For now, this is just a placeholder for the dial input logic
            Log.d(TAG, "WeatherInterface: Selected temperature changed to: $selectedTemp")
        }
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
    WearApp("Preview Android")
}