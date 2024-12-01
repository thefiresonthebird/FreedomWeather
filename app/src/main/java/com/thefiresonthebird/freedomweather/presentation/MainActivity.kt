
package com.thefiresonthebird.freedomweather.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.thefiresonthebird.freedomweather.R
import com.thefiresonthebird.freedomweather.presentation.theme.FreedomWeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            FreedomMainView(0)
        }
    }
}

@Composable
fun FreedomMainView(temp: Int) {
    val focusRequester: FocusRequester = remember { FocusRequester() }
    Log.d("FreedomMainView", "Loaded")
    FreedomWeatherTheme {
        Box (
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    // handle rotary scroll events
                    Log.d("FreedomMainView", "Rotation")
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .background(MaterialTheme.colors.background),
            //contentAlignment = Alignment.Center
        ) {
            TimeText()
            Text("Hello")
            //TempScroller(temp = temp)
        }
    }
}

@Composable
fun TempScroller(temp: Int) {
    val focusRequester: FocusRequester = remember { FocusRequester() }
    Log.d("TempScroller", "Loaded")

    Column (
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                // handle rotary scroll events
                Log.d("TempScroller", "Rotation")
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
    ){
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = stringResource(R.string.hello_world, temp)
        )
    }

}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    FreedomMainView(0)
}