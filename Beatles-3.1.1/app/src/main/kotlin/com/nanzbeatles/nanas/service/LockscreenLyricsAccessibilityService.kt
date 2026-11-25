/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.Player
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.LockscreenLyricsEnabledKey
import com.nanzbeatles.nanas.constants.PreferredLyricsProvider
import com.nanzbeatles.nanas.constants.PreferredLyricsProviderKey
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.db.entities.LyricsEntity
import com.nanzbeatles.nanas.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.nanzbeatles.nanas.extensions.toEnum
import com.nanzbeatles.nanas.lyrics.LyricsHelper
import com.nanzbeatles.nanas.lyrics.LyricsUtils
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.playback.MusicService
import com.nanzbeatles.nanas.ui.theme.NanzBeatlesTheme
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.get
import com.nanzbeatles.nanas.widget.MusicWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object LockscreenLyricsBridge {
    data class PlaybackSnapshot(
        val mediaId: String? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val updatedAtElapsedMs: Long = 0L,
        val lyricsOffsetMs: Long = 0L,
        val title: String? = null,
        val artist: String? = null,
        val durationSec: Int = -1,
        val thumbnailUrl: String? = null,
    )

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    // ---- Video lirik di layar kunci ----
    private val _videoPlayer = MutableStateFlow<Player?>(null)
    val videoPlayer: StateFlow<Player?> = _videoPlayer.asStateFlow()

    private val _videoModeOn = MutableStateFlow(false)
    val videoModeOn: StateFlow<Boolean> = _videoModeOn.asStateFlow()

    /** Dinaikkan setiap overlay melepas surface video, supaya PlayerView di aplikasi dibuat ulang. */
    private val _videoReleaseToken = MutableStateFlow(0)
    val videoReleaseToken: StateFlow<Int> = _videoReleaseToken.asStateFlow()

    fun setVideoOutput(player: Player?, videoModeOn: Boolean) {
        _videoPlayer.value = player
        _videoModeOn.value = videoModeOn
    }

    fun notifyVideoReleased() {
        _videoReleaseToken.value = _videoReleaseToken.value + 1
    }

    fun update(
        mediaId: String?,
        isPlaying: Boolean,
        positionMs: Long,
        lyricsOffsetMs: Long = 0L,
        title: String? = null,
        artist: String? = null,
        durationSec: Int = -1,
        thumbnailUrl: String? = null,
    ) {
        _snapshot.value = PlaybackSnapshot(
            mediaId = mediaId,
            isPlaying = isPlaying,
            positionMs = positionMs,
            updatedAtElapsedMs = SystemClock.elapsedRealtime(),
            lyricsOffsetMs = lyricsOffsetMs,
            title = title,
            artist = artist,
            durationSec = durationSec,
            thumbnailUrl = thumbnailUrl,
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, LockscreenLyricsAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val componentString = splitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentString)
            if (enabledComponent != null && enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }
}

/** Wallpaper layar kunci yang mengikuti cover lagu. */
private class LockArtwork(
    val thumbnailUrl: String? = null,
    /** Cover untuk kartu pemutar. */
    val cover: Bitmap? = null,
    /** Versi sangat kecil (di-upscale = efek blur) untuk wallpaper. */
    val blurred: Bitmap? = null,
    val color: Int = 0xFF2B2B33.toInt(),
)

/** Overlay tetap tampil sebentar saat lagu di-pause, supaya bisa menekan Play lagi. */
private const val PAUSED_KEEP_VISIBLE_MS = 5 * 60 * 1000L

@AndroidEntryPoint
class LockscreenLyricsAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    private lateinit var windowManager: WindowManager
    private var keyguardManager: KeyguardManager? = null
    private var powerManager: PowerManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null
    private var lyricsJob: Job? = null
    private var artworkJob: Job? = null

    private var mainView: ComposeView? = null
    private var controlsView: ComposeView? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lifecycleOwner: LockscreenOverlayLifecycleOwner? = null

    private val lyricsState = MutableStateFlow(LockLyricsState())
    private val artworkState = MutableStateFlow(LockArtwork())
    private val isLockedFlow = MutableStateFlow(false)
    private val overlayVisible = MutableStateFlow(true)
    private var lastPlayingAt = 0L
    private var lockReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isLockedFlow.value = checkLocked()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    // Layar dimatikan / sudah terbuka: overlay HARUS langsung hilang
                    Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT -> {
                        overlayVisible.value = true
                        detachOverlay()
                        updateKeepAwake(false)
                        isLockedFlow.value = checkLocked()
                    }
                    else -> isLockedFlow.value = checkLocked()
                }
            }
        }
        lockReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching { registerReceiver(receiver, filter) }

        // Muat lirik begitu lagu berganti, supaya langsung ada saat layar dikunci
        lyricsJob = scope.launch {
            LockscreenLyricsBridge.snapshot
                .map { it.mediaId }
                .distinctUntilChanged()
                .collectLatest { id ->
                    if (id.isNullOrBlank()) {
                        lyricsState.value = LockLyricsState()
                        return@collectLatest
                    }
                    val prefs = runCatching { dataStore.data.firstOrNull() }.getOrNull()
                    if (prefs?.get(LockscreenLyricsEnabledKey) != true) {
                        lyricsState.value = LockLyricsState(mediaId = id)
                        return@collectLatest
                    }
                    lyricsState.value = LockLyricsState(mediaId = id, loading = true)
                    val lines = loadLockscreenLyrics(
                        context = this@LockscreenLyricsAccessibilityService,
                        database = database,
                        lyricsHelper = lyricsHelper,
                        snap = LockscreenLyricsBridge.snapshot.value,
                    )
                    lyricsState.value = LockLyricsState(mediaId = id, lines = lines, loading = false)
                }
        }

        // Siapkan wallpaper dari cover lagu
        artworkJob = scope.launch {
            LockscreenLyricsBridge.snapshot
                .map { it.thumbnailUrl }
                .distinctUntilChanged()
                .collectLatest { url ->
                    if (url.isNullOrBlank()) {
                        artworkState.value = LockArtwork()
                        return@collectLatest
                    }
                    artworkState.value = loadArtwork(url)
                }
        }

        monitorJob = scope.launch {
            while (isActive) {
                val locked = checkLocked()
                isLockedFlow.value = locked
                val interactive = powerManager?.isInteractive ?: true
                val prefs = runCatching { dataStore.data.firstOrNull() }.getOrNull()
                val enabled = prefs?.get(LockscreenLyricsEnabledKey) ?: false
                val snap = LockscreenLyricsBridge.snapshot.value
                val now = SystemClock.elapsedRealtime()
                if (snap.isPlaying) lastPlayingAt = now
                val recentlyActive = snap.isPlaying ||
                    (lastPlayingAt > 0L && now - lastPlayingAt < PAUSED_KEEP_VISIBLE_MS)

                val shouldShow = enabled && locked && interactive &&
                    !snap.mediaId.isNullOrBlank() && recentlyActive

                if (shouldShow && overlayVisible.value) {
                    attachOverlayIfNeeded()
                    // Layar tidak boleh cepat mati selama lagu (dan lirik) berjalan
                    updateKeepAwake(snap.isPlaying)
                } else {
                    detachOverlay()
                    updateKeepAwake(false)
                    // Setelah benar-benar tidak terkunci, siapkan overlay untuk sesi kunci berikutnya
                    if (!shouldShow) overlayVisible.value = true
                }
                delay(250L)
            }
        }
    }

    private suspend fun loadArtwork(url: String): LockArtwork = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(this@LockscreenLyricsAccessibilityService)
                .data(url)
                .size(512)
                .allowHardware(false)
                .build()
            val cover = ImageLoader(this@LockscreenLyricsAccessibilityService)
                .execute(request).image?.toBitmap()
                ?: return@runCatching LockArtwork(thumbnailUrl = url)
            val palette = Palette.from(cover).generate()
            val rawColor = palette.getVibrantColor(
                palette.getMutedColor(palette.getDominantColor(0xFF2B2B33.toInt()))
            )
            // Pastikan warnanya cukup hidup (tidak jadi hitam) supaya terasa "sesuai lagu"
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rawColor, hsl)
            if (hsl[1] > 0.12f) hsl[1] = maxOf(hsl[1], 0.40f)
            hsl[2] = hsl[2].coerceIn(0.30f, 0.52f)
            val color = ColorUtils.HSLToColor(hsl)
            val blurred = Bitmap.createScaledBitmap(cover, 24, 24, true)
            LockArtwork(thumbnailUrl = url, cover = cover, blurred = blurred, color = color)
        }.onFailure { Timber.e(it, "Lockscreen artwork load failed") }
            .getOrDefault(LockArtwork(thumbnailUrl = url))
    }

    private fun checkLocked(): Boolean {
        val km = keyguardManager ?: return false
        return km.isKeyguardLocked || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && km.isDeviceLocked)
    }

    /**
     * Dipanggil saat pengguna menggeser ke atas (mau membuka kunci): overlay disembunyikan
     * supaya kunci asli (geser / PIN / sidik jari) terlihat. Kembali lagi di sesi kunci berikutnya.
     */
    private fun onUnlockSwipe() {
        overlayVisible.value = false
        // Tunda sampai event sentuhan selesai, baru lepas jendela
        scope.launch { detachOverlay() }
    }

    @Suppress("DEPRECATION")
    private fun updateKeepAwake(keep: Boolean) {
        // 1) Flag jendela
        mainView?.let { v ->
            val lp = v.layoutParams as? WindowManager.LayoutParams
            if (lp != null) {
                val has = lp.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
                if (has != keep) {
                    lp.flags = if (keep) {
                        lp.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    } else {
                        lp.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                    }
                    runCatching { windowManager.updateViewLayout(v, lp) }
                }
            }
        }
        // 2) Wake lock cadangan (beberapa HP mematikan layar kunci dalam ~2 detik)
        runCatching {
            if (keep) {
                if (wakeLock == null) {
                    wakeLock = powerManager?.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                        "VortexMusic:LockscreenLyrics"
                    )?.apply { setReferenceCounted(false) }
                }
                if (wakeLock?.isHeld != true) wakeLock?.acquire(10 * 60 * 1000L)
            } else if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    private fun sendMusicAction(action: String) {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }
    }

    private fun overlayParams(
        height: Int,
        touchable: Boolean,
        gravity: Int,
        yOffsetDp: Int = 0,
        fullScreen: Boolean = false,
    ): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (fullScreen) flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            y = (yOffsetDp * resources.displayMetrics.density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun attachOverlayIfNeeded() {
        if (mainView != null) return
        try {
            val owner = LockscreenOverlayLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            lifecycleOwner = owner

            fun makeView(content: @Composable () -> Unit) = ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) isForceDarkAllowed = false
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent { NanzBeatlesTheme(darkTheme = true, pureBlack = true) { content() } }
            }

            // Jendela 1: wallpaper + jam + lirik. Ditekan/disentuh biasa: lirik TETAP ada.
            // Hanya geser ke atas (mau buka kunci) yang menyembunyikannya.
            val main = makeView {
                LockscreenMainContent(
                    lyricsFlow = lyricsState,
                    artworkFlow = artworkState,
                    onSwipeUp = { onUnlockSwipe() }
                )
            }
            windowManager.addView(
                main,
                overlayParams(
                    height = WindowManager.LayoutParams.MATCH_PARENT,
                    touchable = true,
                    gravity = Gravity.TOP or Gravity.START,
                    fullScreen = true
                )
            )
            mainView = main

            // Jendela 2: kartu pemutar (bisa disentuh: prev / play-pause / next)
            val controls = makeView {
                LockscreenPlayerCard(
                    artworkFlow = artworkState,
                    onPrevious = { sendMusicAction(MusicWidgetReceiver.ACTION_PREVIOUS) },
                    onPlayPause = { sendMusicAction(MusicWidgetReceiver.ACTION_PLAY_PAUSE) },
                    onNext = { sendMusicAction(MusicWidgetReceiver.ACTION_NEXT) },
                )
            }
            windowManager.addView(
                controls,
                overlayParams(
                    height = WindowManager.LayoutParams.WRAP_CONTENT,
                    touchable = true,
                    gravity = Gravity.BOTTOM or Gravity.START,
                    yOffsetDp = 96
                )
            )
            controlsView = controls
        } catch (e: Exception) {
            Timber.e(e, "Failed to attach lockscreen lyrics overlay")
            detachOverlay()
        }
    }

    private fun detachOverlay() {
        if (mainView == null && controlsView == null) return
        runCatching { controlsView?.let { windowManager.removeViewImmediate(it) } }
        runCatching { mainView?.let { windowManager.removeViewImmediate(it) } }
        controlsView = null
        mainView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        isLockedFlow.value = checkLocked()
    }

    override fun onInterrupt() {
        detachOverlay()
        updateKeepAwake(false)
    }

    override fun onDestroy() {
        lockReceiver?.let { runCatching { unregisterReceiver(it) } }
        lockReceiver = null
        monitorJob?.cancel()
        lyricsJob?.cancel()
        artworkJob?.cancel()
        detachOverlay()
        updateKeepAwake(false)
        scope.cancel()
        super.onDestroy()
    }
}

