package com.nathaniel.tvhome.wallpaper

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.Month

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
    private fun photo(id: String, title: String, credit: String = "Unsplash") =
        Wallpaper("unsplash-$id", title, credit, Wallpaper.Source.Remote(id))

    private fun painted(palette: ScenePainter.Palette) =
        Wallpaper("painted-${palette.name.lowercase()}", palette.title, "Personal TV Home", Wallpaper.Source.Generated(palette))

    val christmas: Wallpaper = photo(
        "1642335911245-238e13dcabf2",
        "Christmas Living Room",
        "Photo by Collab Media on Unsplash",
    )

    fun isChristmasSeason(date: LocalDate = LocalDate.now()): Boolean =
        date.month == Month.DECEMBER && date.dayOfMonth in 1..25

    /** Selected Unsplash photographs, selectable in Wallpapers. */
    val all: List<Wallpaper> = listOf(
        photo("1512386923336-1440f4afe1d9", "Street Silhouette"),
        photo("1585978075589-fc6561e20296", "City Traffic"),
        photo("1679089391720-dab2bca0ffc5", "Night Signal"),
        photo("1519692933481-e162a57d6721", "Rainy Street"),
        photo("1551050003-07fad65e9e47", "Subway Arrival"),
        photo("1611070674042-3957c8a7f700", "Denim Field"),
        photo("1604614765191-02ea6bdb941b", "Three in the Field"),
        photo("1667909547154-85b04eb47a8d", "Banners"),
        photo("1601402161866-cdf996ca8dd0", "Taipei at Night", "Photo by Lisanto on Unsplash"),
        photo("1514320291840-2e0a9bf2a9ae", "Music Room", "Photo by John Matychuk on Unsplash"),
        photo("1496638449072-3d7265888ddc", "Silhouette Singer", "Photo by Quilia on Unsplash"),
        photo("1614943002430-346af6a35f3d", "Franschhoek Road", "Photo by Josh McGregor on Unsplash"),
        photo("1655202904864-f551a782630e", "Johannesburg Street", "Photo by Voguish Trails Media on Unsplash"),
        photo("1504293476958-6d6d0257d014", "Street Dancer", "Photo by Keenan Constance on Unsplash"),
        photo("1535746532749-ab96788e61b4", "Green Hoodie", "Photo by Maria Remez on Unsplash"),
        photo("1595188126266-cd4e0335bf3f", "Umbrella in the Field", "Photo by Karabo Mdluli on Unsplash"),
        photo("1562181839-802c025c3738", "Crosswalk at Sunrise", "Photo by Rachel Martin on Unsplash"),
        photo("1700332878626-ad1ff5fba14b", "Bowling Night", "Photo by Thokozani Nkala on Unsplash"),
        photo("1767819657067-7ff0486884ac", "Cliffside Mountain View", "Photo by Marina Zvada on Unsplash"),
        photo("1529576590443-3b51f217368b", "DJ at the Decks", "Photo by Bongani Ngcobo on Unsplash"),
        photo("1544624281-e43c8255ec78", "Denim Cap", "Photo by Bongani Ngcobo on Unsplash"),
        photo("1595239094544-cd47f94236d7", "Color Run", "Photo by Nqobile Vundla on Unsplash"),
        photo("1523947060924-f1fb2a7bbdec", "Pink Alley", "Photo by Keenan Constance on Unsplash"),
        photo("1523946638354-61fa6b810524", "Camera in Smoke", "Photo by Keenan Constance on Unsplash"),
        photo("1599477652523-934493edcdce", "Text", "Photo by Keenan Constance on Unsplash"),
        christmas,
    )

    val default: Wallpaper = all.first()

    /** Shown only when the chosen photo can't be downloaded (offline); not listed in the gallery. */
    val offlineFallback: Wallpaper = painted(ScenePainter.Palette.BlueHour)

    fun byId(id: String): Wallpaper = all.firstOrNull { it.id == id } ?: offlineFallback.takeIf { it.id == id } ?: default
}
