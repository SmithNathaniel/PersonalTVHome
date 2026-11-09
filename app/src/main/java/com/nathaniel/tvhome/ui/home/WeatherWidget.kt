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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.nathaniel.tvhome.ui.theme.InterText
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.weather.CurrentWeather
import com.nathaniel.tvhome.weather.WeatherCondition
import com.nathaniel.tvhome.weather.WeatherState

private val Sun = Color(0xFFFFD60A)
private val Moon = Color(0xFFE6E9F2)
private val CloudWhite = Color(0xFFF4F6FA)
private val Rain = Color(0xFF64D2FF)
private val WidgetShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 5f)

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

/** Apple Weather–style widget: place, large temperature, and the condition with today's high and low. */
@Composable
fun WeatherWidget(weather: WeatherState, location: WeatherLocation, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = weather,
        contentKey = { state ->
            when (state) {
                is WeatherState.Ready -> state.weather.let { "${it.temperatureC}|${it.code}|${it.isDay}|${it.highC}|${it.lowC}|${state.stale}" }
                else -> state::class.simpleName
            }
        },
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
        label = "weatherWidget",
        modifier = modifier.width(WIDGET_WIDTH).height(WIDGET_HEIGHT),
    ) { state ->
        when (state) {
            is WeatherState.Ready -> ReadyContent(state.weather, state.stale, location)
            WeatherState.Loading -> Placeholder(location, "Updating…")
            is WeatherState.Unavailable -> Placeholder(location, "Weather unavailable")
        }
    }
}

@Composable
private fun ReadyContent(w: CurrentWeather, stale: Boolean, location: WeatherLocation) {
    Column(Modifier.fillMaxWidth().height(WIDGET_HEIGHT)) {
        PlaceLabel(location)
        Row(Modifier.fillMaxWidth().weight(1f).offset(y = (-2).dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${w.temperatureC}°", style = BigTemp, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(8.dp))
            WeatherGlyph(w.condition, w.isDay, size = 31.dp)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    if (stale) "${w.condition.label} · Offline" else w.condition.label,
                    style = ConditionStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                if (w.highC != null && w.lowC != null) {
                    Text("H:${w.highC}°  L:${w.lowC}°", style = HighLowStyle, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun Placeholder(location: WeatherLocation, message: String) {
    Column(Modifier.fillMaxWidth().height(WIDGET_HEIGHT)) {
        PlaceLabel(location)
        Row(Modifier.fillMaxWidth().weight(1f).offset(y = (-2).dp), verticalAlignment = Alignment.CenterVertically) {
            Text("—°", style = BigTemp, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(8.dp))
            WeatherGlyph(WeatherCondition.Cloudy, isDay = true, size = 31.dp)
            Spacer(Modifier.width(6.dp))
            Text(message, style = ConditionStyle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlaceLabel(location: WeatherLocation) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            location.name,
            style = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 23.sp, color = Color.White, shadow = WidgetShadow),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.NearMe, contentDescription = null, tint = Scenic.TextPrimary, modifier = Modifier.size(10.dp).offset(y = 0.5.dp))
    }
}

private val WIDGET_WIDTH = 243.dp
private val WIDGET_HEIGHT = 76.dp
private val BigTemp = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Normal, fontSize = 46.sp, lineHeight = 49.sp, color = Color.White, shadow = WidgetShadow)
private val ConditionStyle = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 19.sp, color = Color.White, shadow = WidgetShadow)
private val HighLowStyle = TextStyle(fontFamily = InterText, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 16.sp, color = Color.White.copy(alpha = 0.95f), fontFeatureSettings = "tnum", shadow = WidgetShadow)
