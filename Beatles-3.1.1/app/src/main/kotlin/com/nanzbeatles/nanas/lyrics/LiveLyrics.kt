/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.lyrics

import android.content.Context
import com.nanzbeatles.nanas.constants.PreferredLyricsProvider
import com.nanzbeatles.nanas.constants.PreferredLyricsProviderKey
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.db.entities.LyricsEntity
import com.nanzbeatles.nanas.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.nanzbeatles.nanas.extensions.toEnum
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.get
import com.nanzbeatles.simpmusic.SimpMusicLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Satu kata / suku kata berikut waktu mulai-selesai (ms). */
data class LiveWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

/** Satu baris lirik. [words] kosong jika lirik hanya sinkron per baris. */
data class LiveLine(
    val startMs: Long,
    /** -1 jika tidak diketahui */
    val endMs: Long,
    val text: String,
    val words: List<LiveWord> = emptyList(),
)

data class LiveLyricsState(
    val mediaId: String? = null,
    val lines: List<LiveLine> = emptyList(),
    val loading: Boolean = false,
)

/** Lirik lagu yang sedang diputar, dibagikan ke layar kunci dan Dynamic Island. */
object LiveLyricsRepository {
    private val _state = MutableStateFlow(LiveLyricsState())
    val state: StateFlow<LiveLyricsState> = _state.asStateFlow()

    fun publish(newState: LiveLyricsState) {
        _state.value = newState
    }

    suspend fun load(
        context: Context,
        database: MusicDatabase,
        lyricsHelper: LyricsHelper,
        mediaId: String,
        title: String?,
        artist: String?,
        durationSec: Int,
        thumbnailUrl: String?,
    ): List<LiveLine> = withContext(Dispatchers.IO) {
        runCatching {
            var raw = database.lyrics(mediaId).firstOrNull()?.lyrics
            if (raw.isNullOrBlank()) {
                val songWithArtists = database.song(mediaId).firstOrNull()
                val metadata = if (songWithArtists != null) {
                    MediaMetadata(
                        id = songWithArtists.song.id,
                        title = songWithArtists.song.title,
                        artists = songWithArtists.artists.map {
                            MediaMetadata.Artist(id = it.id, name = it.name)
                        },
                        duration = songWithArtists.song.duration,
                        thumbnailUrl = songWithArtists.song.thumbnailUrl,
                    )
                } else if (!title.isNullOrBlank()) {
                    MediaMetadata(
                        id = mediaId,
                        title = title,
                        artists = listOf(MediaMetadata.Artist(id = null, name = artist.orEmpty())),
                        duration = durationSec,
                        thumbnailUrl = thumbnailUrl,
                    )
                } else null
                if (metadata != null) {
                    val preferredProvider = context.dataStore
                        .get(PreferredLyricsProviderKey)
                        .toEnum(PreferredLyricsProvider.SIMPMUSIC)
                    val fetched = lyricsHelper.getLyricsFromProvider(metadata, preferredProvider)
                    raw = fetched.lyrics
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = mediaId,
                                lyrics = fetched.lyrics,
                                provider = fetched.provider,
                            )
                        )
                    }
                }
            }
            if (!raw.isNullOrBlank() && raw != LYRICS_NOT_FOUND) {
                LyricsUtils.parseLyrics(raw)
                    .mapNotNull { entry ->
                        val text = entry.text.lineSequence().firstOrNull()?.trim().orEmpty()
                        if (text.isBlank()) return@mapNotNull null
                        val words = entry.words.orEmpty()
                            .map {
                                LiveWord(
                                    text = it.text,
                                    startMs = (it.startTime * 1000).toLong(),
                                    endMs = (it.endTime * 1000).toLong(),
                                )
                            }
                            .filter { it.text.isNotBlank() }
                        val end = when {
                            entry.endTime > 0L -> entry.endTime
                            words.isNotEmpty() -> words.maxOf { it.endMs }
                            else -> -1L
                        }
                        // Waktu mulai baris = kata pertama dinyanyikan (bukan awal jeda), supaya
                        // lirik tidak muncul/bergerak saat masih musik pembuka.
                        val firstWordStart = words.firstOrNull()?.startMs
                        val start = if (firstWordStart != null && firstWordStart > entry.time &&
                            firstWordStart - entry.time <= 2000L
                        ) firstWordStart else entry.time
                        LiveLine(startMs = start, endMs = end, text = text, words = words)
                    }
                    .sortedBy { it.startMs }
                    .let { lines -> fixSyllableSpacing(lines, mediaId, title, artist, durationSec) }
            } else {
                emptyList()
            }
        }.onFailure { Timber.e(it, "Live lyrics load failed") }
            .getOrDefault(emptyList())
    }
}

private val SYNCED_LINE_REGEX = Regex("\\[\\d{1,2}:\\d{2}\\.\\d{2,3}\\]")

/** True jika teks lirik terpotong per suku kata ("Ma af kan", "A ku ja tuh su ka"). */
private fun looksSyllableSplit(lines: List<LiveLine>): Boolean {
    var tokens = 0
    var chars = 0
    for (l in lines) {
        val parts = if (l.words.isNotEmpty()) l.words.map { it.text } else l.text.split(' ')
        for (p in parts) {
            val c = p.count { !it.isWhitespace() }
            if (c > 0) {
                tokens++
                chars += c
            }
        }
    }
    if (tokens < 20) return false
    return chars.toFloat() / tokens.toFloat() < 3.4f
}

