package com.nathaniel.tvhome.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.lyrics.Lyrics
import com.nathaniel.tvhome.lyrics.LyricsState
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.FocusSpring
import com.nathaniel.tvhome.ui.components.GlassButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.tvClickable
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.delay

/** Live playback position, extrapolated from the session's last report; ticks on whole seconds. */
@Composable
fun rememberPosition(np: NowPlaying): Long {
    val position by produceState(np.positionAt(SystemClock.elapsedRealtime()), np) {
        while (true) {
            value = np.positionAt(SystemClock.elapsedRealtime())
            if (!np.isPlaying) break
            delay((1000 - value % 1000).coerceIn(50, 1000))
        }
    }
    return position
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Apple-style remaining time, e.g. "−2:41". */
fun formatRemaining(position: Long, duration: Long): String =
    if (duration <= 0) "Live" else "\u2212" + formatDuration((duration - position).coerceAtLeast(0))

/** Keeps text crisp on light, transparent glass. */
internal val PlayerShadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 6f)

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
            MediaState.Idle -> GlassButton(text = "Not Playing", icon = Icons.Rounded.MusicNote, onClick = onOpen, height = 39.dp, contentPadding = 17.dp, textSize = 13.sp, iconSize = 19.dp)
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
    val highlight by animateColorAsState(if (focused) Color.White.copy(alpha = 0.16f) else Color.Transparent, tween(180), label = "rowFocus")

    GlassSurface(RoundedCornerShape(17.dp), modifier = Modifier.width(236.dp), tintBoost = -0.06f, elevation = 14.dp) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(highlight)
                    .tvClickable(interaction, onClick = onOpen)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(np, 46.dp, RoundedCornerShape(10.dp), Modifier.shadow(8.dp, RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    TrackTitle(np.title, Scenic.Label.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, shadow = PlayerShadow), focused)
                    Text(
                        np.artist,
                        style = Scenic.Caption.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Scenic.TextPrimary.copy(alpha = 0.72f), shadow = PlayerShadow),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    SourceBadge(np, iconSize = 12.dp, textSize = 9.sp)
                }
            }
            WidgetLyrics(np)
            Spacer(Modifier.height(5.dp))
            Scrubber(np, position, Modifier.padding(horizontal = 3.dp), barHeight = 3.dp, labelSize = 9.sp)
            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TransportControls(np, onPlayPause, onNext, onPrevious, playSize = 30.dp, skipSize = 26.dp, spacing = 14.dp)
            }
        }
    }
}

/** The current synced lyric line, shown only when lyrics are on and available for this song. */
@Composable
private fun WidgetLyrics(np: NowPlaying) {
    val container = LocalContainer.current
    val state by container.lyrics.state.collectAsStateWithLifecycle()
    val synced = (state as? LyricsState.Ready)?.takeIf { it.trackKey == np.trackKey }?.lyrics as? Lyrics.Synced
    AnimatedVisibility(
        visible = synced != null,
        enter = fadeIn(tween(300)) + expandVertically(tween(320)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(260)),
    ) {
        if (synced != null) {
            WidgetLyricLine(np, synced, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, top = 5.dp))
        }
    }
}

/** Title that scrolls tvOS-style only while focused and too long to fit; otherwise ellipsised. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackTitle(title: String, style: TextStyle, focused: Boolean, maxLines: Int = 1) {
    Text(
        title,
        style = style,
        maxLines = maxLines,
        overflow = if (focused && maxLines == 1) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (focused && maxLines == 1) Modifier.basicMarquee(initialDelayMillis = 1200) else Modifier,
    )
}

/** The music app's own icon and name, e.g. Spotify, SoundCloud or YouTube Music. */
@Composable
internal fun SourceBadge(np: NowPlaying, iconSize: Dp, textSize: TextUnit) {
    val container = LocalContainer.current
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val icon = remember(apps, np.packageName) { apps.firstOrNull { it.packageName == np.packageName }?.icon }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Image(icon, contentDescription = null, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Medium, modifier = Modifier.size(iconSize).clip(RoundedCornerShape(iconSize * 0.24f)))
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Scenic.TextSecondary, modifier = Modifier.size(iconSize))
        }
        Spacer(Modifier.width(iconSize * 0.45f))
        Text(np.platform, style = Scenic.Caption.copy(fontSize = textSize, color = Scenic.TextPrimary.copy(alpha = 0.6f), shadow = PlayerShadow), maxLines = 1)
    }
}

@Composable
fun Artwork(np: NowPlaying, size: Dp, shape: RoundedCornerShape, modifier: Modifier = Modifier) {
    val art = np.artwork
    if (art != null) {
        Image(art, contentDescription = "${np.title} artwork", contentScale = ContentScale.Crop, filterQuality = FilterQuality.High, modifier = modifier.size(size).clip(shape))
    } else {
        // No artwork published by the app: a neutral placeholder rather than invented art.
        Box(
            modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(Color(0xFF4A4F5C), Color(0xFF22252D)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(size * 0.38f))
        }
    }
}

