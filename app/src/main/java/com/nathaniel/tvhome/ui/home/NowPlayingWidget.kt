package com.nathaniel.tvhome.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.components.FocusSpring
import com.nathaniel.tvhome.ui.components.GlassButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.tvClickable
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.delay

/** Live playback position, extrapolated from the session's last report. */
@Composable
fun rememberPosition(np: NowPlaying): Long {
    val position by produceState(np.positionAt(SystemClock.elapsedRealtime()), np) {
        while (true) {
            value = np.positionAt(SystemClock.elapsedRealtime())
            if (!np.isPlaying) break
            // Tick on whole seconds of playback so the time label changes exactly once per second.
            delay((1000 - value % 1000).coerceIn(50, 1000))
        }
    }
    return position
}

/** Keeps text crisp on lighter, more transparent glass. */
private val TextShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 6f)

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun NowPlayingWidget(
    state: MediaState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onEnableAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.96f)) togetherWith fadeOut(tween(250)) },
        label = "widget",
        modifier = modifier,
    ) { s ->
        when (s) {
            is MediaState.Active -> ActiveWidget(s.nowPlaying, onOpen, onPlayPause, onNext, onPrevious)
            MediaState.Idle -> GlassButton(text = "Nothing playing", icon = Icons.Rounded.MusicNote, onClick = onOpen, height = 39.dp, contentPadding = 17.dp, textSize = 13.sp, iconSize = 19.dp)
            MediaState.PermissionRequired -> GlassButton(text = "Enable Now Playing", icon = Icons.Rounded.GraphicEq, onClick = onEnableAccess, height = 39.dp, contentPadding = 17.dp, textSize = 13.sp, iconSize = 19.dp)
        }
    }
}

@Composable
private fun ActiveWidget(
    np: NowPlaying,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val position = rememberPosition(np)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, FocusSpring, label = "widgetScale")
    GlassSurface(RoundedCornerShape(17.dp), modifier = Modifier.width(273.dp), tintBoost = -0.08f, elevation = 17.dp) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = if (focused) 0.14f else 0f))
                        .tvClickable(interaction, onClick = onOpen),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(np, 45.dp, RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        PlatformLabel(np.platform, np.isPlaying, scale = 0.94f)
                        Text(np.title, style = Scenic.Label.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 16.sp, shadow = TextShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(np.artist, style = Scenic.Caption.copy(fontSize = 10.sp, lineHeight = 13.sp, color = Scenic.TextPrimary.copy(alpha = 0.85f), shadow = TextShadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(position), style = Scenic.Tabular.copy(fontSize = 9.5.sp, color = Scenic.TextPrimary, shadow = TextShadow))
                Spacer(Modifier.width(7.dp))
                ProgressBar(
                    fraction = if (np.durationMs > 0) position / np.durationMs.toFloat() else 0f,
                    modifier = Modifier.weight(1f),
                    height = 3.dp,
                )
                Spacer(Modifier.width(7.dp))
                Text(if (np.durationMs > 0) formatDuration(np.durationMs) else "--:--", style = Scenic.Tabular.copy(fontSize = 9.5.sp, color = Scenic.TextPrimary, shadow = TextShadow))
                Spacer(Modifier.width(7.dp))
                MediaControls(np, onPlayPause, onNext, onPrevious, buttonSize = 25.dp, spacing = 2.dp)
            }
        }
    }
}

@Composable
fun PlatformLabel(platform: String, playing: Boolean, scale: Float = 1f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val pulse by animateFloatAsState(if (playing) 1f else 0.35f, tween(600), label = "dot")
        Box(Modifier.size(6.dp * scale).clip(CircleShape).background(Scenic.Accent.copy(alpha = pulse)))
        Spacer(Modifier.width(6.dp * scale))
        Text(platform.uppercase(), style = Scenic.Caption.copy(fontSize = 10.sp * scale, lineHeight = 13.sp * scale, letterSpacing = 1.2.sp * scale, color = Scenic.TextPrimary.copy(alpha = 0.8f), shadow = TextShadow))
    }
}

@Composable
fun Artwork(np: NowPlaying, size: Dp, shape: RoundedCornerShape, modifier: Modifier = Modifier) {
    val art = np.artwork
    if (art != null) {
        Image(art, contentDescription = "${np.title} artwork", contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(shape))
    } else {
        // No artwork published by the app: show a neutral placeholder rather than invented art.
        Box(
            modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(Color(0xFF39425A), Color(0xFF151A26)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Scenic.TextTertiary, modifier = Modifier.size(size * 0.4f))
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    // Steps once per second without easing: animating each tick kept the TV redrawing constantly.
    val animated = fraction.coerceIn(0f, 1f)
    Canvas(modifier.height(height).fillMaxWidth().graphicsLayer()) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.22f), cornerRadius = r)
        drawRoundRect(Color.White.copy(alpha = 0.92f), size = Size(size.width * animated, size.height), cornerRadius = r, topLeft = Offset.Zero)
    }
}

@Composable
fun MediaControls(
    np: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    buttonSize: Dp,
    spacing: Dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        if (np.canSkipPrevious) ControlButton(Icons.Rounded.SkipPrevious, "Previous", buttonSize * 0.86f, onPrevious)
        ControlButton(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (np.isPlaying) "Pause" else "Play", buttonSize, onPlayPause, emphasised = true)
        if (np.canSkipNext) ControlButton(Icons.Rounded.SkipNext, "Next", buttonSize * 0.86f, onNext)
    }
}

@Composable
private fun ControlButton(icon: ImageVector, description: String, size: Dp, onClick: () -> Unit, emphasised: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, FocusSpring, label = "ctl")
    val bg = when {
        focused -> Color.White.copy(alpha = 0.95f)
        emphasised -> Color.White.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bg)
            .tvClickable(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (focused) Scenic.Night else Scenic.TextPrimary, modifier = Modifier.size(size * 0.58f))
    }
}