private fun parseLineLevel(raw: String): List<Pair<Long, String>> =
    LyricsUtils.parseLyrics(raw)
        .map { it.time to it.text.lineSequence().firstOrNull()?.trim().orEmpty() }
        .filter { it.second.isNotBlank() }

/** Cari versi lirik per baris (teks utuh, spasi benar) dari penyedia lain. */
private suspend fun fetchLineLevelText(
    mediaId: String,
    title: String?,
    artist: String?,
    durationSec: Int,
): List<Pair<Long, String>> {
    val variants = mutableListOf<String>()
    runCatching { SimpMusicLyrics.getAllLyrics(mediaId, durationSec) { variants += it } }
    variants.firstOrNull { !it.contains('<') && SYNCED_LINE_REGEX.containsMatchIn(it) }?.let { raw ->
        val parsed = runCatching { parseLineLevel(raw) }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) return parsed
    }
    if (!title.isNullOrBlank()) {
        val raw = runCatching {
            LrcLibLyricsProvider.getLyrics(mediaId, title, artist.orEmpty(), durationSec, null).getOrNull()
        }.getOrNull()
        if (!raw.isNullOrBlank() && !raw.contains('<') && SYNCED_LINE_REGEX.containsMatchIn(raw)) {
            val parsed = runCatching { parseLineLevel(raw) }.getOrDefault(emptyList())
            if (parsed.isNotEmpty()) return parsed
        }
    }
    return emptyList()
}

private suspend fun fixSyllableSpacing(
    lines: List<LiveLine>,
    mediaId: String,
    title: String?,
    artist: String?,
    durationSec: Int,
): List<LiveLine> {
    if (!looksSyllableSplit(lines)) return lines
    val reference = fetchLineLevelText(mediaId, title, artist, durationSec)
    if (reference.isEmpty()) return lines

    val used = HashSet<Int>()
    return lines.map { line ->
        val lineNorm = normalizeForMatch(line.text)
        if (lineNorm.isEmpty()) return@map line
        var bestIdx = -1
        var bestScore = 0f
        for (i in reference.indices) {
            if (i in used) continue
            val ref = reference[i]
            if (kotlin.math.abs(ref.first - line.startMs) > 1500L) continue
            val score = similarityRatio(lineNorm, normalizeForMatch(ref.second))
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        // Hanya ganti teks jika huruf-hurufnya benar-benar sama (selain spasi/tanda baca),
        // sehingga baris tidak pernah tertukar dengan baris lain.
        if (bestIdx < 0 || bestScore < 0.9f) return@map line
        used += bestIdx
        line.copy(text = reference[bestIdx].second)
    }
}

private fun normalizeForMatch(s: String): String =
    s.lowercase().filter { it.isLetterOrDigit() }

private fun similarityRatio(a: String, b: String): Float {
    if (a == b) return 1f
    if (a.isEmpty() || b.isEmpty()) return 0f
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.length) {
            val tmp = dp[j]
            dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = tmp
        }
    }
    return 1f - dp[b.length].toFloat() / maxOf(a.length, b.length).toFloat()
}

/** Indeks baris yang sedang dinyanyikan (-1 sebelum baris pertama). */
fun List<LiveLine>.activeIndexAt(posMs: Long): Int = indexOfLast { it.startMs <= posMs }

/**
 * Progres pengisian karaoke (0..1) yang stabil dan tidak terlalu cepat:
 * - Untuk rich-sync per kata: merentangkan kata terakhir hingga mendekati baris berikutnya
 *   (maksimal +1500 ms) agar vokal yang ditahan tidak selesai lebih cepat dari suara penyanyi.
 * - Untuk sinkron per baris: mengikuti rentang waktu alami menuju baris berikutnya
 *   (dibatasi maksimal 6500 ms pada jeda instrumental panjang).
 */
fun LiveLine.karaokeFill(posMs: Long, nextStartMs: Long?): Float {
    if (posMs <= startMs) return 0f

    val naturalGap = ((nextStartMs ?: (startMs + 4500L)) - startMs).coerceAtLeast(400L)

    if (words.isNotEmpty()) {
        val lastIdx = words.lastIndex
        val extendedLineEnd = if (nextStartMs != null && nextStartMs > words[lastIdx].endMs) {
            minOf(nextStartMs - 80L, words[lastIdx].endMs + 1500L)
        } else {
            words[lastIdx].endMs + 400L
        }

        var total = 0
        var done = 0f
        for (i in words.indices) {
            val w = words[i]
            val len = w.text.count { !it.isWhitespace() }.coerceAtLeast(1)
            val wStart = w.startMs
            val wEnd = when {
                i == lastIdx -> maxOf(w.endMs, extendedLineEnd)
                else -> maxOf(w.endMs, minOf(words[i + 1].startMs, w.endMs + 350L))
            }.coerceAtLeast(wStart + 1L)

            total += len
            done += len * when {
                posMs >= wEnd -> 1f
                posMs <= wStart -> 0f
                else -> (posMs - wStart).toFloat() / (wEnd - wStart).toFloat()
            }
        }
        return if (total == 0) 0f else (done / total).coerceIn(0f, 1f)
    }

    val span = if (endMs > startMs) {
        val paddedEnd = if (nextStartMs != null && nextStartMs > endMs) {
            minOf(nextStartMs - 100L, endMs + 1200L)
        } else {
            endMs
        }
        minOf(paddedEnd - startMs, naturalGap, 6500L)
    } else {
        minOf(naturalGap, 6500L)
    }.coerceAtLeast(1L)

    return ((posMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
}
