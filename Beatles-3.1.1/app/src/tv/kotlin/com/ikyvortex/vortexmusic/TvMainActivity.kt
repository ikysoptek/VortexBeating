/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
 import com.ikyvortex.vortexmusic.LocalDatabase
 import com.ikyvortex.vortexmusic.LocalPlayerConnection
import com.ikyvortex.vortexmusic.constants.DarkModeKey
import com.ikyvortex.vortexmusic.constants.DynamicThemeKey
import com.ikyvortex.vortexmusic.constants.FontBoldnessKey
import com.ikyvortex.vortexmusic.constants.FontScaleKey
import com.ikyvortex.vortexmusic.constants.PureBlackKey
import com.ikyvortex.vortexmusic.constants.SelectedFontKey
import com.ikyvortex.vortexmusic.constants.SelectedThemeColorKey
import com.ikyvortex.vortexmusic.constants.ContentCountryKey
import com.ikyvortex.vortexmusic.constants.CountryCodeToName
import com.ikyvortex.vortexmusic.constants.SYSTEM_DEFAULT
 import com.ikyvortex.vortexmusic.db.MusicDatabase
 import com.ikyvortex.vortexmusic.listentogether.ListenTogetherManager
 import com.ikyvortex.vortexmusic.playback.MusicService
 import com.ikyvortex.vortexmusic.playback.MusicService.MusicBinder
 import com.ikyvortex.vortexmusic.playback.PlayerConnection
 import com.ikyvortex.vortexmusic.ui.component.LocalMenuState
 import com.ikyvortex.vortexmusic.ui.theme.AuraMusicTheme
