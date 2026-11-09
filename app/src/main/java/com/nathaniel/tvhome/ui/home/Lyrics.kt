package com.nathaniel.tvhome.ui.home

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Apple Music–style lyrics, three lines at a time: the line just sung (dim), the line being sung
 * (bright) and the next line (dim). Each new line slides the trio up.
 */
@Composable
fun LyricsPanel(np: NowPlaying, lyrics: Lyrics, modifier: Modifier = Modifier, compact: Boolean = false) {
    when (lyrics) {
        is Lyrics.Synced -> {
            val index = rememberLyricIndex(np, lyrics.lines)
            ThreeLines(index, lyrics.lines.size, { lyrics.lines[it].text }, modifier, compact)
        }
        is Lyrics.Plain -> {
            // No timing: estimate the line from how far through the song we are.
            val lines = lyrics.lines.filter { it.isNotBlank() }
            val position = rememberPosition(np)
            val index = if (np.durationMs > 0 && lines.isNotEmpty()) ((position.toFloat() / np.durationMs) * lines.size).toInt().coerceIn(0, lines.lastIndex) else 0
            ThreeLines(index, lines.size, { lines[it] }, modifier, compact)
        }
        Lyrics.Instrumental -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, contentDescription = "Instrumental", tint = Scenic.TextPrimary.copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun ThreeLines(index: Int, count: Int, line: (Int) -> String, modifier: Modifier, compact: Boolean) {
    fun textAt(i: Int): String? = if (i in 0 until count) line(i).ifBlank { "♪" } else null
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(420, delayMillis = 60)) + slideInVertically(tween(520, easing = FastOutSlowInEasing)) { if (forward) it / 3 else -it / 3 }) togetherWith
                    (fadeOut(tween(260)) + slideOutVertically(tween(520, easing = FastOutSlowInEasing)) { if (forward) -it / 3 else it / 3 })
            },
            label = "lyricTrio",
        ) { i ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LyricRow(textAt(i - 1), LineRole.Previous, compact)
                // Before the first line, show a quiet note in the "current" slot.
                LyricRow(textAt(i) ?: if (i < 0) "♪" else null, LineRole.Current, compact)
                LyricRow(textAt(i + 1), LineRole.Next, compact)
            }
        }
    }
}

private enum class LineRole { Previous, Current, Next }

@Composable
private fun LyricRow(text: String?, role: LineRole, compact: Boolean) {
    val color = when (role) {
        LineRole.Current -> Color.White
        LineRole.Previous -> Color.White.copy(alpha = 0.26f)
        LineRole.Next -> Color.White.copy(alpha = 0.34f)
    }
    val scale = if (role == LineRole.Current) 1f else 0.9f
    Text(
        text ?: "",
        style = Scenic.Title.copy(fontSize = if (compact) 24.77.sp else 30.6.sp, lineHeight = if (compact) 29.83.sp else 38.3.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, color = color, shadow = if (role == LineRole.Current) PlayerShadow else null),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        // Keep a steady rhythm even when a slot is empty (start or end of the song).
        minLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            },
        textAlign = TextAlign.Center,
    )
}
