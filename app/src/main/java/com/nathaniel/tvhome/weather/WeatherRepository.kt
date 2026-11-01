package com.nathaniel.tvhome.weather

import android.util.Log
import com.nathaniel.tvhome.prefs.WeatherLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.min
import kotlin.math.roundToInt

data class CurrentWeather(
    val temperatureC: Int,
    val feelsLikeC: Int?,
    val highC: Int?,
    val lowC: Int?,
    val code: Int,
    val isDay: Boolean,
    val updatedAtMillis: Long,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(code)
}

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(val weather: CurrentWeather, val stale: Boolean = false) : WeatherState
    data class Unavailable(val reason: String) : WeatherState
}

/** Live weather from Open-Meteo (no API key required). */
class WeatherRepository(private val scope: CoroutineScope, locations: Flow<WeatherLocation>) {

    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state: StateFlow<WeatherState> = _state.asStateFlow()
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        scope.launch {
            locations.distinctUntilChanged().collectLatest { location ->
                _state.value = WeatherState.Loading
                var failures = 0
                while (true) {
                    val result = runCatching { fetch(location) }
                    result.onSuccess {
                        failures = 0
                        _state.value = WeatherState.Ready(it)
                    }.onFailure { error ->
                        failures++
                        Log.w(TAG, "Weather refresh failed", error)
                        _state.value = when (val current = _state.value) {
                            is WeatherState.Ready -> current.copy(stale = true)
                            else -> WeatherState.Unavailable("Weather unavailable")
                        }
                    }
                    val wait = if (failures == 0) REFRESH_MS else min(REFRESH_MS, RETRY_MS * (1L shl min(failures - 1, 4)))
                    withTimeoutOrNull(wait) { refreshRequests.first() }
                }
            }
        }
    }

    /** Requests an immediate refresh, e.g. when the launcher returns to the foreground. */
    fun refreshNow() {
        val current = _state.value
        val age = (current as? WeatherState.Ready)?.weather?.updatedAtMillis?.let { System.currentTimeMillis() - it }
        if (current !is WeatherState.Ready || current.stale || (age ?: Long.MAX_VALUE) > MIN_FOREGROUND_REFRESH_MS) {
            refreshRequests.tryEmit(Unit)
        }
    }

    private suspend fun fetch(location: WeatherLocation): CurrentWeather = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&forecast_days=1&timezone=auto"
        val json = JSONObject(httpGet(url))
        val current = json.getJSONObject("current")
        val daily = json.optJSONObject("daily")
        CurrentWeather(
            temperatureC = current.getDouble("temperature_2m").roundToInt(),
            feelsLikeC = current.optDouble("apparent_temperature").takeIf { !it.isNaN() }?.roundToInt(),
            highC = daily?.optJSONArray("temperature_2m_max")?.optDouble(0)?.takeIf { !it.isNaN() }?.roundToInt(),
            lowC = daily?.optJSONArray("temperature_2m_min")?.optDouble(0)?.takeIf { !it.isNaN() }?.roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    /** City search for the weather-location setting. */
    suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=en&format=json"))
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val region = listOf(r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() && it != "null" }.distinct().joinToString(", ")
            WeatherLocation(r.getString("name"), region, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "WeatherRepository"
        const val REFRESH_MS = 15 * 60_000L
        const val RETRY_MS = 30_000L
        const val MIN_FOREGROUND_REFRESH_MS = 5 * 60_000L
    }
}
