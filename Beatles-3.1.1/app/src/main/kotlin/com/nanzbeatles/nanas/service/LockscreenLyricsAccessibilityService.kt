/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.ComposeView
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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nanzbeatles.nanas.constants.LockscreenLyricsEnabledKey
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.nanzbeatles.nanas.lyrics.LyricsUtils
import com.nanzbeatles.nanas.ui.theme.NanzBeatlesTheme
import com.nanzbeatles.nanas.utils.dataStore
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
import javax.inject.Inject

/**
 * Jembatan data pemutaran musik real-time dari MusicService ke LockscreenLyricsAccessibilityService
 * agar lirik tampil di atas lapisan Keyguard (layar kunci) Android 12+ / MIUI / HyperOS.
 */
object LockscreenLyricsBridge {
    data class PlaybackSnapshot(
        val mediaId: String? = null,
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val updatedAtElapsedMs: Long = 0L,
        val lyricsOffsetMs: Long = 0L,
    )

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    fun update(
        mediaId: String?,
        isPlaying: Boolean,
        positionMs: Long,
        lyricsOffsetMs: Long = 0L,
    ) {
        _snapshot.value = PlaybackSnapshot(
            mediaId = mediaId,
            isPlaying = isPlaying,
            positionMs = positionMs,
            updatedAtElapsedMs = SystemClock.elapsedRealtime(),
            lyricsOffsetMs = lyricsOffsetMs,
        )
    }
}

@AndroidEntryPoint
class LockscreenLyricsAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var database: MusicDatabase

    private lateinit var windowManager: WindowManager
    private var keyguardManager: KeyguardManager? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    private var composeView: ComposeView? = null
    private var lifecycleOwner: LockscreenOverlayLifecycleOwner? = null
    private var isAttached = false
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
                    attachWindowIfNeeded()
                } else {
                    detachWindowIfNeeded()
                }
                delay(280L)
            }
        }
    }

    private fun checkLocked(): Boolean {
        val km = keyguardManager ?: return false
        return km.isKeyguardLocked || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && km.isDeviceLocked)
    }

    private fun attachWindowIfNeeded() {
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
                            database = database,
                            isLockedFlow = isLockedFlow
                        )
                    }
                }
            }

            // TYPE_ACCESSIBILITY_OVERLAY tampil langsung DI ATAS layar kunci Android / MIUI / HyperOS
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (290 * resources.displayMetrics.density).toInt()
            }

            windowManager.addView(view, params)
            composeView = view
            isAttached = true
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
    database: MusicDatabase,
    isLockedFlow: StateFlow<Boolean>,
) {
    val (enabled, _) = rememberPreference(LockscreenLyricsEnabledKey, defaultValue = false)
    val isLocked by isLockedFlow.collectAsState()
    val snap by LockscreenLyricsBridge.snapshot.collectAsState()

    if (!enabled || !isLocked || !snap.isPlaying || snap.mediaId.isNullOrBlank()) return

    var parsedLines by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    LaunchedEffect(snap.mediaId) {
        val songId = snap.mediaId ?: return@LaunchedEffect
        parsedLines = withContext(Dispatchers.IO) {
            runCatching {
                val entity = database.lyrics(songId).firstOrNull()
                val raw = entity?.lyrics
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

    if (parsedLines.isEmpty()) return

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
