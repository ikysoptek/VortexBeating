/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.streak

import android.content.Context
import android.net.Uri
import com.nanzbeatles.nanas.constants.AccountEmailKey
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

data class DeveloperRealtimeState(
    val displayName: String = "ɪᴋʏ ᴠᴏʀᴛᴇx | 技術者",
    val role: String = "Developer",
    val avatarUrl: String = DEFAULT_AVATAR_URL,
    val videoUrl: String = DEFAULT_VIDEO_URL,
    val level: Int = 10,
    val levelTitle: String = "Ikon Musik",
    val levelSubtitle: String = "Puncak tertinggi pencinta musik sejati!",
    val xp: Int = 100077,
    val xpTarget: Int = 105000,
    val streakDays: Int = 999,
    val dailyRecordText: String = "999 Jam",
    val monthlyRecordText: String = "999 Jam",
    val nowPlayingTitle: String = "",
    val nowPlayingArtist: String = "",
    val nowPlayingThumb: String = "",
    val isPlaying: Boolean = false,
    val topSongs: List<TopSongStatItem> = emptyList(),
) {
    companion object {
        const val DEFAULT_AVATAR_URL = "https://files.catbox.moe/wjsgt5.jpg"
        const val DEFAULT_VIDEO_URL = "https://files.catbox.moe/483jh1.mp4"
    }
}

object DeveloperRealtimeManager {
    const val DEVELOPER_EMAIL = "ikyvortex12@gmail.com"

    private const val SUPABASE_REST_URL = "https://aglwqdrunfsepsrvfjma.supabase.co/rest/v1"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFnbHdxZHJ1bmZzZXBzcnZmam1hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUzMTAzNjQsImV4cCI6MjExMDg4NjM2NH0.auoxFHN4mm7gS44Lgk1sr1LeaHSefb_z7s987DaJ2Yg"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(DeveloperRealtimeState())
    val state: StateFlow<DeveloperRealtimeState> = _state.asStateFlow()

    private var lastSyncedSongId: String? = null
    private var lastSyncedPlayingState: Boolean? = null

    fun isCurrentUserDeveloper(context: Context): Boolean {
        val email = context.dataStore.get(AccountEmailKey, "").trim()
        return email.equals(DEVELOPER_EMAIL, ignoreCase = true)
    }