import com.ikyvortex.vortexmusic.ui.theme.DefaultThemeColor
import com.ikyvortex.vortexmusic.ui.screens.settings.DarkMode
import com.ikyvortex.vortexmusic.ui.tv.TvApp
import com.ikyvortex.vortexmusic.utils.SyncUtils
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.rememberEnumPreference
import com.ikyvortex.vortexmusic.utils.rememberPreference
import com.ikyvortex.innertube.YouTube
import com.ikyvortex.innertube.models.YouTubeLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Activity that hosts the Android TV (Compose) shell for AuraMusic.
 *
 * It owns the [PlayerConnection] lifecycle and exposes it as a flow that the
 * Compose UI observes. This avoids the race where fragments/screens were
 * being created with a `null` player connection because the [MusicService]
 * binds asynchronously after `onCreate`.
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    /**
     * Design reference width (in dp) for the TV UI.
     *
     * TV devices report wildly different densities: Google TV hardware
     * (Chromecast with Google TV, Google TV Streamer, Google TV sets) reports
     * xhdpi (320dpi -> 960dp at 1080p) per the Android TV MDPI design
     * reference, while many Android TV boxes under-report density (160-213dpi
     * -> 1443-1920dp at 1080p). The same physical TV therefore yields a very
     * different dp viewport, so the layout (which was tuned on the wider
     * Android-TV dp sizes) comes out cramped and inconsistent on Google TV.
     *
     * We normalise density so every TV renders at the same reference width
     * (1920dp at 1080p), making Google TV and Android TV render identically.
     */
    private companion object {
        const val TV_REFERENCE_WIDTH_DP = 1920
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        try {
            val widthPx = newBase.resources.displayMetrics.widthPixels
            if (widthPx > 0) {
                val targetDensityDpi =
                    (widthPx * DisplayMetrics.DENSITY_DEFAULT.toFloat() / TV_REFERENCE_WIDTH_DP).toInt()
                        .coerceIn(DisplayMetrics.DENSITY_LOW, 640)
                if (targetDensityDpi != configuration.densityDpi) {
                    configuration.densityDpi = targetDensityDpi
                }
            }
        } catch (e: Exception) {
            Timber.tag("TvMainActivity").w(e, "Failed to normalise TV display density")
        }
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(configuration)
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var listenTogetherManager: ListenTogetherManager

    @Inject
    lateinit var syncUtils: SyncUtils

    private val playerConnectionFlow = MutableStateFlow<PlayerConnection?>(null)
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicBinder
            if (binder == null) {
                Timber.tag("TvMainActivity").e("Music service returned an invalid binder")
                rebindMusicService()
                return
            }
            try {
                disposePlayerConnection()
                val connection = PlayerConnection(
                    this@TvMainActivity,
                    binder,
                    database,
                    lifecycleScope,
                )
                listenTogetherManager.setPlayerConnection(connection)
                playerConnectionFlow.value = connection
                Timber.tag("TvMainActivity").d("PlayerConnection created successfully")
            } catch (e: Exception) {
                Timber.tag("TvMainActivity").e(e, "Failed to create PlayerConnection")
                rebindMusicService()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.tag("TvMainActivity").w("Music service disconnected")
            disposePlayerConnection()
        }

        override fun onBindingDied(name: ComponentName?) {
            Timber.tag("TvMainActivity").w("Music service binding died; reconnecting")
            disposePlayerConnection()
            rebindMusicService()
        }

        override fun onNullBinding(name: ComponentName?) {
            Timber.tag("TvMainActivity").e("Music service returned a null binding; reconnecting")
            disposePlayerConnection()
            rebindMusicService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val idLocale = java.util.Locale("id", "ID")
            java.util.Locale.setDefault(idLocale)
            val config = resources.configuration
            config.setLocale(idLocale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (_: Exception) {}

        // Initialize sync on TV launch
        lifecycleScope.launch {
            syncUtils.tryAutoSync()
        }
        
          setContent {
              val playerConnection by playerConnectionFlow.collectAsState()

              // Theme preferences
              val darkMode by rememberEnumPreference(DarkModeKey, com.ikyvortex.vortexmusic.ui.screens.settings.DarkMode.AUTO)
              val isSystemInDarkTheme = isSystemInDarkTheme()
              val useDarkTheme = when (darkMode) {
                  com.ikyvortex.vortexmusic.ui.screens.settings.DarkMode.AUTO -> isSystemInDarkTheme
                  com.ikyvortex.vortexmusic.ui.screens.settings.DarkMode.ON -> true
                  com.ikyvortex.vortexmusic.ui.screens.settings.DarkMode.OFF -> false
              }

              val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
              val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
              val selectedThemeColorInt by rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
              val selectedThemeColor = Color(selectedThemeColorInt)
              val dynamicThemeSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
              val themeColor = if (dynamicTheme && dynamicThemeSupported) DefaultThemeColor else selectedThemeColor
              val selectedFont by rememberPreference(SelectedFontKey, defaultValue = "DEFAULT")
              val fontScale by rememberPreference(FontScaleKey, defaultValue = 1.15f)
              val fontBoldness by rememberPreference(FontBoldnessKey, defaultValue = 0f)

              // Sync content country with YouTube so region-locked content is filtered correctly.
              val contentCountry by rememberPreference(ContentCountryKey, SYSTEM_DEFAULT)
              YouTube.locale = YouTubeLocale(
                  gl = contentCountry.takeIf { it != SYSTEM_DEFAULT }
                      ?: java.util.Locale.getDefault().country.takeIf { it in CountryCodeToName }
                      ?: "ID",
                  hl = "id",
              )

              com.ikyvortex.vortexmusic.ui.theme.VortexMusicTheme(
                  darkTheme = useDarkTheme,
                  pureBlack = pureBlack,
                  themeColor = themeColor,
                  selectedFont = selectedFont,
                  fontScale = fontScale,
                  fontBoldness = fontBoldness,
              ) {
                  CompositionLocalProvider(
                      LocalDatabase provides database,
                      LocalPlayerConnection provides playerConnection
                  ) {
                      TvApp(
                          playerConnection = playerConnection,
                      )
                  }
              }
          }
    }

    override fun onStart() {
        super.onStart()
        bindMusicService()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun disposePlayerConnection() {
        listenTogetherManager.setPlayerConnection(null)
        playerConnectionFlow.value?.dispose()
        playerConnectionFlow.value = null
    }

    private fun bindMusicService() {
        if (!serviceBound && !isFinishing && !isDestroyed) {
            serviceBound = bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
        }
    }

    private fun rebindMusicService() {
        if (serviceBound) {
            runCatching { unbindService(serviceConnection) }
            serviceBound = false
        }
        lifecycleScope.launch {
            delay(250)
            bindMusicService()
        }
    }

    override fun onDestroy() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Timber.tag("TvMainActivity").w(e, "Service was not bound")
            }
            serviceBound = false
        }
        disposePlayerConnection()
        super.onDestroy()
    }
}
