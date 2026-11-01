package com.nathaniel.tvhome.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.LocalActions
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.EmptyState
import com.nathaniel.tvhome.ui.components.GlassButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.home.Artwork
import com.nathaniel.tvhome.ui.home.MediaControls
import com.nathaniel.tvhome.ui.home.PlatformLabel
import com.nathaniel.tvhome.ui.home.ProgressBar
import com.nathaniel.tvhome.ui.home.formatDuration
import com.nathaniel.tvhome.ui.home.rememberPosition
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun NowPlayingScreen() {
    val container = LocalContainer.current
    val navigator = LocalNavigator.current
    val actions = LocalActions.current
    val state by container.media.state.collectAsStateWithLifecycle()
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
        if (active != null) {
            ReadabilityShade()
            NowPlayingContent(active.nowPlaying)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassSurface(RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state is MediaState.PermissionRequired) {
                            EmptyState(Icons.Rounded.MusicNote, "Now Playing needs access", "Allow notification access so music from YouTube Music and SoundCloud can appear here.")
                            GlassButton("Allow access", onClick = actions::enableNowPlaying)
                        } else {
                            EmptyState(Icons.Rounded.MusicNote, "Nothing playing", "Start music in YouTube Music or SoundCloud.")
                            val focus = remember { FocusRequester() }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GlassButton("YouTube Music", onClick = { actions.launch("com.google.android.youtube.tvmusic") }, modifier = Modifier.focusRequester(focus))
                                GlassButton("SoundCloud", onClick = { actions.launch("com.soundcloud.android") })
                            }
                            LaunchedEffect(Unit) { awaitFrame(); runCatching { focus.requestFocus() } }
                        }
                    }
                }
            }
        }
    }
}

/** Gentle shading over the scenic wallpaper so the large text stays readable. */
@Composable
internal fun ReadabilityShade() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Black.copy(alpha = 0.18f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startY = size.height * 0.6f))
    }
}

@Composable
internal fun NowPlayingContent(np: NowPlaying) {
    val container = LocalContainer.current
    val playFocus = remember { FocusRequester() }
    val position = rememberPosition(np)
    LaunchedEffect(Unit) { awaitFrame(); runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        AmbientWaves(np.isPlaying, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(np.trackKey, transitionSpec = {
                (fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 12 }) togetherWith fadeOut(tween(300))
            }, label = "npArt") { _ ->
                Artwork(np, 300.dp, RoundedCornerShape(26.dp), Modifier.shadow(40.dp, RoundedCornerShape(26.dp)))
            }
            Spacer(Modifier.width(56.dp))
            Column(Modifier.weight(1f)) {
                PlatformLabel(np.platform, np.isPlaying)
                Spacer(Modifier.height(12.dp))
                AnimatedContent(np.trackKey, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(250)) }, label = "npText") { _ ->
                    Column {
                        Text(np.title, style = Scenic.ScreenTitle.copy(fontSize = 38.sp, fontWeight = FontWeight.Normal), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(np.artist, style = Scenic.Body.copy(fontSize = 20.sp, color = Scenic.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!np.album.isNullOrBlank()) {
                            Text(np.album, style = Scenic.BodySecondary.copy(color = Scenic.TextTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                ProgressBar(if (np.durationMs > 0) position / np.durationMs.toFloat() else 0f, height = 4.dp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(position), style = Scenic.Tabular)
                    Spacer(Modifier.weight(1f))
                    Text(if (np.durationMs > 0) formatDuration(np.durationMs) else "Live", style = Scenic.Tabular)
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.focusRequester(playFocus)) {
                        MediaControls(np, container.media::playPause, container.media::skipNext, container.media::skipPrevious, buttonSize = 56.dp, spacing = 14.dp)
                    }
                    Spacer(Modifier.width(24.dp))
                    GlassButton("Open ${np.platform}", icon = Icons.AutoMirrored.Rounded.OpenInNew, onClick = { container.media.openSourceApp() })
                }
            }
        }
    }
}

/** Gentle ambient light waves; purely decorative and paused when playback pauses. */
@Composable
private fun AmbientWaves(playing: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "waves")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val intensity by animateFloatAsState(if (playing) 1f else 0.25f, tween(1200), label = "intensity")
    Canvas(modifier) {
        val bars = 48
        val barW = size.width / bars
        for (i in 0 until bars) {
            val t = i / bars.toFloat()
            val wave = (sin((t * 3f + phase) * 2 * PI) * 0.5 + 0.5).toFloat()
            val wave2 = (sin((t * 7f - phase * 2) * 2 * PI) * 0.5 + 0.5).toFloat()
            val h = size.height * (0.12f + 0.55f * wave * wave2) * intensity
            drawRoundRect(
                Color.White.copy(alpha = 0.06f + 0.06f * wave),
                topLeft = Offset(i * barW + barW * 0.3f, size.height - h),
                size = Size(barW * 0.4f, h),
                cornerRadius = CornerRadius(barW * 0.2f),
            )
        }
    }
}
