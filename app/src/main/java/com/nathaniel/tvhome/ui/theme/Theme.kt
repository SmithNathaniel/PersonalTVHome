package com.nathaniel.tvhome.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.nathaniel.tvhome.R
import com.nathaniel.tvhome.prefs.GlassOpacity
import com.nathaniel.tvhome.prefs.GlassTint

/** Inter (SIL OFL 1.1, see third_party/inter): the closest open match to Apple's SF Pro. */
val InterText = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Inter Display: tighter spacing and finer detail for large sizes, like SF Pro Display. */
val InterDisplay = FontFamily(
    Font(R.font.inter_display_light, FontWeight.Light),
    Font(R.font.inter_display_regular, FontWeight.Normal),
    Font(R.font.inter_display_medium, FontWeight.Medium),
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    // Bold requests fall back to SemiBold so large text stays refined.
    Font(R.font.inter_display_semibold, FontWeight.Bold),
)

object Scenic {
    val TextPrimary = Color(0xFFF8F7F4)
    val TextSecondary = Color(0xFFF8F7F4).copy(alpha = 0.74f)
    val TextTertiary = Color(0xFFF8F7F4).copy(alpha = 0.52f)
    val Accent = Color(0xFFFFE3B0)
    val Night = Color(0xFF0B1220)

    private val softShadow = Shadow(Color.Black.copy(alpha = 0.32f), Offset(0f, 1.5f), 10f)
    private val text = InterText
    private val display = InterDisplay

    val Welcome = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 17.sp, letterSpacing = 0.1.sp, color = TextPrimary.copy(alpha = 0.72f), shadow = softShadow)
    val Name = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.4).sp, color = TextPrimary, shadow = softShadow)
    val Clock = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 84.sp, lineHeight = 88.sp, letterSpacing = (-3).sp, color = TextPrimary, fontFeatureSettings = "tnum, ss01", shadow = softShadow)
    val Date = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 19.sp, letterSpacing = (-0.1).sp, color = TextPrimary.copy(alpha = 0.82f), shadow = softShadow)
    val Body = TextStyle(fontFamily = text, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = (-0.1).sp, color = TextPrimary)
    val BodySecondary = Body.copy(color = TextSecondary)
    val Caption = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSecondary)
    val Label = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = (-0.1).sp, color = TextPrimary)
    val Title = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.2).sp, color = TextPrimary)
    val ScreenTitle = TextStyle(fontFamily = display, fontWeight = FontWeight.Medium, fontSize = 34.sp, letterSpacing = (-0.8).sp, color = TextPrimary, shadow = softShadow)
    val Section = TextStyle(fontFamily = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp, color = TextTertiary)
    val Tabular = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSecondary, fontFeatureSettings = "tnum")
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
