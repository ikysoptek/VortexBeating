/**
 * Vortex Music Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.ikyvortex.vortexmusic.LocalPlayerConnection
import com.ikyvortex.vortexmusic.R
import com.ikyvortex.vortexmusic.constants.DynamicIslandAccentColorKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandAutoEnableOnStartKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandBgColorKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandBlurRadiusKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandBorderGlowKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandDismissedKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandEnabledKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandMode
import com.ikyvortex.vortexmusic.constants.DynamicIslandModeKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandPermissionConfirmedKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandTheme
import com.ikyvortex.vortexmusic.constants.DynamicIslandThemeKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandUseDynamicAccentKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandUseDynamicBgKey
import com.ikyvortex.vortexmusic.constants.LockscreenLyricsEnabledKey
import com.ikyvortex.vortexmusic.extensions.togglePlayPause
import com.ikyvortex.vortexmusic.extensions.toggleRepeatMode
import com.ikyvortex.vortexmusic.lyrics.LiveLine
import com.ikyvortex.vortexmusic.lyrics.LiveLyricsRepository
import com.ikyvortex.vortexmusic.lyrics.activeIndexAt
import com.ikyvortex.vortexmusic.lyrics.karaokeFill
import com.ikyvortex.vortexmusic.ui.utils.frostedGlassSurface
import com.ikyvortex.vortexmusic.ui.utils.liquidGlassPill
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.makeTimeString
import com.ikyvortex.vortexmusic.utils.rememberEnumPreference
import com.ikyvortex.vortexmusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object DynamicIslandOverlayState {
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    @Volatile
    private var hasInitializedProcessSession = false

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    suspend fun initializeOnAppProcessStart(context: Context) {
        if (hasInitializedProcessSession) return
        hasInitializedProcessSession = true
        withContext(Dispatchers.IO) {
            runCatching {
                context.dataStore.edit { settings ->
                    settings[DynamicIslandDismissedKey] = false
                    settings[DynamicIslandEnabledKey] = false
                }
            }
        }
    }

    fun resetOnAppExit(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                context.dataStore.edit { settings ->
                    settings[DynamicIslandEnabledKey] = false
                    settings[DynamicIslandDismissedKey] = false
                }
            }
        }
    }
}

internal fun Bitmap.extractVibrantIslandColor(): Color {
    val palette = Palette.from(this).maximumColorCount(16).generate()
    val candidateInt = palette.lightVibrantSwatch?.rgb
        ?: palette.vibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: return Color(0xFF38BDF8)

    val raw = Color(candidateInt)
    val maxChannel = max(raw.red, max(raw.green, raw.blue))
    val minChannel = min(raw.red, min(raw.green, raw.blue))
    val saturation = if (maxChannel > 0f) (maxChannel - minChannel) / maxChannel else 0f

    if (saturation < 0.14f) {
        return Color(0xFF38BDF8)
    }

    return if (raw.luminance() < 0.50f) {
        lerp(raw, Color.White, 0.45f)
    } else {
        raw
    }
}

data class DynamicIslandColorPreset(
    val name: String,
    val colorInt: Int,
)

val DynamicIslandAccentPresets = listOf(
    DynamicIslandColorPreset("Spotify Green", 0xFF1DB954.toInt()),
    DynamicIslandColorPreset("Electric Cyan", 0xFF00E5FF.toInt()),
    DynamicIslandColorPreset("Neon Purple", 0xFFA855F7.toInt()),
    DynamicIslandColorPreset("Crimson Rose", 0xFFFF4D67.toInt()),
    DynamicIslandColorPreset("Amber Gold", 0xFFF59E0B.toInt()),
    DynamicIslandColorPreset("Ocean Blue", 0xFF3B82F6.toInt()),
    DynamicIslandColorPreset("Coral Sunset", 0xFFFF6B4A.toInt()),
    DynamicIslandColorPreset("Pure White", 0xFFFFFFFF.toInt()),
)

val DynamicIslandBgPresets = listOf(
    DynamicIslandColorPreset("Crystal Glass", 0xFF1A2234.toInt()),
    DynamicIslandColorPreset("Deep Black", 0xFF000000.toInt()),
    DynamicIslandColorPreset("Charcoal Glass", 0xFF12141C.toInt()),
    DynamicIslandColorPreset("Midnight Blue", 0xFF0B132B.toInt()),
    DynamicIslandColorPreset("Dark Emerald", 0xFF071C15.toInt()),
    DynamicIslandColorPreset("Cosmic Violet", 0xFF160C28.toInt()),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandOverlay(
    isPlayerExpanded: Boolean,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val (enabled, onEnabledChange) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (autoMode, _) = rememberPreference(DynamicIslandAutoEnableOnStartKey, defaultValue = false)
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

    var showSettingsDialog by remember { mutableStateOf(false) }
    if (showSettingsDialog) {
        DynamicIslandSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsState()?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsState()?.value ?: false
    val playbackState = playerConnection?.playbackState?.collectAsState()?.value ?: Player.STATE_IDLE
    val currentSong = playerConnection?.currentSong?.collectAsState(initial = null)?.value
    val canSkipPrevious = playerConnection?.canSkipPrevious?.collectAsState()?.value ?: false
    val canSkipNext = playerConnection?.canSkipNext?.collectAsState()?.value ?: false
    val shuffleModeEnabled = playerConnection?.shuffleModeEnabled?.collectAsState()?.value ?: false
    val repeatMode = playerConnection?.repeatMode?.collectAsState()?.value ?: Player.REPEAT_MODE_OFF

    val metadata = mediaMetadata

    LaunchedEffect(isPlaying, metadata?.id, autoMode) {
        if (autoMode && isPlaying && metadata != null) {
            if (isDismissed) {
                onDismissedChange(false)
            }
            if (!enabled) {
                onEnabledChange(true)
            }
        }
    }

    val shouldShowIsland = enabled && !isDismissed
    if (!shouldShowIsland) return

    var extractedArtworkColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(metadata?.thumbnailUrl, useDynamicAccent, useDynamicBg) {
        val thumb = metadata?.thumbnailUrl
        if (thumb.isNullOrBlank()) {
            extractedArtworkColor = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
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

    LaunchedEffect(playerConnection, playbackState, isPlaying, metadata?.id) {
        val conn = playerConnection ?: return@LaunchedEffect
        if (playbackState == Player.STATE_READY || isPlaying) {
            while (isActive) {
                position = conn.player.currentPosition.coerceAtLeast(0L)
                duration = conn.player.duration.coerceAtLeast(0L)
                delay(250L)
            }
        } else {
            position = conn.player.currentPosition.coerceAtLeast(0L)
            duration = conn.player.duration.coerceAtLeast(0L)
        }
    }

    // Lirik untuk mode Kecil & Sedang: waktu halus (50 ms) supaya isi karaoke pas dengan suara
    val liveLyrics by LiveLyricsRepository.state.collectAsState()
    val islandLyricLines = if (liveLyrics.mediaId == metadata?.id) liveLyrics.lines else emptyList()
    val lyricsOffsetMs = (currentSong?.song?.lyricsOffset ?: 0).toLong()
    var lyricPosMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection, mode, isPlaying, metadata?.id, lyricsOffsetMs) {
        val conn = playerConnection ?: return@LaunchedEffect
        if (mode == DynamicIslandMode.BOLA) return@LaunchedEffect
        while (isActive) {
            lyricPosMs = (conn.player.currentPosition + lyricsOffsetMs).coerceAtLeast(0L)
            delay(if (isPlaying) 50L else 400L)
        }
    }

    val topStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val isLiked = currentSong?.song?.liked == true
    val displayTitle = metadata?.title ?: "Vortex Music"
    val displayArtist = metadata?.artists?.joinToString(", ") { it.name } ?: "Putar lagu untuk mulai"
    val displayDuration = if (metadata != null) {
        duration.takeIf { it > 0L } ?: ((metadata.duration.toLong()) * 1000L)
    } else {
        0L
    }

    val handleDismissWithoutStoppingMusic = {
        onDismissedChange(true)
        Toast.makeText(
            context,
            "Dynamic Island disembunyikan (musik tetap berputar)",
            Toast.LENGTH_SHORT
        ).show()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = (topStatusBarPadding - 2.dp).coerceAtLeast(8.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        DynamicIslandCapsuleSurface(
            mode = mode,
            islandTheme = islandTheme,
            effectiveBgColor = effectiveBgColor,
            effectiveAccentColor = effectiveAccentColor,
            borderGlow = borderGlow,
            blurRadius = blurRadius,
            isSystemOverlay = false,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            thumbnailUrl = metadata?.thumbnailUrl,
            isPlaying = isPlaying,
            isLiked = isLiked,
            shuffleModeEnabled = shuffleModeEnabled,
            repeatMode = repeatMode,
            position = if (metadata != null) position else 0L,
            duration = displayDuration,
            canSkipPrevious = canSkipPrevious && metadata != null,
            canSkipNext = canSkipNext && metadata != null,
            onModeChange = onModeChange,
            onDismissIsland = handleDismissWithoutStoppingMusic,
            onTogglePlayPause = {
                if (playerConnection != null && metadata != null) {
                    playerConnection.player.togglePlayPause()
                } else {
                    showSettingsDialog = true
                }
            },
            onSeekTo = { newPos ->
                if (playerConnection != null && metadata != null) {
                    playerConnection.player.seekTo(newPos)
                }
            },
            onPrevious = { playerConnection?.seekToPrevious() },
            onNext = { playerConnection?.seekToNext() },
            onToggleLike = { playerConnection?.toggleLike() },
            onToggleShuffle = {
                playerConnection?.let { conn ->
                    conn.player.shuffleModeEnabled = !conn.player.shuffleModeEnabled
                }
            },
            onToggleRepeat = {
                playerConnection?.player?.toggleRepeatMode()
            },
            onOpenFullPlayer = {
                if (metadata != null && !isPlayerExpanded) {
                    onExpandPlayer()
                } else {
                    onModeChange(DynamicIslandMode.KECIL)
                }
            },
            onOpenCustomizeOrApp = { showSettingsDialog = true },
            lyricLines = islandLyricLines,
            lyricPositionMs = lyricPosMs
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandCapsuleSurface(
    mode: DynamicIslandMode,
    islandTheme: DynamicIslandTheme,
    effectiveBgColor: Color,
    effectiveAccentColor: Color,
    borderGlow: Boolean,
    blurRadius: Int,
    isSystemOverlay: Boolean,
    displayTitle: String,
    displayArtist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    shuffleModeEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    position: Long,
    duration: Long,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onModeChange: (DynamicIslandMode) -> Unit,
    onDismissIsland: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onOpenFullPlayer: () -> Unit,
    onOpenCustomizeOrApp: () -> Unit,
    lyricLines: List<LiveLine> = emptyList(),
    lyricPositionMs: Long = 0L,
) {
    val isLiquidGlass = islandTheme == DynamicIslandTheme.LIQUID_GLASS
    val cornerRadius by animateDpAsState(
        targetValue = if (mode == DynamicIslandMode.SEDANG) 32.dp else 50.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandCorner"
    )
    val shape = RoundedCornerShape(cornerRadius)

    var horizontalDragOffset by remember(mode) { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = horizontalDragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandDragOffsetX"
    )

    Surface(
        shape = shape,
        color = if (isLiquidGlass) {
            effectiveBgColor.copy(alpha = 0.36f)
        } else {
            effectiveBgColor.copy(alpha = 1f)
        },
        contentColor = Color.White,
        shadowElevation = if (isSystemOverlay) 0.dp else 18.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .padding(horizontal = if (isSystemOverlay) 4.dp else 10.dp)
            .pointerInput(mode) {
                var horizontalDragSum = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        horizontalDragSum = 0f
                    },
                    onDragCancel = {
                        horizontalDragSum = 0f
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!change.isConsumed) {
                            change.consume()
                            horizontalDragSum += dragAmount
                            horizontalDragOffset = (horizontalDragSum * 0.45f).coerceIn(-72f, 48f)
                        }
                    },
                    onDragEnd = {
                        horizontalDragOffset = 0f
                        if (horizontalDragSum < -35f) {
                            if (mode != DynamicIslandMode.BOLA) {
                                onModeChange(DynamicIslandMode.BOLA)
                            }
                        } else if (horizontalDragSum > 35f && mode == DynamicIslandMode.BOLA) {
                            onModeChange(DynamicIslandMode.KECIL)
                        }
                    }
                )
            }
            .pointerInput(mode) {
                var verticalDragSum = 0f
                detectVerticalDragGestures(
                    onDragStart = { verticalDragSum = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragSum += dragAmount
                    },
                    onDragEnd = {
                        if (verticalDragSum < -40f) {
                            if (mode == DynamicIslandMode.SEDANG) {
                                onModeChange(DynamicIslandMode.KECIL)
                            } else {
                                onDismissIsland()
                            }
                        } else if (verticalDragSum > 40f) {
                            if (mode == DynamicIslandMode.BOLA) {
                                onModeChange(DynamicIslandMode.KECIL)
                            } else if (mode == DynamicIslandMode.KECIL) {
                                onModeChange(DynamicIslandMode.SEDANG)
                            }
                        }
                    }
                )
            }
            .then(
                if (isLiquidGlass) {
                    Modifier.border(
                        width = if (borderGlow) 1.8.dp else 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = if (borderGlow) {
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    effectiveAccentColor.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.45f),
                                    effectiveAccentColor.copy(alpha = 0.72f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.80f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.48f)
                                )
                            }
                        ),
                        shape = shape
                    )
                } else if (borderGlow) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                effectiveAccentColor.copy(alpha = 0.90f),
                                Color.White.copy(alpha = 0.45f),
                                effectiveAccentColor.copy(alpha = 0.65f)
                            )
                        ),
                        shape = shape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.24f),
                        shape = shape
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Max)
        ) {
            // Vivid & Bright Liquid Glass backdrop with blurred album artwork & top specular reflection
            if (isLiquidGlass) {
                val safeBlurDp = blurRadius.coerceIn(8, 36).dp
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(safeBlurDp)
                                .alpha(0.78f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            effectiveAccentColor.copy(alpha = 0.60f),
                                            effectiveBgColor.copy(alpha = 0.72f),
                                            effectiveAccentColor.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                                .blur(safeBlurDp)
                        )
                    }
                    // Bright Crystal-Clear Liquid Glass sheen + contrast protection
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.36f),
                                        Color(0xFF0E131F).copy(alpha = 0.18f),
                                        effectiveBgColor.copy(alpha = 0.32f)
                                    )
                                )
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        effectiveAccentColor.copy(alpha = 0.22f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.16f)
                                    )
                                )
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.90f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.90f, animationSpec = tween(150)))
                },
                label = "DynamicIslandModeContent"
            ) { targetMode ->
                when (targetMode) {
                    DynamicIslandMode.BOLA -> {
                        DynamicIslandBallContent(
                            title = displayTitle,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            accentColor = effectiveAccentColor,
                            onTap = {
                                onModeChange(DynamicIslandMode.KECIL)
                            },
                            onLongPress = {
                                onOpenFullPlayer()
                            }
                        )
                    }

                    DynamicIslandMode.KECIL -> {
                        DynamicIslandSmallContent(
                            title = displayTitle,
                            artist = displayArtist,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            accentColor = effectiveAccentColor,
                            lyricLines = lyricLines,
                            lyricPositionMs = lyricPositionMs,
                            onTap = {
                                onModeChange(DynamicIslandMode.SEDANG)
                            },
                            onLongPress = {
                                onOpenFullPlayer()
                            },
                            onDismissIsland = onDismissIsland
                        )
                    }

                    DynamicIslandMode.SEDANG -> {
                        DynamicIslandMediumContent(
                            title = displayTitle,
                            artist = displayArtist,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            isLiked = isLiked,
                            shuffleModeEnabled = shuffleModeEnabled,
                            repeatMode = repeatMode,
                            position = position,
                            duration = duration,
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            accentColor = effectiveAccentColor,
                            lyricLines = lyricLines,
                            lyricPositionMs = lyricPositionMs,
                            onTogglePlayPause = onTogglePlayPause,
                            onSeekTo = onSeekTo,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onToggleLike = onToggleLike,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onCollapseToSmall = { onModeChange(DynamicIslandMode.KECIL) },
                            onDismissIsland = onDismissIsland,
                            onOpenFullPlayer = onOpenFullPlayer,
                            onOpenCustomize = onOpenCustomizeOrApp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandBallContent(
    title: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val rawProgress = if (duration > 0L) {
        (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "ballRingProgress"
    )
    val ringColor = remember(accentColor) {
        if (accentColor.luminance() < 0.30f) Color(0xFF38BDF8) else accentColor
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.2.dp.toPx()
            val inset = strokePx / 2f + 1.5.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            drawArc(
                color = Color.White.copy(alpha = 0.28f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0.005f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = (animatedProgress * 360f).coerceIn(4f, 360f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isPlaying) 0.32f else 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandWaveform(
                        isPlaying = isPlaying,
                        accentColor = Color.White,
                        barCount = 3,
                        modifier = Modifier.size(width = 17.dp, height = 15.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        DynamicIslandWaveform(
                            isPlaying = true,
                            accentColor = Color.White,
                            barCount = 3,
                            modifier = Modifier.size(width = 18.dp, height = 15.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandSmallContent(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDismissIsland: () -> Unit,
    lyricLines: List<LiveLine> = emptyList(),
    lyricPositionMs: Long = 0L,
) {
    val configuration = LocalConfiguration.current
    val pillWidth = (configuration.screenWidthDp.dp * 0.86f).coerceIn(272.dp, 368.dp)

    // Tampilan bawaan: LIRIK. Digeser (kiri/kanan) -> judul muncul 3 detik, lalu kembali ke lirik.
    var showTitle by remember { mutableStateOf(false) }
    LaunchedEffect(showTitle) {
        if (showTitle) {
            delay(3000L)
            showTitle = false
        }
    }
    val activeLyricIdx = lyricLines.activeIndexAt(lyricPositionMs)
    val showLyric = lyricLines.isNotEmpty() && activeLyricIdx >= 0 && !showTitle

    val safeDuration = duration.coerceAtLeast(1L)
    val rawProgress = if (duration > 0L) {
        (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "smallLineProgress"
    )
    val barColor = remember(accentColor) {
        if (accentColor.luminance() < 0.30f) Color(0xFF38BDF8) else accentColor
    }

    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(54.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, _ -> showTitle = true }
            }
            .combinedClickable(
                onClick = {
                    showTitle = true
                    onTap()
                },
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 9.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .background(Color.DarkGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            AnimatedContent(
                targetState = showLyric,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "SmallLyricOrTitle",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) { lyricMode ->
                if (lyricMode) {
                    // LIRIK karaoke (maks. 2 baris) menggantikan judul lagu
                    val line = lyricLines.getOrNull(activeLyricIdx)
                    val next = lyricLines.getOrNull(activeLyricIdx + 1)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (line != null) {
                            KaraokeLine(
                                text = line.text,
                                fill = line.karaokeFill(lyricPositionMs, next?.startMs),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                dimAlpha = 0.45f,
                                brightColor = Color.White,
                                maxLines = 2
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFFFFFFF),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF),
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        if (artist.isNotBlank() && artist != "Putar lagu untuk mulai") {
                            Text(
                                text = artist,
                                color = Color.White.copy(alpha = 0.85f),
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DynamicIslandWaveform(
                    isPlaying = isPlaying,
                    accentColor = barColor,
                    barCount = 5,
                    modifier = Modifier.size(width = 24.dp, height = 18.dp)
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .clickable { onDismissIsland() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Tutup Dynamic Island",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Bottom horizontal music progress line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.30f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                barColor,
                                Color.White
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandMediumContent(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    position: Long,
    duration: Long,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    accentColor: Color,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onCollapseToSmall: () -> Unit,
    onDismissIsland: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenCustomize: () -> Unit,
    lyricLines: List<LiveLine> = emptyList(),
    lyricPositionMs: Long = 0L,
) {
    val configuration = LocalConfiguration.current
    val cardWidth = (configuration.screenWidthDp.dp * 0.94f).coerceIn(316.dp, 408.dp)

    val safeDuration = duration.coerceAtLeast(1L)
    val progress = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (safeDuration - position).coerceAtLeast(0L)
    val visibleAccent = remember(accentColor) {
        if (accentColor.luminance() < 0.30f) Color(0xFF38BDF8) else accentColor
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onOpenFullPlayer() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Top Row: Larger Framed Album Art + Song Title & Artist + Waveform + Customize + Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.2.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                        .background(Color.DarkGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color(0xFFFFFFFF),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFFFFF),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist.ifBlank { "Unknown Artist" },
                    color = Color.White.copy(alpha = 0.90f),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCollapseToSmall() }
                    .padding(horizontal = 5.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicIslandWaveform(
                    isPlaying = isPlaying,
                    accentColor = visibleAccent,
                    barCount = 5,
                    modifier = Modifier.size(width = 22.dp, height = 19.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { onOpenCustomize() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = "Kostumisasi Dynamic Island",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.26f))
                    .clickable { onDismissIsland() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Tutup Dynamic Island",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Lirik karaoke (hanya tampil jika lirik lagu ini tersedia)
        if (lyricLines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            IslandLyricsBlock(
                lines = lyricLines,
                posMs = lyricPositionMs,
                accent = visibleAccent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Middle Row: Elapsed time + Bright Interactive Progress Track + Remaining time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = makeTimeString(position),
                color = Color.White,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .pointerInput(safeDuration) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekTo((frac * safeDuration).toLong())
                        }
                    }
                    .pointerInput(safeDuration) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekTo((frac * safeDuration).toLong())
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.34f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(visibleAccent, Color.White)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "-${makeTimeString(remainingMs)}",
                color = Color.White,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Row: Larger & Clearer Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (shuffleModeEnabled) Color.White.copy(alpha = 0.26f) else Color.Transparent
                    )
                    .clickable { onToggleShuffle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                    ),
                    contentDescription = "Acak Lagu",
                    tint = if (shuffleModeEnabled) visibleAccent else Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = { onSeekTo((position - 10_000L).coerceAtLeast(0L)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = "Mundur 10 Detik",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                enabled = canSkipPrevious,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Sebelumnya",
                    tint = if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Center Play/Pause Button (Large Bright White Circle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = onNext,
                enabled = canSkipNext,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Selanjutnya",
                    tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }

            val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIconRes = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                else -> R.drawable.repeat
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRepeatActive) Color.White.copy(alpha = 0.26f) else Color.Transparent
                    )
                    .clickable { onToggleRepeat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(repeatIconRes),
                    contentDescription = "Ulangi Lagu",
                    tint = if (isRepeatActive) visibleAccent else Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(21.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLiked) Color.White.copy(alpha = 0.24f) else Color.Transparent
                    )
                    .clickable { onToggleLike() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = "Sukai",
                    tint = if (isLiked) Color(0xFFFF4D67) else Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun DynamicIslandWaveform(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "islandWave")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(510, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val phase5 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    val phases = listOf(phase1, phase2, phase3, phase4, phase5)

    val visibleWaveColor = remember(accentColor) {
        if (accentColor.luminance() < 0.30f) {
            Color(0xFF38BDF8)
        } else {
            accentColor
        }
    }

    Canvas(modifier = modifier) {
        val count = barCount.coerceIn(3, 5)
        val spacing = size.width * 0.11f
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.4f)

        for (i in 0 until count) {
            val fraction = if (isPlaying) phases[i % phases.size] else 0.34f
            val barHeight = (size.height * fraction).coerceAtLeast(size.height * 0.28f)
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                color = visibleWaveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun DynamicIslandSettingsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (enabled, onEnabledChange) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (autoEnableOnStart, onAutoEnableOnStartChange) = rememberPreference(
        DynamicIslandAutoEnableOnStartKey,
        defaultValue = false
    )
    val (lockscreenLyricsEnabled, _) = rememberPreference(
        LockscreenLyricsEnabledKey,
        defaultValue = false
    )
    val (isDismissed, onDismissedChange) = rememberPreference(
        DynamicIslandDismissedKey,
        defaultValue = false
    )
    val (mode, onModeChange) = rememberEnumPreference(
        DynamicIslandModeKey,
        defaultValue = DynamicIslandMode.KECIL
    )
    val (islandTheme, onIslandThemeChange) = rememberEnumPreference(
        DynamicIslandThemeKey,
        defaultValue = DynamicIslandTheme.LIQUID_GLASS
    )
    val (accentColorInt, onAccentColorChange) = rememberPreference(
        DynamicIslandAccentColorKey,
        defaultValue = 0xFF38BDF8.toInt()
    )
    val (useDynamicAccent, onUseDynamicAccentChange) = rememberPreference(
        DynamicIslandUseDynamicAccentKey,
        defaultValue = true
    )
    val (bgColorInt, onBgColorChange) = rememberPreference(
        DynamicIslandBgColorKey,
        defaultValue = 0xFF161D2C.toInt()
    )
    val (useDynamicBg, onUseDynamicBgChange) = rememberPreference(
        DynamicIslandUseDynamicBgKey,
        defaultValue = true
    )
    val (borderGlow, onBorderGlowChange) = rememberPreference(
        DynamicIslandBorderGlowKey,
        defaultValue = true
    )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(
        DynamicIslandBlurRadiusKey,
        defaultValue = 20
    )
    val (permissionConfirmed, onPermissionConfirmedChange) = rememberPreference(
        DynamicIslandPermissionConfirmedKey,
        defaultValue = false
    )

    var canDrawOverlays by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (canDrawOverlays) {
                    onPermissionConfirmedChange(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val isMusicPlayingNow =
        (playerConnection?.isPlaying?.collectAsState()?.value == true) &&
            (playerConnection.mediaMetadata.collectAsState().value != null)

    var showPermissionPromptDialog by remember { mutableStateOf(false) }
    var pendingManualActivation by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        onPermissionConfirmedChange(true)
        onDismissedChange(false)
        if (pendingManualActivation || isMusicPlayingNow) {
            onEnabledChange(true)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        onPermissionConfirmedChange(true)
        onDismissedChange(false)
        if (pendingManualActivation || isMusicPlayingNow) {
            onEnabledChange(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            runCatching {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                )
            }.onFailure {
                runCatching {
                    overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
        }
    }

    val launchOverlaySettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                )
            }.onFailure {
                runCatching {
                    overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
        }
    }

    val previewAccent = Color(accentColorInt)
    val previewBg = Color(bgColorInt)

    if (showPermissionPromptDialog) {
        Dialog(
            onDismissRequest = { showPermissionPromptDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val permDialogShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .clip(permDialogShape)
                    .frostedGlassSurface(
                        shape = permDialogShape,
                        isHighlighted = true,
                        baseTint = Color(0xFF0A1120).copy(alpha = 0.44f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(permDialogShape)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(28.dp)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    previewAccent.copy(alpha = 0.16f),
                                    Color(0xFF38BDF8).copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .liquidGlassPill(
                                    shape = CircleShape,
                                    isSelected = true,
                                    accentColor = Color(0xFF1DB954)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.notification),
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Dynamic Island",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Muncul di dalam & di luar aplikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Agar Dynamic Island dapat tampil dengan optimal baik di dalam maupun di luar aplikasi, periksa izin berikut:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val permCardShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(permCardShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .frostedGlassSurface(
                                shape = permCardShape,
                                isHighlighted = canDrawOverlays,
                                baseTint = if (canDrawOverlays) Color(0xFF1DB954).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(if (canDrawOverlays) R.drawable.check else R.drawable.info),
                                    contentDescription = null,
                                    tint = if (canDrawOverlays) Color(0xFF1DB954) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tampilkan di Atas Aplikasi Lain",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Diperlukan agar kapsul Dynamic Island tetap melayang di layar saat kamu membuka Home atau aplikasi lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showPermissionPromptDialog = false
                                onPermissionConfirmedChange(true)
                                onDismissedChange(false)
                                if (pendingManualActivation || isMusicPlayingNow) {
                                    onEnabledChange(true)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (!canDrawOverlays) {
                                    launchOverlaySettings()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = if (!canDrawOverlays) "Berikan Izin Overlay" else "Izinkan & Aktifkan",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = {
                                showPermissionPromptDialog = false
                                onPermissionConfirmedChange(true)
                                onDismissedChange(false)
                                if (pendingManualActivation || isMusicPlayingNow) {
                                    onEnabledChange(true)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Aktifkan di Dalam Aplikasi Saja",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val mainDialogShape = RoundedCornerShape(30.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 440.dp)
                .padding(vertical = 18.dp)
                .clip(mainDialogShape)
                .frostedGlassSurface(
                    shape = mainDialogShape,
                    isHighlighted = true,
                    baseTint = Color(0xFF0A1222).copy(alpha = 0.38f)
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(mainDialogShape)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(blurRadius.dp.coerceIn(16.dp, 36.dp))
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.24f),
                                previewAccent.copy(alpha = 0.18f),
                                Color(0xFF38BDF8).copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pengaturan Dynamic Island",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kapsul musik interaktif • Liquid Glass Vivid",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .liquidGlassPill(
                                shape = CircleShape,
                                isSelected = true,
                                accentColor = previewAccent
                            )
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Stage
                val previewStageShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(previewStageShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    previewAccent.copy(alpha = 0.22f),
                                    Color(0xFF38BDF8).copy(alpha = 0.14f)
                                )
                            )
                        )
                        .frostedGlassSurface(
                            shape = previewStageShape,
                            isHighlighted = true,
                            baseTint = Color.White.copy(alpha = 0.12f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val previewModeLabel = when (mode) {
                            DynamicIslandMode.BOLA -> "MODE BOLA"
                            DynamicIslandMode.KECIL -> "MODE KECIL"
                            DynamicIslandMode.SEDANG -> "MODE SEDANG"
                        }
                        val previewThemeLabel = when (islandTheme) {
                            DynamicIslandTheme.NORMAL -> "TEMA NORMAL"
                            DynamicIslandTheme.LIQUID_GLASS -> "TEMA LIQUID GLASS"
                        }
                        Text(
                            text = "PRATINJAU LANGSUNG • $previewModeLabel • $previewThemeLabel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val previewShape = RoundedCornerShape(
                            if (mode == DynamicIslandMode.SEDANG) 28.dp else 50.dp
                        )
                        val isPreviewGlass = islandTheme == DynamicIslandTheme.LIQUID_GLASS
                        Surface(
                            shape = previewShape,
                            color = if (isPreviewGlass) previewBg.copy(alpha = 0.40f) else previewBg,
                            shadowElevation = 12.dp,
                            modifier = Modifier.then(
                                if (isPreviewGlass) {
                                    Modifier
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.32f),
                                                    previewAccent.copy(alpha = 0.26f),
                                                    Color.White.copy(alpha = 0.12f)
                                                )
                                            ),
                                            shape = previewShape
                                        )
                                        .border(
                                            width = 1.6.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.92f),
                                                    previewAccent.copy(alpha = 0.82f),
                                                    Color.White.copy(alpha = 0.35f)
                                                )
                                            ),
                                            shape = previewShape
                                        )
                                } else {
                                    Modifier.border(
                                        1.2.dp,
                                        previewAccent.copy(alpha = 0.72f),
                                        previewShape
                                    )
                                }
                            )
                        ) {
                            if (mode == DynamicIslandMode.BOLA) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .clickable { onModeChange(DynamicIslandMode.KECIL) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    DynamicIslandWaveform(
                                        isPlaying = true,
                                        accentColor = Color.White,
                                        barCount = 3,
                                        modifier = Modifier.size(width = 16.dp, height = 14.dp)
                                    )
                                }
                            } else if (mode == DynamicIslandMode.KECIL) {
                                Box(
                                    modifier = Modifier
                                        .width(250.dp)
                                        .height(50.dp)
                                        .clickable { onModeChange(DynamicIslandMode.SEDANG) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(previewAccent.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.music_note),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Passionfruit • Drake",
                                            color = Color.White,
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        DynamicIslandWaveform(
                                            isPlaying = true,
                                            accentColor = previewAccent,
                                            barCount = 5,
                                            modifier = Modifier.size(width = 20.dp, height = 16.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onModeChange(DynamicIslandMode.KECIL) }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(previewAccent.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.music_note),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Passionfruit",
                                                color = Color.White,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "Drake",
                                                color = Color.White.copy(alpha = 0.88f),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.88f)
                                                )
                                            )
                                        }
                                        DynamicIslandWaveform(
                                            isPlaying = true,
                                            accentColor = previewAccent,
                                            barCount = 5,
                                            modifier = Modifier.size(width = 20.dp, height = 16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.skip_previous),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.pause),
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.skip_next),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (enabled && isDismissed) {
                    val restoreShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(restoreShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .frostedGlassSurface(
                                shape = restoreShape,
                                isHighlighted = true,
                                baseTint = previewAccent.copy(alpha = 0.2f)
                            )
                            .clickable { onDismissedChange(false) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Island Disembunyikan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketuk untuk menampilkan kembali kapsul di atas layar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .liquidGlassPill(
                                        shape = RoundedCornerShape(50),
                                        isSelected = true,
                                        accentColor = previewAccent
                                    )
                                    .clickable { onDismissedChange(false) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Munculkan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Toggles
                val toggleCardShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(toggleCardShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .frostedGlassSurface(
                            shape = toggleCardShape,
                            isHighlighted = true,
                            baseTint = if (enabled || autoEnableOnStart || lockscreenLyricsEnabled) {
                                previewAccent.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.08f)
                            }
                        )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!enabled) {
                                        pendingManualActivation = true
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(true)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Manual",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (enabled) {
                                        "Aktif • Tetap tampil saat musik di-pause"
                                    } else {
                                        "Nonaktif • Nyalakan manual atau gunakan Otomatis"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable) {
                                        pendingManualActivation = true
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(true)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextAuto = !autoEnableOnStart
                                    onAutoEnableOnStartChange(nextAuto)
                                    if (nextAuto) {
                                        pendingManualActivation = false
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(isMusicPlayingNow)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Otomatis Saat Memutar Musik",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (autoEnableOnStart) {
                                        "Aktif • Muncul otomatis saat musik diputar"
                                    } else {
                                        "Nonaktif • Hanya muncul jika diaktifkan manual"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = autoEnableOnStart,
                                onCheckedChange = { nextAuto ->
                                    onAutoEnableOnStartChange(nextAuto)
                                    if (nextAuto) {
                                        pendingManualActivation = false
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(isMusicPlayingNow)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay permission card
                val overlayPermShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(overlayPermShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .frostedGlassSurface(
                            shape = overlayPermShape,
                            isHighlighted = true,
                            baseTint = if (canDrawOverlays) {
                                Color(0xFF1DB954).copy(alpha = 0.12f)
                            } else {
                                Color(0xFFF59E0B).copy(alpha = 0.16f)
                            }
                        )
                        .clickable {
                            if (!canDrawOverlays) {
                                launchOverlaySettings()
                            } else {
                                showPermissionPromptDialog = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                if (canDrawOverlays) R.drawable.check else R.drawable.info
                            ),
                            contentDescription = null,
                            tint = if (canDrawOverlays) Color(0xFF1DB954) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (canDrawOverlays) "Izin Overlay Luar Aplikasi" else "Izin Tampil di Atas Aplikasi",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canDrawOverlays) "Aktif" else "Berikan Izin",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (canDrawOverlays) Color(0xFF4ADE80) else Color(0xFFFDE68A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Selection
                Text(
                    text = "Ukuran & Mode Dynamic Island",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = previewAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeCardShape = RoundedCornerShape(16.dp)
                    listOf(
                        Triple(DynamicIslandMode.BOLA, "Bola", "Ring progres"),
                        Triple(DynamicIslandMode.KECIL, "Kecil", "Garis progres"),
                        Triple(DynamicIslandMode.SEDANG, "Sedang", "Kontrol penuh")
                    ).forEach { (itemMode, label, sub) ->
                        val isSel = mode == itemMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(modeCardShape)
                                .background(Color.White.copy(alpha = if (isSel) 0.12f else 0.05f))
                                .frostedGlassSurface(
                                    shape = modeCardShape,
                                    isHighlighted = isSel,
                                    baseTint = if (isSel) previewAccent.copy(alpha = 0.24f) else null
                                )
                                .clickable {
                                    onDismissedChange(false)
                                    onModeChange(itemMode)
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Selection
                Text(
                    text = "Tema Dynamic Island",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = previewAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themeCardShape = RoundedCornerShape(16.dp)
                    listOf(
                        Triple(DynamicIslandTheme.NORMAL, "Normal", "Kapsul gelap solid"),
                        Triple(DynamicIslandTheme.LIQUID_GLASS, "Liquid Glass", "Kaca transparan 3D")
                    ).forEach { (tItem, tLabel, tSub) ->
                        val isSel = islandTheme == tItem
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(themeCardShape)
                                .background(Color.White.copy(alpha = if (isSel) 0.12f else 0.05f))
                                .frostedGlassSurface(
                                    shape = themeCardShape,
                                    isHighlighted = isSel,
                                    baseTint = if (isSel) previewAccent.copy(alpha = 0.24f) else null
                                )
                                .clickable { onIslandThemeChange(tItem) }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = tSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = islandTheme == DynamicIslandTheme.LIQUID_GLASS) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val blurOptions = listOf(
                            "Halus" to 12,
                            "Sedang" to 20,
                            "Kuat" to 32
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blurOptions.forEach { (label, radiusVal) ->
                                val selected = abs(blurRadius - radiusVal) <= 4
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .liquidGlassPill(
                                            shape = RoundedCornerShape(12.dp),
                                            isSelected = selected,
                                            accentColor = previewAccent
                                        )
                                        .clickable { onBlurRadiusChange(radiusVal) }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Blur $label",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Customization Panel
                val colorPanelShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(colorPanelShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .frostedGlassSurface(
                            shape = colorPanelShape,
                            isHighlighted = true,
                            baseTint = previewAccent.copy(alpha = 0.12f)
                        )
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Warna Aksen & Latar Sampul",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = previewAccent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUseDynamicAccentChange(!useDynamicAccent) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Aksen otomatis ikuti sampul lagu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = useDynamicAccent,
                                onCheckedChange = onUseDynamicAccentChange
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUseDynamicBgChange(!useDynamicBg) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latar kaca ikuti warna sampul lagu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = useDynamicBg,
                                onCheckedChange = onUseDynamicBgChange
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DynamicIslandAccentPresets.forEach { preset ->
                                val isSelected = !useDynamicAccent && accentColorInt == preset.colorInt
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        onUseDynamicAccentChange(false)
                                        onAccentColorChange(preset.colorInt)
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.colorInt))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.name.substringBefore(" "),
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBorderGlowChange(!borderGlow) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Garis Tepi Berkilau (Border Glow)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = borderGlow,
                                onCheckedChange = onBorderGlowChange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = previewAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Selesai",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandLyricsBlock(
    lines: List<LiveLine>,
    posMs: Long,
    accent: Color,
) {
    val activeIdx = lines.activeIndexAt(posMs)
    if (activeIdx < 0) return

    val currentLine = lines.getOrNull(activeIdx) ?: return
    val nextLine = lines.getOrNull(activeIdx + 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        KaraokeLine(
            text = currentLine.text,
            fill = currentLine.karaokeFill(posMs, nextLine?.startMs),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            dimAlpha = 0.45f,
            brightColor = Color.White,
            maxLines = 2
        )

        if (nextLine != null && nextLine.text.isNotBlank()) {
            Text(
                text = nextLine.text,
                color = accent.copy(alpha = 0.72f),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
