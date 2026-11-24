/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.MainActivity
import com.nanzbeatles.nanas.constants.DynamicIslandAccentColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandAutoEnableOnStartKey
import com.nanzbeatles.nanas.constants.DynamicIslandBgColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandBlurRadiusKey
import com.nanzbeatles.nanas.constants.DynamicIslandBorderGlowKey
import com.nanzbeatles.nanas.constants.DynamicIslandDismissedKey
import com.nanzbeatles.nanas.constants.DynamicIslandEnabledKey
import com.nanzbeatles.nanas.constants.DynamicIslandMode
import com.nanzbeatles.nanas.constants.DynamicIslandModeKey
import com.nanzbeatles.nanas.constants.DynamicIslandTheme
import com.nanzbeatles.nanas.constants.DynamicIslandThemeKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicAccentKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicBgKey
import com.nanzbeatles.nanas.extensions.togglePlayPause
import com.nanzbeatles.nanas.extensions.toggleRepeatMode
import com.nanzbeatles.nanas.playback.MusicService
import com.nanzbeatles.nanas.ui.theme.NanzBeatlesTheme
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class DynamicIslandSystemOverlayManager(
    private val service: MusicService,
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val keyguardManager = service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    private var composeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayWindowLifecycleOwner? = null
    private var isWindowAttached = false

    private val isPlayerPlayingFlow = MutableStateFlow(false)
    private val isScreenLockedFlow = MutableStateFlow(false)
    private var playerListener: Player.Listener? = null
    private var lockStateReceiver: BroadcastReceiver? = null

    private fun checkIsLocked(): Boolean {
        val km = keyguardManager ?: return false
        return km.isKeyguardLocked || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && km.isDeviceLocked)
    }

    fun start() {
        if (monitorJob != null) return

        isPlayerPlayingFlow.value = service.player.isPlaying
        isScreenLockedFlow.value = checkIsLocked()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isScreenLockedFlow.value = checkIsLocked()
            }
        }
        lockStateReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching { service.registerReceiver(receiver, filter) }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlayingFlow.value = isPlaying
                isScreenLockedFlow.value = checkIsLocked()
                if (isPlaying) {
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            service.dataStore.edit { prefs ->
                                val autoMode = prefs[DynamicIslandAutoEnableOnStartKey] ?: false
                                if (autoMode) {
                                    prefs[DynamicIslandDismissedKey] = false
                                    prefs[DynamicIslandEnabledKey] = true
                                }
                            }
                        }
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                isPlayerPlayingFlow.value = player.isPlaying
                isScreenLockedFlow.value = checkIsLocked()
            }
        }
        playerListener = listener
        service.player.addListener(listener)

        monitorJob = scope.launch {
            val prefsFlow = service.dataStore.data
                .map { prefs ->
                    val enabled = prefs[DynamicIslandEnabledKey] ?: false
                    val dismissed = prefs[DynamicIslandDismissedKey] ?: false
                    enabled to dismissed
                }
                .distinctUntilChanged()

            combine(
                prefsFlow,
                isPlayerPlayingFlow,
                service.currentMediaMetadata,
                DynamicIslandOverlayState.isAppInForeground,
                isScreenLockedFlow
            ) { (enabled, dismissed), _, _, inForeground, isLocked ->
                val shouldShow = enabled && !dismissed && !isLocked
                shouldShow to inForeground
            }.collect { (shouldShow, inForeground) ->
                updateOverlayVisibility(shouldShow = shouldShow, inForeground = inForeground)
            }
        }

        scope.launch {
            while (isActive) {
                delay(500L)
                isScreenLockedFlow.value = checkIsLocked()
                val inForeground = DynamicIslandOverlayState.isAppInForeground.value
                if (!inForeground && isWindowAttached && !canDrawOverlays()) {
                    hideOverlayWindow()
                }
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(service)
        } else {
            true
        }
    }

    private fun updateOverlayVisibility(shouldShow: Boolean, inForeground: Boolean) {
        if (shouldShow && !inForeground && canDrawOverlays()) {
            showOverlayWindow()
        } else {
            hideOverlayWindow()
        }
    }

    private fun showOverlayWindow() {
        if (isWindowAttached) return
        if (!canDrawOverlays()) return

        try {
            val lifecycleOwner = OverlayWindowLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            overlayLifecycleOwner = lifecycleOwner

            val view = ComposeView(service).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isForceDarkAllowed = false
                }
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    NanzBeatlesTheme(
                        darkTheme = true,
                        pureBlack = true
                    ) {
                        SystemDynamicIslandContent(
                            service = service,
                            onOpenApp = { openMainActivity() }
                        )
                    }
                }
            }

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.RGBA_8888
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(8)
                alpha = 1.0f
            }

            windowManager.addView(view, params)
            composeView = view
            isWindowAttached = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to attach Dynamic Island system overlay")
        }
    }

    private fun hideOverlayWindow() {
        if (!isWindowAttached) return
        try {
            composeView?.let { view ->
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to remove Dynamic Island system overlay")
        } finally {
            overlayLifecycleOwner?.onDestroy()
            overlayLifecycleOwner = null
            composeView = null
            isWindowAttached = false
        }
    }

    private fun openMainActivity() {
        runCatching {
            val intent = Intent(service, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            service.startActivity(intent)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * service.resources.displayMetrics.density).toInt()
    }

    fun release() {
        lockStateReceiver?.let { runCatching { service.unregisterReceiver(it) } }
        lockStateReceiver = null
        playerListener?.let { service.player.removeListener(it) }
        playerListener = null
        monitorJob?.cancel()
        monitorJob = null
        hideOverlayWindow()
        scope.cancel()
    }
}

@Composable
private fun SystemDynamicIslandContent(
    service: MusicService,
    onOpenApp: () -> Unit,
) {
    val (enabled, _) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (isDismissed, onDismissedChange) = rememberPreference(
        DynamicIslandDismissedKey,
        defaultValue = false
    )
    val (mode, onModeChange) = rememberEnumPreference(
        DynamicIslandModeKey,
        defaultValue = DynamicIslandMode.KECIL
    )
    val (islandTheme, _) = rememberEnumPreference(
        DynamicIslandThemeKey,
        defaultValue = DynamicIslandTheme.LIQUID_GLASS
    )
    val (accentColorInt, _) = rememberPreference(
        DynamicIslandAccentColorKey,
        defaultValue = 0xFF38BDF8.toInt()
    )
    val (useDynamicAccent, _) = rememberPreference(
        DynamicIslandUseDynamicAccentKey,
        defaultValue = true
    )
    val (bgColorInt, _) = rememberPreference(
        DynamicIslandBgColorKey,
        defaultValue = 0xFF161D2C.toInt()
    )
    val (useDynamicBg, _) = rememberPreference(
        DynamicIslandUseDynamicBgKey,
        defaultValue = true
    )
    val (borderGlow, _) = rememberPreference(
        DynamicIslandBorderGlowKey,
        defaultValue = true
    )
    val (blurRadius, _) = rememberPreference(
        DynamicIslandBlurRadiusKey,
        defaultValue = 20
    )

    val metadata by service.currentMediaMetadata.collectAsState()
    val currentSong by service.currentSong.collectAsState()

    var isPlaying by remember { mutableStateOf(service.player.isPlaying) }
    var playbackState by remember { mutableIntStateOf(service.player.playbackState) }
    var canSkipPrevious by remember { mutableStateOf(service.player.hasPreviousMediaItem()) }
    var canSkipNext by remember { mutableStateOf(service.player.hasNextMediaItem()) }
    var shuffleModeEnabled by remember { mutableStateOf(service.player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(service.player.repeatMode) }

    DisposableEffect(service.player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                shuffleModeEnabled = shuffleEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                playbackState = player.playbackState
                canSkipPrevious = player.hasPreviousMediaItem() || player.currentPosition > 3000L
                canSkipNext = player.hasNextMediaItem()
                shuffleModeEnabled = player.shuffleModeEnabled
                repeatMode = player.repeatMode
            }
        }
        service.player.addListener(listener)
        onDispose {
            service.player.removeListener(listener)
        }
    }

    val shouldShow = enabled && !isDismissed
    if (!shouldShow) return

    var extractedArtworkColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(metadata?.thumbnailUrl, useDynamicAccent, useDynamicBg) {
        val thumb = metadata?.thumbnailUrl
        if (thumb.isNullOrBlank()) {
            extractedArtworkColor = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val result = service.imageLoader.execute(
                    ImageRequest.Builder(service)
                        .data(thumb)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                )
                extractedArtworkColor = result.image?.toBitmap()?.extractVibrantIslandColor()
            }
        }
    }

    val effectiveAccentColor = remember(accentColorInt, useDynamicAccent, extractedArtworkColor) {
        if (useDynamicAccent && extractedArtworkColor != null) {
            extractedArtworkColor!!
        } else {
            Color(accentColorInt)
        }
    }

    val effectiveBgColor = remember(bgColorInt, useDynamicBg, extractedArtworkColor) {
        if (useDynamicBg && extractedArtworkColor != null) {
            lerp(Color(0xFF141A26), extractedArtworkColor!!, 0.34f).copy(alpha = 1f)
        } else {
            Color(bgColorInt).copy(alpha = 1f)
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playbackState, isPlaying, metadata?.id) {
        if
