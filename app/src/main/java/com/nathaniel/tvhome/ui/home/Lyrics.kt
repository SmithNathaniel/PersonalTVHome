package com.nathaniel.tvhome.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nathaniel.tvhome.lyrics.LyricLine
import com.nathaniel.tvhome.lyrics.Lyrics
import com.nathaniel.tvhome.media.NowPlaying
import com.nathaniel.tvhome.ui.theme.Scenic
import kotlinx.coroutines.delay

/**
 * Index of the line being sung (-1 before the first). Sleeps until the next line is due instead of
 * polling, so the UI only recomposes when the line actually changes.
 */
@Composable
fun rememberLyricIndex(np: NowPlaying, lines: List<LyricLine>): Int {
    val index by produceState(currentIndex(np, lines), np, lines) {
        while (true) {
            val position = np.positionAt(SystemClock.elapsedRealtime())
            val i = indexAt(lines, position)
            value = i
            if (!np.isPlaying || i >= lines.lastIndex) break
            val untilNext = ((lines[i + 1].timeMs - position) / np.speed.coerceAtLeast(0.25f)).toLong()
            delay(untilNext.coerceIn(30, 10_000))
        }
    }
    return index
}

private fun currentIndex(np: NowPlaying, lines: List<LyricLine>) = indexAt(lines, np.positionAt(SystemClock.elapsedRealtime()))

private fun indexAt(lines: List<LyricLine>, positionMs: Long): Int {
    // Lyrics are shown a touch early so they land as the line is sung.
    val p = positionMs + LEAD_MS
    var lo = 0
    var hi = lines.lastIndex
    var found = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (lines[mid].timeMs <= p) { found = mid; lo = mid + 1 } else hi = mid - 1
    }
    return found
}

private const val LEAD_MS = 250L

/** The current line, rising into place as each new line begins. For the compact widget. */
@Composable
fun WidgetLyricLine(np: NowPlaying, lyrics: Lyrics.Synced, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val index = rememberLyricIndex(np, lyrics.lines)
    val text = lyrics.lines.getOrNull(index)?.text?.ifBlank { "♪" } ?: "♪"
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(320)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 }) togetherWith
                (fadeOut(tween(200)) + slideOutVertically(tween(300)) { -it / 2 })
        },
        label = "widgetLyric",
        modifier = modifier,
    ) { line ->
        Text(
            line,
            style = Scenic.Label.copy(fontSize = fontSize, lineHeight = fontSize * 1.25f, fontWeight = FontWeight.SemiBold, color = Scenic.TextPrimary, shadow = PlayerShadow),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Apple Music–style lyrics: the sung line is bright and large, the rest recede, all scrolling smoothly. */
@Composable
fun LyricsPanel(np: NowPlaying, lyrics: Lyrics, modifier: Modifier = Modifier) {
    when (lyrics) {
        is Lyrics.Synced -> SyncedLyrics(np, lyrics.lines, modifier)
        is Lyrics.Plain -> PlainLyrics(np, lyrics.lines, modifier)
        Lyrics.Instrumental -> Box(modifier, contentAlignment = Alignment.CenterStart) {
            Icon(Icons.Rounded.MusicNote, contentDescription = "Instrumental", tint = Scenic.TextPrimary.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun SyncedLyrics(np: NowPlaying, lines: List<LyricLine>, modifier: Modifier) {
    val index = rememberLyricIndex(np, lines)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceAtLeast(0))
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
    LaunchedEffect(index) {
        listState.animateScrollToItem(index.coerceAtLeast(0))
    }
    BoxWithConstraints(modifier.fadingEdges()) {
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            // The active line sits about a third of the way down, like Apple Music.
            contentPadding = PaddingValues(top = maxHeight * 0.32f, bottom = maxHeight * 0.6f),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
                // Each line rises in a beat after the one above it.
                val step = (i - index).coerceIn(0, 6) * 0.09f
                LyricRow(
                    line.text.ifBlank { "♪" },
                    active = i == index,
                    past = i < index,
                    entrance = { ((entrance.value - step) / 0.45f).coerceIn(0f, 1f) },
                )
            }
        }
    }
}

@Composable
private fun LyricRow(text: String, active: Boolean, past: Boolean, entrance: () -> Float = { 1f }) {
    val color by animateColorAsState(
        when {
            active -> Color.White
            past -> Color.White.copy(alpha = 0.28f)
            else -> Color.White.copy(alpha = 0.38f)
        },
        tween(420),
        label = "lyricColor",
    )
    val scale by animateFloatAsState(if (active) 1f else 0.96f, tween(420, easing = FastOutSlowInEasing), label = "lyricScale")
    Text(
        text,
        style = Scenic.Title.copy(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp, color = color, shadow = PlayerShadow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .graphicsLayer {
                val e = entrance()
                alpha = e
                translationY = (1f - e) * 18.dp.toPx()
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            },
    )
}

/** Unsynced lyrics glide down the sheet in step with the song's progress. */
@Composable
private fun PlainLyrics(np: NowPlaying, lines: List<String>, modifier: Modifier) {
    val position = rememberPosition(np)
    val listState = rememberLazyListState()
    val target = if (np.durationMs > 0) ((position.toFloat() / np.durationMs) * lines.size).toInt().coerceIn(0, lines.lastIndex) else 0
    LaunchedEffect(target) { listState.animateScrollToItem(target) }
    BoxWithConstraints(modifier.fadingEdges()) {
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            contentPadding = PaddingValues(top = maxHeight * 0.32f, bottom = maxHeight * 0.6f),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(lines, key = { i, _ -> i }) { _, line ->
                Text(
                    line,
                    style = Scenic.Title.copy(fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.75f), shadow = PlayerShadow),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
    }
}

/** Fades lyrics out toward the top and bottom edges. */
private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.18f to Color.Black,
                0.78f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
