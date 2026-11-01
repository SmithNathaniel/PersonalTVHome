package com.nathaniel.tvhome.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.prefs.WeatherLocation
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.weather.WeatherState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val OWNER_NAME = "Nathaniel Lucky Smith"

/** Ticks once per second (or per minute), aligned to the wall clock. */
@Composable
fun rememberNow(withSeconds: Boolean): LocalDateTime {
    val now by produceState(LocalDateTime.now(), withSeconds) {
        while (true) {
            val current = LocalDateTime.now()
            value = current
            val waitMs = if (withSeconds) 1000L - current.nano / 1_000_000 else (60 - current.second) * 1000L - current.nano / 1_000_000
            delay(waitMs.coerceAtLeast(50))
        }
    }
    return now
}

@Composable
fun PersonalHeader(
    use24Hour: Boolean,
    showSeconds: Boolean,
    weather: WeatherState,
    location: WeatherLocation,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow(showSeconds)
    val locale = Locale.getDefault()
    val timePattern = when {
        use24Hour && showSeconds -> "HH:mm:ss"
        use24Hour -> "HH:mm"
        showSeconds -> "h:mm:ss"
        else -> "h:mm"
    }
    Column(modifier) {
        Text("Welcome", style = Scenic.Welcome)
        Text(OWNER_NAME, style = Scenic.Name)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(now.format(DateTimeFormatter.ofPattern(timePattern, locale)), style = Scenic.Clock)
            if (!use24Hour) {
                Spacer(Modifier.width(8.dp))
                Text(
                    now.format(DateTimeFormatter.ofPattern("a", locale)),
                    style = Scenic.Date.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), style = Scenic.Date)
        Spacer(Modifier.height(14.dp))
        WeatherLine(weather, location)
    }
}

@Composable
private fun WeatherLine(weather: WeatherState, location: WeatherLocation) {
    AnimatedContent(
        targetState = weather,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
        contentKey = { state ->
            when (state) {
                is WeatherState.Ready -> "${state.weather.temperatureC}|${state.weather.code}|${state.weather.isDay}"
                else -> state::class.simpleName
            }
        },
        label = "weather",
    ) { state ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state) {
                    is WeatherState.Ready -> {
                        val w = state.weather
                        Icon(
                            w.condition.icon(w.isDay),
                            contentDescription = w.condition.label,
                            tint = Scenic.TextPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${w.temperatureC}°C", style = Scenic.Weather)
                        Spacer(Modifier.width(12.dp))
                        Text(w.condition.label, style = Scenic.WeatherDetail)
                        if (w.highC != null && w.lowC != null) {
                            Spacer(Modifier.width(12.dp))
                            Text("H ${w.highC}°  L ${w.lowC}°", style = Scenic.WeatherDetail)
                        }
                    }
                    WeatherState.Loading -> Text("Fetching weather…", style = Scenic.WeatherDetail)
                    is WeatherState.Unavailable -> Text("Weather unavailable", style = Scenic.WeatherDetail)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(location.displayName, style = Scenic.WeatherDetail.copy(fontSize = 16.sp))
        }
    }
}
