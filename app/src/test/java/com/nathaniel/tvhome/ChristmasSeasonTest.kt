package com.nathaniel.tvhome

import com.nathaniel.tvhome.wallpaper.WallpaperCatalog
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChristmasSeasonTest {
    @Test
    fun christmasThemeRunsFromDecemberFirstThroughDecemberTwentyFifth() {
        assertFalse(WallpaperCatalog.isChristmasSeason(LocalDate.of(2026, 11, 30)))
        assertTrue(WallpaperCatalog.isChristmasSeason(LocalDate.of(2026, 12, 1)))
        assertTrue(WallpaperCatalog.isChristmasSeason(LocalDate.of(2026, 12, 25)))
        assertFalse(WallpaperCatalog.isChristmasSeason(LocalDate.of(2026, 12, 26)))
    }
}