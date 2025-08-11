# Project Overview
This is a WearOS application written in Kotlin using Jetpack Compose. The app includes:
- Main activity with WearOS-specific UI components
- Tile service for quick access
- Modern Material Design 3 theming
- Designed specifically for the Pixel Watch 3, so only round watch face designs relevant

## Key Features
- **Standalone WearOS App**: Runs independently without requiring a companion phone app
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Tiles Support**: Quick access information on the watch face

## Development Tips
- Use the WearOS-specific preview devices in Compose previews
- Consider battery life when implementing features
- Use WearOS-specific UI components for better user experience


# Freedom Weather Project
- This project is a simple weather app with the key feature being it shows temperature in Celsius and Fahrenheit side by side
- The app is designed to help users quickly convert between temperature units, relative to the current local weather
- The applciation will consist of a single Main activity and a Tile service

## Main Activity
- When loaded, the main activity will present the user with a very simple interface
   - Top row: the user's current location
   - Center row, left: the current local temperature in Celsius
   - Center row, right: the curent local temperature in Fahrenheit
   - Bottom row: The local weather's minimum and maximum daily temperature in Fahrenheit
- The main activity should allow the user to modify either of the Celsius or Fahrenheit numbers, with the other value cahnging in real time
   - If the user presses the Celsius number on the center row, it should be highlighted in the UI
   - Once highlighted, the user should be able to scroll the watches dial to increase, or decrease the number shown
   - As the value changes following user input, the corresponsing Fahrenheit number should update to match
   - If the user presses the Fahrenheit number, that one should be highlighted in the UI and updated via the watches dial, with Celsius updating to match
   - If the user presses anywhere else on the display, the UI should revert to default with nothing highlighted


## Tile Service
- The tile service will act as a quick view for the current weather at the user's cloation in both Celsius and Fahrenheit
- The view will have three rows of content:
   - Top row: small text showing the current location
   - Centre row: On the left an icon of the current weather, in the middle the current temperature in Celsius, on the right in smaller text the maximum temperature on top of the minimum temperature in Celsius
   - Bottom row: the same size, layout, and design as the center row, but with all temperatures in Fahrenheit