/** Satu keadaan lirik untuk overlay (dimuat di service, jadi sudah siap saat layar dikunci). */
private data class LockLyricsState(
    val mediaId: String? = null,
    val lines: List<Pair<Long, String>> = emptyList(),
    val loading: Boolean = false,
)

private suspend fun loadLockscreenLyrics(
    context: Context,
    database: MusicDatabase,
    lyricsHelper: LyricsHelper,
    snap: LockscreenLyricsBridge.PlaybackSnapshot,
): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
    val songId = snap.mediaId ?: return@withContext emptyList()
    runCatching {
                var raw = database.lyrics(songId).firstOrNull()?.lyrics
                if (raw.isNullOrBlank()) {
                    val songWithArtists = database.song(songId).firstOrNull()
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
                    } else if (!snap.title.isNullOrBlank()) {
                        // Lagu belum ada di database: pakai metadata dari player
                        MediaMetadata(
                            id = songId,
                            title = snap.title.orEmpty(),
                            artists = listOf(MediaMetadata.Artist(id = null, name = snap.artist.orEmpty())),
                            duration = snap.durationSec,
                            thumbnailUrl = snap.thumbnailUrl,
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
                                    id = songId,
                                    lyrics = fetched.lyrics,
                                    provider = fetched.provider,
                                )
                            )
                        }
                    }
                }
                if (!raw.isNullOrBlank() && raw != LYRICS_NOT_FOUND) {
                    LyricsUtils.parseLyrics(raw)
                        .map { entry ->
                            entry.time to entry.text.lineSequence().firstOrNull()?.trim().orEmpty()
                        }
                        .filter { it.second.isNotBlank() }
                } else {
                    emptyList()
                }
            }.onFailure { Timber.e(it, "Lockscreen lyrics load failed") }
        .getOrDefault(emptyList())
}

