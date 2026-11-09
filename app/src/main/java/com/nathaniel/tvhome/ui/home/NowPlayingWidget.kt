package com.nathaniel.tvhome.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote as OutlinedMusicNote
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.LocalContainer
import com.nathaniel.tvhome.ui.components.FocusSpring
import com.nathaniel.tvhome.ui.components.GlassButton
import com.nathaniel.tvhome.ui.components.GlassIconButton
import com.nathaniel.tvhome.ui.components.GlassSurface
import com.nathaniel.tvhome.ui.components.tvClickable
import com.nathaniel.tvhome.ui.theme.GlassSpec
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
            is MediaState.Active -> ActiveWidget(s.nowPlaying, onOpen, onPlayPause, onPrevious, onNext)
            MediaState.Idle -> GlassIconButton(
                icon = Icons.Outlined.OutlinedMusicNote,
                contentDescription = "Now Playing",
                onClick = onOpen,
                size = 52.dp,
                background = GlassSpec.StatusBackground,
                borderAlpha = GlassSpec.StatusBorderAlpha,
                shadowAlpha = GlassSpec.StatusShadowAlpha,
            )
            MediaState.PermissionRequired -> GlassIconButton(
                icon = Icons.Outlined.OutlinedMusicNote,
                contentDescription = "Enable Now Playing access",
                onClick = onEnableAccess,
                size = 52.dp,
                background = GlassSpec.StatusBackground,
                borderAlpha = GlassSpec.StatusBorderAlpha,
                shadowAlpha = GlassSpec.StatusShadowAlpha,
            )
        }
    }
}

@Composable
private fun ActiveWidget(
    np: NowPlaying,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val position = rememberPosition(np)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    GlassSurface(
        RoundedCornerShape(26.dp),
        modifier = Modifier.width(320.dp).height(92.dp),
        elevation = 8.dp,
        background = Color(0xFF0F1113).copy(alpha = 0.12f),
        borderAlpha = if (focused) 0.9f else GlassSpec.StatusBorderAlpha,
        shadowAlpha = GlassSpec.StatusShadowAlpha,
    ) {
        Box(Modifier.fillMaxSize()) {
            np.artwork?.let { artwork ->
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(1.dp).clip(RoundedCornerShape(25.dp)).graphicsLayer { alpha = 0.18f },
                )
                Box(
                    Modifier.fillMaxSize().padding(1.dp).clip(RoundedCornerShape(25.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                )
            }
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .tvClickable(interaction, onClick = onOpen)
                            .semantics { contentDescription = "Now playing: ${np.title} by ${np.artist}" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(np, 40.dp, RoundedCornerShape(9.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            TrackTitle(np.title, Scenic.Label.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp, shadow = PlayerShadow), focused)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                np.artist,
                                style = Scenic.Caption.copy(fontSize = 14.sp, lineHeight = 16.sp, color = Color.White, shadow = PlayerShadow),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    CompactTransportButton(Icons.Rounded.FastRewind, "Previous track", np.canSkipPrevious, onPrevious, 26.dp)
                    Spacer(Modifier.width(4.dp))
                    CompactTransportButton(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (np.isPlaying) "Pause" else "Play", true, onPlayPause, 30.dp)
                    Spacer(Modifier.width(4.dp))
                    CompactTransportButton(Icons.Rounded.FastForward, "Next track", np.canSkipNext, onNext, 26.dp)
                }
                Spacer(Modifier.height(4.dp))
                Scrubber(
                    np,
                    position,
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    barHeight = 3.dp,
                    labelSize = 9.sp,
                    labelsBesideTrack = false,
                    labelTrackSpacing = 2.dp,
                    trackTouchHeight = 18.dp,
                )
            }
        }
    }
}

@Composable
private fun CompactTransportButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = if (enabled) 0.08f else 0.03f))
            .border(1.dp, Color.White.copy(alpha = if (focused) 0.95f else 0.22f), CircleShape)
            .tvClickable(interaction) { if (enabled) onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = when {
                focused -> Scenic.Night
                enabled -> Color.White
                else -> Color.White.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(size * 0.62f),
        )
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
internal fun Scrubber(
    np: NowPlaying,
    position: Long,
    modifier: Modifier = Modifier,
    barHeight: Dp,
    labelSize: TextUnit,
    onSeek: ((Long) -> Unit)? = null,
    labelsBesideTrack: Boolean = false,
    labelTrackSpacing: Dp = 4.dp,
    trackTouchHeight: Dp = 32.dp,
) {
    val targetFraction = if (np.durationMs > 0) (position / np.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = if (np.isPlaying) 900 else 180, easing = LinearEasing),
        label = "scrubberProgress",
    )
    val latestOnSeek by rememberUpdatedState(onSeek)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val label = Scenic.Tabular.copy(fontSize = labelSize, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.95f), shadow = PlayerShadow)
    Column(modifier) {
        if (labelsBesideTrack) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(position), modifier = Modifier.widthIn(min = 40.dp), style = label, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                ScrubberTrack(np, position, targetFraction, fraction, barHeight, focused, interaction, latestOnSeek, Modifier.weight(1f), trackTouchHeight)
                Spacer(Modifier.width(8.dp))
                Text(formatRemaining(position, np.durationMs), modifier = Modifier.widthIn(min = 40.dp), style = label, textAlign = TextAlign.End, maxLines = 1)
            }
        } else {
            ScrubberTrack(np, position, targetFraction, fraction, barHeight, focused, interaction, latestOnSeek, Modifier.fillMaxWidth(), trackTouchHeight)
            Spacer(Modifier.height(labelTrackSpacing))
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration(position), style = label)
                Spacer(Modifier.weight(1f))
                Text(formatRemaining(position, np.durationMs), style = label)
            }
        }
    }
}

