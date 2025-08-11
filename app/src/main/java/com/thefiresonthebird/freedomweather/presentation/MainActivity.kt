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
        
        // The actual rotary input handling will be implemented in the next iteration
        Log.d(TAG, "onResume: Rotary input handler setup complete")
    }
    
    // Method to handle rotary input from the crown/dial
    fun handleRotaryInput(increment: Boolean) {
        Log.d(TAG, "handleRotaryInput: Rotary input received, increment: $increment, selected: $currentSelectedTemp")
        
        when (currentSelectedTemp) {
            SelectedTemperature.CELSIUS -> {
                val newTemp = if (increment) currentCelsiusTemp - 0.5 else currentCelsiusTemp + 0.5
                if (newTemp in -50.0..50.0) {
                    currentCelsiusTemp = newTemp
                    currentFahrenheitTemp = (newTemp * 9/5) + 32
                    Log.d(TAG, "handleRotaryInput: Updated Celsius to $newTemp, Fahrenheit to $currentFahrenheitTemp")
                    // Trigger UI recomposition
                    stateUpdateTrigger++
                }
            }
            SelectedTemperature.FAHRENHEIT -> {
                val newTemp = if (increment) currentFahrenheitTemp - 0.5 else currentFahrenheitTemp + 0.5
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
                
                // Convert scroll amount to increment/decrement
                val increment = scrollAmount > 0
                handleRotaryInput(increment)
                
                return true // Event handled
            }
            
            // Log other motion events for debugging
            if (motionEvent.action != android.view.MotionEvent.ACTION_MOVE) {
                Log.d(TAG, "onGenericMotionEvent: Other motion event - action: ${motionEvent.action}")
            }
        }
        return super.onGenericMotionEvent(event)
    }
    
    // Public method to simulate rotary input for testing
    fun simulateRotaryInput(increment: Boolean) {
        Log.d(TAG, "simulateRotaryInput: Simulating rotary input, increment: $increment")
        handleRotaryInput(increment)
    }
}

@Composable
fun WearApp(
    onTemperatureSelected: (SelectedTemperature) -> Unit,
    onTemperatureDeselected: () -> Unit,
    celsiusTemp: Double,
    fahrenheitTemp: Double,
    selectedTemp: SelectedTemperature,
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
    
    // Placeholder location
    val location = "San Francisco, CA"
    
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
        LocationRow(location = location)
        
        // Status indicator showing which temperature is selected
        if (selectedTemp != SelectedTemperature.NONE) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Editing: ${if (selectedTemp == SelectedTemperature.CELSIUS) "Celsius" else "Fahrenheit"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Text(
                    text = "Use crown/dial to adjust",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        
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
        
        // Simple test buttons to demonstrate rotary input functionality
        // These simulate what the actual crown/dial input will do
        if (selectedTemp != SelectedTemperature.NONE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "↻ -0.5°",
                    modifier = Modifier
                        .clickable { 
                            // Simulate crown/dial input for testing
                            (context as? MainActivity)?.simulateRotaryInput(false)
                        }
                        .padding(8.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary
                )
                Text(
                    text = "↻ +0.5°",
                    modifier = Modifier
                        .clickable { 
                            // Simulate crown/dial input for testing
                            (context as? MainActivity)?.simulateRotaryInput(true)
                        }
                        .padding(8.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primary
                )
            }
        }
        
        // Handle actual watch dial input for temperature editing
        LaunchedEffect(selectedTemp) {
            Log.d(TAG, "WeatherInterface: Selected temperature changed to: $selectedTemp")
            
            // TODO: In a real implementation, this would connect to the actual rotary input
            // For now, we'll simulate the behavior and prepare the structure
            // The actual rotary input handling will be implemented in the next iteration
        }
        
        // Remove test buttons - replaced with actual watch dial input
        // The user will now use the physical crown/dial to adjust temperatures
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
        onTemperatureChanged = { celsius, fahrenheit ->
            Log.d(TAG, "DefaultPreview: Temperature updated - C: $celsius, F: $fahrenheit")
        }
    )
}