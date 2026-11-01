package com.nathaniel.tvhome.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Loads full-screen wallpapers and thumbnails, caching downloads on disk for offline use. */
class WallpaperRepository(context: Context) {

    private val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
    private val metrics = context.resources.displayMetrics
    val screenWidth: Int = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1280)
    val screenHeight: Int = minOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(720)

    private val thumbs = LruCache<String, ImageBitmap>(32)
    private val fullCache = LruCache<String, Bitmap>(2)
    private val downloadLock = Mutex()

    /** Returns the wallpaper cropped exactly to the screen, or null if it is unavailable. */
    suspend fun loadFull(wallpaper: Wallpaper): Bitmap? = withContext(Dispatchers.IO) {
        fullCache.get(wallpaper.id)?.let { return@withContext it }
        val bitmap = when (val source = wallpaper.source) {
            is Wallpaper.Source.Generated -> ScenePainter.paint(source.palette, screenWidth, screenHeight)
            is Wallpaper.Source.Remote -> {
                val file = File(dir, "${wallpaper.id}.jpg")
                if (!file.exists()) download(source.url(screenWidth, screenHeight), file)
                decode(file, screenWidth)?.let { Blur.centerCrop(it, screenWidth, screenHeight) }
            }
        }
        bitmap?.also { fullCache.put(wallpaper.id, it) }
    }

    suspend fun loadThumbnail(wallpaper: Wallpaper): ImageBitmap? = withContext(Dispatchers.IO) {
        thumbs.get(wallpaper.id)?.let { return@withContext it }
        val bitmap = when (val source = wallpaper.source) {
            is Wallpaper.Source.Generated -> ScenePainter.paint(source.palette, THUMB_W, THUMB_H)
            is Wallpaper.Source.Remote -> {
                val full = File(dir, "${wallpaper.id}.jpg")
                val file = if (full.exists()) full else File(dir, "${wallpaper.id}_thumb.jpg").also {
                    if (!it.exists()) download(source.url(THUMB_W, THUMB_H, 80), it)
                }
                decode(file, THUMB_W)?.let { Blur.centerCrop(it, THUMB_W, THUMB_H) }
            }
        }
        bitmap?.asImageBitmap()?.also { thumbs.put(wallpaper.id, it) }
    }

    /** Downloads a photo to disk without decoding it, so a later switch is instant. */
    suspend fun prefetch(wallpaper: Wallpaper) {
        val source = wallpaper.source as? Wallpaper.Source.Remote ?: return
        val file = File(dir, "${wallpaper.id}.jpg")
        if (!file.exists()) download(source.url(screenWidth, screenHeight), file)
    }

    fun isCached(wallpaper: Wallpaper): Boolean =
        wallpaper.source is Wallpaper.Source.Generated || File(dir, "${wallpaper.id}.jpg").exists()

    private suspend fun download(url: String, target: File): Boolean = downloadLock.withLock {
        if (target.exists()) return true
        val tmp = File(target.parentFile, target.name + ".part")
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "PersonalTVHome/1.0 (Android TV)")
            }
            try {
                check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
                connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            } finally {
                connection.disconnect()
            }
            check(tmp.renameTo(target)) { "rename failed" }
            true
        }.onFailure {
            tmp.delete()
            Log.w(TAG, "Wallpaper download failed: $url", it)
        }.getOrDefault(false)
    }

    private fun decode(file: File, targetWidth: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) {
            file.delete()
            return null
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private companion object {
        const val TAG = "WallpaperRepository"
        const val THUMB_W = 480
        const val THUMB_H = 270
    }
}
