package com.nathaniel.tvhome.apps

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class AppCategory(val label: String) { Video("Watch"), Music("Listen"), Games("Play"), Other("Utilities") }

@Immutable
data class LauncherApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    /** Landscape TV banner published by the app, if any. */
    val banner: ImageBitmap?,
    val category: AppCategory,
)

/** The five personal favourites, each with known package aliases across TV builds. */
object FavouriteApps {
    data class Entry(val name: String, val packages: List<String>)

    val defaults = listOf(
        Entry("DStv Now", listOf("com.dstvmobile.android", "com.multichoice.dstvnow", "com.dstv.now")),
        Entry("Netflix", listOf("com.netflix.ninja", "com.netflix.mediaclient")),
        Entry("SoundCloud", listOf("com.soundcloud.android")),
        Entry("YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube.googletv", "com.google.android.youtube")),
        Entry("Rumble", listOf("com.rumble.battles")),
    )

    /** Returns a display name for packages that are pinned but no longer installed. */
    fun nameFor(packageName: String): String? = defaults.firstOrNull { packageName in it.packages }?.name
}
