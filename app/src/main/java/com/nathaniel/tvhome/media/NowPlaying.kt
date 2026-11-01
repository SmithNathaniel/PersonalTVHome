package com.nathaniel.tvhome.media

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** Music apps whose sessions drive the Now Playing experience. */
object MusicSources {
    data class Launchable(val name: String, val packageName: String)

    /** Offered on the empty Now Playing screen, in this order, when installed. */
    val launchable = listOf(
        Launchable("Spotify", "com.spotify.tv.android"),
        Launchable("Spotify", "com.spotify.music"),
        Launchable("YouTube Music", "com.google.android.youtube.tvmusic"),
        Launchable("YouTube Music", "com.google.android.apps.youtube.music"),
        Launchable("SoundCloud", "com.soundcloud.android"),
    )

    private val names = mapOf(
        "com.google.android.youtube.tvmusic" to "YouTube Music",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.soundcloud.android" to "SoundCloud",
        "com.spotify.tv.android" to "Spotify",
        "com.spotify.music" to "Spotify",
        "com.apple.android.music" to "Apple Music",
        "deezer.android.tv" to "Deezer",
        "deezer.android.app" to "Deezer",
        "com.aspiro.tidal" to "TIDAL",
        "com.amazon.mp3" to "Amazon Music",
        "com.audiomack" to "Audiomack",
        // Music cast to the TV (e.g. Spotify Connect or YouTube Music via Chromecast built-in).
        "com.google.android.apps.mediashell" to "Google Cast",
    )

    fun isMusic(packageName: String) = packageName in names
    fun nameOf(packageName: String): String? = names[packageName]
}

@Immutable
data class NowPlaying(
    val packageName: String,
    val platform: String,
    val title: String,
    val artist: String,
    val album: String?,
    val artwork: ImageBitmap?,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    val isPlaying: Boolean,
    val canPlayPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
) {
    val trackKey: String get() = "$packageName|$title|$artist"

    /** Extrapolates the playback position from the last report, as Android recommends. */
    fun positionAt(elapsedRealtime: Long): Long {
        if (positionMs < 0) return 0
        val advanced = if (isPlaying) positionMs + ((elapsedRealtime - positionUpdatedAt) * speed).toLong() else positionMs
        return if (durationMs > 0) advanced.coerceIn(0, durationMs) else advanced.coerceAtLeast(0)
    }
}

sealed interface MediaState {
    /** Notification access has not been granted, so sessions cannot be read. */
    data object PermissionRequired : MediaState
    data object Idle : MediaState
    data class Active(val nowPlaying: NowPlaying) : MediaState
}
