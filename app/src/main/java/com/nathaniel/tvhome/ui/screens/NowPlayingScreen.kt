package com.nathaniel.tvhome.ui.screens

import androidx.compose.animation.AnimatedContent
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.nathaniel.tvhome.ui.components.tvClickable
import com.nathaniel.tvhome.ui.components.FocusSpring
import androidx.tv.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.rounded.Lyrics
import com.nathaniel.tvhome.lyrics.LyricsState
import com.nathaniel.tvhome.lyrics.Lyrics
import com.nathaniel.tvhome.ui.home.LyricsPanel
import kotlinx.coroutines.launch
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.mutableIntStateOf
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
import com.nathaniel.tvhome.ui.home.TransportControls
import com.nathaniel.tvhome.ui.home.rememberPosition
import com.nathaniel.tvhome.ui.navigation.LocalNavigator
import com.nathaniel.tvhome.ui.navigation.Navigator
import com.nathaniel.tvhome.ui.navigation.Screen
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
                                installed.forEachIndexed { index, source ->
                                    GlassButton(
                                        source.name,
                                        onClick = { actions.launch(source.packageName) },
                                        modifier = if (index == 0) Modifier.focusRequester(focus) else Modifier,
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
    val readyLyrics = (lyricsState as? LyricsState.Ready)?.takeIf { it.trackKey == np.trackKey }?.lyrics
    val noLyricsForTrack = (lyricsState as? LyricsState.Unavailable)?.trackKey == np.trackKey
    LaunchedEffect(Unit) { awaitFrame(); runCatching { playFocus.requestFocus() } }
    CenteredPlayer(
        np = np,
        position = position,
        lyrics = readyLyrics.takeIf { prefs.showLyrics && !noLyricsForTrack },
        lyricsEnabled = prefs.showLyrics && !noLyricsForTrack,
        lyricsState = lyricsState,
        playFocus = playFocus,
        onToggleLyrics = { container.scope.launch { container.preferences.setShowLyrics(!prefs.showLyrics) } },
        onOpenSource = { container.media.openSourceApp() },
    )
}

@Composable
internal fun CenteredPlayer(
    np: NowPlaying,
    position: Long,
    lyrics: Lyrics?,
    lyricsEnabled: Boolean,
    lyricsState: LyricsState,
    playFocus: FocusRequester,
    onToggleLyrics: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val media = LocalContainer.current.media
    var partyMode by remember { mutableStateOf(false) }
    val partyDim by animateFloatAsState(if (partyMode) 0.7f else 0f, tween(520, easing = FastOutSlowInEasing), label = "partyDim")
    val partyBarsAlpha by animateFloatAsState(if (partyMode) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "partyBarsAlpha")
    val partyCoverScale by animateFloatAsState(if (partyMode) 1.045f else 1f, tween(420, easing = FastOutSlowInEasing), label = "partyCoverScale")
    var activityTick by remember { mutableIntStateOf(0) }
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(partyMode, activityTick) {
        idle = false
        delay(5_000)
        idle = true
    }
    val idleScale by animateFloatAsState(if (idle) 1.2075f else 1f, tween(600, easing = FastOutSlowInEasing), label = "idleScale")
    val idleNow = idle
    val idleShiftPlain by animateDpAsState(if (idleNow) 45.dp else 0.dp, tween(600, easing = FastOutSlowInEasing), label = "idleShiftPlain")
    val idleShiftLyrics by animateDpAsState(if (idleNow) 45.dp else 0.dp, tween(600, easing = FastOutSlowInEasing), label = "idleShiftLyrics")
    val lyricsIdleScale by animateFloatAsState(if (idleNow) 1.2f else 1f, tween(600, easing = FastOutSlowInEasing), label = "lyricsIdleScale")
    val idleGap by animateDpAsState(if (idleNow) 6.dp else 0.dp, tween(600, easing = FastOutSlowInEasing), label = "idleGap")
    val infoIdleFactor by animateFloatAsState(if (idleNow) 0.8f else 1f, tween(600, easing = FastOutSlowInEasing), label = "infoIdleFactor")
    val controlsAlpha by animateFloatAsState(if (idleNow) 0f else 1f, tween(500, easing = FastOutSlowInEasing), label = "controlsAlpha")
    BoxWithConstraints(
        Modifier.fillMaxSize().onPreviewKeyEvent {
            val wasIdle = idle
            idle = false
            activityTick++
            wasIdle
        },
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = partyDim)))
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 24.dp)) {
        val coverSize = if (lyrics == null) {
            minOf(300.dp, maxHeight * 0.43f, maxWidth * 0.3f)
        } else {
            minOf(170.dp, maxHeight * 0.34f, maxWidth * 0.22f)
        }
        val reflectionHeight = coverSize * 0.22f
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceBadge(np, iconSize = 17.dp, textSize = 13.sp)
                Spacer(Modifier.weight(1f))
                LyricsStatus(lyricsEnabled, lyricsState, np)
                Spacer(Modifier.width(10.dp))
                QuietIconButton(
                    icon = Icons.Rounded.Lyrics,
                    description = if (lyricsEnabled) "Hide lyrics" else "Show lyrics",
                    active = lyricsEnabled,
                    onClick = onToggleLyrics,
                )
                Spacer(Modifier.width(6.dp))
                QuietIconButton(
                    icon = Icons.Rounded.GraphicEq,
                    description = if (partyMode) "Turn off Party Mode" else "Turn on Party Mode",
                    active = partyMode,
                    onClick = { partyMode = !partyMode },
                )
                Spacer(Modifier.width(6.dp))
                QuietIconButton(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    description = "Open ${np.platform}",
                    onClick = onOpenSource,
                )
            }

            if (lyrics == null) {
                Column(
                    Modifier.fillMaxSize().offset(y = 23.dp + idleShiftPlain).graphicsLayer { scaleX = 0.926f; scaleY = 0.926f },
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerArtwork(np, coverSize * 0.92f, partyCoverScale, artOffset = (-7).dp)
                    PlayerTrackInfo(np, compact = false)
                    Scrubber(
                        np,
                        position,
                        Modifier.fillMaxWidth(0.476f).graphicsLayer { alpha = controlsAlpha },
                        barHeight = 4.dp,
                        labelSize = 12.sp,
                        onSeek = media::seekTo,
                        labelTrackSpacing = 2.dp,
                    )
                    Row(
                        Modifier.offset(y = (-15).dp).padding(horizontal = 14.dp).graphicsLayer { alpha = controlsAlpha },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TransportControls(
                            np,
                            onPlayPause = media::playPause,
                            onNext = media::skipNext,
                            onPrevious = media::skipPrevious,
                            playSize = 54.dp,
                            skipSize = 44.dp,
                            spacing = 10.dp,
                            expressive = true,
                            playModifier = Modifier.focusRequester(playFocus),
                        )
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .offset(y = 32.dp + idleShiftLyrics)
                        .graphicsLayer { scaleX = 0.95f; scaleY = 0.95f },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerArtwork(np, coverSize * 0.72f * idleScale, partyCoverScale, artOffset = (-7).dp)
                    Spacer(Modifier.height(10.dp + idleGap))
                    PlayerTrackInfo(np, compact = true, scale = idleScale * infoIdleFactor)
                    Spacer(Modifier.height(10.dp + idleGap))
                    Box(Modifier.fillMaxWidth(0.9f).height(109.dp).graphicsLayer { scaleX = lyricsIdleScale; scaleY = lyricsIdleScale }) {
                        LyricsPanel(np, lyrics, Modifier.fillMaxSize(), compact = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Scrubber(
                        np,
                        position,
                        Modifier.fillMaxWidth(0.476f).graphicsLayer { alpha = controlsAlpha },
                        barHeight = 4.dp,
                        labelSize = 12.sp,
                        onSeek = media::seekTo,
                        labelsBesideTrack = false,
                        labelTrackSpacing = 1.dp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        Modifier.offset(y = (-32).dp).padding(horizontal = 14.dp, vertical = 8.dp).graphicsLayer { alpha = controlsAlpha },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TransportControls(
                            np,
                            onPlayPause = media::playPause,
                            onNext = media::skipNext,
                            onPrevious = media::skipPrevious,
                            playSize = 54.dp,
                            skipSize = 44.dp,
                            spacing = 10.dp,
                            expressive = true,
                            playModifier = Modifier.focusRequester(playFocus),
                        )
                    }
                }
            }

            if (!partyMode) {
                BottomAudioVisualizer(
                    np.isPlaying,
                    np.artwork,
                    Modifier.align(Alignment.BottomCenter).offset(y = 24.dp).fillMaxWidth(0.9f).height(18.dp),
                )
            }
        }
        }
        EdgeAudioVisualizer(
            np.isPlaying,
            np.artwork,
            Modifier.fillMaxSize().graphicsLayer { alpha = partyBarsAlpha },
        )
    }
}

