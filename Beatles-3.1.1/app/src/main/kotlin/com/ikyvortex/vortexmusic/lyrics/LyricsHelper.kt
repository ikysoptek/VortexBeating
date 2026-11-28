/**
 * Vortex Music Project (C) 2026
 */

package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import android.util.LruCache
import androidx.datastore.preferences.core.Preferences
import com.ikyvortex.vortexmusic.constants.LyricsProviderOrderKey
import com.ikyvortex.vortexmusic.constants.PreferredLyricsProvider
import com.ikyvortex.vortexmusic.constants.PreferredLyricsProviderKey
import com.ikyvortex.vortexmusic.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.ikyvortex.vortexmusic.extensions.toEnum
import com.ikyvortex.vortexmusic.models.MediaMetadata
import com.ikyvortex.vortexmusic.utils.NetworkConnectivityObserver
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

private const val MAX_LYRICS_FETCH_MS = 30_000L
private const val PER_PROVIDER_TIMEOUT_MS = 6_000L
private const val PROVIDER_NONE = "Unknown"

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val cache = LruCache<String, LyricsWithProvider>(MAX_CACHE_SIZE)
    private val allLyricsCache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val orderedProviders = resolveOrderedProviders()
        val preferredProviderName = orderedProviders.firstOrNull()?.name ?: PROVIDER_NONE

        cache.get(mediaMetadata.id)?.let { cached ->
            if (cached.lyrics != LYRICS_NOT_FOUND && cached.provider == preferredProviderName) {
                Timber.tag(TAG).d("Returning cached lyrics from preferred provider ${cached.provider}")
                return cached
            }
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) {
            Timber.tag(TAG).w("Network not available, returning not found")
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
        val artistString = mediaMetadata.artists.joinToString { it.name }
        val durationSeconds = normalizeDurationSeconds(mediaMetadata.duration)
        val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

        Timber.tag(TAG).d("Searching lyrics for: '$cleanedTitle' by '$artistString' (${durationSeconds}s)")
        Timber.tag(TAG).d("Enabled providers in order: ${enabledProviders.joinToString { it.name }}")

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            var fallbackCandidate: LyricsWithProvider? = null

            for (provider in enabledProviders) {
                Timber.tag(TAG).d("Trying provider: ${provider.name}")
                val providerResult = try {
                    withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            cleanedTitle,
                            artistString,
                            durationSeconds,
                            mediaMetadata.album?.title,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w("${provider.name} threw: ${e.message}")
                    reportException(e)
                    null
                }

                if (providerResult != null && providerResult.isSuccess) {
                    val lyrics = providerResult.getOrNull()
                    if (isValidLyricsContent(lyrics)) {
                        if (isLyricsCompatibleWithTrack(lyrics!!, artistString, durationSeconds) &&
                            !isWrongLanguageForTrack(lyrics, mediaMetadata.title, artistString, mediaMetadata.album?.title)
                        ) {
                            Timber.tag(TAG).i("Got duration-matched lyrics from ${provider.name}")
                            return@withTimeoutOrNull LyricsWithProvider(lyrics, provider.name)
                        } else {
                            Timber.tag(TAG).w("${provider.name} lyrics rejected due to duration/artist mismatch, trying next provider")
                            if (fallbackCandidate == null) {
                                fallbackCandidate = LyricsWithProvider(lyrics, provider.name)
                            }
                        }
                    } else {
                        Timber.tag(TAG).w("${provider.name} returned success with empty/NOT_FOUND body")
                    }
                } else {
                    val errorMsg = providerResult?.exceptionOrNull()?.message ?: "timeout or no result"
                    Timber.tag(TAG).w("${provider.name} failed: $errorMsg")
                }
            }

            if (fallbackCandidate != null) {
                Timber.tag(TAG).w("Using fallback lyrics from ${fallbackCandidate.provider}")
                return@withTimeoutOrNull fallbackCandidate
            }

            Timber.tag(TAG).w("All providers failed for '${mediaMetadata.title}'")
            LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        } ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)

        if (result.lyrics != LYRICS_NOT_FOUND) {
            cache.put(mediaMetadata.id, result)
        }
        return result
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        allLyricsCache.get(cacheKey)?.let { results ->
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        val orderedProviders = resolveOrderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            orderedProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            if (isValidLyricsContent(lyrics)) {
                                val result = LyricsResult(provider.name, lyrics)
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            allLyricsCache.put(cacheKey, allResult)
        }
        currentLyricsJob?.join()
    }

    fun invalidateCache(mediaId: String?) {
        if (mediaId == null) {
            cache.evictAll()
            allLyricsCache.evictAll()
        } else {
            cache.remove(mediaId)
        }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    suspend fun getLyricsFromProvider(
        mediaMetadata: MediaMetadata,
        preferred: PreferredLyricsProvider,
    ): LyricsWithProvider {
        currentLyricsJob?.cancel()
        invalidateCache(mediaMetadata.id)

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
        val artistString = mediaMetadata.artists.joinToString { it.name }
        val durationSeconds = normalizeDurationSeconds(mediaMetadata.duration)

        val primaryProvider = providerInstanceOf(preferred)
        val fallbackOrder = listOf(
            primaryProvider,
            LrcLibLyricsProvider,
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            RushLyricsProvider,
            PaxsenixLyricsProvider,
            MusixmatchLyricsProvider,
            KuGouLyricsProvider,
        ).distinctBy { it.name }

        val providersToTry = fallbackOrder.filter { it.name == primaryProvider.name || it.isEnabled(context) }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            var directFallback: LyricsWithProvider? = null
            for (provider in providersToTry) {
                Timber.tag(TAG).d("Direct provider switch trying: ${provider.name}")
                val providerResult = try {
                    withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            cleanedTitle,
                            artistString,
                            durationSeconds,
                            mediaMetadata.album?.title,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w("${provider.name} threw: ${e.message}")
                    reportException(e)
                    null
                }

                if (providerResult != null && providerResult.isSuccess) {
                    val lyrics = providerResult.getOrNull()
                    if (isValidLyricsContent(lyrics)) {
                        // Pastikan lirik cocok dengan lagu (durasi/artis/bahasa) supaya tidak
                        // salah versi (intro beda) atau salah bahasa. Jika tidak ada yang cocok,
                        // pakai kandidat pertama.
                        if (isLyricsCompatibleWithTrack(lyrics!!, artistString, durationSeconds) &&
                            !isWrongLanguageForTrack(lyrics, mediaMetadata.title, artistString, mediaMetadata.album?.title)
                        ) {
                            Timber.tag(TAG).i("Direct switch got compatible lyrics from ${provider.name}")
                            return@withTimeoutOrNull LyricsWithProvider(lyrics, provider.name)
                        }
                        if (directFallback == null) {
                            directFallback = LyricsWithProvider(lyrics, provider.name)
                        }
                    }
                }
            }
            directFallback ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        } ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)

        if (result.lyrics != LYRICS_NOT_FOUND) {
            cache.put(mediaMetadata.id, result)
        }
        return result
    }

    private suspend fun resolveOrderedProviders(): List<LyricsProvider> {
        val preferences = context.dataStore.data
            .map { it }
            .first()
        return resolveOrderedProviders(preferences)
    }

    private fun resolveOrderedProviders(preferences: Preferences): List<LyricsProvider> {
        val defaultOrder = LyricsProviderRegistry.serializeProviderOrder(
            LyricsProviderRegistry.getDefaultProviderOrder()
        )
        val providerOrder = preferences[LyricsProviderOrderKey] ?: defaultOrder
        val preferredProvider = preferences[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.LRCLIB)

        val preferredInstance = providerInstanceOf(preferredProvider)

        return if (providerOrder != defaultOrder) {
            val customOrder = LyricsProviderRegistry.getOrderedProviders(providerOrder)
            val rest = customOrder.filter { it.name != preferredInstance.name }
            listOf(preferredInstance) + rest
        } else {
            when (preferredProvider) {
                PreferredLyricsProvider.LRCLIB -> listOf(
                    LrcLibLyricsProvider, SimpMusicLyricsProvider, BetterLyricsProvider,
                    RushLyricsProvider, PaxsenixLyricsProvider, MusixmatchLyricsProvider,
                    KuGouLyricsProvider,
                )
                PreferredLyricsProvider.SIMPMUSIC -> listOf(
                    SimpMusicLyricsProvider, LrcLibLyricsProvider, BetterLyricsProvider,
                    RushLyricsProvider, PaxsenixLyricsProvider, MusixmatchLyricsProvider,
                    KuGouLyricsProvider,
                )
                PreferredLyricsProvider.KUGOU -> listOf(
                    KuGouLyricsProvider, LrcLibLyricsProvider, SimpMusicLyricsProvider,
                    BetterLyricsProvider, RushLyricsProvider, PaxsenixLyricsProvider,
                    MusixmatchLyricsProvider,
                )
                PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                    BetterLyricsProvider, LrcLibLyricsProvider, SimpMusicLyricsProvider,
                    RushLyricsProvider, PaxsenixLyricsProvider, MusixmatchLyricsProvider,
                    KuGouLyricsProvider,
                )
                PreferredLyricsProvider.RUSH_LYRICS -> listOf(
                    RushLyricsProvider, LrcLibLyricsProvider, SimpMusicLyricsProvider,
                    BetterLyricsProvider, PaxsenixLyricsProvider, MusixmatchLyricsProvider,
                    KuGouLyricsProvider,
                )
                PreferredLyricsProvider.PAXSENIX -> listOf(
                    PaxsenixLyricsProvider, LrcLibLyricsProvider, SimpMusicLyricsProvider,
                    BetterLyricsProvider, RushLyricsProvider, MusixmatchLyricsProvider,
                    KuGouLyricsProvider,
                )
                PreferredLyricsProvider.MUSIXMATCH -> listOf(
                    MusixmatchLyricsProvider, LrcLibLyricsProvider, SimpMusicLyricsProvider,
                    BetterLyricsProvider, RushLyricsProvider, PaxsenixLyricsProvider,
                    KuGouLyricsProvider,
                )
            }
        }
    }

    private fun providerInstanceOf(preferred: PreferredLyricsProvider): LyricsProvider = when (preferred) {
        PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
        PreferredLyricsProvider.RUSH_LYRICS -> RushLyricsProvider
        PreferredLyricsProvider.PAXSENIX -> PaxsenixLyricsProvider
        PreferredLyricsProvider.MUSIXMATCH -> MusixmatchLyricsProvider
    }

    companion object {
        private const val TAG = "LyricsHelper"
        private const val MAX_CACHE_SIZE = 3

        fun providerNameOf(preferred: PreferredLyricsProvider): String = when (preferred) {
            PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider.name
            PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider.name
            PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider.name
            PreferredLyricsProvider.RUSH_LYRICS -> RushLyricsProvider.name
            PreferredLyricsProvider.PAXSENIX -> PaxsenixLyricsProvider.name
            PreferredLyricsProvider.MUSIXMATCH -> MusixmatchLyricsProvider.name
            PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider.name
        }

        fun displayNameOf(preferred: PreferredLyricsProvider): String = when (preferred) {
            PreferredLyricsProvider.SIMPMUSIC -> "SimpMusic"
            PreferredLyricsProvider.BETTER_LYRICS -> "Better Lyrics"
            PreferredLyricsProvider.LRCLIB -> "LrcLib"
            PreferredLyricsProvider.RUSH_LYRICS -> "RushLyrics"
            PreferredLyricsProvider.PAXSENIX -> "Paxsenix"
            PreferredLyricsProvider.MUSIXMATCH -> "Musixmatch"
            PreferredLyricsProvider.KUGOU -> "KuGou"
        }

        fun subtitleOf(preferred: PreferredLyricsProvider): String = when (preferred) {
            PreferredLyricsProvider.SIMPMUSIC -> "Penyedia Utama • Akurat & Cepat"
            PreferredLyricsProvider.BETTER_LYRICS -> "Sinkronisasi Per Kata (Apple Music Style)"
            PreferredLyricsProvider.LRCLIB -> "Database LRC Komunitas Terbuka"
            PreferredLyricsProvider.RUSH_LYRICS -> "Lirik Sinkron Cepat"
            PreferredLyricsProvider.PAXSENIX -> "Multi-Source Synced Lyrics"
            PreferredLyricsProvider.MUSIXMATCH -> "Katalog Lirik Musik Global"
            PreferredLyricsProvider.KUGOU -> "Katalog Asia & Internasional"
        }

        fun fromProviderName(name: String?): PreferredLyricsProvider? = when (name?.trim()) {
            SimpMusicLyricsProvider.name, "SimpMusic" -> PreferredLyricsProvider.SIMPMUSIC
            BetterLyricsProvider.name, "BetterLyrics", "Better Lyrics" -> PreferredLyricsProvider.BETTER_LYRICS
            LrcLibLyricsProvider.name, "LrcLib" -> PreferredLyricsProvider.LRCLIB
            RushLyricsProvider.name, "RushLyrics" -> PreferredLyricsProvider.RUSH_LYRICS
            PaxsenixLyricsProvider.name, "Paxsenix" -> PreferredLyricsProvider.PAXSENIX
            MusixmatchLyricsProvider.name, "Musixmatch" -> PreferredLyricsProvider.MUSIXMATCH
            KuGouLyricsProvider.name, "KuGou" -> PreferredLyricsProvider.KUGOU
            else -> null
        }

        /** Durasi lagu di database & metadata dalam DETIK; jika ternyata milidetik, ubah ke detik. */
        fun normalizeDurationSeconds(raw: Int): Int =
            if (raw > 10_000) raw / 1000 else raw

        private val ENGLISH_WORDS = setOf(
            "the", "and", "you", "your", "that", "with", "for", "are", "this", "but", "not", "have",
            "was", "just", "know", "love", "baby", "like", "all", "she", "his", "her", "they", "when",
            "what", "can", "will", "would", "don't", "i'm", "it's", "me", "my", "we", "us", "of", "in",
            "on", "to", "is", "it", "be", "so", "do", "no", "got", "want", "never", "ever", "oh",
        )
        private val INDONESIAN_WORDS = setOf(
            "yang", "dan", "aku", "kamu", "kau", "dia", "tak", "tidak", "cinta", "rindu", "hati",
            "dalam", "waktu", "untuk", "dengan", "pada", "ini", "itu", "ada", "sama", "mau", "bisa",
            "akan", "masih", "lagi", "jangan", "saat", "hari", "malam", "kita", "kami", "mereka",
            "sudah", "belum", "pernah", "apa", "bukan", "seperti", "tapi", "hanya", "sebuah", "dunia",
            "hidup", "rasa", "kasih", "sayang", "pergi", "pulang", "menunggu", "terbuang", "jalan",
            "di", "ke", "dari", "oleh", "atau", "karena", "biar", "supaya", "agar", "kini", "nanti",
        )

        private fun wordsOf(text: String): List<String> =
            text.lowercase().split(Regex("[^a-z']+")).filter { it.isNotBlank() }

        /**
         * True jika lirik didominasi bahasa Inggris padahal judul/artis/album terlihat berbahasa
         * Indonesia (penyebab lagu Indonesia tampil dengan lirik bahasa Inggris).
         */
        fun isWrongLanguageForTrack(
            lyrics: String,
            title: String?,
            artist: String?,
            album: String?,
        ): Boolean {
            val meta = wordsOf("${title.orEmpty()} ${artist.orEmpty()} ${album.orEmpty()}")
            val titleLooksIndonesian = meta.any { it in INDONESIAN_WORDS && it !in ENGLISH_WORDS && it.length > 2 }
            if (!titleLooksIndonesian) return false
            val body = lyrics.lineSequence()
                .map { it.replace(Regex("<[^>]*>"), " ").replace(Regex("\\[[^\\]]*\\]"), " ") }
                .take(60)
                .joinToString(" ")
            val words = wordsOf(body)
            if (words.size < 20) return false
            val en = words.count { it in ENGLISH_WORDS }
            val id = words.count { it in INDONESIAN_WORDS }
            return en >= 8 && en > id * 3
        }

        /**
         * Validates that LRC lyrics match the track's artist metadata (if [ar:] tag is present)
         * and that the final lyric timestamp does not exceed or fall short of the song duration
         * by an unreasonable margin (prevents wrong-song collisions like Gamma1 vs Javanese covers).
         */
        fun isLyricsCompatibleWithTrack(
            lyrics: String,
            expectedArtist: String,
            expectedDurationSec: Int,
        ): Boolean {
            val lines = lyrics.lines()
            val arTag = lines.firstOrNull { it.trim().startsWith("[ar:", ignoreCase = true) }
                ?.substringAfter(":")?.substringBefore("]")?.trim()

            if (!arTag.isNullOrBlank() && expectedArtist.isNotBlank()) {
                val normExpected = expectedArtist.lowercase().replace(Regex("[^a-z0-9]"), "")
                val normTag = arTag.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (normExpected.length >= 3 && normTag.length >= 3 &&
                    !normExpected.contains(normTag) && !normTag.contains(normExpected)
                ) {
                    return false
                }
            }

            if (expectedDurationSec > 20) {
                val timeRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
                var maxLyricSec = 0
                for (line in lines) {
                    val match = timeRegex.find(line) ?: continue
                    val min = match.groupValues[1].toIntOrNull() ?: 0
                    val sec = match.groupValues[2].toIntOrNull() ?: 0
                    val totalSec = min * 60 + sec
                    if (totalSec > maxLyricSec) {
                        maxLyricSec = totalSec
                    }
                }
                if (maxLyricSec > 0) {
                    // Tolak lirik versi lain (live/remix/durasi beda): berakhir > 12 dtk setelah lagu
                    // selesai, atau berakhir terlalu cepat (< 55% durasi lagu).
                    if (maxLyricSec > expectedDurationSec + 6) return false
                    if (maxLyricSec < (expectedDurationSec * 0.55).toInt()) return false
                }
                // Tag [length:mm:ss] dari penyedia lirik: harus cocok dengan durasi lagu (+-10 dtk)
                val lengthTag = lines.firstOrNull { it.trim().startsWith("[length:", ignoreCase = true) }
                    ?.substringAfter(":")?.substringBefore("]")?.trim()
                val lengthParts = lengthTag?.split(":")
                if (lengthParts != null && lengthParts.size == 2) {
                    val lm = lengthParts[0].trim().toIntOrNull()
                    val ls = lengthParts[1].trim().toIntOrNull()
                    if (lm != null && ls != null) {
                        val tagSec = lm * 60 + ls
                        if (tagSec > 20 && kotlin.math.abs(tagSec - expectedDurationSec) > 10) return false
                    }
                }
            }
            return true
        }

        fun isValidLyricsContent(lyrics: String?): Boolean {
            if (lyrics.isNullOrBlank() || lyrics == LYRICS_NOT_FOUND) return false
            val lines = lyrics.lines()
            return lines.any { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank()) return@any false
                if (line.startsWith("[ti:", ignoreCase = true) ||
                    line.startsWith("[ar:", ignoreCase = true) ||
                    line.startsWith("[al:", ignoreCase = true) ||
                    line.startsWith("[by:", ignoreCase = true) ||
                    line.startsWith("[offset:", ignoreCase = true) ||
                    line.startsWith("[re:", ignoreCase = true) ||
                    line.startsWith("[ve:", ignoreCase = true) ||
                    line.startsWith("[length:", ignoreCase = true) ||
                    line.startsWith("[kana:", ignoreCase = true) ||
                    line.startsWith("[id:", ignoreCase = true) ||
                    line.startsWith("[au:", ignoreCase = true)) {
                    return@any false
                }
                val withoutTags = line.replace(Regex("\\[[0-9:.]+\\]"), "")
                    .replace(Regex("<[0-9:.]*>"), "")
                    .replace("♪", "")
                    .trim()
                withoutTags.isNotBlank()
            }
        }
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
