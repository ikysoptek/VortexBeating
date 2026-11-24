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
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import com.nanzbeatles.nanas.ui.theme.NanzBeatlesTheme
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.get
import com.nanzbeatles.nanas.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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

@AndroidEntryPoint
class LockscreenLyricsAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    private lateinit var windowManager: WindowManager
    private var keyguardManager: KeyguardManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    private var composeView: ComposeView? = null
    private var lifecycleOwner: LockscreenOverlayLifecycleOwner? = null
    private var isAttached = false
    private var attachedFullScreen = false
    private val isLockedFlow = MutableStateFlow(false)
    private var lockReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isLockedFlow.value = checkLocked()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isLockedFlow.value = checkLocked()
            }
        }
        lockReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching { registerReceiver(receiver, filter) }

        monitorJob = scope.launch {
            while (isActive) {
                val locked = checkLocked()
                isLockedFlow.value = locked
                val prefs = runCatching { dataStore.data.firstOrNull() }.getOrNull()
                val enabled = prefs?.get(LockscreenLyricsEnabledKey) ?: false
                val snap = LockscreenLyricsBridge.snapshot.value

                if (enabled && locked && snap.isPlaying && !snap.mediaId.isNullOrBlank()) {
                    val wantVideo = LockscreenLyricsBridge.videoModeOn.value &&
                        LockscreenLyricsBridge.videoPlayer.value != null
                    // Ganti jendela jika mode video berubah (teks saja <-> layar penuh video)
                    if (isAttached && attachedFullScreen != wantVideo) {
                        detachWindowIfNeeded()
                    }
                    attachWindowIfNeeded(wantVideo)
                } else {
                    detachWindowIfNeeded()
                }
                delay(250L)
            }
        }
    }

    private fun checkLocked(): Boolean {
        val km = keyguardManager ?: return false
        return km.isKeyguardLocked || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && km.isDeviceLocked)
    }

    private fun attachWindowIfNeeded(fullScreen: Boolean) {
        if (isAttached) return
        try {
            val owner = LockscreenOverlayLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            lifecycleOwner = owner

            val view = ComposeView(this).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isForceDarkAllowed = false
                }
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    NanzBeatlesTheme(darkTheme = true, pureBlack = true) {
                        LockscreenLyricsOverlayContent(
                            context = this@LockscreenLyricsAccessibilityService,
                            database = database,
                            lyricsHelper = lyricsHelper,
                            isLockedFlow = isLockedFlow
                        )
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                if (fullScreen) WindowManager.LayoutParams.MATCH_PARENT
                else WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            ).apply {
                if (fullScreen) {
                    // Mode video: layar penuh, sentuhan tetap tembus ke layar kunci (geser untuk buka)
                    gravity = Gravity.TOP or Gravity.START
                    flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    y = 0
                } else {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = (285 * resources.displayMetrics.density).toInt()
                }
            }

            windowManager.addView(view, params)
            composeView = view
            isAttached = true
            attachedFullScreen = fullScreen
        } catch (e: Exception) {
            Timber.e(e, "Failed to attach TYPE_ACCESSIBILITY_OVERLAY for lockscreen lyrics")
        }
    }

    private fun detachWindowIfNeeded() {
        if (!isAttached) return
        try {
            composeView?.let { windowManager.removeViewImmediate(it) }
        } catch (_: Exception) {
        } finally {
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
            composeView = null
            isAttached = false
            attachedFullScreen = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        isLockedFlow.value = checkLocked()
    }

    override fun onInterrupt() {
        detachWindowIfNeeded()
    }

    override fun onDestroy() {
        lockReceiver?.let { runCatching { unregisterReceiver(it) } }
        lockReceiver = null
        monitorJob?.cancel()
        detachWindowIfNeeded()
        scope.cancel()
        super.onDestroy()
    }
}

