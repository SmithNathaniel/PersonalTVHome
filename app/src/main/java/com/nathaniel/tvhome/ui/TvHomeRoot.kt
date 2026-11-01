package com.nathaniel.tvhome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nathaniel.tvhome.AppContainer
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.ui.components.BackdropState
import com.nathaniel.tvhome.ui.components.LocalBackdrop
import com.nathaniel.tvhome.ui.components.MessageHost
import com.nathaniel.tvhome.ui.components.ScenicBackground
import com.nathaniel.tvhome.ui.home.HomeScreen
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Navigator
import com.nathaniel.tvhome.ui.navigation.Screen
import com.nathaniel.tvhome.ui.screens.AppOptionsSheet
import com.nathaniel.tvhome.ui.screens.AppearanceScreen
import com.nathaniel.tvhome.ui.screens.AppsScreen
import com.nathaniel.tvhome.ui.screens.NowPlayingScreen
import com.nathaniel.tvhome.ui.screens.SettingsPanel
import com.nathaniel.tvhome.ui.screens.WallpapersScreen
import com.nathaniel.tvhome.ui.theme.GlassStyle
import com.nathaniel.tvhome.ui.theme.LocalGlassStyle
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvHomeRoot(container: AppContainer, navigator: Navigator) {
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val scene by container.scenes.scene.collectAsStateWithLifecycle()
    val media by container.media.state.collectAsStateWithLifecycle()
    val backdrop = remember { BackdropState() }
    LaunchedEffect(scene) { backdrop.show(scene) }

    CompositionLocalProvider(
        LocalContainer provides container,
        LocalNavigator provides navigator,
        LocalBackdrop provides backdrop,
        LocalGlassStyle provides GlassStyle.from(prefs.glassTint, prefs.glassOpacity),
    ) {
        val actions = rememberLauncherActions(container)
        CompositionLocalProvider(LocalActions provides actions) {
            // Back always stays inside the launcher; on Home it simply does nothing.
            BackHandler { navigator.back() }
            AutoNowPlaying(navigator, media, prefs.autoNowPlaying)

            val screenFocus = remember { FocusRequester() }
            val overlayOpen = navigator.settingsOpen || navigator.appOptions != null
            var overlayWasOpen by remember { mutableStateOf(false) }
            LaunchedEffect(overlayOpen) {
                if (overlayOpen) {
                    overlayWasOpen = true
                } else if (overlayWasOpen) {
                    overlayWasOpen = false
                    // Return focus to whatever was focused before the overlay appeared.
                    if (navigator.current != Screen.Home) {
                        awaitFrame()
                        runCatching { screenFocus.requestFocus() }
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                ScenicBackground(backdrop)

                val dim by animateFloatAsState(
                    when (navigator.current) {
                        Screen.Home, Screen.NowPlaying -> 0f
                        Screen.Wallpapers -> 0.12f
                        else -> 0.36f
                    },
                    tween(500),
                    label = "dim",
                )
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = dim }.background(Color.Black))

                AnimatedContent(
                    targetState = navigator.current,
                    transitionSpec = {
                        (fadeIn(tween(420, delayMillis = 60, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(480, easing = FastOutSlowInEasing), initialScale = 0.975f)) togetherWith
                            (fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 1.015f))
                    },
                    modifier = Modifier.focusRequester(screenFocus),
                    label = "screens",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen()
                        Screen.Apps -> AppsScreen()
                        Screen.Wallpapers -> WallpapersScreen()
                        Screen.Appearance -> AppearanceScreen()
                        Screen.NowPlaying -> NowPlayingScreen()
                    }
                }

                AnimatedVisibility(navigator.settingsOpen, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
                }
                AnimatedVisibility(
                    visible = navigator.settingsOpen,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp),
                    enter = slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(300)),
                    exit = slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(240)),
                ) {
                    SettingsPanel()
                }

                // Keep the last package so the sheet can animate out after it is dismissed.
                var lastOptions by remember { mutableStateOf<String?>(null) }
                navigator.appOptions?.let { lastOptions = it }
                AnimatedVisibility(
                    visible = navigator.appOptions != null,
                    enter = fadeIn(tween(220)) + scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.94f),
                    exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = 0.97f),
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)), contentAlignment = Alignment.Center) {
                        lastOptions?.let { AppOptionsSheet(it) }
                    }
                }

                MessageHost()
            }
        }
    }
}

/** Opens the full-screen Now Playing experience when music starts (if enabled in Appearance). */
@Composable
private fun AutoNowPlaying(navigator: Navigator, media: MediaState, enabled: Boolean) {
    val np = (media as? MediaState.Active)?.nowPlaying
    val playingPackage = np?.takeIf { it.isPlaying }?.packageName
    val previous = remember { arrayOf(playingPackage) }
    LaunchedEffect(playingPackage) {
        val startedNow = playingPackage != null && previous[0] == null
        previous[0] = playingPackage
        if (startedNow && enabled && navigator.current == Screen.Home && !navigator.settingsOpen && navigator.appOptions == null) {
            navigator.navigate(Screen.NowPlaying)
        }
    }
}
