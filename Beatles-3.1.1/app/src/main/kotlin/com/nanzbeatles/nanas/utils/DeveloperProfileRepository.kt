/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.utils

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

/**
 * ISI DENGAN LINK JSON PROFIL DEVELOPER (harus bisa dibuka publik, hasilnya JSON mentah).
 * Contoh: Firebase Realtime Database -> "https://NAMA-PROJECT-default-rtdb.firebaseio.com/developer.json"
 *         atau GitHub raw     -> "https://raw.githubusercontent.com/USER/REPO/main/developer.json"
 * Kosong = halaman Developer memakai data bawaan di bawah.
 */
const val DEVELOPER_PROFILE_URL: String = ""

/** Seberapa sering halaman Developer memeriksa pembaruan (selama halamannya terbuka). */
private const val DEVELOPER_POLL_INTERVAL_MS = 20_000L

data class DeveloperNowPlaying(
    val title: String,
    val artist: String,
    val artworkUrl: String,
)

data class DeveloperTopSong(
    val title: String,
    val artist: String,
    val plays: Int,
    val artworkUrl: String,
)

data class DeveloperProfile(
    val name: String = "ɪᴋʏ ᴠᴏʀᴛᴇx | 技術者",
    val role: String = "Developer",
    val avatarUrl: String = "https://files.catbox.moe/wjsgt5.jpg",
    /** Gambar atau video latar. Kalau backgroundIsVideo = true, harus link .mp4 */
    val backgroundUrl: String = "https://lunara.softbotz.my.id/f/q4h7pILeRjvo.mp4",
    val backgroundIsVideo: Boolean = true,
    val level: Int = 10,
    val xp: Int = 100_080,
    val xpTarget: Int = 105_000,
    val streakDays: Int = 999,
    val dailyRecordHours: Int = 999,
    val monthlyRecordHours: Int = 999,
    val nowPlaying: DeveloperNowPlaying? = null,
    val topSongs: List<DeveloperTopSong> = emptyList(),
)

/*
 * Contoh isi JSON (semua field opsional, yang kosong memakai data bawaan):
 * {
 *   "name": "IKY VORTEX | 技術者",
 *   "role": "Developer",
 *   "avatarUrl": "https://.../pp.jpg",
 *   "backgroundUrl": "https://.../bg.jpg",
 *   "backgroundIsVideo": false,
 *   "level": 10, "xp": 100077, "xpTarget": 105000,
 *   "streakDays": 999, "dailyRecordHours": 999, "monthlyRecordHours": 999,
 *   "nowPlaying": { "title": "Bahagia Lagi", "artist": "Piche Kota", "artworkUrl": "https://..." },
 *   "topSongs": [ { "title": "Bahagia Lagi", "artist": "Piche Kota", "plays": 2, "artworkUrl": "https://..." } ]
 * }
 */
object DeveloperProfileRepository {
    private val _state = MutableStateFlow(DeveloperProfile())
    val state: StateFlow<DeveloperProfile> = _state.asStateFlow()

    /** Dipanggil dari LaunchedEffect: berhenti otomatis saat halaman Developer ditutup. */
    suspend fun pollWhileActive() {
        if (DEVELOPER_PROFILE_URL.isBlank()) return
        while (coroutineContext.isActive) {
            fetchOnce()?.let { _state.value = it }
            delay(DEVELOPER_POLL_INTERVAL_MS)
        }
    }

    private suspend fun fetchOnce(): DeveloperProfile? = withContext(Dispatchers.IO) {
        runCatching {
            val sep = if (DEVELOPER_PROFILE_URL.contains("?")) "&" else "?"
            val conn = (URL("$DEVELOPER_PROFILE_URL${sep}t=${System.currentTimeMillis()}")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun parse(raw: String): DeveloperProfile {
        val d = DeveloperProfile()
        val o = JSONObject(raw)

        fun str(key: String, def: String) = o.optString(key, def).takeIf { it.isNotBlank() } ?: def

        val now = o.optJSONObject("nowPlaying")?.let {
            val t = it.optString("title")
            if (t.isBlank()) null
            else DeveloperNowPlaying(t, it.optString("artist"), it.optString("artworkUrl"))
        }
        val tops = buildList {
            val arr = o.optJSONArray("topSongs") ?: return@buildList
            for (i in 0 until minOf(arr.length(), 5)) {
                val s = arr.optJSONObject(i) ?: continue
                val t = s.optString("title")
                if (t.isBlank()) continue
                add(DeveloperTopSong(t, s.optString("artist"), s.optInt("plays", 0), s.optString("artworkUrl")))
            }
        }

        return DeveloperProfile(
            name = str("name", d.name),
            role = str("role", d.role),
            avatarUrl = str("avatarUrl", d.avatarUrl),
            backgroundUrl = str("backgroundUrl", d.backgroundUrl),
            backgroundIsVideo = if (o.has("backgroundIsVideo")) o.optBoolean("backgroundIsVideo") else
                str("backgroundUrl", d.backgroundUrl).substringBefore('?').endsWith(".mp4", true),
            level = o.optInt("level", d.level).coerceIn(1, 10),
            xp = o.optInt("xp", d.xp),
            xpTarget = o.optInt("xpTarget", d.xpTarget).coerceAtLeast(1),
            streakDays = o.optInt("streakDays", d.streakDays),
            dailyRecordHours = o.optInt("dailyRecordHours", d.dailyRecordHours),
            monthlyRecordHours = o.optInt("monthlyRecordHours", d.monthlyRecordHours),
            nowPlaying = now,
            topSongs = tops,
        )
    }
}