@Composable
private fun rememberLivePosition(
    snap: LockscreenLyricsBridge.PlaybackSnapshot,
    withLyricsOffset: Boolean,
    tickMs: Long,
): State<Long> {
    val pos = remember { mutableLongStateOf(snap.positionMs) }
    LaunchedEffect(snap, withLyricsOffset) {
        while (isActive) {
            val elapsed = if (snap.isPlaying) {
                (SystemClock.elapsedRealtime() - snap.updatedAtElapsedMs).coerceAtLeast(0L)
            } else 0L
            val offset = if (withLyricsOffset) snap.lyricsOffsetMs else 0L
            pos.longValue = (snap.positionMs + elapsed + offset).coerceAtLeast(0L)
            delay(tickMs)
        }
    }
    return pos
}

private fun formatMs(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(total / 60, total % 60)
}

/** Wallpaper (dari cover lagu) + tanggal + jam + lirik karaoke. */
@Composable
private fun LockscreenMainContent(
    lyricsFlow: StateFlow<LockLyricsState>,
    artworkFlow: StateFlow<LockArtwork>,
    onSwipeUp: () -> Unit,
) {
    val snap by LockscreenLyricsBridge.snapshot.collectAsState()
    val lyr by lyricsFlow.collectAsState()
    val art by artworkFlow.collectAsState()

    val parsedLines = if (lyr.mediaId == snap.mediaId) lyr.lines else emptyList()
    val lyricsLoading = lyr.loading || lyr.mediaId != snap.mediaId
    val livePosMs by rememberLivePosition(snap, withLyricsOffset = true, tickMs = 60L)

    val base = Color(art.color)
    val top = lerp(base, Color.Black, 0.10f)
    val mid = lerp(base, Color.Black, 0.35f)
    val bottom = lerp(base, Color.Black, 0.65f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                var fired = false
                val threshold = 64.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = { total = 0f; fired = false },
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (!fired && total < -threshold) {
                            fired = true
                            onSwipeUp()
                        }
                    }
                )
            }
    ) {
        // Wallpaper pengganti: gradasi warna cover
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(top, mid, bottom)))
        )
        // Cover yang di-blur (diperkecil lalu diperbesar)
        art.blurred?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.30f,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Album bundar di belakang lirik (berputar pelan seperti piringan)
        art.cover?.let { cover ->
            val spin = rememberInfiniteTransition(label = "album_spin")
            val angle by spin.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
                label = "album_angle"
            )
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.38f,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 150.dp, y = 70.dp)
                    .size(400.dp)
                    .graphicsLayer { rotationZ = angle }
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            )
        }
        // Peredam supaya teks selalu terbaca
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.30f),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Icon(
                painter = painterResource(R.drawable.lock),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
            LockscreenClock()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (parsedLines.isNotEmpty()) {
                    LockscreenLyricsLines(parsedLines = parsedLines, livePosMs = livePosMs)
                } else {
                    Text(
                        text = if (lyricsLoading) "Memuat lirik…" else "Lirik tidak ditemukan",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }

            // Ruang untuk kartu pemutar (jendela terpisah) + petunjuk geser
            Spacer(Modifier.height(322.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Geser ke atas untuk membuka",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.9f))
            )
        }
    }
}