@Composable
private fun PlayerArtwork(
    np: NowPlaying,
    coverSize: androidx.compose.ui.unit.Dp,
    partyCoverScale: Float,
    artOffset: androidx.compose.ui.unit.Dp = 2.dp,
) {
    Box(Modifier.size(coverSize).offset(y = artOffset)) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(coverSize)
                .graphicsLayer { scaleX = partyCoverScale; scaleY = partyCoverScale }
                .shadow(30.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            AnimatedContent(
                targetState = np,
                contentKey = { it.trackKey },
                transitionSpec = {
                    (fadeIn(tween(440)) + scaleIn(tween(520, easing = FastOutSlowInEasing), initialScale = 0.92f) +
                        slideInHorizontally(tween(500, easing = FastOutSlowInEasing)) { it / 12 }) togetherWith
                        (fadeOut(tween(260)) + scaleOut(tween(360), targetScale = 1.03f) + slideOutHorizontally(tween(360)) { -it / 14 })
                },
                label = "centeredAlbumArt",
            ) { track ->
                Artwork(track, coverSize, RoundedCornerShape(16.dp), Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PlayerTrackInfo(np: NowPlaying, compact: Boolean, scale: Float = 1f) {
    AnimatedContent(
        targetState = np,
        contentKey = { it.trackKey },
        transitionSpec = {
            (fadeIn(tween(360)) + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 8 }) togetherWith
                (fadeOut(tween(220)) + slideOutVertically(tween(300)) { -it / 10 })
        },
        label = "centeredTrackInfo",
        modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 760.dp).padding(horizontal = 16.dp),
    ) { track ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                track.title,
                style = Scenic.Title.copy(fontSize = (if (compact) 20.sp else 22.sp) * scale, fontWeight = FontWeight.SemiBold, color = Color.White, shadow = PlayerShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = Scenic.Body.copy(fontSize = (if (compact) 14.sp else 16.sp) * scale, color = Color.White.copy(alpha = 0.9f), shadow = PlayerShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BottomAudioVisualizer(isPlaying: Boolean, artwork: androidx.compose.ui.graphics.ImageBitmap?, modifier: Modifier = Modifier) {
    val palette = remember(artwork) { sampleArtworkPalette(artwork) }
    val transition = rememberInfiniteTransition(label = "bottomVisualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "bottomVisualizerPhase",
    )
    Canvas(modifier) {
        val count = 56
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.dp.toPx())
        val center = (count - 1) / 2f
        repeat(count) { index ->
            val envelope = 0.18f + 0.82f * (1f - kotlin.math.abs(index - center) / (center + 1f))
            val wave = (kotlin.math.sin(phase - index * 0.2f) + 1f) / 2f
            val pulse = if (isPlaying) 0.12f + 0.88f * wave else 0.08f
            val height = size.height * envelope * pulse
            drawRoundRect(
                color = palette[index % palette.size].copy(alpha = 0.3f + 0.68f * pulse),
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
private fun EdgeAudioVisualizer(
    isPlaying: Boolean,
    artwork: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val palette = remember(artwork) { sampleArtworkPalette(artwork) }
    val transition = rememberInfiniteTransition(label = "partyEdgeVisualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(3_800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "partyEdgePhase",
    )
    val playbackLevel by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.12f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "partyPlaybackLevel",
    )
    Canvas(modifier) {
        val horizontalCount = 72
        val verticalCount = 36
        val barThickness = 4.dp.toPx()
        val maxLength = 38.dp.toPx()

        fun lengthFor(index: Int, count: Int, sidePhase: Float): Pair<Float, Float> {
            val spatialPhase = index.toFloat() / count * 6.283f
            val wave = (kotlin.math.sin(phase + spatialPhase + sidePhase) + 1f) / 2f
            val smoothed = wave * wave * (3f - 2f * wave)
            val strength = playbackLevel * (0.12f + smoothed * 0.88f)
            val edgeEnvelope = 0.36f + 0.64f * (1f - kotlin.math.abs(index - count / 2f) / (count / 2f))
            return partyVisualizerBarLength(barThickness, maxLength, strength, edgeEnvelope) to strength
        }

        repeat(horizontalCount) { index ->
            val x = index * size.width / horizontalCount
            val (length, strength) = lengthFor(index, horizontalCount, 0f)
            val color = palette[index * palette.size / horizontalCount]
            drawRoundRect(color.copy(alpha = 0.18f + 0.56f * strength), Offset(x, 0f), androidx.compose.ui.geometry.Size(size.width / horizontalCount * 0.58f, length), CornerRadius(barThickness / 2f))
            drawRoundRect(color.copy(alpha = 0.18f + 0.56f * strength), Offset(x, size.height - length), androidx.compose.ui.geometry.Size(size.width / horizontalCount * 0.58f, length), CornerRadius(barThickness / 2f))
        }
        repeat(verticalCount) { index ->
            val y = index * size.height / verticalCount
            val (length, strength) = lengthFor(index, verticalCount, 1.1f)
            val color = palette[index * palette.size / verticalCount]
            drawRoundRect(color.copy(alpha = 0.18f + 0.56f * strength), Offset(0f, y), androidx.compose.ui.geometry.Size(length, size.height / verticalCount * 0.5f), CornerRadius(barThickness / 2f))
            drawRoundRect(color.copy(alpha = 0.18f + 0.56f * strength), Offset(size.width - length, y), androidx.compose.ui.geometry.Size(length, size.height / verticalCount * 0.5f), CornerRadius(barThickness / 2f))
        }
    }
}

internal fun partyVisualizerBarLength(barThickness: Float, maxLength: Float, strength: Float, edgeEnvelope: Float): Float =
    (barThickness + maxLength * strength * edgeEnvelope) * 0.9f

private fun sampleArtworkPalette(artwork: androidx.compose.ui.graphics.ImageBitmap?): List<Color> {
    if (artwork == null || artwork.width == 0 || artwork.height == 0) return listOf(Color(0xFFB7D9F5), Color(0xFFF2C68A), Color(0xFFC6A7E8))
    val pixels = artwork.toPixelMap()
    return List(12) { index ->
        val x = ((index + 0.5f) * pixels.width / 12).toInt().coerceIn(0, pixels.width - 1)
        val samples = listOf(pixels.height / 3, pixels.height / 2, pixels.height * 2 / 3)
            .map { y -> pixels[x, y.coerceIn(0, pixels.height - 1)] }
        val color = Color(
            red = samples.sumOf { it.red.toDouble() }.toFloat() / samples.size,
            green = samples.sumOf { it.green.toDouble() }.toFloat() / samples.size,
            blue = samples.sumOf { it.blue.toDouble() }.toFloat() / samples.size,
        )
        Color(
            red = (color.red * 0.76f + 0.24f).coerceIn(0f, 1f),
            green = (color.green * 0.76f + 0.24f).coerceIn(0f, 1f),
            blue = (color.blue * 0.76f + 0.24f).coerceIn(0f, 1f),
        )
    }
}

private fun lerpInt(a: Int, b: Int, t: Float) = (a + (b - a) * t).roundToInt()

private val ART = 300.dp
private val SMALL_ART = 72.dp
private val GAP = 56.dp
private val INFO = 340.dp

@Composable
private fun QuietIconButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, FocusSpring, label = "quietScale")
    val bg by animateColorAsState(
        when {
            focused -> Color.White.copy(alpha = 0.9f)
            active -> Color.White.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        tween(180),
        label = "quietBg",
    )
    val tint = when {
        focused -> Color(0xFF111111)
        else -> Color.White.copy(alpha = 0.6f)
    }
    Box(
        Modifier
            .size(SMALL_BUTTON)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 5.dp else 0.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(bg)
            .tvClickable(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(SMALL_BUTTON * 0.52f))
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
        if (t != null) Text(t, style = Scenic.Caption.copy(fontSize = 11.sp, color = Scenic.TextPrimary.copy(alpha = 0.5f), shadow = PlayerShadow))
    }
}

private val SMALL_BUTTON = 34.dp
private val LYRICS_HEIGHT = 260.dp
