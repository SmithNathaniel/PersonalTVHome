package com.nathaniel.tvhome.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.GlassIconButton
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Screen
import com.nathaniel.tvhome.system.SystemSettings
import com.nathaniel.tvhome.ui.theme.GlassSpec
import kotlinx.coroutines.android.awaitFrame

@Composable
fun HomeScreen() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val weather by container.weather.state.collectAsStateWithLifecycle()
    val media by container.media.state.collectAsStateWithLifecycle()
    val christmasSeason by container.scenes.christmasSeason.collectAsStateWithLifecycle()

    val entries = remember(prefs.dockApps, apps) {
        prefs.dockApps.orEmpty().map { pkg -> DockEntry(pkg, apps.firstOrNull { it.packageName == pkg }) }
    }
    val requesters = remember(entries.size) { List(entries.size) { FocusRequester() } }
    val settingsRequester = remember { FocusRequester() }

    // Restore focus to the last dock icon whenever Home appears or the Home key is pressed.
    LaunchedEffect(navigator.homeFocusRequest, entries.size, navigator.appOptions) {
        if (navigator.settingsOpen || navigator.appOptions != null || entries.isEmpty()) return@LaunchedEffect
        awaitFrame()
        val index = navigator.homeFocusIndex.coerceIn(0, entries.lastIndex)
        runCatching { requesters[index].requestFocus() }
    }
    // Return focus to the Settings button after the side panel closes.
    val settingsWasOpen = remember { booleanArrayOf(false) }
    LaunchedEffect(navigator.settingsOpen) {
        if (navigator.settingsOpen) {
            settingsWasOpen[0] = true
        } else if (settingsWasOpen[0]) {
            settingsWasOpen[0] = false
            awaitFrame()
            runCatching { settingsRequester.requestFocus() }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth / 1280.dp, maxHeight / 720.dp).coerceIn(0.75f, 1.5f)
        val baseDensity = LocalDensity.current
        val scaledMaxWidth = maxWidth / scale
        val scaledMaxHeight = maxHeight / scale
        CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * scale, baseDensity.fontScale * scale)) {
            PersonalHeader(
                use24Hour = prefs.use24Hour,
                showSeconds = prefs.showSeconds,
                weather = weather,
                location = prefs.weatherLocation,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 56.dp, top = 40.dp)
                    .widthIn(max = (scaledMaxWidth - 56.dp - TOP_RIGHT_RESERVED).coerceAtLeast(240.dp)),
            )

            Row(
                Modifier.align(Alignment.TopEnd).padding(end = 44.dp, top = 40.dp),
                verticalAlignment = Alignment.Top,
            ) {
                    Box {
                        NowPlayingWidget(
                            state = media,
                            onOpen = { navigator.navigate(Screen.NowPlaying) },
                            onPlayPause = container.media::playPause,
                            onNext = container.media::skipNext,
                            onPrevious = container.media::skipPrevious,
                            onEnableAccess = actions::enableNowPlaying,
                        )
                        if (christmasSeason) ChristmasHat(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-9).dp).size(20.dp))
                    }
                Spacer(Modifier.width(12.dp))
                    Box {
                        GlassIconButton(
                            icon = Icons.Outlined.Wifi,
                            contentDescription = "Network settings",
                            onClick = { actions.openSystem(SystemSettings.Target.Network) },
                            size = 52.dp,
                            background = GlassSpec.StatusBackground,
                            borderAlpha = GlassSpec.StatusBorderAlpha,
                            shadowAlpha = GlassSpec.StatusShadowAlpha,
                        )
                        if (christmasSeason) ChristmasHat(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-7).dp).size(18.dp))
                    }
                Spacer(Modifier.width(12.dp))
                    Box {
                        GlassIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            onClick = { navigator.openSettings() },
                            modifier = Modifier.focusRequester(settingsRequester),
                            size = 52.dp,
                            background = GlassSpec.StatusBackground,
                            borderAlpha = GlassSpec.StatusBorderAlpha,
                            shadowAlpha = GlassSpec.StatusShadowAlpha,
                        )
                        if (christmasSeason) ChristmasHat(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-7).dp).size(18.dp))
                    }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = scaledMaxHeight * 0.75f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (entries.isNotEmpty()) {
                    AppDock(
                        entries = entries,
                        iconSize = prefs.dockSize.iconDp,
                        gap = prefs.dockSize.gapDp,
                        focusRequesters = requesters,
                        focusedIndex = navigator.homeFocusIndex,
                        onFocusIndex = { navigator.homeFocusIndex = it },
                        onOpen = { entry -> actions.launch(entry.packageName) },
                        onOptions = { entry -> navigator.appOptions = entry.packageName },
                        onAllApps = { navigator.navigate(Screen.Apps) },
                        festive = christmasSeason,
                    )
                }
            }
        }
    }
}

/** End padding + Settings button + gap + Wi-Fi button + gap + Now Playing widget + breathing room. */
private val TOP_RIGHT_RESERVED = 44.dp + 52.dp + 12.dp + 52.dp + 12.dp + 320.dp + 28.dp