@Composable
private fun LockscreenClock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val minuteKey = now / 60_000L
    val timeText = remember(minuteKey) {
        SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date(now))
    }
    val dateText = remember(minuteKey) {
        SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date(now))
    }
    Text(
        text = dateText,
        style = TextStyle(
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.95f)
        ),
        modifier = Modifier.padding(top = 6.dp)
    )
    Text(
        text = timeText,
        style = TextStyle(
            fontSize = 104.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 110.sp
        )
    )
}

/** Kartu pemutar: cover, judul, progres, dan tombol (jendela ini yang bisa disentuh). */
@Composable
private fun LockscreenPlayerCard(
    artworkFlow: StateFlow<LockArtwork>,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val snap by LockscreenLyricsBridge.snapshot.collectAsState()
    val art by artworkFlow.collectAsState()
    val posMs by rememberLivePosition(snap, withLyricsOffset = false, tickMs = 250L)

    val durationMs = if (snap.durationSec > 0) snap.durationSec * 1000L else 0L
    val progress = if (durationMs > 0) (posMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val cardColor = lerp(Color(art.color), Color.Black, 0.45f).copy(alpha = 0.72f)
    val shape = RoundedCornerShape(30.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(cardColor, shape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                art.cover?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snap.title.orEmpty(),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = snap.artist.orEmpty(),
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val small = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Text(text = formatMs(posMs), style = small)
                if (durationMs > 0) Text(text = "-" + formatMs(durationMs - posMs), style = small)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onPrevious)
                    .padding(6.dp)
            )
            Icon(
                painter = painterResource(if (snap.isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onPlayPause)
                    .padding(8.dp)
            )
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNext)
                    .padding(6.dp)
            )
        }
    }
}

