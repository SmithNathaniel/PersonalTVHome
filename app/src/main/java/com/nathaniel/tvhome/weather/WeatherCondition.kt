package com.nathaniel.tvhome.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.ui.graphics.vector.ImageVector

/** WMO weather interpretation codes, as returned by Open-Meteo. */
enum class WeatherCondition(val label: String) {
    Clear("Clear"), PartlyCloudy("Partly cloudy"), Cloudy("Cloudy"), Fog("Fog"),
    Drizzle("Drizzle"), Rain("Rain"), Snow("Snow"), Showers("Showers"), Thunderstorm("Thunderstorm");

    fun icon(isDay: Boolean): ImageVector = when (this) {
        Clear -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.Bedtime
        PartlyCloudy -> Icons.Rounded.WbCloudy
        Cloudy -> Icons.Rounded.Cloud
        Fog -> Icons.Rounded.Dehaze
        Drizzle -> Icons.Rounded.Grain
        Rain, Showers -> Icons.Rounded.WaterDrop
        Snow -> Icons.Rounded.AcUnit
        Thunderstorm -> Icons.Rounded.Thunderstorm
    }

    companion object {
        fun fromWmo(code: Int): WeatherCondition = when (code) {
            0 -> Clear
            1, 2 -> PartlyCloudy
            3 -> Cloudy
            45, 48 -> Fog
            51, 53, 55, 56, 57 -> Drizzle
            61, 63, 65, 66, 67 -> Rain
            71, 73, 75, 77, 85, 86 -> Snow
            80, 81, 82 -> Showers
            95, 96, 99 -> Thunderstorm
            else -> Cloudy
        }
    }
}
