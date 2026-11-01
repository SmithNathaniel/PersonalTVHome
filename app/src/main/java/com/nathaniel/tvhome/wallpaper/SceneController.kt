package com.nathaniel.tvhome.wallpaper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.nathaniel.tvhome.prefs.BlurLevel
import com.nathaniel.tvhome.prefs.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** The wallpaper behind the interface plus its blurred copy for glass. */
@Immutable
data class Scene(
    val key: String,
    val image: ImageBitmap,
    val glass: ImageBitmap,
)

/** Prepares the chosen wallpaper (or a gallery preview) plus its blurred copy for glass surfaces. */
@OptIn(ExperimentalCoroutinesApi::class)
class SceneController(
    scope: CoroutineScope,
    private val wallpapers: WallpaperRepository,
    preferences: StateFlow<UserPreferences>,
) {
    /** Temporarily shows a wallpaper while browsing the gallery. */
    val previewWallpaperId = MutableStateFlow<String?>(null)

    private val _failedWallpaper = MutableStateFlow<String?>(null)
    /** Id of a wallpaper that could not be downloaded (offline), so the UI can say so. */
    val failedWallpaper: StateFlow<String?> = _failedWallpaper

    private data class WallpaperRequest(val id: String, val blur: BlurLevel)

    private val wallpaperScene: Flow<Scene> = combine(
        preferences.map { it.wallpaperId to it.blurLevel }.distinctUntilChanged(),
        previewWallpaperId,
    ) { (id, blur), preview -> WallpaperRequest(preview ?: id, blur) }
        .distinctUntilChanged()
        .transformLatest { request ->
            val wallpaper = if (request.id.isEmpty()) WallpaperCatalog.default else WallpaperCatalog.byId(request.id)
            val bitmap = wallpapers.loadFull(wallpaper)
            if (bitmap != null) {
                _failedWallpaper.value = null
                emit(scene(wallpaper.id, bitmap, request.blur))
            } else {
                _failedWallpaper.value = wallpaper.id
                val fallback = WallpaperCatalog.offlineFallback
                wallpapers.loadFull(fallback)?.let { emit(scene(fallback.id, it, request.blur)) }
            }
        }
        .flowOn(Dispatchers.Default)

    val scene: StateFlow<Scene?> = wallpaperScene.stateIn(scope, SharingStarted.Eagerly, null)

    private fun scene(id: String, bitmap: android.graphics.Bitmap, blur: BlurLevel) = Scene(
        key = "wall:$id",
        image = bitmap.asImageBitmap(),
        glass = Blur.blurred(bitmap, blur.radius).asImageBitmap(),
    )
}