    fun fetchLatestDeveloperProfile() {
        scope.launch {
            runCatching {
                val request = Request.Builder()
                    .url("$SUPABASE_REST_URL/developer_realtime_profile?id=eq.iky_vortex&select=*")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string().orEmpty()
                    val arr = JSONArray(body)
                    if (arr.length() == 0) return@use
                    val obj = arr.getJSONObject(0)

                    val topSongsParsed = mutableListOf<TopSongStatItem>()
                    val songsJsonStr = obj.optString("top_songs_json", "[]")
                    runCatching {
                        val songsArr = JSONArray(songsJsonStr)
                        for (i in 0 until songsArr.length()) {
                            val s = songsArr.getJSONObject(i)
                            topSongsParsed.add(
                                TopSongStatItem(
                                    songId = s.optString("songId"),
                                    title = s.optString("title"),
                                    artist = s.optString("artist"),
                                    playCount = s.optInt("playCount", 1),
                                    listeningSeconds = s.optLong("listeningSeconds", 60L),
                                    thumbnailUrl = s.optString("thumbnailUrl").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }

                    _state.value = DeveloperRealtimeState(
                        displayName = obj.optString("display_name").ifBlank { "ɪᴋʏ ᴠᴏʀᴛᴇx | 技術者" },
                        role = obj.optString("role").ifBlank { "Developer" },
                        avatarUrl = obj.optString("avatar_url").ifBlank { DeveloperRealtimeState.DEFAULT_AVATAR_URL },
                        videoUrl = obj.optString("video_url").ifBlank { DeveloperRealtimeState.DEFAULT_VIDEO_URL },
                        level = obj.optInt("level", 10),
                        levelTitle = obj.optString("level_title").ifBlank { "Ikon Musik" },
                        levelSubtitle = obj.optString("level_subtitle").ifBlank { "Puncak tertinggi pencinta musik sejati!" },
                        xp = obj.optInt("xp", 100077),
                        xpTarget = obj.optInt("xp_target", 105000),
                        streakDays = obj.optInt("streak_days", 999),
                        dailyRecordText = obj.optString("daily_record_text").ifBlank { "999 Jam" },
                        monthlyRecordText = obj.optString("monthly_record_text").ifBlank { "999 Jam" },
                        nowPlayingTitle = obj.optString("now_playing_title", ""),
                        nowPlayingArtist = obj.optString("now_playing_artist", ""),
                        nowPlayingThumb = obj.optString("now_playing_thumb", ""),
                        isPlaying = obj.optBoolean("is_playing", false),
                        topSongs = topSongsParsed
                    )
                }
            }.onFailure {
                Timber.w(it, "DeveloperRealtimeManager: failed to fetch profile")
            }
        }
    }

    suspend fun uploadMediaToCatbox(file: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@withContext null
            val mimeType = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart(
                    "fileToUpload",
                    file.name,
                    file.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val result = response.body?.string()?.trim().orEmpty()
                if (response.isSuccessful && result.startsWith("https://")) result else null
            }
        }.getOrNull()
    }

    fun syncMediaChangeIfDeveloper(
        context: Context,
        avatarLocalUri: String,
        bannerLocalUri: String,
        bannerIsVideo: Boolean,
    ) {
        if (!isCurrentUserDeveloper(context)) return
        scope.launch {
            var uploadedAvatarUrl: String? = null
            var uploadedVideoUrl: String? = null

            if (avatarLocalUri.startsWith("file://")) {
                val path = Uri.parse(avatarLocalUri.substringBefore("?t=")).path
                if (path != null) {
                    uploadedAvatarUrl = uploadMediaToCatbox(File(path))
                }
            }

            if (bannerIsVideo && bannerLocalUri.startsWith("file://")) {
                val path = Uri.parse(bannerLocalUri.substringBefore("?t=")).path
                if (path != null) {
                    uploadedVideoUrl = uploadMediaToCatbox(File(path))
                }
            }

            val current = _state.value
            val newAvatar = uploadedAvatarUrl ?: current.avatarUrl
            val newVideo = uploadedVideoUrl ?: current.videoUrl
            _state.value = current.copy(avatarUrl = newAvatar, videoUrl = newVideo)

            patchDeveloperRow(
                JSONObject().apply {
                    put("avatar_url", newAvatar)
                    put("video_url", newVideo)
                    put("updated_at", java.time.Instant.now().toString())
                }
            )
        }
    }

    fun syncPlaybackIfDeveloper(
        context: Context,
        songId: String?,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        isPlaying: Boolean,
    ) {
        if (!isCurrentUserDeveloper(context)) return
        if (songId == lastSyncedSongId && isPlaying == lastSyncedPlayingState) return
        lastSyncedSongId = songId
        lastSyncedPlayingState = isPlaying

        scope.launch {
            val journey = MusicJourneyManager.state.value
            val streak = StreakManager.state.value

            val topSongsJson = JSONArray().apply {
                journey.topSongs.take(5).forEach { item ->
                    put(
                        JSONObject().apply {
                            put("songId", item.songId)
                            put("title", item.title)
                            put("artist", item.artist)
                            put("playCount", item.playCount)
                            put("listeningSeconds", item.listeningSeconds)
                            put("thumbnailUrl", item.thumbnailUrl.orEmpty())
                        }
                    )
                }
            }.toString()

            val cleanTitle = if (songId.isNullOrBlank()) "" else title
            val cleanArtist = if (songId.isNullOrBlank()) "" else artist
            val cleanThumb = if (songId.isNullOrBlank()) "" else thumbnailUrl.orEmpty()

            _state.value = _state.value.copy(
                level = journey.levelSpec.level,
                levelTitle = journey.levelSpec.title,
                levelSubtitle = journey.levelSpec.subtitle,
                xp = journey.xp,
                xpTarget = journey.xpTargetForCurrentBanner,
                streakDays = streak.currentStreak.coerceAtLeast(1),
                dailyRecordText = journey.dailyRecordCompactText,
                monthlyRecordText = journey.monthlyRecordHoursText,
                nowPlayingTitle = cleanTitle,
                nowPlayingArtist = cleanArtist,
                nowPlayingThumb = cleanThumb,
                isPlaying = isPlaying,
                topSongs = journey.topSongs.take(5)
            )

            patchDeveloperRow(
                JSONObject().apply {
                    put("level", journey.levelSpec.level)
                    put("level_title", journey.levelSpec.title)
                    put("level_subtitle", journey.levelSpec.subtitle)
                    put("xp", journey.xp)
                    put("xp_target", journey.xpTargetForCurrentBanner)
                    put("streak_days", streak.currentStreak)
                    put("daily_record_text", journey.dailyRecordCompactText)
                    put("monthly_record_text", journey.monthlyRecordHoursText)
                    put("now_playing_title", cleanTitle)
                    put("now_playing_artist", cleanArtist)
                    put("now_playing_thumb", cleanThumb)
                    put("is_playing", isPlaying)
                    put("top_songs_json", topSongsJson)
                    put("updated_at", java.time.Instant.now().toString())
                }
            )
        }
    }

    private fun patchDeveloperRow(payload: JSONObject) {
        runCatching {
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_REST_URL/developer_realtime_profile?id=eq.iky_vortex")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Prefer", "return=minimal")
                .patch(requestBody)
                .build()

            httpClient.newCall(request).execute().close()
        }.onFailure {
            Timber.w(it, "DeveloperRealtimeManager: failed to patch profile")
        }
    }
}