@Composable
private fun LockscreenLyricsOverlayContent(
    context: Context,
    database: MusicDatabase,
    lyricsHelper: LyricsHelper,
    isLockedFlow: StateFlow<Boolean>,
) {
    val (enabled, _) = rememberPreference(LockscreenLyricsEnabledKey, defaultValue = false)
    val isLocked by isLockedFlow.collectAsState()
    val snap by LockscreenLyricsBridge.snapshot.collectAsState()

    if (!enabled || !isLocked || !snap.isPlaying || snap.mediaId.isNullOrBlank()) return

    var parsedLines by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var lyricsLoading by remember { mutableStateOf(true) }
    LaunchedEffect(snap.mediaId) {
        val songId = snap.mediaId ?: return@LaunchedEffect
        lyricsLoading = true
        parsedLines = withContext(Dispatchers.IO) {
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
            }.getOrDefault(emptyList())
        }
        lyricsLoading = false
    }

    var livePosMs by remember { mutableLongStateOf(snap.positionMs) }
    LaunchedEffect(snap) {
        while (isActive) {
            val elapsed = if (snap.isPlaying) {
                (SystemClock.elapsedRealtime() - snap.updatedAtElapsedMs).coerceAtLeast(0L)
            } else 0L
            livePosMs = (snap.positionMs + elapsed + snap.lyricsOffsetMs).coerceAtLeast(0L)
            delay(180L)
        }
    }

    val videoOn by LockscreenLyricsBridge.videoModeOn.collectAsState()
    val videoPlayer by LockscreenLyricsBridge.videoPlayer.collectAsState()
    val activePlayer = videoPlayer

    if (videoOn && activePlayer != null) {
        // ===== MODE VIDEO LIRIK: video layar penuh + jam + lirik =====
        Box(modifier = Modifier.fillMaxSize()) {
            LockscreenVideoSurface(player = activePlayer)

            // Gelapkan sedikit supaya lirik tetap terbaca
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.60f),
                            )
                        )
                    )
            )

            LockscreenClock(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
            )

            if (parsedLines.isNotEmpty()) {
                Box(modifier = Modifier.padding(top = 285.dp)) {
                    LockscreenLyricsLines(parsedLines = parsedLines, livePosMs = livePosMs)
                }
            }
        }
    } else {
        // ===== MODE BIASA: hanya teks lirik =====
        if (parsedLines.isNotEmpty()) {
            LockscreenLyricsLines(parsedLines = parsedLines, livePosMs = livePosMs)
        } else {
            // Penanda: kalau teks ini muncul, overlay SUDAH jalan, hanya liriknya yang belum ada
            Text(
                text = if (lyricsLoading) "Memuat lirik…" else "Lirik tidak ditemukan",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.9f),
                        offset = Offset(0f, 3f),
                        blurRadius = 12f
                    )
                )
            )
        }
    }
}

/** Video dari player utama (mode video) yang ditempel ke jendela layar kunci. */
@Composable
private fun LockscreenVideoSurface(player: Player) {
    val context = LocalContext.current
    val playerView = remember(player) {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setShutterBackgroundColor(android.graphics.Color.BLACK)
            setBackgroundColor(android.graphics.Color.BLACK)
            this.player = player
        }
    }

    DisposableEffect(playerView) {
        onDispose {
            playerView.player = null
            // Beri tahu UI aplikasi agar memasang ulang surface videonya setelah unlock
            LockscreenLyricsBridge.notifyVideoReleased()
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LockscreenClock(modifier: Modifier = Modifier) {
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
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))
    }
    val shadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(0f, 3f),
        blurRadius = 12f
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dateText,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                shadow = shadow
            )
        )
        Text(
            text = timeText,
            style = TextStyle(
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                shadow = shadow
            )
        )
    }
}

@Composable
private fun LockscreenLyricsLines(
    parsedLines: List<Pair<Long, String>>,
    livePosMs: Long,
) {
    val activeIdx = remember(parsedLines, livePosMs) {
        parsedLines.indexOfLast { it.first <= livePosMs }
    }

    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.92f),
        offset = Offset(0f, 3f),
        blurRadius = 14f
    )

    AnimatedContent(
        targetState = activeIdx,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 3 })
                .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 3 })
        },
        label = "LockscreenAccessibilityLyrics"
    ) { idx ->
        val prevLine = parsedLines.getOrNull(idx - 1)?.second
        val currentLine = parsedLines.getOrNull(idx)?.second ?: parsedLines.first().second
        val nextLine = parsedLines.getOrNull(idx + 1)?.second
        val afterNextLine = parsedLines.getOrNull(idx + 2)?.second

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!prevLine.isNullOrBlank() && idx > 0) {
                Text(
                    text = prevLine,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.55f),
                        shadow = textShadow
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }

            Text(
                text = currentLine,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = textShadow
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )

            if (!nextLine.isNullOrBlank()) {
                Text(
                    text = nextLine,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.72f),
                        shadow = textShadow
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }

            if (!afterNextLine.isNullOrBlank()) {
                Text(
                    text = afterNextLine,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.48f),
                        shadow = textShadow
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
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
