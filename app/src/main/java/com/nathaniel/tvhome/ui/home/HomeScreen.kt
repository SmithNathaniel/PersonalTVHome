package com.nathaniel.tvhome.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
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
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.GlassIconButton
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Screen
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

    val entries = remember(prefs.dockApps, apps) {
        prefs.dockApps.orEmpty().map { pkg -> DockEntry(pkg, apps.firstOrNull { it.packageName == pkg }) }
    }
    val requesters = remember(entries.size) { List(entries.size) { FocusRequester() } }
    val settingsRequester = remember { FocusRequester() }

    // Restore focus to the last dock icon whenever Home appears or the Home key is pressed.
    LaunchedEffect(navigator.homeFocusRequest, entries.size) {
        if (navigator.settingsOpen || entries.isEmpty()) return@LaunchedEffect
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
        PersonalHeader(
            use24Hour = prefs.use24Hour,
            showSeconds = prefs.showSeconds,
            weather = weather,
            location = prefs.weatherLocation,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 56.dp, top = 40.dp)
                // Never reach under the Now Playing widget and Settings button in the top-right.
                .widthIn(max = (maxWidth - 56.dp - TOP_RIGHT_RESERVED).coerceAtLeast(240.dp)),
        )

        Row(
            Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 40.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NowPlayingWidget(
                state = media,
                onOpen = { navigator.navigate(Screen.NowPlaying) },
                onPlayPause = container.media::playPause,
                onNext = container.media::skipNext,
                onPrevious = container.media::skipPrevious,
                onEnableAccess = actions::enableNowPlaying,
            )
            Spacer(Modifier.width(14.dp))
            GlassIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = { navigator.openSettings() },
                modifier = Modifier.focusRequester(settingsRequester),
                size = 46.dp,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = maxHeight * 0.72f),
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
                    onOpen = { actions.launch(it.packageName) },
                    onOptions = { navigator.appOptions = it.packageName },
                    onAllApps = { navigator.navigate(Screen.Apps) },
                )
            }
        }
    }
}

/** End padding + Settings button + gap + Now Playing widget + breathing room. */
private val TOP_RIGHT_RESERVED = 48.dp + 46.dp + 14.dp + 250.dp + 28.dp
