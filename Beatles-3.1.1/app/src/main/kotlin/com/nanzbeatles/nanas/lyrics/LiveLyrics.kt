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
                    // Lagu belum ada di database: pakai metadata dari player
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
                        LiveLine(startMs = entry.time, endMs = end, text = text, words = words)
                    }
                    .let { lines -> fixSyllableSpacing(lines, mediaId, title, artist, durationSec) }
            } else {
                emptyList()
            }
        }.onFailure { Timber.e(it, "Live lyrics load failed") }
            .getOrDefault(emptyList())
    }
}

private val SYNCED_LINE_REGEX = Regex("\\[\\d{1,2}:\\d{2}\\.\\d{2,3}\\]")

/** True jika teks lirik terpotong per suku kata ("Ber san ding ber sa ma di ri nya"). */
private fun looksSyllableSplit(lines: List<LiveLine>): Boolean {
    var tokens = 0
    var chars = 0
    for (l in lines) {
        for (w in l.words) {
            tokens++
            chars += w.text.count { !it.isWhitespace() }
        }
    }
    if (tokens < 30) return false
    return chars.toFloat() / tokens.toFloat() < 3.3f
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
    // 1) SimpMusic: varian non-rich dari lagu yang sama
    val variants = mutableListOf<String>()
    runCatching { SimpMusicLyrics.getAllLyrics(mediaId, durationSec) { variants += it } }
    variants.firstOrNull { !it.contains('<') && SYNCED_LINE_REGEX.containsMatchIn(it) }?.let { raw ->
        val parsed = runCatching { parseLineLevel(raw) }.getOrDefault(emptyList())
        if (parsed.isNotEmpty()) return parsed
    }
    // 2) LRCLIB
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

/**
 * Jika lirik rich-sync terpotong per suku kata, pakai teks utuh dari versi per baris,
 * tapi waktu per kata tetap dari rich-sync (supaya isi karaoke tetap pas dengan suara).
 */
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

    return lines.map { line ->
        val match = reference.minByOrNull { kotlin.math.abs(it.first - line.startMs) }
        if (match == null || kotlin.math.abs(match.first - line.startMs) > 1500L) return@map line
        val a = line.text.count { !it.isWhitespace() }
        val b = match.second.count { !it.isWhitespace() }
        // Pastikan itu memang baris yang sama (jumlah huruf kira-kira sama)
        if (kotlin.math.abs(a - b) > maxOf(3, (a * 0.35f).toInt())) line
        else line.copy(text = match.second)
    }
}

/** Indeks baris yang sedang dinyanyikan (-1 sebelum baris pertama). */
fun List<LiveLine>.activeIndexAt(posMs: Long): Int = indexOfLast { it.startMs <= posMs }

/**
 * Seberapa banyak baris ini sudah dinyanyikan (0..1).
 * - Jika ada waktu per kata: mengikuti kata demi kata, jadi saat penyanyi JEDA
 *   isinya ikut berhenti (tidak lanjut sendiri).
 * - Jika hanya per baris: diperkirakan dari panjang teks, dibatasi sampai baris berikutnya.
 */
fun LiveLine.karaokeFill(posMs: Long, nextStartMs: Long?): Float {
    if (posMs <= startMs) return 0f

    if (words.isNotEmpty()) {
        var total = 0
        var done = 0f
        for (w in words) {
            val len = w.text.count { !it.isWhitespace() }.coerceAtLeast(1)
            total += len
            done += len * when {
                posMs >= w.endMs -> 1f
                posMs <= w.startMs -> 0f
                else -> (posMs - w.startMs).toFloat() / (w.endMs - w.startMs).coerceAtLeast(1L).toFloat()
            }
        }
        return if (total == 0) 0f else (done / total).coerceIn(0f, 1f)
    }

    val gap = ((nextStartMs ?: (startMs + 4000L)) - startMs).coerceAtLeast(1L)
    val span = if (endMs > startMs) {
        minOf(endMs - startMs, gap)
    } else {
        minOf(gap, (text.length * 85L).coerceIn(1500L, 7000L))
    }.coerceAtLeast(1L)
    return ((posMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
}
