package com.thefiresonthebird.freedomweather.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun FreedomWeatherTheme(
    content: @Composable () -> Unit
) {
   MaterialTheme(
        content = content
    )
}