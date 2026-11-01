package com.nathaniel.tvhome.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.prefs.WeatherLocation
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.theme.InterDisplay
import com.nathaniel.tvhome.ui.theme.InterText
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.weather.CurrentWeather
import com.nathaniel.tvhome.weather.HourlyForecast
import com.nathaniel.tvhome.weather.WeatherCondition
import com.nathaniel.tvhome.weather.WeatherState

private val Sun = Color(0xFFFFD60A)
private val Moon = Color(0xFFE6E9F2)
private val CloudWhite = Color(0xFFF4F6FA)
private val Rain = Color(0xFF64D2FF)
private val WidgetShadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, 1f), 6f)

/** Multicolour weather symbol in the spirit of Apple's SF Symbols. */
@Composable
fun WeatherGlyph(condition: WeatherCondition, isDay: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size)) {
        when (condition) {
            WeatherCondition.Clear ->
                if (isDay) Icon(Icons.Rounded.WbSunny, condition.label, tint = Sun, modifier = Modifier.size(size))
                else Icon(Icons.Rounded.Bedtime, condition.label, tint = Moon, modifier = Modifier.size(size))
            WeatherCondition.PartlyCloudy -> {
                // Sun or moon peeking out behind a cloud.
                Icon(
                    if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = if (isDay) Sun else Moon,
                    modifier = Modifier.size(size * 0.66f).align(Alignment.TopEnd),
                )
                Icon(Icons.Rounded.Cloud, condition.label, tint = CloudWhite, modifier = Modifier.size(size * 0.78f).align(Alignment.BottomStart))
            }
            WeatherCondition.Cloudy -> Icon(Icons.Rounded.Cloud, condition.label, tint = CloudWhite, modifier = Modifier.size(size))
            WeatherCondition.Fog -> Icon(Icons.Rounded.Dehaze, condition.label, tint = CloudWhite, modifier = Modifier.size(size))
            WeatherCondition.Drizzle, WeatherCondition.Rain, WeatherCondition.Showers -> {
                Icon(Icons.Rounded.Cloud, condition.label, tint = CloudWhite, modifier = Modifier.size(size * 0.82f).align(Alignment.TopCenter))
                Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(size * 0.04f)) {
                    val drops = if (condition == WeatherCondition.Drizzle) 2 else 3
                    repeat(drops) {
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Rain, modifier = Modifier.size(size * 0.26f))
                    }
                }
            }
            WeatherCondition.Snow -> Icon(Icons.Rounded.AcUnit, condition.label, tint = CloudWhite, modifier = Modifier.size(size))
            WeatherCondition.Thunderstorm -> Icon(Icons.Rounded.Thunderstorm, condition.label, tint = CloudWhite, modifier = Modifier.size(size))
        }
    }
}

/**
 * Apple Weather–style widget: place, large temperature, condition with today's high and low,
 * then an hourly strip.
 */
@Composable
fun WeatherWidget(weather: WeatherState, location: WeatherLocation, use24Hour: Boolean, modifier: Modifier = Modifier) {
    GlassSurface(RoundedCornerShape(17.dp), modifier = modifier.widthIn(min = 180.dp, max = WIDGET_WIDTH), tintBoost = -0.04f, elevation = 12.dp) {
        AnimatedContent(
            targetState = weather,
            contentKey = { state ->
                when (state) {
                    is WeatherState.Ready -> state.weather.let { "${it.temperatureC}|${it.code}|${it.isDay}|${it.hourly.firstOrNull()?.hour}|${state.stale}" }
                    else -> state::class.simpleName
                }
            },
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
            label = "weatherWidget",
        ) { state ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
                when (state) {
                    is WeatherState.Ready -> ReadyContent(state.weather, state.stale, location, use24Hour)
                    WeatherState.Loading -> Placeholder(location, "Updating…")
                    is WeatherState.Unavailable -> Placeholder(location, "Weather unavailable")
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(w: CurrentWeather, stale: Boolean, location: WeatherLocation, use24Hour: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            PlaceLabel(location)
            Text("${w.temperatureC}°", style = BigTemp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            WeatherGlyph(w.condition, w.isDay, size = 15.dp)
            Spacer(Modifier.height(3.dp))
            Text(if (stale) "${w.condition.label} · Offline" else w.condition.label, style = Detail.copy(fontWeight = FontWeight.SemiBold, color = Scenic.TextPrimary), maxLines = 1)
            if (w.highC != null && w.lowC != null) {
                Text("H:${w.highC}°  L:${w.lowC}°", style = Detail, maxLines = 1)
            }
        }
    }
    if (w.hourly.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        HourlyStrip(w.hourly.take(5), use24Hour)
    }
}

@Composable
private fun HourlyStrip(hours: List<HourlyForecast>, use24Hour: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        hours.forEachIndexed { i, h ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (i == 0) "Now" else hourLabel(h.hour, use24Hour), style = Small, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                WeatherGlyph(h.condition, h.isDay, size = 12.dp)
                Spacer(Modifier.height(3.dp))
                Text("${h.temperatureC}°", style = Small.copy(fontWeight = FontWeight.SemiBold, color = Scenic.TextPrimary), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Placeholder(location: WeatherLocation, message: String) {
    PlaceLabel(location)
    Text("—°", style = BigTemp)
    Text(message, style = Detail)
}

@Composable
private fun PlaceLabel(location: WeatherLocation) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            location.name,
            style = TextStyle(fontFamily = InterText, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Scenic.TextPrimary, shadow = WidgetShadow),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.NearMe, contentDescription = null, tint = Scenic.TextPrimary, modifier = Modifier.size(8.dp).offset(y = 0.5.dp))
    }
}

private fun hourLabel(hour: Int, use24Hour: Boolean): String =
    if (use24Hour) "%02d".format(hour)
    else when (hour) {
        0 -> "12AM"
        12 -> "12PM"
        in 1..11 -> "${hour}AM"
        else -> "${hour - 12}PM"
    }

private val WIDGET_WIDTH = 215.dp
private val BigTemp = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Light, fontSize = 29.sp, lineHeight = 33.sp, letterSpacing = (-1).sp, color = Scenic.TextPrimary, shadow = WidgetShadow)
private val Detail = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Medium, fontSize = 9.5.sp, lineHeight = 12.sp, color = Scenic.TextPrimary.copy(alpha = 0.78f), fontFeatureSettings = "tnum", shadow = WidgetShadow)
private val Small = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Medium, fontSize = 8.5.sp, lineHeight = 10.sp, color = Scenic.TextPrimary.copy(alpha = 0.78f), fontFeatureSettings = "tnum", shadow = WidgetShadow)
