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

    /** Parse langsung dari raw string LRC agar 100% identik dengan pemutar utama. */
    fun parseRawLines(raw: String?): List<LiveLine> {
        if (raw.isNullOrBlank() || raw == LYRICS_NOT_FOUND || !raw.trimStart().startsWith("[")) {
            return emptyList()
        }
        return runCatching {
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
        }.getOrDefault(emptyList())
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
            parseRawLines(raw)
        }.onFailure { Timber.e(it, "Live lyrics load failed") }
            .getOrDefault(emptyList())
    }
}

/**
 * Mengembalikan indeks baris yang BENAR-BENAR sedang dinyanyikan saat [posMs].
 * Mengembalikan -1 jika:
 * - Lagu masih dalam intro musik sebelum bait pertama mulai.
 * - Sedang dalam jeda musik instrumental panjang antar bait (setelah baris selesai + 1.5 detik).
 */
fun List<LiveLine>.activeIndexAt(posMs: Long): Int {
    if (isEmpty()) return -1
    val idx = indexOfLast { it.startMs <= posMs }
    if (idx < 0) return -1

    val current = this[idx]
    val next = getOrNull(idx + 1)
    val lineFinishMs = when {
        current.endMs > current.startMs -> current.endMs + 1200L
        next != null -> minOf(next.startMs, current.startMs + 6500L)
        else -> current.startMs + 5500L
    }
    if (posMs > lineFinishMs) return -1
    return idx
}

/**
 * Progres pengisian karaoke (0..1) murni mengikuti timestamp vokal asli.
 */
fun LiveLine.karaokeFill(posMs: Long, nextStartMs: Long?): Float {
    if (posMs <= startMs) return 0f

    if (words.isNotEmpty()) {
        var total = 0
        var done = 0f
        for (w in words) {
            val len = w.text.count { !it.isWhitespace() }.coerceAtLeast(1)
            val wEnd = w.endMs.coerceAtLeast(w.startMs + 1L)
            total += len
            done += len * when {
                posMs >= wEnd -> 1f
                posMs <= w.startMs -> 0f
                else -> (posMs - w.startMs).toFloat() / (wEnd - w.startMs).toFloat()
            }
        }
        return if (total == 0) 0f else (done / total).coerceIn(0f, 1f)
    }

    val span = when {
        endMs > startMs -> (endMs - startMs)
        nextStartMs != null && nextStartMs > startMs -> minOf(nextStartMs - startMs, 5500L)
        else -> 4000L
    }.coerceAtLeast(1L)

    return ((posMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
}
