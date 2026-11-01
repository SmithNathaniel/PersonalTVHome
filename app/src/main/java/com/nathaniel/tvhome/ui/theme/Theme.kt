package com.nathaniel.tvhome.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.nathaniel.tvhome.prefs.GlassOpacity
import com.nathaniel.tvhome.prefs.GlassTint

object Scenic {
    val TextPrimary = Color(0xFFF8F7F4)
    val TextSecondary = Color(0xFFF8F7F4).copy(alpha = 0.74f)
    val TextTertiary = Color(0xFFF8F7F4).copy(alpha = 0.52f)
    val Accent = Color(0xFFFFE3B0)
    val Night = Color(0xFF0B1220)

    private val softShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 12f)
    private val family = FontFamily.SansSerif

    val Welcome = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 1.sp, color = TextSecondary, shadow = softShadow)
    val Name = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.3.sp, color = TextPrimary, shadow = softShadow)
    val Clock = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 80.sp, letterSpacing = (-1.5).sp, color = TextPrimary, fontFeatureSettings = "tnum", shadow = softShadow)
    val Date = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 21.sp, color = TextSecondary, shadow = softShadow)
    val Body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextPrimary)
    val BodySecondary = Body.copy(color = TextSecondary)
    /** Weather line in the personal header. */
    val Weather = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 21.sp, color = TextPrimary, fontFeatureSettings = "tnum", shadow = softShadow)
    val WeatherDetail = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 18.sp, color = TextSecondary, fontFeatureSettings = "tnum", shadow = softShadow)
    val Caption = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.3.sp, color = TextSecondary)
    val Label = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
    val Title = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
    val ScreenTitle = TextStyle(fontFamily = family, fontWeight = FontWeight.Light, fontSize = 34.sp, letterSpacing = (-0.5).sp, color = TextPrimary, shadow = softShadow)
    val Section = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 1.2.sp, color = TextTertiary)
    val Tabular = TextStyle(fontFamily = family, fontSize = 13.sp, color = TextSecondary, fontFeatureSettings = "tnum")
}

/** Appearance of every glass surface, driven by the Appearance settings. */
@Immutable
data class GlassStyle(val tint: Color, val tintAlpha: Float) {
    companion object {
        fun from(tint: GlassTint, opacity: GlassOpacity) = GlassStyle(
            tint = when (tint) {
                GlassTint.Dark -> Color(0xFF0A0E16)
                GlassTint.Neutral -> Color(0xFF2A2D33)
                GlassTint.Warm -> Color(0xFF2E2117)
                GlassTint.Cool -> Color(0xFF0E2236)
            },
            tintAlpha = opacity.tintAlpha,
        )
    }
}

val LocalGlassStyle = staticCompositionLocalOf { GlassStyle.from(GlassTint.Dark, GlassOpacity.Balanced) }

@Composable
fun PersonalTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Scenic.TextPrimary,
            onPrimary = Scenic.Night,
            surface = Color(0xFF151A24),
            onSurface = Scenic.TextPrimary,
            background = Scenic.Night,
            onBackground = Scenic.TextPrimary,
        ),
        content = content,
    )
}
