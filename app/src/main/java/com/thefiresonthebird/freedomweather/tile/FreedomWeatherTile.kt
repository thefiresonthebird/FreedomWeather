package com.thefiresonthebird.freedomweather.tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class FreedomWeatherTile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {

        }
    }
}