/** Thin rounded progress track with elapsed time on the left and remaining time on the right. */
@Composable
internal fun Scrubber(np: NowPlaying, position: Long, modifier: Modifier = Modifier, barHeight: Dp, labelSize: TextUnit) {
    val fraction = if (np.durationMs > 0) (position / np.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val label = Scenic.Tabular.copy(fontSize = labelSize, fontWeight = FontWeight.Medium, color = Scenic.TextPrimary.copy(alpha = 0.7f), shadow = PlayerShadow)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(barHeight).graphicsLayer()) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color.White.copy(alpha = 0.24f), cornerRadius = r)
            if (fraction > 0f) {
                drawRoundRect(Color.White.copy(alpha = 0.95f), size = Size((size.width * fraction).coerceAtLeast(size.height), size.height), cornerRadius = r)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(position), style = label)
            Spacer(Modifier.weight(1f))
            Text(formatRemaining(position, np.durationMs), style = label)
        }
    }
}

/** ⏪ ⏯ ⏩ glyphs; the focused one sits on a white disc, like tvOS. */
@Composable
internal fun TransportControls(
    np: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    playSize: Dp,
    skipSize: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    playModifier: Modifier = Modifier,
    /** Richer motion for the full-screen player: press bounce, glow halo, icon morph and skip nudge. */
    expressive: Boolean = false,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        if (expressive) {
            ExpressiveButton(Icons.Rounded.FastRewind, "Previous", skipSize, onPrevious, enabled = np.canSkipPrevious, nudge = -1)
            ExpressiveButton(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (np.isPlaying) "Pause" else "Play", playSize, onPlayPause, modifier = playModifier)
            ExpressiveButton(Icons.Rounded.FastForward, "Next", skipSize, onNext, enabled = np.canSkipNext, nudge = 1)
        } else {
            GlyphButton(Icons.Rounded.FastRewind, "Previous", skipSize, onPrevious, enabled = np.canSkipPrevious)
            GlyphButton(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (np.isPlaying) "Pause" else "Play", playSize, onPlayPause, modifier = playModifier)
            GlyphButton(Icons.Rounded.FastForward, "Next", skipSize, onNext, enabled = np.canSkipNext)
        }
    }
}

@Composable
private fun GlyphButton(icon: ImageVector, description: String, size: Dp, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, FocusSpring, label = "glyph")
    val disc by animateColorAsState(if (focused) Color.White else Color.Transparent, tween(160), label = "disc")
    val tint = when {
        focused -> Color(0xFF111111)
        enabled -> Scenic.TextPrimary
        else -> Scenic.TextPrimary.copy(alpha = 0.3f)
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 10.dp else 0.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(disc)
            .tvClickable(interaction) { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.62f))
    }
}

/**
 * Full-screen player button:
 * - focus: springs up with a soft white halo and the disc fills in,
 * - press: squashes down and bounces back,
 * - play/pause: the glyph morphs with a scale-and-fade,
 * - skip: the arrows nudge in their direction.
 */
@Composable
private fun ExpressiveButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    nudge: Int = 0,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(1f) }
    val shift = remember { Animatable(0f) }

    val focusScale by animateFloatAsState(if (focused) 1.16f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "xFocus")
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh), label = "xPress")
    val fill by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "xFill")
    val halo by animateFloatAsState(if (focused) 1f else 0f, tween(360), label = "xHalo")
    val tint = lerp(if (enabled) Scenic.TextPrimary else Scenic.TextPrimary.copy(alpha = 0.3f), Color(0xFF111111), fill)

    Box(modifier.size(size * 1.3f), contentAlignment = Alignment.Center) {
        // Soft glow that blooms behind the focused button.
        Canvas(Modifier.size(size * 1.3f).graphicsLayer { alpha = halo }) {
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), radius = this.size.minDimension / 2),
            )
        }
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    val s = focusScale * pressScale * bounce.value
                    scaleX = s
                    scaleY = s
                }
                .shadow((16 * fill).dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = fill))
                .tvClickable(interaction) {
                    if (!enabled) return@tvClickable
                    onClick()
                    scope.launch {
                        bounce.snapTo(0.86f)
                        bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                    }
                    if (nudge != 0) scope.launch {
                        shift.animateTo(nudge.toFloat(), tween(110))
                        shift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(180), targetScale = 0.6f))
                },
                label = "xIcon",
            ) { glyph ->
                Icon(
                    glyph,
                    contentDescription = description,
                    tint = tint,
                    modifier = Modifier
                        .size(size * 0.56f)
                        .graphicsLayer { translationX = shift.value * size.toPx() * 0.12f },
                )
            }
        }
    }
}
