package com.nathaniel.tvhome.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.nathaniel.tvhome.prefs.WeatherLocation
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.weather.WeatherState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val OWNER_NAME = "Nathaniel Lucky Smith"

/** Time-of-day greeting, like "Good Evening". */
fun greetingFor(now: LocalDateTime): String = when (now.hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    else -> "Good Evening"
}

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
        Row {
            Text(greetingFor(now), style = Scenic.Welcome.copy(fontSize = 27.sp), modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(8.dp))
            Text(
                OWNER_NAME,
                style = Scenic.Name.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.96f),
                    shadow = androidx.compose.ui.graphics.Shadow(
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.22f),
                        androidx.compose.ui.geometry.Offset(0f, 1f),
                        5f,
                    ),
                ),
                modifier = Modifier.alignByBaseline().offset(y = 4.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.trimVertical(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(now.format(DateTimeFormatter.ofPattern(timePattern, locale)), style = Scenic.Clock)
            if (!use24Hour) {
                Spacer(Modifier.width(6.dp))
                Text(
                    now.format(DateTimeFormatter.ofPattern("a", locale)),
                    style = Scenic.Date.copy(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.width(18.dp))
            Box(Modifier.width(1.dp).height(28.dp).background(Scenic.TextPrimary.copy(alpha = 0.35f)))
            Spacer(Modifier.width(18.dp))
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                style = Scenic.Date.copy(fontSize = 26.sp, color = Scenic.TextPrimary.copy(alpha = 0.94f)),
            )
        }
        Spacer(Modifier.height(20.dp))
        WeatherWidget(weather, location)
    }
}

/** Removes the empty space a large font reserves above and below its digits, so lines sit tightly. */
private fun Modifier.trimVertical(top: Dp, bottom: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val t = top.roundToPx()
    val b = bottom.roundToPx()
    val height = (placeable.height - t - b).coerceAtLeast(0)
    layout(placeable.width, height) { placeable.place(0, -t) }
}
