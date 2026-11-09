package com.nathaniel.tvhome.prefs

data class WeatherLocation(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
) {
    val displayName: String get() = if (region.isBlank()) name else "$name, $region"

    companion object {
        val Johannesburg = WeatherLocation("Johannesburg", "South Africa", -26.2041, 28.0473)
    }
}

enum class GlassTint(val label: String) { Dark("Midnight"), Neutral("Graphite"), Warm("Amber"), Cool("Glacier") }

enum class GlassOpacity(val label: String, val tintAlpha: Float) {
    Clear("Clear", 0.16f), Balanced("Balanced", 0.28f), Frosted("Frosted", 0.42f)
}

enum class BlurLevel(val label: String, val radius: Int) { Soft("Soft", 4), Medium("Medium", 7), Strong("Strong", 11) }

enum class DockSize(val label: String, val iconDp: Int, val gapDp: Int) {
    Compact("Compact", 44, 23), Standard("Standard", 52, 17), Large("Large", 61, 20)
}

data class UserPreferences(
    val wallpaperId: String = "",
    val glassTint: GlassTint = GlassTint.Dark,
    val glassOpacity: GlassOpacity = GlassOpacity.Balanced,
    val blurLevel: BlurLevel = BlurLevel.Medium,
    val dockSize: DockSize = DockSize.Compact,
    /** Null until the dock has been initialised from installed apps on first run. */
    val dockApps: List<String>? = null,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val weatherLocation: WeatherLocation = WeatherLocation.Johannesburg,
    val autoNowPlaying: Boolean = true,
    /** Cycle through the wallpaper collection every 10 minutes. */
    val rotateWallpapers: Boolean = true,
    /** Show synced lyrics (from LRCLIB) in the Now Playing widget and player. */
    val showLyrics: Boolean = true,
)