/** Lirik besar bergaya karaoke: baris aktif terisi putih mengikuti lagu, baris lain redup. */
@Composable
private fun LockscreenLyricsLines(
    parsedLines: List<Pair<Long, String>>,
    livePosMs: Long,
) {
    val activeIdx = remember(parsedLines, livePosMs) {
        parsedLines.indexOfLast { it.first <= livePosMs }
    }
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.40f),
        offset = Offset(0f, 2f),
        blurRadius = 8f
    )
    val baseStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        shadow = textShadow,
        textAlign = TextAlign.Start
    )

    AnimatedContent(
        targetState = activeIdx,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 6 })
                .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(240)) { -it / 6 })
        },
        label = "LockscreenKaraokeLyrics",
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // Pudar di atas dan bawah seperti contoh
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.14f to Color.Black,
                        0.86f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) { idx ->
        val prev = parsedLines.getOrNull(idx - 1)?.second
        val current = parsedLines.getOrNull(idx)?.second
        val next1 = parsedLines.getOrNull(idx + 1)?.second
        val next2 = parsedLines.getOrNull(idx + 2)?.second

        // Perkiraan lama baris dinyanyikan: dari panjang teks, dibatasi jarak ke baris berikutnya.
        // (Jadi cahaya tidak "ketinggalan" saat ada jeda panjang, dan tidak menyala duluan.)
        val lineStart = parsedLines.getOrNull(idx)?.first ?: 0L
        val gap = (parsedLines.getOrNull(idx + 1)?.first ?: (lineStart + 4000L)) - lineStart
        val estimate = ((current?.length ?: 0) * 85L).coerceIn(1500L, 7000L)
        val span = minOf(gap, estimate).coerceAtLeast(1L)
        val fill = if (idx < 0) 0f else ((livePosMs - lineStart).toFloat() / span.toFloat()).coerceIn(0f, 1f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Baris yang belum / sudah lewat: redup, TIDAK ikut menyala
            if (!prev.isNullOrBlank()) {
                Text(text = prev, style = baseStyle.copy(color = Color.White.copy(alpha = 0.25f)), maxLines = 3)
            }
            if (current != null) {
                KaraokeLine(text = current, fill = fill, style = baseStyle, dimAlpha = 0.42f)
            }
            if (!next1.isNullOrBlank()) {
                Text(text = next1, style = baseStyle.copy(color = Color.White.copy(alpha = 0.30f)), maxLines = 3)
            }
            if (!next2.isNullOrBlank()) {
                Text(text = next2, style = baseStyle.copy(color = Color.White.copy(alpha = 0.20f)), maxLines = 3)
            }
        }
    }
}

