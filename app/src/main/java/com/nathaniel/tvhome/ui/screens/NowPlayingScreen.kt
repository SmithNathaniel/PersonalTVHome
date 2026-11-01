package com.nathaniel.tvhome.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.ui.unit.IntSize
import com.nathaniel.tvhome.lyrics.LyricsState
import com.nathaniel.tvhome.ui.home.LyricsPanel
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.MusicSources
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.EmptyState
import com.nathaniel.tvhome.ui.components.GlassButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.home.Artwork
import com.nathaniel.tvhome.ui.home.PlayerShadow
import com.nathaniel.tvhome.ui.home.Scrubber
import com.nathaniel.tvhome.ui.home.SourceBadge
import com.nathaniel.tvhome.ui.home.TrackTitle
import com.nathaniel.tvhome.ui.home.TransportControls
import com.nathaniel.tvhome.ui.home.rememberPosition
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val state by container.media.state.collectAsStateWithLifecycle()
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val active = state as? MediaState.Active

    // When music stops entirely, glide back to the scenic home screen.
    val hadMusic = remember { booleanArrayOf(false) }
    if (active != null) hadMusic[0] = true
    LaunchedEffect(active == null) {
        if (active == null && hadMusic[0]) {
            delay(4_000)
            navigator.back()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ReadabilityShade()
        if (active != null) {
            NowPlayingContent(active.nowPlaying)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassSurface(RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state is MediaState.PermissionRequired) {
                            EmptyState(Icons.Rounded.MusicNote, "Now Playing needs access", "Allow notification access so music from Spotify, YouTube Music and SoundCloud can appear here.")
                            GlassButton("Allow access", onClick = actions::enableNowPlaying)
                        } else {
                            EmptyState(Icons.Rounded.MusicNote, "Not Playing", "Start music in Spotify, YouTube Music or SoundCloud.")
                            val installed = remember(apps) {
                                MusicSources.launchable.filter { src -> apps.any { it.packageName == src.packageName } }
                            }
                            val focus = remember { FocusRequester() }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                installed.forEachIndexed { i, src ->
                                    GlassButton(
                                        src.name,
                                        onClick = { actions.launch(src.packageName) },
                                        modifier = if (i == 0) Modifier.focusRequester(focus) else Modifier,
                                    )
                                }
                            }
                            if (installed.isNotEmpty()) {
                                LaunchedEffect(Unit) { awaitFrame(); runCatching { focus.requestFocus() } }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Gentle shading over the scenic wallpaper so the large text stays readable; the photo stays visible. */
@Composable
internal fun ReadabilityShade() {
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.18f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startY = size.height * 0.6f))
    }
}

@Composable
internal fun NowPlayingContent(np: NowPlaying) {
    val container = LocalContainer.current
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle()
    val lyricsState by container.lyrics.state.collectAsStateWithLifecycle()
    val playFocus = remember { FocusRequester() }
    val position = rememberPosition(np)
    LaunchedEffect(Unit) { awaitFrame(); runCatching { playFocus.requestFocus() } }

    val lyrics = (lyricsState as? LyricsState.Ready)?.takeIf { it.trackKey == np.trackKey }?.lyrics
    val lyricsVisible = prefs.showLyrics && lyrics != null
    val titleSize by animateFloatAsState(if (lyricsVisible) 28f else 40f, tween(450, easing = FastOutSlowInEasing), label = "titleSize")
    val layoutSpec = tween<IntSize>(480, easing = FastOutSlowInEasing)

    Row(
        Modifier.fillMaxSize().padding(horizontal = 96.dp, vertical = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Large artwork folds away when lyrics take the stage.
        AnimatedVisibility(
            visible = !lyricsVisible,
            enter = fadeIn(tween(400, delayMillis = 120)) + expandHorizontally(layoutSpec, expandFrom = Alignment.Start),
            exit = fadeOut(tween(200)) + shrinkHorizontally(layoutSpec, shrinkTowards = Alignment.Start),
        ) {
            Row {
                AnimatedContent(
                    np.trackKey,
                    transitionSpec = { (fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith fadeOut(tween(250)) },
                    label = "npArt",
                ) { _ ->
                    val shape = RoundedCornerShape(18.dp)
                    Artwork(np, 300.dp, shape, Modifier.shadow(48.dp, shape, ambientColor = Color.Black, spotColor = Color.Black))
                }
                Spacer(Modifier.width(56.dp))
            }
        }

        Column(if (lyricsVisible) Modifier.width(340.dp) else Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(np, iconSize = 22.dp, textSize = 15.sp)
                Spacer(Modifier.weight(1f))
                LyricsStatus(prefs.showLyrics, lyricsState, np)
                Spacer(Modifier.width(10.dp))
                GlassButton(
                    text = null,
                    icon = Icons.Rounded.Lyrics,
                    onClick = { container.scope.launch { container.preferences.setShowLyrics(!prefs.showLyrics) } },
                    selected = prefs.showLyrics,
                    height = SMALL_BUTTON,
                )
                Spacer(Modifier.width(10.dp))
                GlassButton(
                    text = null,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { container.media.openSourceApp() },
                    height = SMALL_BUTTON,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A small cover sits beside the title while lyrics are showing.
                AnimatedVisibility(
                    visible = lyricsVisible,
                    enter = fadeIn(tween(400, delayMillis = 150)) + expandHorizontally(layoutSpec),
                    exit = fadeOut(tween(150)) + shrinkHorizontally(layoutSpec),
                ) {
                    Row {
                        val shape = RoundedCornerShape(12.dp)
                        Artwork(np, 84.dp, shape, Modifier.shadow(20.dp, shape))
                        Spacer(Modifier.width(16.dp))
                    }
                }
                AnimatedContent(np.trackKey, transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(200)) }, label = "npText") { _ ->
                    Column {
                        TrackTitle(
                            np.title,
                            Scenic.Title.copy(fontSize = titleSize.sp, lineHeight = (titleSize * 1.15f).sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, shadow = PlayerShadow),
                            focused = false,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            np.artist,
                            style = Scenic.Body.copy(fontSize = (titleSize * 0.6f).sp, fontWeight = FontWeight.Medium, color = Scenic.TextPrimary.copy(alpha = 0.72f), shadow = PlayerShadow),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!np.album.isNullOrBlank() && !lyricsVisible) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                np.album,
                                style = Scenic.Body.copy(fontSize = 18.sp, color = Scenic.TextPrimary.copy(alpha = 0.5f), shadow = PlayerShadow),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Scrubber(np, position, Modifier.fillMaxWidth(), barHeight = 6.dp, labelSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TransportControls(
                    np,
                    container.media::playPause,
                    container.media::skipNext,
                    container.media::skipPrevious,
                    playSize = 64.dp,
                    skipSize = 52.dp,
                    spacing = 10.dp,
                    expressive = true,
                    playModifier = Modifier.focusRequester(playFocus),
                )
            }
        }

        // Lyrics slide in from the right.
        AnimatedVisibility(
            visible = lyricsVisible,
            enter = fadeIn(tween(450, delayMillis = 150)) + expandHorizontally(layoutSpec, expandFrom = Alignment.End),
            exit = fadeOut(tween(200)) + shrinkHorizontally(layoutSpec, shrinkTowards = Alignment.End),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Row {
                Spacer(Modifier.width(56.dp))
                lyrics?.let { LyricsPanel(np, it, Modifier.fillMaxWidth().height(LYRICS_HEIGHT)) }
            }
        }
    }
}

/** Quiet status next to the lyrics button: searching, or nothing found for this song. */
@Composable
private fun LyricsStatus(enabled: Boolean, state: LyricsState, np: NowPlaying) {
    val text = when {
        !enabled -> null
        state is LyricsState.Loading && state.trackKey == np.trackKey -> "Finding lyrics…"
        state is LyricsState.Unavailable && state.trackKey == np.trackKey -> "No lyrics"
        else -> null
    }
    AnimatedContent(text, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "lyricsStatus") { t ->
        if (t != null) Text(t, style = Scenic.Caption.copy(fontSize = 13.sp, color = Scenic.TextPrimary.copy(alpha = 0.6f), shadow = PlayerShadow))
    }
}

private val SMALL_BUTTON = 40.dp
private val LYRICS_HEIGHT = 400.dp
