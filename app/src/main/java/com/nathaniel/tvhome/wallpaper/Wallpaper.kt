package com.nathaniel.tvhome.wallpaper

import androidx.compose.runtime.Immutable

@Immutable
data class Wallpaper(
    val id: String,
    val title: String,
    val credit: String,
    val source: Source,
) {
    sealed interface Source {
        /** Unsplash photograph, downloaded once and cached on the device. */
        data class Remote(val photoId: String) : Source {
            fun url(width: Int, height: Int, quality: Int = 85) =
                "https://images.unsplash.com/photo-$photoId?w=$width&h=$height&fit=crop&crop=entropy&q=$quality&fm=jpg"
        }

        /** Painted on-device, so a scenic home screen is always available offline. */
        data class Generated(val palette: ScenePainter.Palette) : Source
    }
}

object WallpaperCatalog {
    private fun photo(id: String, title: String) =
        Wallpaper("unsplash-$id", title, "Unsplash", Wallpaper.Source.Remote(id))

    private fun painted(palette: ScenePainter.Palette) =
        Wallpaper("painted-${palette.name.lowercase()}", palette.title, "Personal TV Home", Wallpaper.Source.Generated(palette))

    /** Nathaniel's chosen Unsplash photographs, selectable in Wallpapers. */
    val all: List<Wallpaper> = listOf(
        photo("1512386923336-1440f4afe1d9", "Street Silhouette"),
        photo("1585978075589-fc6561e20296", "City Traffic"),
        photo("1679089391720-dab2bca0ffc5", "Night Signal"),
        photo("1519692933481-e162a57d6721", "Rainy Street"),
        photo("1551050003-07fad65e9e47", "Subway Arrival"),
        photo("1611070674042-3957c8a7f700", "Denim Field"),
        photo("1604614765191-02ea6bdb941b", "Three in the Field"),
        photo("1667909547154-85b04eb47a8d", "Banners"),
    )

    val default: Wallpaper = all.first()

    /** Shown only when the chosen photo can't be downloaded (offline); not listed in the gallery. */
    val offlineFallback: Wallpaper = painted(ScenePainter.Palette.BlueHour)

    fun byId(id: String): Wallpaper = all.firstOrNull { it.id == id } ?: offlineFallback.takeIf { it.id == id } ?: default
}
