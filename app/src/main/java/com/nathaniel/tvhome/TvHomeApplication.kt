package com.nathaniel.tvhome

import android.app.Application
import android.content.Context
import com.nathaniel.tvhome.apps.AppRepository
import com.nathaniel.tvhome.apps.FavouriteApps
import com.nathaniel.tvhome.lyrics.LyricsRepository
import com.nathaniel.tvhome.media.MediaRepository
import com.nathaniel.tvhome.prefs.PreferencesRepository
import com.nathaniel.tvhome.system.SystemSettings
import com.nathaniel.tvhome.wallpaper.SceneController
import com.nathaniel.tvhome.wallpaper.WallpaperRepository
import com.nathaniel.tvhome.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.nathaniel.tvhome.wallpaper.WallpaperCatalog

class TvHomeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container: one instance of each repository for the launcher's lifetime. */
class AppContainer(context: Context) {
    private companion object {
        const val WALLPAPER_INTERVAL_MS = 10 * 60_000L
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val preferences = PreferencesRepository(context, scope)
    val apps = AppRepository(context, scope)
    val media = MediaRepository(context, scope)
    val wallpapers = WallpaperRepository(context)
    val weather = WeatherRepository(scope, preferences.preferences.map { it.weatherLocation })
    val scenes = SceneController(scope, wallpapers, preferences.preferences)
    val systemSettings = SystemSettings(context)
    val lyrics = LyricsRepository(scope, media.state, preferences.preferences.map { it.showLyrics })

    init {
        initialiseDock()
        rotateWallpapers()
        media.refresh()
    }

    /**
     * Moves to the next wallpaper every 10 minutes. Choosing one manually restarts the timer,
     * and nothing changes while the gallery is previewing.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun rotateWallpapers() = scope.launch {
        combine(
            preferences.preferences.map { it.rotateWallpapers to it.wallpaperId }.distinctUntilChanged(),
            scenes.christmasSeason,
        ) { (enabled, wallpaperId), christmas -> Triple(enabled, wallpaperId, christmas) }
            .distinctUntilChanged()
            .collectLatest { (enabled, currentId, christmas) ->
                if (!enabled || christmas) return@collectLatest
                val all = WallpaperCatalog.all
                val index = all.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
                val next = all[(index + 1) % all.size]
                // Download the next photo ahead of time so the change is a smooth cross-fade.
                launch(Dispatchers.IO) { wallpapers.prefetch(next) }
                delay(WALLPAPER_INTERVAL_MS)
                scenes.previewWallpaperId.first { it == null }
                preferences.setWallpaper(next.id)
            }
    }

    /** On first run, fills the dock with the five personal favourites using whichever package variant is installed. */
    private fun initialiseDock() = scope.launch {
        apps.loaded.first { it }
        if (preferences.current().dockApps != null) return@launch
        val dock = FavouriteApps.defaults.map { entry ->
            entry.packages.firstOrNull(apps::isInstalled) ?: entry.packages.first()
        }
        preferences.setDockApps(dock)
    }
}