/**
 * Satu baris lirik dengan efek karaoke yang benar untuk teks yang turun baris:
 * putih terisi dari kiri ke kanan di baris pertama, LALU baris kedua, dst.
 */
@Composable
private fun KaraokeLine(
    text: String,
    fill: Float,
    style: TextStyle,
    dimAlpha: Float,
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style.copy(color = Color.White.copy(alpha = dimAlpha)),
        maxLines = 4,
        onTextLayout = { layout = it },
        modifier = Modifier.drawWithContent {
            drawContent() // dasar redup
            val l = layout ?: return@drawWithContent
            var total = 0f
            for (i in 0 until l.lineCount) total += l.getLineRight(i) - l.getLineLeft(i)
            var remaining = fill * total
            for (i in 0 until l.lineCount) {
                val left = l.getLineLeft(i)
                val lineWidth = l.getLineRight(i) - left
                val w = remaining.coerceIn(0f, lineWidth)
                if (w > 0f) {
                    clipRect(
                        left = left,
                        top = l.getLineTop(i),
                        right = left + w,
                        bottom = l.getLineBottom(i)
                    ) {
                        drawText(textLayoutResult = l, color = Color.White)
                    }
                }
                remaining -= lineWidth
            }
        }
    )
}

private class LockscreenOverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun onCreate() {
        controller.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
