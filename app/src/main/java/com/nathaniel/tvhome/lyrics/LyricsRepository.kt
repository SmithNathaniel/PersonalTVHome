package com.nathaniel.tvhome.lyrics

import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Immutable
import com.nathaniel.tvhome.BuildConfig
import com.nathaniel.tvhome.media.MediaState
import com.nathaniel.tvhome.media.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class LyricLine(val timeMs: Long, val text: String)

@Immutable
sealed interface Lyrics {
    /** Time-stamped lines that can follow playback. */
    data class Synced(val lines: List<LyricLine>) : Lyrics
    /** Lyrics without timing; shown as a slowly scrolling sheet. */
    data class Plain(val lines: List<String>) : Lyrics
    data object Instrumental : Lyrics
}

sealed interface LyricsState {
    data object Off : LyricsState
    data object Idle : LyricsState
    data class Loading(val trackKey: String) : LyricsState
    data class Ready(val trackKey: String, val lyrics: Lyrics) : LyricsState
    data class Unavailable(val trackKey: String) : LyricsState
}

/**
 * Looks up lyrics for the current track from LRCLIB (https://lrclib.net), a free and open lyrics
 * database. Only the title, artist, album and duration are sent. Results are cached for the session.
 */
class LyricsRepository(
    scope: CoroutineScope,
    media: StateFlow<MediaState>,
    enabled: Flow<Boolean>,
) {
    private val _state = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    private data class Query(val trackKey: String, val title: String, val artist: String, val album: String?, val durationSec: Int)

    /** Lyrics already found this session, so the same song is never requested twice. */
    private val cache = LruCache<String, Lyrics>(64)
    private val notFound = LinkedHashSet<String>()
    private var retryAfterUntil = 0L

    init {
        scope.launch {
            combine(
                media.map { (it as? MediaState.Active)?.nowPlaying?.let(::queryFor) }.distinctUntilChanged(),
                enabled.distinctUntilChanged(),
            ) { query, on -> query to on }
                .collectLatest { (query, on) ->
                    when {
                        !on -> _state.value = LyricsState.Off
                        query == null -> _state.value = LyricsState.Idle
                        else -> load(query)
                    }
                }
        }
    }

    private suspend fun load(q: Query) {
        cache.get(q.trackKey)?.let { _state.value = LyricsState.Ready(q.trackKey, it); return }
        if (q.trackKey in notFound) { _state.value = LyricsState.Unavailable(q.trackKey); return }
        _state.value = LyricsState.Loading(q.trackKey)
        // Debounce quick skips so browsing through a playlist doesn't fire a burst of requests.
        delay(DEBOUNCE_MS)
        val outcome = runCatching { fetch(q) }.onFailure { Log.w(TAG, "Lyrics lookup failed", it) }
        val result = outcome.getOrNull()
        if (result != null) {
            cache.put(q.trackKey, result)
            _state.value = LyricsState.Ready(q.trackKey, result)
        } else {
            // Only remember genuine "no lyrics" answers; network errors are retried next time the song plays.
            if (outcome.isSuccess) {
                notFound += q.trackKey
                if (notFound.size > 200) notFound.remove(notFound.first())
            }
            _state.value = LyricsState.Unavailable(q.trackKey)
        }
    }

    private fun queryFor(np: NowPlaying): Query? {
        if (np.title.isBlank() || np.artist.isBlank()) return null
        return Query(np.trackKey, np.title.trim(), np.artist.trim(), np.album?.trim()?.takeIf { it.isNotEmpty() }, (np.durationMs / 1000.0).roundToInt())
    }

    private suspend fun fetch(q: Query): Lyrics? = withContext(Dispatchers.IO) {
        val artist = primaryArtist(q.artist)
        val titles = listOf(q.title, cleanTitle(q.title)).distinct()
        for (title in titles) {
            if (q.durationSec in 1..3600) {
                val params = mutableListOf("track_name" to title, "artist_name" to artist, "duration" to q.durationSec.toString())
                if (q.album != null && title == q.title) params += "album_name" to q.album
                get("/api/get", params)?.let { body -> parseRecord(JSONObject(body))?.let { return@withContext it } }
            }
            // Fall back to a search, accepting only results whose length matches the playing track.
            val body = get("/api/search", listOf("track_name" to title, "artist_name" to artist)) ?: continue
            val results = JSONArray(body)
            var best: Lyrics? = null
            for (i in 0 until minOf(results.length(), 10)) {
                val record = results.optJSONObject(i) ?: continue
                val duration = record.optDouble("duration", 0.0)
                if (q.durationSec > 0 && abs(duration - q.durationSec) > 3) continue
                val lyrics = parseRecord(record) ?: continue
                if (lyrics is Lyrics.Synced) return@withContext lyrics
                if (best == null) best = lyrics
            }
            if (best != null) return@withContext best
        }
        null
    }

    private fun parseRecord(record: JSONObject): Lyrics? {
        if (record.optBoolean("instrumental", false)) return Lyrics.Instrumental
        val synced = record.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return Lyrics.Synced(lines)
        }
        val plain = record.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
        if (plain != null) {
            val lines = plain.lines().map { it.trim() }.take(MAX_LINES)
            if (lines.any { it.isNotEmpty() }) return Lyrics.Plain(lines)
        }
        return null
    }

    /** Sends one request, honouring LRCLIB's Retry-After. Returns null on 404 or any error. */
    private suspend fun get(path: String, params: List<Pair<String, String>>): String? {
        val wait = retryAfterUntil - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        val query = params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val connection = (URL("https://lrclib.net$path?$query").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> return connection.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_RESPONSE_BYTES) return null
                        out.write(buffer, 0, n)
                    }
                    out.toString("UTF-8")
                }
                429 -> {
                    val seconds = connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(1, 3600) ?: 30L
                    val jitter = (seconds * 1000 * 0.3 * Random.nextDouble()).toLong()
                    retryAfterUntil = System.currentTimeMillis() + seconds * 1000 + jitter
                    return null
                }
                else -> return null
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "LyricsRepository"
        private const val DEBOUNCE_MS = 600L
        private const val MAX_RESPONSE_BYTES = 512 * 1024
        private const val MAX_LINES = 400
        private val USER_AGENT = "PersonalTVHome v${BuildConfig.VERSION_NAME} (https://github.com/SmithNathaniel/PersonalTVHome)"

        private val timeTag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

        /** Parses LRC text, supporting several time tags per line; metadata tags are ignored. */
        fun parseLrc(lrc: String): List<LyricLine> {
            val lines = mutableListOf<LyricLine>()
            for (raw in lrc.lineSequence().take(MAX_LINES * 2)) {
                val tags = timeTag.findAll(raw).toList()
                if (tags.isEmpty()) continue
                val text = raw.substring(tags.last().range.last + 1).trim()
                for (tag in tags) {
                    val (m, s, frac) = tag.destructured
                    val fraction = when (frac.length) {
                        0 -> 0L
                        1 -> frac.toLong() * 100
                        2 -> frac.toLong() * 10
                        else -> frac.take(3).toLong()
                    }
                    lines += LyricLine(m.toLong() * 60_000 + s.toLong() * 1000 + fraction, text)
                }
            }
            return lines.sortedBy { it.timeMs }.take(MAX_LINES)
        }

        /** "Izono (feat. X & Y)" → "Izono"; also drops "- Remastered 2011", "[Official Video]" and "(prod. by …)". */
        fun cleanTitle(title: String): String = title
            .replace(Regex("""\s*[(\[](?:feat|ft|with|prod|official|lyric|audio|video|remaster|explicit)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+-\s+(?:.*remaster.*|.*version.*|live.*|radio edit.*)$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { title }

        /** "Kelvin Momo, Babalwa M" → "Kelvin Momo". */
        fun primaryArtist(artist: String): String =
            artist.split(Regex("""\s*(?:,|&| x | feat\.? | ft\.? )\s*""", RegexOption.IGNORE_CASE)).first().trim().ifEmpty { artist }
    }
}
