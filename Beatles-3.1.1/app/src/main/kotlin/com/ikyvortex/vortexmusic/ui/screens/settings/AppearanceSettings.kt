/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ikyvortex.vortexmusic.LocalPlayerAwareWindowInsets
import com.ikyvortex.vortexmusic.LocalPlayerConnection
import com.ikyvortex.vortexmusic.R
import com.ikyvortex.vortexmusic.constants.ChipSortTypeKey
import com.ikyvortex.vortexmusic.constants.CropAlbumArtKey
import com.ikyvortex.vortexmusic.constants.DefaultOpenTabKey
import com.ikyvortex.vortexmusic.constants.DynamicThemeKey
import com.ikyvortex.vortexmusic.constants.EnableDynamicIconKey
import com.ikyvortex.vortexmusic.constants.EnhancedLyricsKey
import com.ikyvortex.vortexmusic.constants.EnableHighRefreshRateKey
import com.ikyvortex.vortexmusic.constants.GridItemSize
import com.ikyvortex.vortexmusic.constants.GridItemsSizeKey
import com.ikyvortex.vortexmusic.constants.HidePlayerThumbnailKey
import com.ikyvortex.vortexmusic.constants.LibraryFilter
import com.ikyvortex.vortexmusic.constants.LyricsAnimationStyle
import com.ikyvortex.vortexmusic.constants.LyricsAnimationStyleKey
import com.ikyvortex.vortexmusic.constants.LyricsClickKey
import com.ikyvortex.vortexmusic.constants.LyricsConnectedLinesKey
import com.ikyvortex.vortexmusic.constants.LyricsFont
import com.ikyvortex.vortexmusic.constants.LyricsFontKey
import com.ikyvortex.vortexmusic.constants.LyricsGlowEffectKey
import com.ikyvortex.vortexmusic.constants.LyricsInstrumentalGapMsKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassEffectKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassBlurRadiusKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassCornerRadiusKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassOpacityKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassApplyHomeCardsKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassApplyPlayerKey
import com.ikyvortex.vortexmusic.constants.LiquidGlassApplyNavBarKey
import com.ikyvortex.vortexmusic.constants.HomeLayoutMode
import com.ikyvortex.vortexmusic.constants.HomeLayoutModeKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerHeightKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerCornerRadiusKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerShowFavoriteKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerShowSubscribeKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerShowPlayPauseKey
import com.ikyvortex.vortexmusic.constants.MiniPlayerShowHardwareKey
import com.ikyvortex.vortexmusic.constants.LyricsLineSpacingKey
import com.ikyvortex.vortexmusic.constants.LyricsScrollKey
import com.ikyvortex.vortexmusic.constants.LyricsTextPositionKey
import com.ikyvortex.vortexmusic.constants.LyricsTextSizeKey
import com.ikyvortex.vortexmusic.constants.PlayerBackgroundStyle
import com.ikyvortex.vortexmusic.constants.PlayerBackgroundStyleKey
import com.ikyvortex.vortexmusic.constants.PlayerButtonsStyle
import com.ikyvortex.vortexmusic.constants.PlayerButtonsStyleKey
import com.ikyvortex.vortexmusic.constants.PureBlackMiniPlayerKey
import com.ikyvortex.vortexmusic.constants.SelectedThemeColorKey
import com.ikyvortex.vortexmusic.constants.ShowCachedPlaylistKey
import com.ikyvortex.vortexmusic.constants.ShowDownloadedPlaylistKey
import com.ikyvortex.vortexmusic.constants.ShowLikedPlaylistKey
import com.ikyvortex.vortexmusic.constants.ShowTopPlaylistKey
import com.ikyvortex.vortexmusic.constants.ShowUploadedPlaylistKey
import com.ikyvortex.vortexmusic.constants.SliderStyle
import com.ikyvortex.vortexmusic.constants.SliderStyleKey
import com.ikyvortex.vortexmusic.constants.SlimNavBarKey
import com.ikyvortex.vortexmusic.constants.ListenTogetherAtTopKey
import com.ikyvortex.vortexmusic.constants.SelectedFontKey
import com.ikyvortex.vortexmusic.constants.FontScaleKey
import com.ikyvortex.vortexmusic.constants.FontBoldnessKey
import com.ikyvortex.vortexmusic.constants.SquigglySliderKey
import com.ikyvortex.vortexmusic.constants.SwipeSensitivityKey
import com.ikyvortex.vortexmusic.constants.SwipeThumbnailKey
import com.ikyvortex.vortexmusic.constants.SwipeToRemoveSongKey
import com.ikyvortex.vortexmusic.constants.SwipeToSongKey
import com.ikyvortex.vortexmusic.constants.UseNewMiniPlayerDesignKey
import com.ikyvortex.vortexmusic.constants.UseNewPlayerDesignKey
import com.ikyvortex.vortexmusic.ui.component.DefaultDialog
import com.ikyvortex.vortexmusic.ui.component.EnumDialog
import com.ikyvortex.vortexmusic.ui.component.IconButton
import com.ikyvortex.vortexmusic.ui.component.Material3SettingsGroup
import com.ikyvortex.vortexmusic.ui.component.Material3SettingsItem
import com.ikyvortex.vortexmusic.ui.component.PlayerSliderTrack
import com.ikyvortex.vortexmusic.ui.component.SamsungSlider
import com.ikyvortex.vortexmusic.ui.component.AudioVisualizerPreview
import com.ikyvortex.vortexmusic.ui.component.SquigglySlider
import com.ikyvortex.vortexmusic.ui.component.WavySlider
import com.ikyvortex.vortexmusic.ui.theme.DefaultThemeColor
import com.ikyvortex.vortexmusic.ui.theme.PlayerSliderColors
import com.ikyvortex.vortexmusic.ui.utils.backToMain
import com.ikyvortex.vortexmusic.utils.IconUtils
 import com.ikyvortex.vortexmusic.utils.rememberEnumPreference
 import com.ikyvortex.vortexmusic.utils.rememberPreference
 import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (enableDynamicIcon, onEnableDynamicIconChange) = rememberPreference(
        EnableDynamicIconKey,
        defaultValue = false
    )
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) = rememberPreference(
        EnableHighRefreshRateKey,
        defaultValue = true
    )
    val (selectedThemeColorInt) = rememberPreference(
        SelectedThemeColorKey,
        defaultValue = DefaultThemeColor.toArgb()
    )
    val (selectedFont, onSelectedFontChange) = rememberEnumPreference(
        SelectedFontKey,
        defaultValue = AppFont.DEFAULT
    )
    val (fontScale, onFontScaleChange) = rememberPreference(FontScaleKey, defaultValue = 1f)
    val (fontBoldness, onFontBoldnessChange) = rememberPreference(FontBoldnessKey, defaultValue = 0f)
    // Check if user has selected a custom color (not the default/dynamic color)
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()
    val coroutineScope = rememberCoroutineScope()

    fun handleIconChange(enabled: Boolean) {
        onEnableDynamicIconChange(enabled)
        IconUtils.setIcon(activity, enabled)
    }


    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = true
    )
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(
        UseNewMiniPlayerDesignKey,
        defaultValue = true
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (cropAlbumArt, onCropAlbumArtChange) = rememberPreference(
        CropAlbumArtKey,
        defaultValue = true
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BLUR,
        )

    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(
        LyricsScrollKey,
        defaultValue = true
    )
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) = rememberEnumPreference(
        LyricsAnimationStyleKey,
        defaultValue = LyricsAnimationStyle.NONE
    )
    val (liquidGlassEnabled, onLiquidGlassEnabledChange) = rememberPreference(
        LiquidGlassEffectKey,
        defaultValue = true
    )
    val (liquidGlassBlurRadius, onLiquidGlassBlurRadiusChange) = rememberPreference(
        LiquidGlassBlurRadiusKey,
        defaultValue = 35f
    )
    val (liquidGlassCornerRadius, onLiquidGlassCornerRadiusChange) = rememberPreference(
        LiquidGlassCornerRadiusKey,
        defaultValue = 18f
    )
    val (liquidGlassOpacity, onLiquidGlassOpacityChange) = rememberPreference(
        LiquidGlassOpacityKey,
        defaultValue = 0.20f
    )
    val (liquidGlassApplyHomeCards, onLiquidGlassApplyHomeCardsChange) = rememberPreference(
        LiquidGlassApplyHomeCardsKey,
        defaultValue = true
    )
    val (liquidGlassApplyPlayer, onLiquidGlassApplyPlayerChange) = rememberPreference(
        LiquidGlassApplyPlayerKey,
        defaultValue = true
    )
    val (liquidGlassApplyNavBar, onLiquidGlassApplyNavBarChange) = rememberPreference(
        LiquidGlassApplyNavBarKey,
        defaultValue = true
    )
    val (homeLayoutMode, onHomeLayoutModeChange) = rememberEnumPreference(
        HomeLayoutModeKey,
        defaultValue = HomeLayoutMode.GRID
    )
    val (miniPlayerHeight, onMiniPlayerHeightChange) = rememberPreference(
        MiniPlayerHeightKey,
        defaultValue = 64f
    )
    val (miniPlayerCornerRadius, onMiniPlayerCornerRadiusChange) = rememberPreference(
        MiniPlayerCornerRadiusKey,
        defaultValue = 32f
    )
    val (miniPlayerShowFavorite, onMiniPlayerShowFavoriteChange) = rememberPreference(
        MiniPlayerShowFavoriteKey,
        defaultValue = true
    )
    val (miniPlayerShowSubscribe, onMiniPlayerShowSubscribeChange) = rememberPreference(
        MiniPlayerShowSubscribeKey,
        defaultValue = true
    )
    val (miniPlayerShowPlayPause, onMiniPlayerShowPlayPauseChange) = rememberPreference(
        MiniPlayerShowPlayPauseKey,
        defaultValue = true
    )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 20f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (instrumentalGapMs, onInstrumentalGapMsChange) = rememberPreference(LyricsInstrumentalGapMsKey, defaultValue = 5000)
    val (connectedLines, onConnectedLinesChange) = rememberPreference(LyricsConnectedLinesKey, defaultValue = true)
    val (lyricsFont, onLyricsFontChange) = rememberEnumPreference(LyricsFontKey, defaultValue = LyricsFont.SYSTEM_DEFAULT)
    val (enhancedLyrics, onEnhancedLyricsChange) = rememberPreference(EnhancedLyricsKey, defaultValue = false)


    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    val (squigglySlider, onSquigglySliderChange) = rememberPreference(
        SquigglySliderKey,
        defaultValue = false
    )
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = false
    )

    val (listenTogetherAtTop, onListenTogetherAtTopChange) = rememberPreference(
        ListenTogetherAtTopKey,
        defaultValue = true
    )

    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) = rememberPreference(
        SwipeToRemoveSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) = rememberPreference(
        ShowUploadedPlaylistKey,
        defaultValue = true
    )

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }



    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        onSliderStyleChange(SliderStyle.VISUALIZER)
        onSquigglySliderChange(false)
        showSliderOptionDialog = false
    }

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsAnimationStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsTextSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsLineSpacingDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsFontDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            }
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                }
            }
        )
    }

    if (showLyricsFontDialog) {
        EnumDialog(
            onDismiss = { showLyricsFontDialog = false },
            onSelect = {
                onLyricsFontChange(it)
                showLyricsFontDialog = false
            },
            title = stringResource(R.string.lyrics_custom_font),
            current = lyricsFont,
            values = LyricsFont.values().toList(),
            valueText = {
                when (it) {
                    LyricsFont.SYSTEM_DEFAULT -> stringResource(R.string.lyrics_custom_font_default)
                    LyricsFont.OUTFIT -> "Outfit"
                    LyricsFont.MANROPE -> "Manrope"
                    LyricsFont.SPACE_GROTESK -> "Space Grotesk"
                }
            }
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
        
        DefaultDialog(
            onDismiss = { 
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false 
            },
            buttons = {
                TextButton(
                    onClick = { 
                        tempTextSize = 20f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = { 
                        tempTextSize = lyricsTextSize
                        showLyricsTextSizeDialog = false 
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = { 
                        onLyricsTextSizeChange(tempTextSize)
                        showLyricsTextSizeDialog = false 
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyrics_text_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 16f..36f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        
        DefaultDialog(
            onDismiss = { 
                tempLineSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false 
            },
            buttons = {
                TextButton(
                    onClick = { 
                        tempLineSpacing = 1.3f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = { 
                        tempLineSpacing = lyricsLineSpacing
                        showLyricsLineSpacingDialog = false 
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = { 
                        onLyricsLineSpacingChange(tempLineSpacing)
                        showLyricsLineSpacingDialog = false 
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.lyrics_line_spacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${String.format("%.1f", tempLineSpacing)}x",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempLineSpacing,
                    onValueChange = { tempLineSpacing = it },
                    valueRange = 1.0f..2.0f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = {
                onPlayerButtonsStyleChange(it)
                showPlayerButtonsStyleDialog = false
            },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            }
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = {
                onPlayerBackgroundChange(it)
                showPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.ANIMATED_GRADIENT -> stringResource(R.string.player_background_animated_gradient)
                }
            }
        )
    }



    var showDefaultOpenTabDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                    NavigationTab.PROFILE -> stringResource(R.string.profile)
                }
            }
        )
    }

    var showDefaultChipDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showFontSelectionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showFontSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showFontBoldnessDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.AUDIOBOOKS -> stringResource(R.string.audiobooks)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            }
        )
    }

    if (showFontSelectionDialog) {
        EnumDialog(
            onDismiss = { showFontSelectionDialog = false },
            onSelect = {
                onSelectedFontChange(it)
                showFontSelectionDialog = false
            },
            title = stringResource(R.string.font_selection),
            current = selectedFont,
            values = AppFont.values().toList(),
            valueText = {
                when (it) {
                    AppFont.DEFAULT -> stringResource(R.string.default_)
                    AppFont.OUTFIT -> "Outfit"
                    AppFont.MANROPE -> "Manrope"
                    AppFont.SPACE_GROTESK -> "Space Grotesk"
                }
            }
        )
    }

    if (showFontSizeDialog) {
        var tempFontScale by remember { mutableFloatStateOf(fontScale) }

        DefaultDialog(
            onDismiss = {
                tempFontScale = fontScale
                showFontSizeDialog = false
            },
            buttons = {
                TextButton(
                    onClick = { tempFontScale = 1f }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempFontScale = fontScale
                        showFontSizeDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onFontScaleChange(tempFontScale)
                        showFontSizeDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${(tempFontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempFontScale,
                    onValueChange = { tempFontScale = it },
                    valueRange = 0.8f..1.5f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showFontBoldnessDialog) {
        var tempFontBoldness by remember { mutableFloatStateOf(fontBoldness) }

        DefaultDialog(
            onDismiss = {
                tempFontBoldness = fontBoldness
                showFontBoldnessDialog = false
            },
            buttons = {
                TextButton(
                    onClick = { tempFontBoldness = 0f }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempFontBoldness = fontBoldness
                        showFontBoldnessDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onFontBoldnessChange(tempFontBoldness)
                        showFontBoldnessDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_boldness),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${(tempFontBoldness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempFontBoldness,
                    onValueChange = { tempFontBoldness = it },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    var showGridSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showMiniPlayerHeightDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showMiniPlayerCornerRadiusDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showMiniPlayerHeightDialog) {
        var tempHeight by remember { mutableFloatStateOf(miniPlayerHeight) }

        DefaultDialog(
            onDismiss = {
                tempHeight = miniPlayerHeight
                showMiniPlayerHeightDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempHeight = 64f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempHeight = miniPlayerHeight
                        showMiniPlayerHeightDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onMiniPlayerHeightChange(tempHeight)
                        showMiniPlayerHeightDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mini_player_height),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${tempHeight.roundToInt()} dp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempHeight,
                    onValueChange = { tempHeight = it },
                    valueRange = 48f..96f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showMiniPlayerCornerRadiusDialog) {
        var tempCornerRadius by remember { mutableFloatStateOf(miniPlayerCornerRadius) }

        DefaultDialog(
            onDismiss = {
                tempCornerRadius = miniPlayerCornerRadius
                showMiniPlayerCornerRadiusDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempCornerRadius = 32f
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempCornerRadius = miniPlayerCornerRadius
                        showMiniPlayerCornerRadiusDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onMiniPlayerCornerRadiusChange(tempCornerRadius)
                        showMiniPlayerCornerRadiusDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mini_player_corner_radius),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${tempCornerRadius.roundToInt()} dp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Slider(
                    value = tempCornerRadius,
                    onValueChange = { tempCornerRadius = it },
                    valueRange = 0f..48f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            }
        )
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            val sliderPreviewColors = PlayerSliderColors.getSliderColors(
                MaterialTheme.colorScheme.primary,
                PlayerBackgroundStyle.DEFAULT,
                isSystemInDarkTheme()
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.DEFAULT && !squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.DEFAULT)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        val sliderValue = 0.35f
                        Slider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.default_),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.WAVY && !squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.WAVY)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        val sliderValue = 0.5f
                        WavySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            modifier = Modifier.weight(1f),
                            isPlaying = true,
                            enabled = false
                        )
                        Text(
                            text = stringResource(R.string.wavy),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.SLIM)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        val sliderValue = 0.65f
                        Slider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = sliderPreviewColors
                                )
                            },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = stringResource(R.string.slim),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.WAVY && squigglySlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.WAVY)
                                onSquigglySliderChange(true)
                                showSliderOptionDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        val sliderValue = 0.5f
                        SquigglySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            colors = sliderPreviewColors,
                            isPlaying = true,
                        )
                        Text(
                            text = stringResource(R.string.squiggly),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.LIQUID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.LIQUID)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                            }
                            .padding(12.dp)
                    ) {
                        val sliderValue = 0.4f
                        SamsungSlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.liquid),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.VISUALIZER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSliderStyleChange(SliderStyle.VISUALIZER)
                                onSquigglySliderChange(false)
                                showSliderOptionDialog = false
                                if (!hasRecordPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .padding(12.dp)
                    ) {
                        AudioVisualizerPreview(
                            waveColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.visualizer),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.ic_dynamic_icon),
                        title = { Text(stringResource(R.string.enable_dynamic_icon)) },
                        description = {
                            Text(
                                "Mengubah ikon aplikasi Vortex Music di layar utama (Home Screen) agar mengikuti warna tema sistem / Material You secara otomatis."
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableDynamicIcon,
                                onCheckedChange = { handleIconChange(it) },
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableDynamicIcon) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { handleIconChange(!enableDynamicIcon) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.speed),
                        title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                        description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                        trailingContent = {
                            Switch(
                                checked = enableHighRefreshRate,
                                onCheckedChange = onEnableHighRefreshRateChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Tema Warna Cover / Thumbnail Musik") },
                        description = {
                            Text("Otomatis sesuaikan warna Frosted Glass & aksen aplikasi dengan warna cover lagu yang sedang diputar")
                        },
                        trailingContent = {
                            Switch(
                                checked = dynamicTheme,
                                onCheckedChange = onDynamicThemeChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (dynamicTheme) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onDynamicThemeChange(!dynamicTheme) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.theme)) },
                        description = { Text(stringResource(R.string.theme_desc)) },
                        onClick = { navController.navigate("settings/appearance/theme") }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showDynamicIslandSettings by rememberSaveable { mutableStateOf(false) }
        val (dynamicIslandEnabled, _) = rememberPreference(
            com.ikyvortex.vortexmusic.constants.DynamicIslandEnabledKey,
            defaultValue = false
        )
        val (dynamicIslandMode, _) = rememberEnumPreference(
            com.ikyvortex.vortexmusic.constants.DynamicIslandModeKey,
            defaultValue = com.ikyvortex.vortexmusic.constants.DynamicIslandMode.KECIL
        )

        if (showDynamicIslandSettings) {
            com.ikyvortex.vortexmusic.ui.component.DynamicIslandSettingsDialog(
                onDismiss = { showDynamicIslandSettings = false }
            )
        }

        Material3SettingsGroup(
            title = "Dynamic Island",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.equalizer),
                    title = { Text("Spotify Dynamic Island") },
                    description = {
                        val modeLabel = when (dynamicIslandMode) {
                            com.ikyvortex.vortexmusic.constants.DynamicIslandMode.BOLA -> "Bola"
                            com.ikyvortex.vortexmusic.constants.DynamicIslandMode.KECIL -> "Kecil"
                            com.ikyvortex.vortexmusic.constants.DynamicIslandMode.SEDANG -> "Sedang"
                        }
                        Text(
                            if (dynamicIslandEnabled) {
                                "Aktif • Mode $modeLabel • Kostumisasi Warna"
                            } else {
                                "Nonaktif bawaan • Aktifkan Mode Bola / Kecil / Sedang & Kostumisasi Warna"
                            }
                        )
                    },
                    onClick = { showDynamicIslandSettings = true }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (pureBlackMiniPlayer, onPureBlackMiniPlayerChange) = rememberPreference(
            PureBlackMiniPlayerKey,
            defaultValue = false
        )

        Material3SettingsGroup(
            title = stringResource(id = R.string.mini_player),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.new_mini_player_design)) },
                        trailingContent = {
                            Switch(
                                checked = useNewMiniPlayerDesign,
                                onCheckedChange = onUseNewMiniPlayerDesignChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (useNewMiniPlayerDesign) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onUseNewMiniPlayerDesignChange(!useNewMiniPlayerDesign) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.contrast),
                        title = { Text(stringResource(R.string.pure_black_mini_player)) },
                        trailingContent = {
                            Switch(
                                checked = pureBlackMiniPlayer,
                                onCheckedChange = onPureBlackMiniPlayerChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (pureBlackMiniPlayer) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onPureBlackMiniPlayerChange(!pureBlackMiniPlayer) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.mini_player_height)) },
                        description = { Text("${miniPlayerHeight.roundToInt()} dp") },
                        onClick = { showMiniPlayerHeightDialog = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.rounded_corner),
                        title = { Text(stringResource(R.string.mini_player_corner_radius)) },
                        description = { Text("${miniPlayerCornerRadius.roundToInt()} dp") },
                        onClick = { showMiniPlayerCornerRadiusDialog = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.favorite),
                        title = { Text(stringResource(R.string.mini_player_show_favorite)) },
                        trailingContent = {
                            Switch(
                                checked = miniPlayerShowFavorite,
                                onCheckedChange = onMiniPlayerShowFavoriteChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (miniPlayerShowFavorite) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onMiniPlayerShowFavoriteChange(!miniPlayerShowFavorite) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.subscribe),
                        title = { Text(stringResource(R.string.mini_player_show_subscribe)) },
                        trailingContent = {
                            Switch(
                                checked = miniPlayerShowSubscribe,
                                onCheckedChange = onMiniPlayerShowSubscribeChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (miniPlayerShowSubscribe) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onMiniPlayerShowSubscribeChange(!miniPlayerShowSubscribe) }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = { Text(stringResource(R.string.mini_player_show_play_pause)) },
                        trailingContent = {
                            Switch(
                                checked = miniPlayerShowPlayPause,
                                onCheckedChange = onMiniPlayerShowPlayPauseChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (miniPlayerShowPlayPause) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onMiniPlayerShowPlayPauseChange(!miniPlayerShowPlayPause) }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.new_player_design)) },
                    trailingContent = {
                        Switch(
                            checked = useNewPlayerDesign,
                            onCheckedChange = onUseNewPlayerDesignChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useNewPlayerDesign) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onUseNewPlayerDesignChange(!useNewPlayerDesign) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.gradient),
                    title = { Text(stringResource(R.string.player_background_style)) },
                    description = {
                        Text(
                            when (playerBackground) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.ANIMATED_GRADIENT -> stringResource(R.string.player_background_animated_gradient)
                            }
                        )
                    },
                    onClick = { showPlayerBackgroundDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.hide_image),
                    title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                    description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hidePlayerThumbnail,
                            onCheckedChange = onHidePlayerThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.crop),
                    title = { Text(stringResource(R.string.crop_album_art)) },
                    description = { Text(stringResource(R.string.crop_album_art_desc)) },
                    trailingContent = {
                        Switch(
                            checked = cropAlbumArt,
                            onCheckedChange = onCropAlbumArtChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (cropAlbumArt) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onCropAlbumArtChange(!cropAlbumArt) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    description = {
                        Text(
                            when (playerButtonsStyle) {
                                PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                            }
                        )
                    },
                    onClick = { showPlayerButtonsStyleDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.sliders),
                    title = { Text(stringResource(R.string.player_slider_style)) },
                    description = {
                        Text(
                            when (sliderStyle) {
                                SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                SliderStyle.WAVY -> if (squigglySlider) stringResource(R.string.squiggly) else stringResource(R.string.wavy)
                                SliderStyle.SLIM -> stringResource(R.string.slim)
                                SliderStyle.LIQUID -> stringResource(R.string.liquid)
                                SliderStyle.VISUALIZER -> stringResource(R.string.visualizer)
                            }
                        )
                    },
                    onClick = { showSliderOptionDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    trailingContent = {
                        Switch(
                            checked = swipeThumbnail,
                            onCheckedChange = onSwipeThumbnailChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeThumbnail) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeThumbnailChange(!swipeThumbnail) }
                )
            ) + if (swipeThumbnail) listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.swipe_sensitivity)) },
                    description = {
                        Text(
                            stringResource(
                                R.string.sensitivity_percentage,
                                (swipeSensitivity * 100).roundToInt()
                            )
                        )
                    },
                    onClick = { showSensitivityDialog = true }
                )
            ) else emptyList()
        )

        if (showSensitivityDialog) {
            var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

            DefaultDialog(
                onDismiss = {
                    tempSensitivity = swipeSensitivity
                    showSensitivityDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempSensitivity = 0.73f
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempSensitivity = swipeSensitivity
                            showSensitivityDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onSwipeSensitivityChange(tempSensitivity)
                            showSensitivityDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.swipe_sensitivity),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.sensitivity_percentage,
                            (tempSensitivity * 100).roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempSensitivity,
                        onValueChange = { tempSensitivity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics),
            items = listOfNotNull(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    description = {
                        Text(
                            when (lyricsPosition) {
                                LyricsPosition.LEFT -> stringResource(R.string.left)
                                LyricsPosition.CENTER -> stringResource(R.string.center)
                                LyricsPosition.RIGHT -> stringResource(R.string.right)
                            }
                        )
                    },
                    onClick = { showLyricsPositionDialog = true }
                ),
                // Animation style: hidden when Enhanced Lyrics is enabled — the
                // enhanced renderer ships with its own word-level animations.
                if (!enhancedLyrics) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_animation_style)) },
                    description = {
                        Text(
                            when (lyricsAnimationStyle) {
                                LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                                LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                                LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                                LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                                LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                                LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
                            }
                        )
                    },
                    onClick = { showLyricsAnimationStyleDialog = true }
                ) else null,
                // Glow effect: hidden when Enhanced Lyrics is enabled — the
                // enhanced renderer has a built-in glow on active words.
                if (!enhancedLyrics) Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsGlowEffect,
                            onCheckedChange = onLyricsGlowEffectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) }
                ) else null,
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_text_size)) },
                    description = { Text("${lyricsTextSize.roundToInt()} sp") },
                    onClick = { showLyricsTextSizeDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                    description = { Text("${String.format("%.1f", lyricsLineSpacing)}x") },
                    onClick = { showLyricsLineSpacingDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.lyrics_instrumental_indicator)) },
                    description = {
                        Text(
                            if (instrumentalGapMs <= 0) {
                                stringResource(R.string.disabled)
                            } else {
                                stringResource(R.string.lyrics_instrumental_indicator_desc, instrumentalGapMs / 1000)
                            }
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = instrumentalGapMs > 0,
                            onCheckedChange = { enabled ->
                                onInstrumentalGapMsChange(if (enabled) 5000 else 0)
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (instrumentalGapMs > 0) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = {
                        // Cycle threshold: 3s -> 5s -> 8s -> 12s -> off
                        val next = when (instrumentalGapMs) {
                            0 -> 3000
                            3000 -> 5000
                            5000 -> 8000
                            8000 -> 12000
                            else -> 0
                        }
                        onInstrumentalGapMsChange(next)
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_connected_lines)) },
                    description = { Text(stringResource(R.string.lyrics_connected_lines_desc)) },
                    trailingContent = {
                        Switch(
                            checked = connectedLines,
                            onCheckedChange = onConnectedLinesChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (connectedLines) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onConnectedLinesChange(!connectedLines) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_custom_font)) },
                    description = {
                        Text(
                            when (lyricsFont) {
                                LyricsFont.SYSTEM_DEFAULT -> stringResource(R.string.lyrics_custom_font_default)
                                LyricsFont.OUTFIT -> "Outfit"
                                LyricsFont.MANROPE -> "Manrope"
                                LyricsFont.SPACE_GROTESK -> "Space Grotesk"
                            }
                        )
                    },
                    onClick = { showLyricsFontDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsClick,
                            onCheckedChange = onLyricsClickChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsClick) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsClickChange(!lyricsClick) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                    trailingContent = {
                        Switch(
                            checked = lyricsScroll,
                            onCheckedChange = onLyricsScrollChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (lyricsScroll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onLyricsScrollChange(!lyricsScroll) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.home_layout_mode)) },
                    description = {
                        Text(
                            when (homeLayoutMode) {
                                HomeLayoutMode.LIST -> stringResource(R.string.home_layout_list)
                                HomeLayoutMode.GRID -> stringResource(R.string.home_layout_grid)
                            }
                        )
                    },
                    onClick = { onHomeLayoutModeChange(homeLayoutMode.toggle()) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    description = {
                        Text(
                            when (defaultOpenTab) {
                                NavigationTab.HOME -> stringResource(R.string.home)
                                NavigationTab.SEARCH -> stringResource(R.string.search)
                                NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                NavigationTab.PROFILE -> stringResource(R.string.profile)
                            }
                        )
                    },
                    onClick = { showDefaultOpenTabDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tab),
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    description = {
                        Text(
                            when (defaultChip) {
                                LibraryFilter.SONGS -> stringResource(R.string.songs)
                                LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                LibraryFilter.AUDIOBOOKS -> stringResource(R.string.audiobooks)
                                LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                            }
                        )
                    },
                    onClick = { showDefaultChipDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_add)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToSong,
                            onCheckedChange = onSwipeToSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToSongChange(!swipeToSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                    trailingContent = {
                        Switch(
                            checked = swipeToRemoveSong,
                            onCheckedChange = onSwipeToRemoveSongChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    trailingContent = {
                        Switch(
                            checked = slimNav,
                            onCheckedChange = onSlimNavChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (slimNav) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSlimNavChange(!slimNav) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.group),
                    title = { Text(stringResource(R.string.listen_together_at_top)) },
                    trailingContent = {
                        Switch(
                            checked = listenTogetherAtTop,
                            onCheckedChange = onListenTogetherAtTopChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (listenTogetherAtTop) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onListenTogetherAtTopChange(!listenTogetherAtTop) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.grid_view),
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    description = {
                        Text(
                            when (gridItemSize) {
                                GridItemSize.BIG -> stringResource(R.string.big)
                                GridItemSize.SMALL -> stringResource(R.string.small)
                            }
                        )
                    },
                    onClick = { showGridSizeDialog = true }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text(stringResource(R.string.show_liked_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showLikedPlaylist,
                            onCheckedChange = onShowLikedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showLikedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.offline),
                    title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showDownloadedPlaylist,
                            onCheckedChange = onShowDownloadedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.trending_up),
                    title = { Text(stringResource(R.string.show_top_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showTopPlaylist,
                            onCheckedChange = onShowTopPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showTopPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowTopPlaylistChange(!showTopPlaylist) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.cached),
                    title = { Text(stringResource(R.string.show_cached_playlist)) },
                    trailingContent = {
                        Switch(
                            checked = showCachedPlaylist,
                            onCheckedChange = onShowCachedPlaylistChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showCachedPlaylist) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) }
                )
                // Uploaded songs feature is temporarily disabled
                // Material3SettingsItem(
                //     icon = painterResource(R.drawable.backup),
                //     title = { Text(stringResource(R.string.show_uploaded_playlist)) },
                //     trailingContent = {
                //         Switch(
                //             checked = showUploadedPlaylist,
                //             onCheckedChange = onShowUploadedPlaylistChange,
                //             thumbContent = {
                //                 Icon(
                //                     painter = painterResource(
                //                         id = if (showUploadedPlaylist) R.drawable.check else R.drawable.close
                //                     ),
                //                     contentDescription = null,
                //                     modifier = Modifier.size(SwitchDefaults.IconSize)
                //                 )
                //             }
                //         )
                //     },
                //     onClick = { onShowUploadedPlaylistChange(!showUploadedPlaylist) }
                // )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.fonts),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.ic_font),
                    title = { Text(stringResource(R.string.app_font)) },
                    description = {
                        Text(
                            when (selectedFont) {
                                AppFont.DEFAULT -> stringResource(R.string.default_)
                                AppFont.OUTFIT -> "Outfit"
                                AppFont.MANROPE -> "Manrope"
                                AppFont.SPACE_GROTESK -> "Space Grotesk"
                            }
                        )
                    },
                    onClick = { showFontSelectionDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.ic_font),
                    title = { Text(stringResource(R.string.font_size)) },
                    description = { Text("${(fontScale * 100).toInt()}%") },
                    onClick = { showFontSizeDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.ic_font),
                    title = { Text(stringResource(R.string.font_boldness)) },
                    description = { Text("${(fontBoldness * 100).toInt()}%") },
                    onClick = { showFontBoldnessDialog = true }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
    PROFILE,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

enum class AppFont {
    DEFAULT,
    OUTFIT,
    MANROPE,
    SPACE_GROTESK,
}
