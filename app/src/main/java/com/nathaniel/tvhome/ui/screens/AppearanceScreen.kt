package com.nathaniel.tvhome.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.prefs.BlurLevel
import com.nathaniel.tvhome.prefs.DockSize
import com.nathaniel.tvhome.prefs.GlassOpacity
import com.nathaniel.tvhome.prefs.GlassTint
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.AppIcon
import com.nathaniel.tvhome.ui.components.GlassRow
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.OptionRow
import com.nathaniel.tvhome.ui.components.ScreenScaffold
import com.nathaniel.tvhome.ui.components.SectionTitle
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Screen
import com.nathaniel.tvhome.ui.theme.Scenic
import com.nathaniel.tvhome.wallpaper.WallpaperCatalog
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val p = container.preferences
    fun edit(block: suspend () -> Unit) { container.scope.launch { block() } }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { awaitFrame(); runCatching { first.requestFocus() } }

    ScreenScaffold(title = "Appearance", subtitle = "Changes apply instantly  ·  ◀ ▶ to adjust") {
        Row {
            LazyColumn(Modifier.width(470.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    GlassRow(
                        "Wallpaper",
                        onClick = { navigator.navigate(Screen.Wallpapers) },
                        icon = Icons.Rounded.Wallpaper,
                        value = WallpaperCatalog.byId(prefs.wallpaperId).title,
                        trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        modifier = Modifier.focusRequester(first),
                    )
                }
                item { OptionRow("Change wallpaper", listOf(true, false), prefs.rotateWallpapers, { if (it) "Every 10 minutes" else "Off" }, { v -> edit { p.setRotateWallpapers(v) } }, Icons.Rounded.Autorenew) }
                item { OptionRow("Glass tint", GlassTint.entries, prefs.glassTint, { it.label }, { v -> edit { p.setGlassTint(v) } }, Icons.Rounded.Palette) }
                item { OptionRow("Glass opacity", GlassOpacity.entries, prefs.glassOpacity, { it.label }, { v -> edit { p.setGlassOpacity(v) } }, Icons.Rounded.Opacity) }
                item { OptionRow("Backdrop blur", BlurLevel.entries, prefs.blurLevel, { it.label }, { v -> edit { p.setBlurLevel(v) } }, Icons.Rounded.BlurOn) }
                item { OptionRow("Dock size", DockSize.entries, prefs.dockSize, { it.label }, { v -> edit { p.setDockSize(v) } }, Icons.Rounded.SpaceDashboard) }
                item { OptionRow("Lyrics", listOf(true, false), prefs.showLyrics, { if (it) "On" else "Off" }, { v -> edit { p.setShowLyrics(v) } }, Icons.Rounded.Lyrics) }
                item { OptionRow("Full-screen Now Playing", listOf(true, false), prefs.autoNowPlaying, { if (it) "When music starts" else "Only when opened" }, { v -> edit { p.setAutoNowPlaying(v) } }, Icons.Rounded.Fullscreen) }
            }
            Spacer(Modifier.width(48.dp))
            Column(Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SectionTitle("Preview")
                // A live miniature of the dock using the current glass settings.
                GlassSurface(RoundedCornerShape((prefs.dockSize.iconDp * 0.46f).dp), elevation = 22.dp) {
                    Row(
                        Modifier.padding(horizontal = (prefs.dockSize.gapDp + 4).dp, vertical = (prefs.dockSize.iconDp * 0.27f).dp),
                        horizontalArrangement = Arrangement.spacedBy(prefs.dockSize.gapDp.dp),
                    ) {
                        prefs.dockApps.orEmpty().take(5).forEach { pkg ->
                            val app = apps.firstOrNull { it.packageName == pkg }
                            AppIcon(app, app?.label ?: pkg, prefs.dockSize.iconDp.dp)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                GlassSurface(RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                        Text("Scenic Glass", style = Scenic.Title)
                        Text("${prefs.glassTint.label} · ${prefs.glassOpacity.label} · ${prefs.blurLevel.label} blur", style = Scenic.BodySecondary)
                    }
                }
            }
        }
    }
}