@Composable
private fun ScrubberTrack(
    np: NowPlaying,
    position: Long,
    targetFraction: Float,
    fraction: Float,
    barHeight: Dp,
    focused: Boolean,
    interaction: MutableInteractionSource,
    onSeek: ((Long) -> Unit)?,
    modifier: Modifier,
    trackTouchHeight: Dp,
) {
        val latestOnSeek by rememberUpdatedState(onSeek)
        Box(
            modifier
                .height(trackTouchHeight)
                .semantics {
                    contentDescription = "Playback position"
                    progressBarRangeInfo = ProgressBarRangeInfo(targetFraction, 0f..1f)
                }
                .pointerInput(np.trackKey, np.durationMs) {
                    detectTapGestures { point ->
                        if (np.durationMs > 0) latestOnSeek?.invoke((np.durationMs * (point.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
                .pointerInput(np.trackKey, np.durationMs) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (np.durationMs > 0) latestOnSeek?.invoke((np.durationMs * (change.position.x / size.width).coerceIn(0f, 1f)).toLong())
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || np.durationMs <= 0 || latestOnSeek == null) return@onPreviewKeyEvent false
                    val target = when (event.key) {
                        Key.DirectionLeft -> position - 10_000L
                        Key.DirectionRight -> position + 10_000L
                        else -> return@onPreviewKeyEvent false
                    }
                    latestOnSeek?.invoke(target.coerceIn(0L, np.durationMs))
                    true
                }
                .focusable(interactionSource = interaction),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(barHeight).graphicsLayer()) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color.White.copy(alpha = if (focused) 0.45f else 0.24f), cornerRadius = r)
                if (fraction > 0f) {
                    val progressWidth = (size.width * fraction).coerceAtLeast(size.height)
                    drawRoundRect(Color.White.copy(alpha = 0.95f), size = Size(progressWidth, size.height), cornerRadius = r)
                    if (focused) drawCircle(Color.White, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(progressWidth.coerceAtMost(size.width), size.height / 2))
                }
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
            GlyphButton(Icons.Rounded.SkipPrevious, "Previous", skipSize, onPrevious, enabled = np.canSkipPrevious)
            GlyphButton(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (np.isPlaying) "Pause" else "Play", playSize, onPlayPause, modifier = playModifier)
            GlyphButton(Icons.Rounded.SkipNext, "Next", skipSize, onNext, enabled = np.canSkipNext)
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

    val focusScale by animateFloatAsState(if (focused) 1.08f else 1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "xFocus")
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessHigh), label = "xPress")
    val fill by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "xFill")
    val halo by animateFloatAsState(if (focused) 1f else 0f, tween(360), label = "xHalo")
    val tint = lerp(if (enabled) Scenic.TextPrimary else Scenic.TextPrimary.copy(alpha = 0.3f), Color(0xFF111111), fill)

    Box(Modifier.size(size * 1.3f), contentAlignment = Alignment.Center) {
        // Soft glow that blooms behind the focused button.
        Canvas(Modifier.size(size * 1.3f).graphicsLayer { alpha = halo }) {
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), radius = this.size.minDimension / 2),
            )
        }
        Box(
            modifier
                .size(size)
                .graphicsLayer {
                    val s = focusScale * pressScale * bounce.value
                    scaleX = s
                    scaleY = s
                }
                .shadow((16 * fill).dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(if (focused) Color.White.copy(alpha = 0.92f) else Color(0xFF101316).copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = if (focused) 0.95f else 0.24f), CircleShape)
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
