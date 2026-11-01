package com.nathaniel.tvhome.ui.screens

import androidx.compose.animation.AnimatedContent
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.rounded.Lyrics
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

    val readyLyrics = (lyricsState as? LyricsState.Ready)?.takeIf { it.trackKey == np.trackKey }?.lyrics
    val lyricsVisible = prefs.showLyrics && readyLyrics != null
    // Keep the last lyrics so they can animate out after being switched off.
    var shownLyrics by remember(np.trackKey) { mutableStateOf(readyLyrics) }
    if (readyLyrics != null) shownLyrics = readyLyrics

    // One progress value drives the whole morph: 0 = artwork layout, 1 = lyrics layout.
    val t by animateFloatAsState(if (lyricsVisible) 1f else 0f, tween(MORPH_MS, easing = Emphasized), label = "lyricsMorph")
    val titleSize = lerp(34f, 24f, t)

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 96.dp, vertical = 64.dp)) {
        val w = maxWidth
        val h = maxHeight
        val normalLeft = ((w - (ART + GAP + INFO)) / 2).coerceAtLeast(0.dp)
        val infoX = lerp(normalLeft + ART + GAP, 0.dp, t)
        val lyricsX = INFO + GAP
        var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var slotCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

        Box(Modifier.fillMaxSize().onPlaced { rootCoords = it }) {
            // Song details and controls: a fixed-width column that glides left, so text never reflows.
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(infoX.roundToPx(), 0) }
                    .width(INFO),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(np, iconSize = 16.dp, textSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    LyricsStatus(prefs.showLyrics, lyricsState, np)
                    Spacer(Modifier.width(10.dp))
                    QuietIconButton(
                        icon = Icons.Rounded.Lyrics,
                        description = if (prefs.showLyrics) "Hide lyrics" else "Show lyrics",
                        active = prefs.showLyrics,
                        onClick = { container.scope.launch { container.preferences.setShowLyrics(!prefs.showLyrics) } },
                    )
                    Spacer(Modifier.width(6.dp))
                    QuietIconButton(
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        description = "Open ${np.platform}",
                        onClick = { container.media.openSourceApp() },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Landing spot for the artwork in lyrics mode; it opens up as the cover arrives.
                    Spacer(
                        Modifier
                            .width((SMALL_ART + 16.dp) * t)
                            .height(SMALL_ART * t)
                            .onPlaced { slotCoords = it },
                    )
                    AnimatedContent(np.trackKey, transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(200)) }, label = "npText") { _ ->
                        Column {
                            TrackTitle(
                                np.title,
                                Scenic.Title.copy(fontSize = titleSize.sp, lineHeight = (titleSize * 1.18f).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, shadow = PlayerShadow),
                                focused = false,
                                maxLines = 2,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                np.artist,
                                style = Scenic.Body.copy(fontSize = (titleSize * 0.62f).sp, color = Scenic.TextPrimary.copy(alpha = 0.6f), shadow = PlayerShadow),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!np.album.isNullOrBlank() && t < 0.99f) {
                                Text(
                                    np.album,
                                    style = Scenic.Body.copy(fontSize = 16.sp, color = Scenic.TextPrimary.copy(alpha = 0.42f * (1f - t)), shadow = PlayerShadow),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp * (1f - t)).height(22.dp * (1f - t)),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Scrubber(np, position, Modifier.fillMaxWidth(), barHeight = 4.dp, labelSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TransportControls(
                        np,
                        container.media::playPause,
                        container.media::skipNext,
                        container.media::skipPrevious,
                        playSize = 56.dp,
                        skipSize = 46.dp,
                        spacing = 14.dp,
                        expressive = true,
                        playModifier = Modifier.focusRequester(playFocus),
                    )
                }
            }

            // One artwork that shrinks and glides into its spot beside the title (scaled, not re-laid-out).
            val artSize = lerp(ART, SMALL_ART, t)
            val scale = artSize / ART
            val radius = lerp(18.dp, 12.dp, t) / scale
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val normal = IntOffset(normalLeft.roundToPx(), ((h - ART) / 2).roundToPx())
                        val root = rootCoords
                        val slot = slotCoords
                        val target = if (root != null && slot != null && root.isAttached && slot.isAttached) {
                            val p = root.localPositionOf(slot, Offset.Zero)
                            IntOffset(infoX.roundToPx(), p.y.roundToInt())
                        } else normal
                        IntOffset(lerpInt(normal.x, target.x, t), lerpInt(normal.y, target.y, t))
                    }
                    .size(ART)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                AnimatedContent(
                    np.trackKey,
                    transitionSpec = { (fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith fadeOut(tween(250)) },
                    label = "npArt",
                ) { _ ->
                    val shape = RoundedCornerShape(radius)
                    Artwork(np, ART, shape, Modifier.shadow(lerp(48.dp, 60.dp, t), shape, ambientColor = Color.Black, spotColor = Color.Black))
                }
            }

            // Lyrics drift in from the right as the cover leaves, and drift back out when switched off.
            val lyrics = shownLyrics
            if (lyrics != null && t > 0.01f) {
                val appear = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset((lyricsX + 48.dp * (1f - appear)).roundToPx(), 0) }
                        .width((w - lyricsX).coerceAtLeast(0.dp))
                        .height(LYRICS_HEIGHT)
                        .graphicsLayer { alpha = appear },
                ) {
                    LyricsPanel(np, lyrics, Modifier.fillMaxSize())
                }
            }
        }
    }
}

private fun lerpInt(a: Int, b: Int, t: Float) = (a + (b - a) * t).roundToInt()

/** Apple's "emphasized" curve: quick start, long graceful settle. */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val MORPH_MS = 700
private val ART = 300.dp
private val SMALL_ART = 72.dp
private val GAP = 56.dp
private val INFO = 340.dp

/** Minimal tvOS-style icon button: just a glyph, a white disc when focused, a soft fill when switched on. */
@Composable
private fun QuietIconButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, FocusSpring, label = "quietScale")
    val bg by animateColorAsState(
        when {
            focused -> Color.White
            active -> Color.White.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        tween(180),
        label = "quietBg",
    )
    val tint = when {
        focused -> Color(0xFF111111)
        active -> Color.White
        else -> Color.White.copy(alpha = 0.6f)
    }
    Box(
        Modifier
            .size(SMALL_BUTTON)
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
