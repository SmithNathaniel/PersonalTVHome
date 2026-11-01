package com.nathaniel.tvhome.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "personal_tv_home")

/** Persists every user-facing preference with Jetpack DataStore. */
class PreferencesRepository(context: Context, scope: CoroutineScope) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val wallpaper = stringPreferencesKey("wallpaper")
        val glassTint = stringPreferencesKey("glass_tint")
        val glassOpacity = stringPreferencesKey("glass_opacity")
        val blurLevel = stringPreferencesKey("blur_level")
        val dockSize = stringPreferencesKey("dock_size")
        val dockApps = stringPreferencesKey("dock_apps")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val weatherName = stringPreferencesKey("weather_name")
        val weatherRegion = stringPreferencesKey("weather_region")
        val weatherLat = doublePreferencesKey("weather_lat")
        val weatherLon = doublePreferencesKey("weather_lon")
        val autoNowPlaying = booleanPreferencesKey("auto_now_playing")
        val rotateWallpapers = booleanPreferencesKey("rotate_wallpapers")
        val showLyrics = booleanPreferencesKey("show_lyrics")
    }

    val preferences: StateFlow<UserPreferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::toModel)
        .stateIn(scope, SharingStarted.Eagerly, UserPreferences())

    /** Reads the persisted values directly, bypassing the StateFlow's initial placeholder. */
    suspend fun current(): UserPreferences = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::toModel)
        .first()

    private fun toModel(p: Preferences): UserPreferences {
        val defaults = UserPreferences()
        val lat = p[Keys.weatherLat]
        val lon = p[Keys.weatherLon]
        val location = if (lat != null && lon != null) {
            WeatherLocation(p[Keys.weatherName].orEmpty(), p[Keys.weatherRegion].orEmpty(), lat, lon)
        } else defaults.weatherLocation
        return UserPreferences(
            wallpaperId = p[Keys.wallpaper] ?: defaults.wallpaperId,
            glassTint = enumOr(p[Keys.glassTint], defaults.glassTint),
            glassOpacity = enumOr(p[Keys.glassOpacity], defaults.glassOpacity),
            blurLevel = enumOr(p[Keys.blurLevel], defaults.blurLevel),
            dockSize = enumOr(p[Keys.dockSize], defaults.dockSize),
            dockApps = p[Keys.dockApps]?.let(::decodeList),
            use24Hour = p[Keys.use24Hour] ?: defaults.use24Hour,
            showSeconds = p[Keys.showSeconds] ?: defaults.showSeconds,
            weatherLocation = location,
            autoNowPlaying = p[Keys.autoNowPlaying] ?: defaults.autoNowPlaying,
            rotateWallpapers = p[Keys.rotateWallpapers] ?: defaults.rotateWallpapers,
            showLyrics = p[Keys.showLyrics] ?: defaults.showLyrics,
        )
    }

    suspend fun setWallpaper(id: String) = store.edit { it[Keys.wallpaper] = id }
    suspend fun setGlassTint(v: GlassTint) = store.edit { it[Keys.glassTint] = v.name }
    suspend fun setGlassOpacity(v: GlassOpacity) = store.edit { it[Keys.glassOpacity] = v.name }
    suspend fun setBlurLevel(v: BlurLevel) = store.edit { it[Keys.blurLevel] = v.name }
    suspend fun setDockSize(v: DockSize) = store.edit { it[Keys.dockSize] = v.name }
    suspend fun setDockApps(packages: List<String>) = store.edit { it[Keys.dockApps] = encodeList(packages.distinct()) }
    suspend fun setUse24Hour(v: Boolean) = store.edit { it[Keys.use24Hour] = v }
    suspend fun setShowSeconds(v: Boolean) = store.edit { it[Keys.showSeconds] = v }
    suspend fun setAutoNowPlaying(v: Boolean) = store.edit { it[Keys.autoNowPlaying] = v }
    suspend fun setRotateWallpapers(v: Boolean) = store.edit { it[Keys.rotateWallpapers] = v }
    suspend fun setShowLyrics(v: Boolean) = store.edit { it[Keys.showLyrics] = v }

    suspend fun setWeatherLocation(location: WeatherLocation) = store.edit {
        it[Keys.weatherName] = location.name
        it[Keys.weatherRegion] = location.region
        it[Keys.weatherLat] = location.latitude
        it[Keys.weatherLon] = location.longitude
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: fallback

    private fun encodeList(values: List<String>) = values.joinToString(SEPARATOR)
    private fun decodeList(raw: String) = if (raw.isEmpty()) emptyList() else raw.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
