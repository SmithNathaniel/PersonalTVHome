package com.nathaniel.tvhome.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * Reads real playback metadata from other apps' MediaSessions. Requires notification-listener
 * access; without it the state is [MediaState.PermissionRequired].
 */
class MediaRepository(private val context: Context, private val scope: CoroutineScope) {

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MediaState>(MediaState.Idle)
    val state: StateFlow<MediaState> = _state.asStateFlow()

    private var sessionsListenerRegistered = false
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private var controllers: List<MediaController> = emptyList()
    private var activeController: MediaController? = null

    /** Artwork is loaded once per track and then reused, so metadata refreshes never make it blink. */
    private data class Art(val trackKey: String, val artwork: ImageBitmap?)
    private var art: Art? = null
    private var artJob: Job? = null
    private var artLoadingFor: String? = null

    /** Short gaps between tracks shouldn't make the widget disappear and reappear. */
    private val goIdle = Runnable { _state.value = MediaState.Idle }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        updateControllers(list.orEmpty())
    }

    val hasPermission: Boolean
        get() = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** (Re)connects to media sessions; safe to call repeatedly, e.g. on every resume. */
    fun refresh() {
        mainHandler.post {
            if (!hasPermission) {
                unregisterAll()
                _state.value = MediaState.PermissionRequired
                return@post
            }
            try {
                if (!sessionsListenerRegistered) {
                    sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
                    sessionsListenerRegistered = true
                }
                updateControllers(sessionManager.getActiveSessions(listenerComponent))
            } catch (e: SecurityException) {
                Log.w(TAG, "Media session access denied", e)
                unregisterAll()
                _state.value = MediaState.PermissionRequired
            }
        }
    }

    fun playPause() {
        val c = activeController ?: return
        val playing = c.playbackState?.state?.let(::isPlayingState) == true
        if (playing) c.transportControls.pause() else c.transportControls.play()
    }

    fun skipNext() { activeController?.transportControls?.skipToNext() }
    fun skipPrevious() { activeController?.transportControls?.skipToPrevious() }

    /** Opens the music app that owns the current session. */
    fun openSourceApp(): Boolean {
        val c = activeController ?: return false
        c.sessionActivity?.let { pending ->
            if (runCatching { pending.send() }.isSuccess) return true
        }
        val intent = context.packageManager.getLeanbackLaunchIntentForPackage(c.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(c.packageName)
            ?: return false
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }

    private fun updateControllers(list: List<MediaController>) {
        val music = list.filter { MusicSources.isMusic(it.packageName) }
        callbacks.keys.filter { old -> music.none { it.sessionToken == old.sessionToken } }.forEach { old ->
            callbacks.remove(old)?.let(old::unregisterCallback)
        }
        music.forEach { controller ->
            if (callbacks.keys.none { it.sessionToken == controller.sessionToken }) {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = recompute()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = recompute()
                    override fun onSessionDestroyed() = refresh()
                }
                controller.registerCallback(callback, mainHandler)
                callbacks[controller] = callback
            }
        }
        controllers = music
        recompute()
    }

    private fun recompute() {
        val withMetadata = controllers.filter { !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() }
        val chosen = withMetadata.firstOrNull { isPlayingState(it.playbackState?.state) }
            ?: withMetadata.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
        activeController = chosen
        if (chosen == null) {
            if (_state.value is MediaState.Active) {
                mainHandler.removeCallbacks(goIdle)
                mainHandler.postDelayed(goIdle, IDLE_GRACE_MS)
            } else {
                _state.value = MediaState.Idle
            }
            return
        }
        mainHandler.removeCallbacks(goIdle)
        val metadata = chosen.metadata!!
        val playback = chosen.playbackState
        val actions = playback?.actions ?: 0L
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val trackKey = "${chosen.packageName}|$title|$artist"
        if (art?.trackKey != trackKey) art = Art(trackKey, null)
        val currentArt = art?.artwork
        if (currentArt == null && artLoadingFor != trackKey) loadArtwork(trackKey, metadata)

        val next = NowPlaying(
                packageName = chosen.packageName,
                platform = MusicSources.nameOf(chosen.packageName) ?: chosen.packageName,
                title = title,
                artist = artist,
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
                artwork = currentArt,
                durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
                positionMs = playback?.position ?: 0L,
                positionUpdatedAt = playback?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
                speed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
                isPlaying = isPlayingState(playback?.state),
                canPlayPause = actions and (PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE) != 0L,
                canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
                canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
        )
        val previous = (_state.value as? MediaState.Active)?.nowPlaying
        if (previous != null && isSameDisplay(previous, next)) return
        _state.value = MediaState.Active(next)
    }

    /**
     * Some apps (e.g. SoundCloud) re-report playback every second. If only the position moved,
     * and it matches where we already extrapolated it to be, keep the existing state untouched.
     */
    private fun isSameDisplay(a: NowPlaying, b: NowPlaying): Boolean {
        val now = SystemClock.elapsedRealtime()
        return a.copy(positionMs = 0, positionUpdatedAt = 0) == b.copy(positionMs = 0, positionUpdatedAt = 0) &&
            kotlin.math.abs(a.positionAt(now) - b.positionAt(now)) < POSITION_TOLERANCE_MS
    }

    private fun artBitmap(metadata: MediaMetadata): Bitmap? =
        metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    private fun artUri(metadata: MediaMetadata): String? = listOf(
        MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
        MediaMetadata.METADATA_KEY_ART_URI,
        MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
    ).firstNotNullOfOrNull { metadata.getString(it)?.takeIf(String::isNotBlank) }

    private fun loadArtwork(trackKey: String, metadata: MediaMetadata) {
        val bitmap = artBitmap(metadata)
        val uri = artUri(metadata)
        if (bitmap == null && uri == null) return
        artJob?.cancel()
        artLoadingFor = trackKey
        artJob = scope.launch {
            val artwork = withContext(Dispatchers.Default) {
                runCatching {
                    val source = bitmap ?: withContext(Dispatchers.IO) { loadUri(uri!!) } ?: return@runCatching null
                    val longest = max(source.width, source.height)
                    val scaled = if (longest > 640) {
                        val s = 640f / longest
                        Bitmap.createScaledBitmap(source, (source.width * s).toInt(), (source.height * s).toInt(), true)
                    } else source
                    scaled.asImageBitmap()
                }.onFailure { Log.w(TAG, "Artwork processing failed", it) }.getOrNull()
            }
            if (artLoadingFor == trackKey) artLoadingFor = null
            if (artwork != null && art?.trackKey == trackKey) {
                art = Art(trackKey, artwork)
                recompute()
            }
        }
    }

    private fun loadUri(raw: String): Bitmap? {
        val uri = Uri.parse(raw)
        return when (uri.scheme) {
            "http", "https" -> {
                val connection = (URL(raw).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                try {
                    connection.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    connection.disconnect()
                }
            }
            else -> context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }

    private fun unregisterAll() {
        mainHandler.removeCallbacks(goIdle)
        if (sessionsListenerRegistered) {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
            sessionsListenerRegistered = false
        }
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks.clear()
        controllers = emptyList()
        activeController = null
    }

    private fun isPlayingState(state: Int?): Boolean = when (state) {
        PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING, PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT, PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> true
        else -> false
    }

    private companion object {
        const val TAG = "MediaRepository"
        const val IDLE_GRACE_MS = 3_000L
        const val POSITION_TOLERANCE_MS = 1_500L
    }
}
