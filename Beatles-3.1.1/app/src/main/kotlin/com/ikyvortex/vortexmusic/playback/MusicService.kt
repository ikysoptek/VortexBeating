/**
 * Vortex Music Project (C) 2026
 */

@file:Suppress("DEPRECATION")

package com.ikyvortex.vortexmusic.playback

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.MoreExecutors
import com.ikyvortex.innertube.PoTokenProvider
import com.ikyvortex.innertube.YouTube
import com.ikyvortex.innertube.models.SongItem
import com.ikyvortex.innertube.models.WatchEndpoint
import com.ikyvortex.lastfm.LastFM
import com.ikyvortex.vortexmusic.MainActivity
import com.ikyvortex.vortexmusic.R
import com.ikyvortex.vortexmusic.constants.AudioNormalizationKey
import com.ikyvortex.vortexmusic.constants.AudioOffload
import com.ikyvortex.vortexmusic.constants.AudioQualityKey
import com.ikyvortex.vortexmusic.constants.AudioStyleMode
import com.ikyvortex.vortexmusic.constants.AudioStyleModeKey
import com.ikyvortex.vortexmusic.constants.AudiobookIdsKey
import com.ikyvortex.vortexmusic.constants.AudiobookPositionsKey
import com.ikyvortex.vortexmusic.constants.AutoDownloadOnLikeKey
import com.ikyvortex.vortexmusic.constants.AutoLoadMoreKey
import com.ikyvortex.vortexmusic.constants.AutoSkipNextOnErrorKey
import com.ikyvortex.vortexmusic.constants.AutomixBlendPercentKey
import com.ikyvortex.vortexmusic.constants.AutomixEnabledKey
import com.ikyvortex.vortexmusic.constants.CrossfadeCurve
import com.ikyvortex.vortexmusic.constants.CrossfadeCurveKey
import com.ikyvortex.vortexmusic.constants.CrossfadeDurationKey
import com.ikyvortex.vortexmusic.constants.CrossfadeEnabledKey
import com.ikyvortex.vortexmusic.constants.CrossfadeGaplessKey
import com.ikyvortex.vortexmusic.constants.DisableLoadMoreWhenRepeatAllKey
import com.ikyvortex.vortexmusic.constants.DiscordUseDetailsKey
import com.ikyvortex.vortexmusic.constants.EnableDiscordRPCKey
import com.ikyvortex.vortexmusic.constants.EnableLastFMScrobblingKey
import com.ikyvortex.vortexmusic.constants.HideExplicitKey
import com.ikyvortex.vortexmusic.constants.HideVideoSongsKey
import com.ikyvortex.vortexmusic.constants.HistoryDuration
import com.ikyvortex.vortexmusic.constants.LastFMUseNowPlaying
import com.ikyvortex.vortexmusic.constants.LateNightModeKey
import com.ikyvortex.vortexmusic.constants.MediaSessionConstants.CommandToggleLike
import com.ikyvortex.vortexmusic.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.ikyvortex.vortexmusic.constants.MediaSessionConstants.CommandToggleShuffle
import com.ikyvortex.vortexmusic.constants.MediaSessionConstants.CommandToggleStartRadio
import com.ikyvortex.vortexmusic.constants.PauseListenHistoryKey
import com.ikyvortex.vortexmusic.constants.PauseOnMute
import com.ikyvortex.vortexmusic.constants.PersistentQueueKey
import com.ikyvortex.vortexmusic.constants.PersistentShuffleAcrossQueuesKey
import com.ikyvortex.vortexmusic.constants.PlayerVolumeKey
import com.ikyvortex.vortexmusic.constants.RememberShuffleAndRepeatKey
import com.ikyvortex.vortexmusic.constants.RepeatModeKey
import com.ikyvortex.vortexmusic.constants.ScrobbleDelayPercentKey
import com.ikyvortex.vortexmusic.constants.ScrobbleDelaySecondsKey
import com.ikyvortex.vortexmusic.constants.ScrobbleMinSongDurationKey
import com.ikyvortex.vortexmusic.constants.ShowLyricsKey
import com.ikyvortex.vortexmusic.constants.ShuffleModeKey
import com.ikyvortex.vortexmusic.constants.ShufflePlaylistFirstKey
import com.ikyvortex.vortexmusic.constants.SimilarContent
import com.ikyvortex.vortexmusic.constants.SkipSilenceInstantKey
import com.ikyvortex.vortexmusic.constants.SkipSilenceKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockEnabledKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipFillerKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipInteractionKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipIntroKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipMusicOffTopicKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipOutroKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipPreviewKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipSelfPromoKey
import com.ikyvortex.vortexmusic.constants.SponsorBlockSkipSponsorKey
import com.ikyvortex.vortexmusic.constants.VideoQualityKey
import com.ikyvortex.vortexmusic.db.MusicDatabase
import com.ikyvortex.vortexmusic.db.entities.ArtistEntity
import com.ikyvortex.vortexmusic.db.entities.Event
import com.ikyvortex.vortexmusic.db.entities.FormatEntity
import com.ikyvortex.vortexmusic.db.entities.LyricsEntity
import com.ikyvortex.vortexmusic.db.entities.RelatedSongMap
import com.ikyvortex.vortexmusic.db.entities.Song
import com.ikyvortex.vortexmusic.di.DownloadCache
import com.ikyvortex.vortexmusic.di.PlayerCache
import com.ikyvortex.vortexmusic.discord.DiscordActivity
import com.ikyvortex.vortexmusic.discord.DiscordActivityBuilder
import com.ikyvortex.vortexmusic.discord.DiscordDefaults
import com.ikyvortex.vortexmusic.discord.DiscordRpcManager
import com.ikyvortex.vortexmusic.discord.PresenceStatus
import com.ikyvortex.vortexmusic.eq.EqualizerService
import com.ikyvortex.vortexmusic.eq.audio.CustomEqualizerAudioProcessor
import com.ikyvortex.vortexmusic.eq.audio.VocalSuppressionAudioProcessor
import com.ikyvortex.vortexmusic.eq.data.EQProfileRepository
import com.ikyvortex.vortexmusic.extensions.SilentHandler
import com.ikyvortex.vortexmusic.extensions.collect
import com.ikyvortex.vortexmusic.extensions.collectLatest
import com.ikyvortex.vortexmusic.extensions.currentMetadata
import com.ikyvortex.vortexmusic.extensions.findNextMediaItemById
import com.ikyvortex.vortexmusic.extensions.mediaItems
import com.ikyvortex.vortexmusic.extensions.metadata
import com.ikyvortex.vortexmusic.extensions.setOffloadEnabled
import com.ikyvortex.vortexmusic.extensions.toEnum
import com.ikyvortex.vortexmusic.extensions.toMediaItem
import com.ikyvortex.vortexmusic.extensions.toPersistQueue
import com.ikyvortex.vortexmusic.extensions.toQueue
import com.ikyvortex.vortexmusic.constants.LockscreenLyricsEnabledKey
import com.ikyvortex.vortexmusic.constants.DynamicIslandEnabledKey
import com.ikyvortex.vortexmusic.lyrics.LiveLyricsRepository
import com.ikyvortex.vortexmusic.lyrics.LiveLyricsState
import com.ikyvortex.vortexmusic.lyrics.LyricsHelper
import com.ikyvortex.vortexmusic.models.MediaMetadata
import com.ikyvortex.vortexmusic.models.PersistPlayerState
import com.ikyvortex.vortexmusic.models.PersistQueue
import com.ikyvortex.vortexmusic.models.toMediaMetadata
import com.ikyvortex.vortexmusic.playback.audio.DynamicRangeCompressionAudioProcessor
import com.ikyvortex.vortexmusic.playback.audio.ReverbAudioProcessor
import com.ikyvortex.vortexmusic.playback.audio.DualStemAudioEngine
import com.ikyvortex.vortexmusic.playback.audio.RemoveMusicVocalRemover
import com.ikyvortex.vortexmusic.playback.audio.VocalSeparationStatus
import com.ikyvortex.vortexmusic.playback.audio.SilenceDetectorAudioProcessor
import com.ikyvortex.vortexmusic.playback.audio.StudioAudioProcessorChain
import com.ikyvortex.vortexmusic.playback.queues.EmptyQueue
import com.ikyvortex.vortexmusic.playback.queues.Queue
import com.ikyvortex.vortexmusic.playback.queues.YouTubeQueue
import com.ikyvortex.vortexmusic.playback.queues.filterExplicit
import com.ikyvortex.vortexmusic.playback.queues.filterVideoSongs
import com.ikyvortex.vortexmusic.service.LockscreenLyricsBridge
import com.ikyvortex.vortexmusic.sponsorblock.SponsorBlockManager
import com.ikyvortex.vortexmusic.streak.StreakManager
import com.ikyvortex.vortexmusic.subtitles.SubtitleInfo
import com.ikyvortex.vortexmusic.utils.AUDIOBOOK_MIN_DURATION_SECONDS
import com.ikyvortex.vortexmusic.utils.AUDIOBOOK_RESUME_THRESHOLD_MS
import com.ikyvortex.vortexmusic.utils.VortexPlayerUtils
import com.ikyvortex.vortexmusic.utils.CoilBitmapLoader
import com.ikyvortex.vortexmusic.utils.NetworkConnectivityObserver
import com.ikyvortex.vortexmusic.utils.ScrobbleManager
import com.ikyvortex.vortexmusic.utils.SyncUtils
import com.ikyvortex.vortexmusic.utils.YTPlayerUtils
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.decodeAudiobookIds
import com.ikyvortex.vortexmusic.utils.decodeAudiobookPositions
import com.ikyvortex.vortexmusic.utils.encodeAudiobookPositions
import com.ikyvortex.vortexmusic.utils.get
import com.ikyvortex.vortexmusic.utils.reportException
import com.ikyvortex.vortexmusic.widget.CompactSquareWidgetReceiver
import com.ikyvortex.vortexmusic.widget.CompactWideWidgetReceiver
import com.ikyvortex.vortexmusic.widget.LiquidGlassWidgetReceiver
import com.ikyvortex.vortexmusic.widget.LiquidGlassWideWidgetReceiver
import com.ikyvortex.vortexmusic.widget.MusicWidgetReceiver
import com.ikyvortex.vortexmusic.widget.VortexMusicWidgetManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest as flowCollectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

private const val INSTANT_SILENCE_SKIP_STEP_MS = 15_000L
private const val INSTANT_SILENCE_SKIP_SETTLE_MS = 350L

private data class SponsorBlockPreferences(
    val enabled: Boolean,
    val skipSponsor: Boolean,
    val skipSelfPromo: Boolean,
    val skipInteraction: Boolean,
    val skipIntro: Boolean,
    val skipOutro: Boolean,
    val skipPreview: Boolean,
    val skipMusicOffTopic: Boolean,
    val skipFiller: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var equalizerService: EqualizerService

    @Inject
    lateinit var eqProfileRepository: EQProfileRepository

    @Inject
    lateinit var widgetManager: VortexMusicWidgetManager

    @Inject
    lateinit var poTokenProvider: PoTokenProvider

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false
    private var reentrantFocusGain = false
    private var wasPlayingBeforeVolumeMute = false
    private var isPausedByVolumeMute = false
    private var dynamicRangeCompressionProcessor: DynamicRangeCompressionAudioProcessor? = null

    private lateinit var foregroundNotification: Notification

    @Volatile
    private var latestMediaNotification: Notification? = null

    private var crossfadeEnabled = false
    private var crossfadeDuration = 5000f
    private var crossfadeGapless = true
    private var crossfadeCurve = CrossfadeCurve.EQUAL_POWER
    private var crossfadeTriggerJob: Job? = null
    private var automixEnabled = false
    private var automixBlendPercent = 90f

    private val secondaryPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Timber.tag(TAG).e(error, "Secondary player error")
            secondaryPlayer?.let { failedPlayer ->
                failedPlayer.stop()
                failedPlayer.clearMediaItems()
                playerSilenceProcessors.remove(failedPlayer)
                playerReverbProcessors.remove(failedPlayer)
                failedPlayer.release()
            }
            secondaryPlayer = null
        }
    }

    private var scope = CoroutineScope(Dispatchers.Main) + Job()

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private lateinit var audioQuality: com.ikyvortex.vortexmusic.constants.AudioQuality

    private var currentQueue: Queue = EmptyQueue
    private var currentQueueGeneration = 0L
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.ikyvortex.vortexmusic.models.MediaMetadata?>(null)
    val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private var dynamicIslandSystemOverlayManager: com.ikyvortex.vortexmusic.ui.component.DynamicIslandSystemOverlayManager? = null
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    private fun MediaMetadata.toDiscordSong() =
        Song(
            song = toSongEntity(),
            artists = artists.map { artist ->
                ArtistEntity(
                    id = artist.id ?: "discord:${artist.name}",
                    name = artist.name,
                )
            },
            album = null,
        )

    private fun syncDiscordState() {
        if (!discordRpcEnabled) return
        scope.launch {
            val metadata = currentMediaMetadata.value
            val song = currentSong.value
                ?.takeIf { it.song.id == metadata?.id }
                ?: metadata?.toDiscordSong()
            if (song == null) {
                if (DiscordRpcManager.isReady()) DiscordRpcManager.clear()
                return@launch
            }
            if (!DiscordRpcManager.isReady()) {
                val token = DiscordRpcManager.getAccessToken()
                if (token != null) {
                    if (!DiscordRpcManager.isInitialized()) DiscordRpcManager.init(this@MusicService)
                    DiscordRpcManager.reconnectWithToken(token)
                }
                return@launch
            }
            updateDiscordRPC(song, player.isPlaying)
        }
    }

    private fun updateDiscordRPC(song: Song, isPlaying: Boolean) {
        if (!DiscordRpcManager.isReady() || !discordRpcEnabled) return
        val useDetails = dataStore.get(DiscordUseDetailsKey, false)
        val position = player.currentPosition
        val speed = player.playbackParameters.speed.takeIf { it > 0f } ?: 1f
        val now = System.currentTimeMillis()
        val startTimestamp = now - (position / speed).toLong()
        val durationMs = song.song.duration.takeIf { it > 0 }?.times(1000L)
        val endTimestamp = durationMs?.let { d ->
            val remaining = d - position
            if (remaining > 0) now + (remaining / speed).toLong() else null
        }
        val artistName = song.artists.joinToString { it.name }.ifEmpty { DiscordDefaults.UNKNOWN_ARTIST }
        val activity = DiscordActivityBuilder.build(
            song = song,
            artistName = artistName,
            albumName = song.album?.title,
            artistThumbnail = song.artists.firstOrNull()?.thumbnailUrl,
            songTitle = song.song.title,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            advancedMode = false,
            activityType = DiscordActivity.TYPE_LISTENING,
        )
        DiscordRpcManager.setActivity(
            activity = activity,
            songId = song.song.id,
            isPlaying = isPlaying,
            status = PresenceStatus.Online,
            statusDisplayType = if (useDetails) 2 else 1,
        )
    }

    lateinit var playerVolume: MutableStateFlow<Float>
    val isMuted = MutableStateFlow(false)

    fun toggleMute() {
        isMuted.value = !isMuted.value
    }

    fun setMuted(muted: Boolean) {
        isMuted.value = muted
    }

    lateinit var sleepTimer: SleepTimer
    lateinit var sponsorBlockManager: SponsorBlockManager

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
        private set
    private var secondaryPlayer: ExoPlayer? = null
    private var fadingPlayer: ExoPlayer? = null
    private var isCrossfading = false
    private var crossfadeJob: Job? = null

    private lateinit var mediaSession: MediaLibrarySession

    private val playerInitialized = MutableStateFlow(false)
    val isPlayerReady: kotlinx.coroutines.flow.StateFlow<Boolean> = playerInitialized.asStateFlow()

    private val _playerFlow = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow = _playerFlow.asStateFlow()

    private val playerSilenceProcessors = java.util.concurrent.ConcurrentHashMap<Player, SilenceDetectorAudioProcessor>()
    private val playerReverbProcessors = java.util.concurrent.ConcurrentHashMap<Player, ReverbAudioProcessor>()

    val audioStyleMode = MutableStateFlow(AudioStyleMode.NORMAL)

    val vocalRemoverState = RemoveMusicVocalRemover.state

    private val dualStemAudioEngine = DualStemAudioEngine()
    private var loadedStemSongId: String? = null
    private var stemSyncJob: Job? = null
    @Volatile
    private var stemRefPositionMs: Long = 0L
    @Volatile
    private var stemRefElapsedRealtimeMs: Long = 0L
    @Volatile
    private var stemMainIsPlaying: Boolean = false

    private fun refreshStemClockFromMainThread() {
        if (::player.isInitialized) {
            stemRefPositionMs = player.currentPosition.coerceAtLeast(0L)
            stemRefElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime()
            stemMainIsPlaying = player.playWhenReady && player.playbackState == Player.STATE_READY
        }
    }

    fun startVocalInstrumentSeparation() {
        val meta = currentMediaMetadata.value
        val currentSongId = meta?.id
            ?: (if (::player.isInitialized) player.currentMediaItem?.mediaId else null)
        val songTitle = meta?.title
        val songArtist = meta?.artists?.joinToString(", ") { it.name }
        val songDurationSec = meta?.duration ?: 0

        // Reset speed/pitch/reverb style to NORMAL while Vocal & Instrument Remover is active
        audioStyleMode.value = AudioStyleMode.NORMAL
        playerReverbProcessors.values.forEach {
            it.setAudioStyleMode(AudioStyleMode.NORMAL)
        }
        try {
            if (::player.isInitialized) {
                player.setOffloadEnabled(false)
                player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            }
            secondaryPlayer?.let { sec ->
                sec.setOffloadEnabled(false)
                sec.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to prepare player for vocal separation")
        }
        refreshStemClockFromMainThread()
        RemoveMusicVocalRemover.startOrActivate(
            context = this,
            songId = currentSongId,
            downloadUtil = downloadUtil,
            songTitle = songTitle,
            songArtist = songArtist,
            songDurationSec = songDurationSec,
        )
    }

    fun toggleVocalInstrumentSeparation() {
        if (RemoveMusicVocalRemover.state.value.active) {
            deactivateVocalInstrumentSeparation()
        } else {
            startVocalInstrumentSeparation()
        }
    }

    fun deactivateVocalInstrumentSeparation() {
        RemoveMusicVocalRemover.deactivate()
        stopExternalStemPlayers()
        equalizerService.setStemMixer(false, 1.0f, 1.0f)
    }

    fun isVocalSeparationCached(): Boolean {
        val currentSongId = currentMediaMetadata.value?.id
            ?: (if (::player.isInitialized) player.currentMediaItem?.mediaId else null)
            ?: "current_track"
        return RemoveMusicVocalRemover.isSessionCached(currentSongId)
    }

    fun setVocalStemVolume(volume: Float) {
        RemoveMusicVocalRemover.setVocalVolume(volume)
    }

    fun setInstrumentStemVolume(volume: Float) {
        RemoveMusicVocalRemover.setInstrumentVolume(volume)
    }

    fun saveVocalStemToDevice() {
        val meta = currentMediaMetadata.value
        val currentSongId = meta?.id
            ?: (if (::player.isInitialized) player.currentMediaItem?.mediaId else null)
        RemoveMusicVocalRemover.downloadAndSaveStemToDevice(
            context = this,
            songId = currentSongId,
            songTitle = meta?.title,
            songArtist = meta?.artists?.joinToString(", ") { it.name },
            songDurationSec = meta?.duration ?: 0,
            downloadUtil = downloadUtil,
            isVocal = true,
        )
    }

    fun saveInstrumentStemToDevice() {
        val meta = currentMediaMetadata.value
        val currentSongId = meta?.id
            ?: (if (::player.isInitialized) player.currentMediaItem?.mediaId else null)
        RemoveMusicVocalRemover.downloadAndSaveStemToDevice(
            context = this,
            songId = currentSongId,
            songTitle = meta?.title,
            songArtist = meta?.artists?.joinToString(", ") { it.name },
            songDurationSec = meta?.duration ?: 0,
            downloadUtil = downloadUtil,
            isVocal = false,
        )
    }

    private fun stopExternalStemPlayers() {
        stemSyncJob?.cancel()
        stemSyncJob = null
        loadedStemSongId = null
        dualStemAudioEngine.stop { mute ->
            equalizerService.setMuteMainForExternalStems(mute)
        }
        equalizerService.setMuteMainForExternalStems(false)
    }

    private fun releaseExternalStemPlayers() {
        stopExternalStemPlayers()
    }

    private fun ensureExternalStemPlayersLoaded(
        songId: String,
        vocalPcmFile: File,
        instrumentPcmFile: File,
        sampleRate: Int,
    ) {
        refreshStemClockFromMainThread()

        if (stemSyncJob?.isActive != true) {
            stemSyncJob = scope.launch {
                while (isActive) {
                    val vrState = RemoveMusicVocalRemover.state.value
                    if (!vrState.active ||
                        vrState.status != VocalSeparationStatus.READY_CACHED ||
                        !vrState.hasCloudStems
                    ) {
                        equalizerService.setMuteMainForExternalStems(false)
                        break
                    }
                    refreshStemClockFromMainThread()
                    delay(120)
                }
            }
        }

        if (!dualStemAudioEngine.isRunningFor(songId) || loadedStemSongId != songId) {
            loadedStemSongId = songId
            dualStemAudioEngine.start(
                songId = songId,
                vocalPcmFile = vocalPcmFile,
                instrumentPcmFile = instrumentPcmFile,
                sampleRate = sampleRate,
                isMainPlayingProvider = {
                    stemMainIsPlaying && !isMuted.value
                },
                mainPositionMsProvider = {
                    if (stemMainIsPlaying) {
                        val elapsed = (android.os.SystemClock.elapsedRealtime() - stemRefElapsedRealtimeMs)
                            .coerceIn(0L, 500L)
                        stemRefPositionMs + elapsed
                    } else {
                        stemRefPositionMs
                    }
                },
                vocalGainProvider = {
                    val sleepMult = sleepTimer.fadeVolumeMultiplier.coerceIn(0f, 1f)
                    if (isMuted.value) 0f else (RemoveMusicVocalRemover.state.value.vocalVolume * sleepMult).coerceIn(0f, 1.5f)
                },
                instrumentGainProvider = {
                    val sleepMult = sleepTimer.fadeVolumeMultiplier.coerceIn(0f, 1f)
                    if (isMuted.value) 0f else (RemoveMusicVocalRemover.state.value.instrumentVolume * sleepMult).coerceIn(0f, 1.5f)
                },
                onMuteMainPlayer = { mute ->
                    equalizerService.setMuteMainForExternalStems(mute)
                },
            )
        }
    }

    fun setAudioStyleMode(mode: AudioStyleMode) {
        // Ganti Gaya Suara = reset mode pemisah vokal & instrumen
        RemoveMusicVocalRemover.onAudioStyleChanged()
        stopExternalStemPlayers()
        equalizerService.setStemMixer(false, 1.0f, 1.0f)

        audioStyleMode.value = mode
        playerReverbProcessors.values.forEach {
            it.setAudioStyleMode(mode)
        }
        try {
            if (::player.isInitialized) {
                if (mode != AudioStyleMode.NORMAL) {
                    player.setOffloadEnabled(false)
                }
                player.playbackParameters = PlaybackParameters(mode.speed, mode.pitch)
            }
            secondaryPlayer?.let { sec ->
                if (mode != AudioStyleMode.NORMAL) {
                    sec.setOffloadEnabled(false)
                }
                sec.playbackParameters = PlaybackParameters(mode.speed, mode.pitch)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to apply audio style mode: $mode")
        }
        scope.launch {
            dataStore.edit { settings ->
                settings[AudioStyleModeKey] = mode.name
            }
        }
    }

    private val instantSilenceSkipEnabled = MutableStateFlow(false)

    private var isAudioEffectSessionOpened = false
    private var audioEffectSessionId = C.AUDIO_SESSION_ID_UNSET
    private var loudnessEnhancer: LoudnessEnhancer? = null

    @Volatile private var discordRpcEnabled = false
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: kotlinx.coroutines.Job? = null

    private var scrobbleManager: ScrobbleManager? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var originalQueueSize: Int = 0

    private var consecutivePlaybackErr = 0
    private var retryJob: Job? = null
    private var retryCount = 0
    private var silenceSkipJob: Job? = null

    private data class StreamUrlEntry(
        val url: String,
        val expiresAtMs: Long,
        val headers: Map<String, String> = emptyMap(),
    )

    private val songUrlCache = HashMap<String, StreamUrlEntry>()

    private var currentMediaIdRetryCount = mutableMapOf<String, Int>()
    private val MAX_RETRY_PER_SONG = 3
    private val RETRY_DELAY_MS = 1000L

    private val recentlyFailedSongs = mutableSetOf<String>()
    private var failedSongsClearJob: Job? = null

    var castConnectionHandler: CastConnectionHandler? = null
        private set

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (!player.isPlaying && DiscordRpcManager.isReady()) {
                        DiscordRpcManager.disconnect()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (player.isPlaying) {
                        syncDiscordState()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        // Muat lirik lagu yang sedang diputar (dipakai layar kunci + Dynamic Island).
        // Hanya jika salah satu fitur itu aktif, supaya tidak menghabiskan data tanpa perlu.
        scope.launch {
            val wantLyricsFlow = applicationContext.dataStore.data
                .map { prefs ->
                    (prefs[LockscreenLyricsEnabledKey] ?: false) || (prefs[DynamicIslandEnabledKey] ?: false)
                }
                .distinctUntilChanged()
            combine(
                LockscreenLyricsBridge.snapshot.map { it.mediaId }.distinctUntilChanged(),
                wantLyricsFlow
            ) { id, want -> id to want }
                .flowCollectLatest { (id, want) ->
                    if (id.isNullOrBlank() || !want) {
                        LiveLyricsRepository.publish(LiveLyricsState(mediaId = id))
                        return@flowCollectLatest
                    }
                    LiveLyricsRepository.publish(LiveLyricsState(mediaId = id, loading = true))
                    val snap = LockscreenLyricsBridge.snapshot.value
                    val lines = LiveLyricsRepository.load(
                        context = applicationContext,
                        database = database,
                        lyricsHelper = lyricsHelper,
                        mediaId = id,
                        title = snap.title,
                        artist = snap.artist,
                        durationSec = snap.durationSec,
                        thumbnailUrl = snap.thumbnailUrl,
                    )
                    LiveLyricsRepository.publish(LiveLyricsState(mediaId = id, lines = lines, loading = false))
                }
        }

        // Kirim player + status mode video ke overlay layar kunci (video lirik)
        scope.launch {
            combine(videoModeEnabled, playerFlow) { videoOn, exo -> videoOn to exo }
                .collect { (videoOn, exo) ->
                    LockscreenLyricsBridge.setVideoOutput(exo, videoOn)
                }
        }

        setListener(
            object : MediaSessionService.Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    Timber.tag(TAG).w("onForegroundServiceStartNotAllowedException: re-promoting to foreground")
                    promoteToForegroundWithLatestNotification()
                }
            },
        )

        playerInitialized.value = false

        val isTv = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)

        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.music_player),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            if (isTv) {
                nm?.createNotificationChannel(
                    NotificationChannel(
                        TV_CHANNEL_ID,
                        "Sedang Diputar",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Menampilkan musik yang sedang diputar di peluncur TV"
                        setShowBadge(false)
                    }
                )
            }
            val pendingIntentClass = if (isTv) {
                try { Class.forName("com.ikyvortex.vortexmusic.TvMainActivity") } catch (_: Exception) { null }
            } else null
            val pending = PendingIntent.getActivity(
                this,
                0,
                Intent(this, pendingIntentClass ?: MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notificationChannelId = if (isTv) TV_CHANNEL_ID else CHANNEL_ID
            val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle(getString(R.string.music_player))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pending)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .build()
            foregroundNotification = notification
            latestMediaNotification = notification
            startForegroundSafely(notification)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to create foreground notification")
            reportException(e)
        }

        val defaultMediaNotificationProvider =
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                if (isTv) TV_CHANNEL_ID else CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.ic_notification_icon)
                }

        setMediaNotificationProvider(
            object : MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    mediaButtonPreferences: ImmutableList<CommandButton>,
                    actionFactory: MediaNotification.ActionFactory,
                    onNotificationChangedCallback: MediaNotification.Provider.Callback,
                ): MediaNotification {
                    val trackingCallback =
                        MediaNotification.Provider.Callback { notification ->
                            latestMediaNotification = notification.notification
                            onNotificationChangedCallback.onNotificationChanged(notification)
                        }

                    return defaultMediaNotificationProvider
                        .createNotification(
                            mediaSession,
                            mediaButtonPreferences,
                            actionFactory,
                            trackingCallback,
                        ).also { mediaNotification ->
                            latestMediaNotification = mediaNotification.notification
                        }
                }

                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: Bundle,
                ): Boolean = defaultMediaNotificationProvider.handleCustomCommand(session, action, extras)

                override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
                    val info = defaultMediaNotificationProvider.notificationChannelInfo
                    return if (isTv) {
                        MediaNotification.Provider.NotificationChannelInfo(
                            TV_CHANNEL_ID,
                            getString(R.string.music_player),
                        )
                    } else {
                        info
                    }
                }
            },
        )

        player = createExoPlayer()
        player.addListener(this@MusicService)

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.w("Video error listener: errorCode=${error.errorCode}, message=${error.message}")
            }
        })
        sleepTimer = SleepTimer(scope, player)
        player.addListener(sleepTimer)
        player.addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))

        sponsorBlockManager = SponsorBlockManager(this@MusicService, scope)
        scope.launch { sponsorBlockManager.loadPreferences() }

        scope.launch {
            while (true) {
                if (sponsorBlockManager.enabled.value && player.isPlaying &&
                    !_isVideoSwitching.value && !isCrossfading && secondaryPlayer == null
                ) {
                    val skipTo = sponsorBlockManager.findSkipTarget(player.currentPosition)
                    if (skipTo != null && skipTo > player.currentPosition) {
                        runCatching { player.seekTo(skipTo) }
                            .onFailure { Timber.e(it, "periodic SponsorBlock skip failed") }
                    }
                }
                delay(250)
            }
        }

        playerInitialized.value = true
        Timber.tag(TAG).d("Player successfully initialized")

        scope.launch {
            automixItems.collect {
                enqueueNextAutomixItemIfNeeded()
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(
                            this,
                            if (isTv) {
                                try {
                                    Class.forName("com.ikyvortex.vortexmusic.TvMainActivity")
                                } catch (_: Exception) {
                                    MainActivity::class.java
                                }
                            } else {
                                MainActivity::class.java
                            },
                        ),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            player.shuffleModeEnabled = dataStore.get(ShuffleModeKey, false)
        }

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)

        audioQuality = dataStore.get(AudioQualityKey).toEnum(com.ikyvortex.vortexmusic.constants.AudioQuality.AUTO)
        playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

        val savedVideoQuality = dataStore.get(VideoQualityKey, "QUALITY_720P")
        val auraVideoQuality = when (savedVideoQuality) {
            "QUALITY_1080P" -> com.ikyvortex.auravideo.VortexVideo.VideoQuality.QUALITY_1080P
            "QUALITY_720P" -> com.ikyvortex.auravideo.VortexVideo.VideoQuality.QUALITY_720P
            "QUALITY_480P" -> com.ikyvortex.auravideo.VortexVideo.VideoQuality.QUALITY_480P
            "QUALITY_360P" -> com.ikyvortex.auravideo.VortexVideo.VideoQuality.QUALITY_360P
            else -> com.ikyvortex.auravideo.VortexVideo.VideoQuality.QUALITY_720P
        }
        com.ikyvortex.auravideo.VortexVideo.setPreferredVideoQuality(auraVideoQuality)

        initializeCast()

        dynamicIslandSystemOverlayManager =
            com.ikyvortex.vortexmusic.ui.component.DynamicIslandSystemOverlayManager(this).also {
                it.start()
            }

        scope.launch {
            eqProfileRepository.activeProfile.collect { profile ->
                if (profile != null) {
                    val result = equalizerService.applyProfile(profile)
                    if (result.isSuccess && player.playbackState == Player.STATE_READY && player.isPlaying) {
                        player.seekTo(player.currentPosition)
                    }
                } else {
                    equalizerService.disable()
                    if (player.playbackState == Player.STATE_READY && player.isPlaying) {
                        player.seekTo(player.currentPosition)
                    }
                }
            }
        }

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    triggerRetry()
                }
                if (isConnected && discordRpcEnabled && player.isPlaying) {
                    syncDiscordState()
                }
            }
        }

        dataStore.data
            .map {
                (it[com.ikyvortex.vortexmusic.constants.SleepTimerFadeOutEnabledKey] ?: true) to
                    (it[com.ikyvortex.vortexmusic.constants.SleepTimerFadeOutDurationKey] ?: 30f).toInt()
            }
            .distinctUntilChanged()
            .collectLatest(scope) { (fadeEnabled, fadeDurationSec) ->
                sleepTimer.updateFadeSettings(fadeEnabled, fadeDurationSec)
            }

        combine(playerVolume, isMuted, sleepTimer.fadeVolumeMultiplierFlow) { volume, muted, fadeMultiplier ->
            if (muted) 0f else (volume * fadeMultiplier).coerceIn(0f, 1f)
        }.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) {
            updateNotification()
            updateWidgetUI(player.isPlaying)
        }

        // Koreksi posisi lirik layar kunci tiap detik (anti-geser setelah seek / ganti kecepatan).
        scope.launch {
            while (true) {
                delay(1000)
                if (player.isPlaying) refreshLockscreenSnapshot()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
            dataStore.data.map {
                it[com.ikyvortex.vortexmusic.constants.PreferredLyricsProviderKey]
                    .toEnum(com.ikyvortex.vortexmusic.constants.PreferredLyricsProvider.SIMPMUSIC)
            }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics, preferredProvider ->
            Triple(mediaMetadata, showLyrics, preferredProvider)
        }.collectLatest(scope) { (mediaMetadata, showLyrics, preferredProvider) ->
            val isTvLocal = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
            if (!isTvLocal && showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyricsWithProvider = lyricsHelper.getLyricsFromProvider(mediaMetadata, preferredProvider)
                val existingLyrics = database.lyrics(mediaMetadata.id).first()
                if (existingLyrics == null) {
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = mediaMetadata.id,
                                lyrics = lyricsWithProvider.lyrics,
                                provider = lyricsWithProvider.provider,
                            ),
                        )
                    }
                }
            }
        }

        dataStore.data
            .map { (it[SkipSilenceKey] ?: false) to (it[SkipSilenceInstantKey] ?: false) }
            .distinctUntilChanged()
            .collectLatest(scope) { (skipSilence, instantSkip) ->
                player.skipSilenceEnabled = skipSilence
                secondaryPlayer?.skipSilenceEnabled = skipSilence

                val enableInstant = skipSilence && instantSkip
                instantSilenceSkipEnabled.value = enableInstant

                playerSilenceProcessors.values.forEach { processor ->
                    processor.instantModeEnabled = enableInstant
                    if (!enableInstant) {
                        processor.resetTracking()
                    }
                }

                if (!enableInstant) {
                    silenceSkipJob?.cancel()
                }
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { setupLoudnessEnhancer() }

        combine(
            dataStore.data.map { it[AudioOffload] ?: false },
            dataStore.data.map {
                (it[CrossfadeEnabledKey] ?: false) ||
                    (it[AutomixEnabledKey] ?: false) ||
                    ((it[AudioStyleModeKey] ?: AudioStyleMode.NORMAL.name) != AudioStyleMode.NORMAL.name)
            }
        ) { offloadPref, forceDisableOffload ->
            if (forceDisableOffload) false else offloadPref
        }.distinctUntilChanged()
            .collectLatest(scope) { useOffload ->
                player.setOffloadEnabled(useOffload)
                secondaryPlayer?.setOffloadEnabled(useOffload)
            }

        if (!DiscordRpcManager.isInitialized()) {
            DiscordRpcManager.init(this@MusicService)
        }

        dataStore.data
            .map { it[EnableDiscordRPCKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                discordRpcEnabled = enabled
                if (enabled) {
                    if (DiscordRpcManager.isReady()) {
                        syncDiscordState()
                    } else {
                        val token = DiscordRpcManager.getAccessToken()
                        if (token != null) {
                            if (!DiscordRpcManager.isInitialized()) DiscordRpcManager.init(this@MusicService)
                            DiscordRpcManager.reconnectWithToken(token)
                        }
                    }
                } else if (DiscordRpcManager.isReady()) {
                    DiscordRpcManager.disconnect()
                }
            }

        DiscordRpcManager.accessTokenFlow.collect(scope) { token ->
            if (token.isNullOrEmpty()) {
                if (DiscordRpcManager.isReady()) DiscordRpcManager.disconnect()
                return@collect
            }
            if (!discordRpcEnabled) return@collect
            if (!DiscordRpcManager.isInitialized()) DiscordRpcManager.init(this@MusicService)
            if (!DiscordRpcManager.isAuthorized()) {
                DiscordRpcManager.reconnectWithToken(token)
            }
        }

        DiscordRpcManager.connectionStatus.collect(scope) { status ->
            if (status == DiscordRpcManager.Status.Connected && discordRpcEnabled) {
                syncDiscordState()
            }
        }

        DiscordRpcManager.settingsChanged.collect(scope) {
            if (discordRpcEnabled && DiscordRpcManager.isReady()) {
                syncDiscordState()
            }
        }

        dataStore.data
            .map { it[LateNightModeKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                dynamicRangeCompressionProcessor?.enabled = enabled
            }

        dataStore.data
            .map { (it[AudioStyleModeKey] ?: AudioStyleMode.NORMAL.name).toEnum(AudioStyleMode.NORMAL) }
            .distinctUntilChanged()
            .collectLatest(scope) { mode ->
                audioStyleMode.value = mode
                if (!RemoveMusicVocalRemover.state.value.active) {
                    equalizerService.disableVocalSuppression()
                }
                playerReverbProcessors.values.forEach { processor ->
                    processor.setAudioStyleMode(mode)
                }
            }

        RemoveMusicVocalRemover.state.collect(scope) { vrState ->
            if (!vrState.active || vrState.status != VocalSeparationStatus.READY_CACHED) {
                stopExternalStemPlayers()
                equalizerService.setStemMixer(false, 1.0f, 1.0f)
            } else {
                if (::player.isInitialized) {
                    runCatching { player.setOffloadEnabled(false) }
                }
                val vPcm = vrState.vocalPcmFile
                val iPcm = vrState.instrumentPcmFile
                val sId = vrState.songId
                if (vrState.hasCloudStems && vPcm != null && iPcm != null && !sId.isNullOrBlank()) {
                    // Use sample-accurate single-AudioTrack DualStemAudioEngine (turn off mid/side DSP filter)
                    equalizerService.setStemMixer(false, 1.0f, 1.0f)
                    ensureExternalStemPlayersLoaded(sId, vPcm, iPcm, vrState.pcmSampleRate)
                } else {
                    stopExternalStemPlayers()
                    equalizerService.setStemMixer(
                        enabled = true,
                        vocalVolume = vrState.vocalVolume,
                        instrumentVolume = vrState.instrumentVolume,
                    )
                }
            }
        }

        dataStore.data
            .map { it[DiscordUseDetailsKey] ?: false }
            .debounce(1000)
            .distinctUntilChanged()
            .collect(scope) {
                DiscordRpcManager.notifySettingsChanged()
            }

        scope.launch {
            while (true) {
                delay(5000)
                if (discordRpcEnabled && player.isPlaying &&
                    player.playbackState == Player.STATE_READY) {
                    syncDiscordState()
                }
            }
        }

        dataStore.data
            .map { it[EnableLastFMScrobblingKey] ?: false }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && scrobbleManager == null) {
                    val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                    val minSongDuration = dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                    val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                    scrobbleManager = ScrobbleManager(
                        scope,
                        minSongDuration = minSongDuration,
                        scrobbleDelayPercent = delayPercent,
                        scrobbleDelaySeconds = delaySeconds
                    )
                    scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                } else if (!enabled && scrobbleManager != null) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map { it[LastFMUseNowPlaying] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                scrobbleManager?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                )
            }
            .distinctUntilChanged()
            .collect(scope) { (delayPercent, minSongDuration, delaySeconds) ->
                scrobbleManager?.let {
                    it.scrobbleDelayPercent = delayPercent
                    it.minSongDuration = minSongDuration
                    it.scrobbleDelaySeconds = delaySeconds
                }
            }

        dataStore.data
            .map { prefs ->
                val cfEnabled = prefs[CrossfadeEnabledKey] ?: false
                val cfDuration = prefs[CrossfadeDurationKey] ?: 5f
                val cfGapless = prefs[CrossfadeGaplessKey] ?: true
                val cfCurve = prefs[CrossfadeCurveKey]?.let {
                    runCatching { CrossfadeCurve.valueOf(it) }.getOrNull()
                } ?: CrossfadeCurve.EQUAL_POWER
                val amEnabled = prefs[AutomixEnabledKey] ?: false
                val amBlend = prefs[AutomixBlendPercentKey] ?: 90f
                listOf(cfEnabled, cfDuration, cfGapless, cfCurve, amEnabled, amBlend)
            }
            .distinctUntilChanged()
            .collect(scope) { values ->
                val cfEnabled = values[0] as Boolean
                val cfDuration = values[1] as Float
                val cfGapless = values[2] as Boolean
                val cfCurve = values[3] as CrossfadeCurve
                val amEnabled = values[4] as Boolean
                val amBlend = values[5] as Float

                automixEnabled = amEnabled
                automixBlendPercent = amBlend.coerceIn(10f, 100f)
                crossfadeCurve = cfCurve
                if (amEnabled) {
                    crossfadeEnabled = true
                    crossfadeDuration = 4000f
                    crossfadeGapless = false
                } else {
                    crossfadeEnabled = cfEnabled
                    crossfadeDuration = cfDuration.coerceIn(1f, 15f) * 1000f
                    crossfadeGapless = cfGapless
                }
                scheduleCrossfade()
            }

        dataStore.data
            .map { prefs ->
                SponsorBlockPreferences(
                    enabled = prefs[SponsorBlockEnabledKey] ?: false,
                    skipSponsor = prefs[SponsorBlockSkipSponsorKey] != false,
                    skipSelfPromo = prefs[SponsorBlockSkipSelfPromoKey] != false,
                    skipInteraction = prefs[SponsorBlockSkipInteractionKey] != false,
                    skipIntro = prefs[SponsorBlockSkipIntroKey] != false,
                    skipOutro = prefs[SponsorBlockSkipOutroKey] != false,
                    skipPreview = prefs[SponsorBlockSkipPreviewKey] != false,
                    skipMusicOffTopic = prefs[SponsorBlockSkipMusicOffTopicKey] != false,
                    skipFiller = prefs[SponsorBlockSkipFillerKey] != false,
                )
            }
            .distinctUntilChanged()
            .collect(scope) { sponsorBlockPrefs ->
                sponsorBlockManager.updateEnabled(sponsorBlockPrefs.enabled)
                if (sponsorBlockPrefs.enabled) {
                    currentSponsorBlockVideoId()?.let { videoId ->
                        sponsorBlockManager.forceReload(videoId, currentPlaybackDurationMs())
                    }
                }
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            val queueFile = filesDir.resolve(PERSISTENT_QUEUE_FILE)
            if (queueFile.exists()) {
                runCatching {
                    queueFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    runCatching {
                        val restoredQueue = queue.toQueue()
                        scope.launch {
                            playerInitialized.first { it }
                            if (isActive) {
                                playQueue(
                                    queue = restoredQueue,
                                    playWhenReady = false,
                                )
                            }
                        }
                    }.onFailure { error ->
                        Timber.tag(TAG).w(error, "Failed to restore persisted queue, clearing data")
                        clearPersistedQueueFiles()
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read persisted queue, clearing data")
                    clearPersistedQueueFiles()
                }
            }

            val automixFile = filesDir.resolve(PERSISTENT_AUTOMIX_FILE)
            if (automixFile.exists()) {
                runCatching {
                    automixFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    runCatching {
                        automixItems.value = queue.items.map { it.toMediaItem() }
                    }.onFailure { error ->
                        Timber.tag(TAG).w(error, "Failed to restore automix queue, clearing data")
                        clearPersistedQueueFiles()
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read automix queue, clearing data")
                    clearPersistedQueueFiles()
                }
            }

            val playerStateFile = filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE)
            if (playerStateFile.exists()) {
                runCatching {
                    playerStateFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistPlayerState
                        }
                    }
                }.onSuccess { playerState ->
                    scope.launch {
                        delay(1000)
                        player.repeatMode = playerState.repeatMode
                        player.shuffleModeEnabled = playerState.shuffleModeEnabled
                        playerVolume.value = playerState.volume

                        if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                            player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                        }
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read player state, clearing data")
                    clearPersistedQueueFiles()
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
                saveAudiobookResumePosition()
            }
        }
    }

    private suspend fun saveAudiobookResumePosition() {
        val metadata = player.currentMetadata ?: return
        val mediaId = metadata.id
        val durationMs = if (player.duration != C.TIME_UNSET) {
            player.duration
        } else {
            metadata.duration.takeIf { it > 0 }?.times(1000L) ?: 0L
        }
        if (durationMs <= 0L) return

        val preferences = dataStore.data.first()
        val audiobookIds = decodeAudiobookIds(preferences[AudiobookIdsKey])
        val isAudiobook = mediaId in audiobookIds || metadata.duration >= AUDIOBOOK_MIN_DURATION_SECONDS
        if (!isAudiobook) return

        val position = player.currentPosition.coerceIn(0L, durationMs)
        val shouldKeepPosition = position >= AUDIOBOOK_RESUME_THRESHOLD_MS && position < durationMs - AUDIOBOOK_RESUME_THRESHOLD_MS

        dataStore.edit { settings ->
            val positions = decodeAudiobookPositions(settings[AudiobookPositionsKey]).toMutableMap()
            if (shouldKeepPosition) {
                positions[mediaId] = position
            } else {
                positions -= mediaId
            }
            settings[AudiobookPositionsKey] = encodeAudiobookPositions(positions)
        }
    }

    private fun createExoPlayer(publishPlayer: Boolean = true): ExoPlayer {
        val eqProcessor = CustomEqualizerAudioProcessor()
        equalizerService.addAudioProcessor(eqProcessor)

        val vocalSuppressionProcessor = VocalSuppressionAudioProcessor()
        equalizerService.addVocalSuppressionProcessor(vocalSuppressionProcessor)

        val dynamicRangeCompressionProcessor = DynamicRangeCompressionAudioProcessor().also {
            this.dynamicRangeCompressionProcessor = it
        }

        val silenceProcessor = SilenceDetectorAudioProcessor { handleLongSilenceDetected() }
        val reverbProcessor = ReverbAudioProcessor()
        var initialAudioStyle = AudioStyleMode.NORMAL

        runBlocking {
            val skipSilence = dataStore.get(SkipSilenceKey, false)
            val instantSkip = dataStore.get(SkipSilenceInstantKey, false)
            silenceProcessor.instantModeEnabled = skipSilence && instantSkip
            dynamicRangeCompressionProcessor.enabled = dataStore.get(LateNightModeKey, false)
            initialAudioStyle = dataStore.get(AudioStyleModeKey, AudioStyleMode.NORMAL.name)
                .toEnum(AudioStyleMode.NORMAL)
            audioStyleMode.value = initialAudioStyle
            reverbProcessor.setAudioStyleMode(initialAudioStyle)

            equalizerService.disableVocalSuppression()
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(
                createRenderersFactory(
                    eqProcessor,
                    vocalSuppressionProcessor,
                    dynamicRangeCompressionProcessor,
                    silenceProcessor,
                    reverbProcessor,
                )
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .setDeviceVolumeControlEnabled(true)
            .build()

        playerSilenceProcessors[player] = silenceProcessor
        playerReverbProcessors[player] = reverbProcessor

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setPreferredTextLanguage("en")
            .build()

        player.apply {
            runBlocking {
                val offload = dataStore.get(AudioOffload, false)
                val crossfade = dataStore.get(CrossfadeEnabledKey, false) ||
                    dataStore.get(AutomixEnabledKey, false)
                val disableOffload = crossfade || initialAudioStyle != AudioStyleMode.NORMAL
                setOffloadEnabled(if (disableOffload) false else offload)
                skipSilenceEnabled = dataStore.get(SkipSilenceKey, false)
                if (initialAudioStyle != AudioStyleMode.NORMAL) {
                    playbackParameters = PlaybackParameters(initialAudioStyle.speed, initialAudioStyle.pitch)
                }
            }
        }
        if (publishPlayer) _playerFlow.value = player
        return player
    }

    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss && !player.isPlaying && !reentrantFocusGain) {
                    reentrantFocusGain = true
                    scope.launch {
                        delay(300)
                        if (hasAudioFocus && wasPlayingBeforeAudioFocusLoss && !player.isPlaying) {
                            if (castConnectionHandler?.isCasting?.value != true) {
                                player.play()
                            }
                            wasPlayingBeforeAudioFocusLoss = false
                        }
                        reentrantFocusGain = false
                    }
                }

                val fadeMult = sleepTimer.fadeVolumeMultiplierFlow.value
                player.volume = if (isMuted.value) 0f else (playerVolume.value * fadeMult).coerceIn(0f, 1f)
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    val fadeMult = sleepTimer.fadeVolumeMultiplierFlow.value
                    player.volume = if (isMuted.value) 0f else (playerVolume.value * 0.2f * fadeMult).coerceIn(0f, 1f)
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                val fadeMult = sleepTimer.fadeVolumeMultiplierFlow.value
                player.volume = if (isMuted.value) 0f else (playerVolume.value * fadeMult).coerceIn(0f, 1f)
                lastAudioFocusState = focusChange
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    private fun clearPersistedQueueFiles() {
        runCatching { filesDir.resolve(PERSISTENT_QUEUE_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete() }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun waitOnNetworkError() {
        if (waitingForNetworkConnection.value) return

        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.tag(TAG).w("Max retry count ($MAX_RETRY_COUNT) reached, stopping playback")
            stopOnError()
            retryCount = 0
            return
        }

        waitingForNetworkConnection.value = true

        retryJob?.cancel()
        retryJob = scope.launch {
            val delayMs = minOf(3000L * (1 shl retryCount), 30000L)
            delay(delayMs)

            if (isNetworkConnected.value && waitingForNetworkConnection.value) {
                retryCount++
                triggerRetry()
            }
        }
    }

    private fun triggerRetry() {
        waitingForNetworkConnection.value = false
        retryJob?.cancel()

        if (player.currentMediaItem != null) {
            if (retryCount > 3) {
                val currentPosition = player.currentPosition
                player.seekTo(player.currentMediaItemIndex, currentPosition)
            }
            player.prepare()
        }
    }

    private fun skipOnError() {
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            try {
                player.seekTo(nextWindowIndex, C.TIME_UNSET)
                player.prepare()
                if (castConnectionHandler?.isCasting?.value != true) {
                    player.play()
                }
                return
            } catch (e: Exception) {
                Timber.e(e, "skipOnError: Failed to skip to next item")
            }
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.ic_heart else R.drawable.ic_heart_outline)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else {
                var updatedSong = song.song
                if (song.song.duration == -1) {
                    updatedSong = updatedSong.copy(duration = duration)
                }
                if (song.song.isVideo != mediaMetadata.isVideoSong) {
                    updatedSong = updatedSong.copy(isVideo = mediaMetadata.isVideoSong)
                }
                if (updatedSong != song.song) {
                    update(updatedSong)
                }
            }
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()

        if (!playerInitialized.value) {
            scope.launch {
                playerInitialized.first { it }
                playQueue(queue, playWhenReady)
            }
            return
        }

        currentQueue = queue
        val queueGeneration = ++currentQueueGeneration
        queueTitle = null
        val persistShuffleAcrossQueues = dataStore.get(PersistentShuffleAcrossQueuesKey, false)
        if (!persistShuffleAcrossQueues) {
            player.shuffleModeEnabled = false
        }
        originalQueueSize = 0
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }
            if (queueGeneration != currentQueueGeneration) return@launch
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (queue.preloadItem != null) {
                val preloadId = queue.preloadItem!!.id
                val preloadIndex = initialStatus.items.indexOfFirst {
                    it.mediaId == preloadId || it.metadata?.id == preloadId
                }
                val itemsBeforePreload = if (preloadIndex > 0) {
                    initialStatus.items.subList(0, preloadIndex)
                } else {
                    emptyList()
                }
                val itemsAfterPreload = if (preloadIndex >= 0) {
                    initialStatus.items.subList(preloadIndex + 1, initialStatus.items.size)
                } else {
                    initialStatus.items.filterNot { it.mediaId == preloadId || it.metadata?.id == preloadId }
                }
                originalQueueSize = itemsBeforePreload.size + 1 + itemsAfterPreload.size
                if (itemsBeforePreload.isNotEmpty()) {
                    player.addMediaItems(0, itemsBeforePreload)
                }
                if (itemsAfterPreload.isNotEmpty()) {
                    player.addMediaItems(itemsAfterPreload)
                }
                if (preloadIndex >= 0) {
                    player.replaceMediaItem(player.currentMediaItemIndex, initialStatus.items[preloadIndex])
                }
            } else {
                originalQueueSize = initialStatus.items.size
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0,
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }

            if (player.shuffleModeEnabled) {
                val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
            }
        }
    }

    fun startRadioSeamlessly() {
        if (!playerInitialized.value) return

        val currentMediaMetadata = player.currentMetadata ?: return

        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMediaMetadata.id

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(
                    videoId = currentMediaId,
                    playlistId = "RDAMVM$currentMediaId",
                    params = "wAEB"
                )
            )

            try {
                val initialStatus = withContext(Dispatchers.IO) {
                    radioQueue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }

                if (initialStatus.title != null) {
                    queueTitle = initialStatus.title
                }

                val existingMediaIds = (0 until player.mediaItemCount)
                    .map { player.getMediaItemAt(it).mediaId }
                    .toSet()
                val radioItems = initialStatus.items
                    .distinctBy { it.mediaId }
                    .filterNot { it.mediaId in existingMediaIds }

                if (player.currentMediaItem?.mediaId != currentMediaId) return@launch
                if (radioItems.isEmpty()) {
                    throw IllegalStateException("Radio returned no new recommendations")
                }
                if (radioItems.isNotEmpty()) {
                    val itemCount = player.mediaItemCount

                    if (itemCount > currentIndex + 1) {
                        player.removeMediaItems(currentIndex + 1, itemCount)
                    }

                    player.addMediaItems(currentIndex + 1, radioItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }

                currentQueue = radioQueue
            } catch (e: Exception) {
                try {
                    val nextResult = withContext(Dispatchers.IO) {
                        YouTube.next(WatchEndpoint(videoId = currentMediaId)).getOrNull()
                    }
                    nextResult?.relatedEndpoint?.let { relatedEndpoint ->
                        val relatedPage = withContext(Dispatchers.IO) {
                            YouTube.related(relatedEndpoint).getOrNull()
                        }
                        relatedPage?.songs?.let { songs ->
                            val existingMediaIds = (0 until player.mediaItemCount)
                                .map { player.getMediaItemAt(it).mediaId }
                                .toSet()
                            val radioItems = songs
                                .distinctBy { it.id }
                                .filterNot { it.id in existingMediaIds }
                                .map { it.toMediaItem() }
                                .filterExplicit(dataStore.get(HideExplicitKey, false))
                                .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))

                            if (radioItems.isNotEmpty() && player.currentMediaItem?.mediaId == currentMediaId) {
                                val itemCount = player.mediaItemCount
                                if (itemCount > currentIndex + 1) {
                                    player.removeMediaItems(currentIndex + 1, itemCount)
                                }
                                player.addMediaItems(currentIndex + 1, radioItems)
                                if (player.shuffleModeEnabled) {
                                    val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                                    applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore.get(SimilarContent, true) &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                try {
                    YouTube.next(WatchEndpoint(playlistId = playlistId))
                        .onSuccess { firstResult ->
                            YouTube.next(WatchEndpoint(playlistId = firstResult.endpoint.playlistId))
                                .onSuccess { secondResult ->
                                    val queuedIds = (0 until player.mediaItemCount)
                                        .map { player.getMediaItemAt(it).mediaId }
                                        .toSet()
                                    automixItems.value = secondResult.items
                                        .distinctBy { it.id }
                                        .filterNot { it.id in queuedIds }
                                        .map { it.toMediaItem() }
                                }
                                .onFailure {
                                    if (firstResult.items.isNotEmpty()) {
                                        val queuedIds = (0 until player.mediaItemCount)
                                            .map { player.getMediaItemAt(it).mediaId }
                                            .toSet()
                                        automixItems.value = firstResult.items
                                            .distinctBy { it.id }
                                            .filterNot { it.id in queuedIds }
                                            .map { it.toMediaItem() }
                                    }
                                }
                        }
                        .onFailure {
                            val currentSong = player.currentMetadata
                            if (currentSong != null) {
                                YouTube.next(WatchEndpoint(
                                    videoId = currentSong.id,
                                    playlistId = "RDAMVM${currentSong.id}",
                                    params = "wAEB"
                                )).onSuccess { radioResult ->
                                    val filteredItems = radioResult.items
                                        .filter { it.id != currentSong.id }
                                        .map { it.toMediaItem() }
                                    if (filteredItems.isNotEmpty()) {
                                        automixItems.value = filteredItems
                                    }
                                }.onFailure {
                                    YouTube.next(WatchEndpoint(videoId = currentSong.id)).getOrNull()?.relatedEndpoint?.let { relatedEndpoint ->
                                        YouTube.related(relatedEndpoint).onSuccess { relatedPage ->
                                            val relatedItems = relatedPage.songs
                                                .filter { it.id != currentSong.id }
                                                .map { it.toMediaItem() }
                                            if (relatedItems.isNotEmpty()) {
                                                automixItems.value = relatedItems
                                            }
                                        }
                                    }
                                }
                            }
                        }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    private fun enqueueNextAutomixItemIfNeeded() {
        if (automixItems.value.isEmpty() || player.mediaItemCount == 0) return
        if (player.repeatMode != REPEAT_MODE_OFF || player.currentTimeline.isEmpty) return
        if (isTvDevice && isVideoMode) return

        val nextIndex = player.currentTimeline.getNextWindowIndex(
            player.currentMediaItemIndex,
            REPEAT_MODE_OFF,
            player.shuffleModeEnabled,
        )
        if (nextIndex == C.INDEX_UNSET) {
            val nextItem = automixItems.value.firstOrNull { it.metadata?.isVideoSong != true }
            if (nextItem != null) {
                val index = automixItems.value.indexOf(nextItem)
                addToQueueAutomix(nextItem, index)
            }
        }
    }

    fun playNext(items: List<MediaItem>) {
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            if (castConnectionHandler?.isCasting?.value != true) {
                player.play()
            }
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse()

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        if (player.shuffleModeEnabled) {
            val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
        player.prepare()
    }

    fun toggleLibrary() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val isInLibrary = it.song.inLibrary != null
                val token = if (isInLibrary) it.song.libraryRemoveToken else it.song.libraryAddToken

                token?.let { feedbackToken ->
                    YouTube.feedback(listOf(feedbackToken))
                }

                database.query {
                    update(it.song.toggleLibrary())
                }
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun toggleLike() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val song = it.song.toggleLike()
                database.query {
                    update(song)
                    syncUtils.likeSong(song)

                    if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                        val downloadRequest =
                            androidx.media3.exoplayer.offline.DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.title.toByteArray())
                                .build()
                        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                            this@MusicService,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                    }
                }
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return

        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }

                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }
                    val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb

                    withContext(Dispatchers.Main) {
                        if (loudness != null) {
                            val loudnessDb = loudness.toFloat()
                            val targetGain = (-loudnessDb * 100).toInt()
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)

                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                            } catch (e: Exception) {
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            reportException(e)
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        isAudioEffectSessionOpened = true
        audioEffectSessionId = sessionId
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        val sessionId = audioEffectSessionId
        isAudioEffectSessionOpened = false
        audioEffectSessionId = C.AUDIO_SESSION_ID_UNSET
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (isInjectingVideoSource) return
        currentMediaMetadata.value = mediaItem?.metadata
        runCatching { enqueueNextAutomixItemIfNeeded() }

        if (sponsorBlockManager.enabled.value && !isVideoMode && !_isVideoSwitching.value && mediaItem?.mediaId != null) {
            val mediaId = mediaItem.mediaId
            scope.launch {
                try {
                    sponsorBlockManager.loadSegments(mediaId, currentPlaybackDurationMs())
                } catch (e: Exception) {
                    Timber.e(e, "onMediaItemTransition: SponsorBlock load failed")
                }
            }
        }

        val newMediaId = mediaItem?.mediaId
        val isNewSong = newMediaId != null && newMediaId != lastTransitionMediaId
        lastTransitionMediaId = newMediaId

        if (isNewSong) {
            if (RemoveMusicVocalRemover.state.value.active) {
                stopExternalStemPlayers()
                RemoveMusicVocalRemover.onSongChanged(this, newMediaId, downloadUtil)
            }
            if (isVideoMode) {
                videoSwitchJob?.cancel()
                _isVideoSwitching.value = false
                sponsorBlockManager.reset()
                currentVideoUrl = null
                currentVideoSourceMediaId = null
                _currentVideoId.value = null
                _videoFetchError.value = null
                _videoModeMessage.value = null
            } else {
                resetVideoMode()
            }

            if (isVideoMode) {
                resetVideoMode()
            } else {
                _isVideoSwitching.value = false
            }
        }

        lastPlaybackSpeed = -1.0f
        setupLoudnessEnhancer()
        discordUpdateJob?.cancel()

        scrobbleManager?.onSongStop()
        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
        }

        if (player.playWhenReady && !player.isPlaying && player.playbackState == Player.STATE_READY) {
            player.play()
        }

        if (castConnectionHandler?.isCasting?.value == true &&
            castConnectionHandler?.isSyncingFromCast != true &&
            mediaItem != null) {
            val metadata = mediaItem.metadata
            if (metadata != null) {
                val navigated = castConnectionHandler?.navigateToMediaIfInQueue(metadata.id) ?: false
                if (!navigated) {
                    castConnectionHandler?.loadMedia(metadata)
                }
            }
        }

        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = withContext(Dispatchers.IO) {
                    currentQueue.nextPage()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }
                if (player.playbackState != STATE_IDLE && mediaItems.isNotEmpty()) {
                    val queuedIds = (0 until player.mediaItemCount)
                        .map { player.getMediaItemAt(it).mediaId }
                        .toSet()
                    val newMediaItems = mediaItems
                        .distinctBy { it.mediaId }
                        .filterNot { it.mediaId in queuedIds }
                    player.addMediaItems(newMediaItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }
            }
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }

        val isTvLocal = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        if (isTvLocal) {
            try {
                val helperClass = Class.forName("com.ikyvortex.vortexmusic.tv.TvRecommendationHelper")
                val method = helperClass.getMethod("scheduleUpdate", android.content.Context::class.java)
                method.invoke(null, this)
            } catch (_: ClassNotFoundException) {
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to update TV recommendations")
            }
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (dataStore.get(PersistentQueueKey, true) && !isSilenceSkipping) {
            saveQueueToDisk()
        }

        if (playbackState == Player.STATE_READY) {
            consecutivePlaybackErr = 0
            retryCount = 0
            waitingForNetworkConnection.value = false
            retryJob?.cancel()
            if (::sponsorBlockManager.isInitialized) {
                sponsorBlockManager.updateDuration(currentPlaybackDurationMs())
            }

            player.currentMediaItem?.mediaId?.let { mediaId ->
                resetRetryCount(mediaId)
            }
            scheduleCrossfade()
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()

            val isTvLocal = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
            if (isTvLocal) {
                try {
                    val helperClass = Class.forName("com.ikyvortex.vortexmusic.tv.TvRecommendationHelper")
                    val method = helperClass.getMethod("scheduleUpdate", android.content.Context::class.java)
                    method.invoke(null, this@MusicService)
                } catch (_: ClassNotFoundException) {
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed to update TV recommendations on stop")
                }
            }
        }
        if (playbackState == Player.STATE_ENDED && RemoveMusicVocalRemover.state.value.active) {
            deactivateVocalInstrumentSeparation()
        }
        refreshStemClockFromMainThread()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady && castConnectionHandler?.isCasting?.value == true) {
            player.pause()
            return
        }

        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            if (playWhenReady) {
                isPausedByVolumeMute = false
            }

            if (!playWhenReady && !isPausedByVolumeMute) {
                wasPlayingBeforeVolumeMute = false
            }
        }

        if (playWhenReady) {
            setupLoudnessEnhancer()
        }
        refreshStemClockFromMainThread()
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            )
        ) {
            scheduleCrossfade()
        }
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) {
                    openAudioEffectSession()
                }
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            if (!isInjectingVideoSource) {
                currentMediaMetadata.value = player.currentMetadata
            }
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            updateWidgetUI(player.isPlaying)
            if (player.isPlaying) {
                startWidgetUpdates()
                if (player.currentMetadata != null) {
                    StreakManager.onMusicPlayed(applicationContext)
                }
            } else {
                stopWidgetUpdates()
            }
            if (!player.isPlaying && !events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                if (DiscordRpcManager.isReady()) DiscordRpcManager.clear()
            }
        }

        if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
            (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying)
        ) {
            syncDiscordState()
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            if (player.mediaItemCount == 0) return

            val shufflePlaylistFirst = dataStore.get(ShufflePlaylistFirstKey, false)
            val currentIndex = player.currentMediaItemIndex
            val totalCount = player.mediaItemCount

            applyShuffleOrder(currentIndex, totalCount, shufflePlaylistFirst)
        }
        scheduleCrossfade()

        if (dataStore.get(RememberShuffleAndRepeatKey, true)) {
            scope.launch {
                dataStore.edit { settings ->
                    settings[ShuffleModeKey] = shuffleModeEnabled
                }
            }
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scheduleCrossfade()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
    }

    private fun applyShuffleOrder(
        currentIndex: Int,
        totalCount: Int,
        shufflePlaylistFirst: Boolean
    ) {
        if (totalCount == 0) return

        if (shufflePlaylistFirst && originalQueueSize > 0 && originalQueueSize < totalCount) {
            val originalIndices = (0 until originalQueueSize).filter { it != currentIndex }.toMutableList()
            val addedIndices = (originalQueueSize until totalCount).filter { it != currentIndex }.toMutableList()

            originalIndices.shuffle()
            addedIndices.shuffle()

            val shuffledIndices = IntArray(totalCount)
            var pos = 0
            shuffledIndices[pos++] = currentIndex

            if (currentIndex < originalQueueSize) {
                originalIndices.forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            } else {
                (0 until originalQueueSize).shuffled().forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        } else {
            val shuffledIndices = IntArray(totalCount) { it }
            shuffledIndices.shuffle()
            val currentItemIndexInShuffled = shuffledIndices.indexOf(currentIndex)
            if (currentItemIndexInShuffled != -1) {
                val temp = shuffledIndices[0]
                shuffledIndices[0] = shuffledIndices[currentItemIndexInShuffled]
                shuffledIndices[currentItemIndexInShuffled] = temp
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        refreshLockscreenSnapshot()
        if (playbackParameters.speed != lastPlaybackSpeed) {
            lastPlaybackSpeed = playbackParameters.speed
            discordUpdateJob?.cancel()

            DiscordRpcManager.notifySettingsChanged()
            discordUpdateJob = scope.launch {
                delay(1000)
                if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                    syncDiscordState()
                }
            }
        }
    }

    private fun getHttpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }

    private fun isExpiredUrlError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 403
    }

    private fun isRateLimitError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 429 || responseCode == 503
    }

    private fun isRangeNotSatisfiableError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 416
    }

    private fun isPageReloadError(error: PlaybackException): Boolean {
        val errorMessage = error.message?.lowercase() ?: ""
        val causeMessage = error.cause?.message?.lowercase() ?: ""
        val innerCauseMessage = error.cause?.cause?.message?.lowercase() ?: ""

        val reloadKeywords = listOf(
            "page needs to be reloaded",
            "pagina deve essere ricaricata",
            "la pagina deve essere ricaricata",
            "page must be reloaded",
            "reload",
            "ricaricata"
        )

        return reloadKeywords.any { keyword ->
            errorMessage.contains(keyword) ||
                causeMessage.contains(keyword) ||
                innerCauseMessage.contains(keyword)
        }
    }

    private fun isNetworkRelatedError(error: PlaybackException): Boolean {
        if (isExpiredUrlError(error) || isRangeNotSatisfiableError(error) || isPageReloadError(error)) {
            return false
        }
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
            error.cause is java.net.ConnectException ||
            error.cause is java.net.UnknownHostException ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
    }

    private fun isStreamDecodingError(error: PlaybackException): Boolean {
        val message = error.message ?: ""
        val causeMessage = error.cause?.message ?: ""
        return message.contains("srcPos=") ||
            message.contains("src.length") ||
            message.contains("AudioTrack") ||
            causeMessage.contains("srcPos=") ||
            causeMessage.contains("src.length") ||
            causeMessage.contains("AudioTrack")
    }

    private fun isAudioRendererError(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
            (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        if (!playerInitialized.value) return

        val mediaId = player.currentMediaItem?.mediaId

        if (isVideoMode && mediaId != null) {
            currentVideoSourceMediaId?.let { videoSearchCache.remove(it) }
            _videoModeMessage.value = if (error.message?.contains("inappropriate", ignoreCase = true) == true) {
                "Video dibatasi, memutar audio saja"
            } else {
                "Video gagal dimuat, memutar audio saja"
            }
            scope.launch {
                switchToAudioMode()
            }
            return
        }

        reportException(error)

        if (mediaId != null && hasExceededRetryLimit(mediaId)) {
            markSongAsFailed(mediaId)
            handleFinalFailure()
            return
        }

        if (mediaId != null) {
            performAggressiveCacheClear(mediaId)
        }

        when {
            isStreamDecodingError(error) -> {
                skipOnError()
                return
            }
            isAudioRendererError(error) -> {
                handleAudioRendererError(mediaId)
                return
            }
            isRangeNotSatisfiableError(error) -> {
                handleRangeNotSatisfiableError(mediaId)
                return
            }
            isPageReloadError(error) -> {
                handlePageReloadError(mediaId)
                return
            }
            isRateLimitError(error) -> {
                handleRateLimitError(mediaId)
                return
            }
            isExpiredUrlError(error) -> {
                handleExpiredUrlError(mediaId)
                return
            }
            !isNetworkConnected.value || isNetworkRelatedError(error) -> {
                waitOnNetworkError()
                return
            }
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED) {
            handleGenericIOError(mediaId)
            return
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
            handleGenericIOError(mediaId)
            return
        }

        if (dataStore.get(AutoSkipNextOnErrorKey, true)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    private fun performAggressiveCacheClear(mediaId: String) {
        songUrlCache.remove(mediaId)

        try {
            playerCache.removeResource(mediaId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear player cache for $mediaId")
        }

        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear decryption caches for $mediaId")
        }
    }

    private fun hasExceededRetryLimit(mediaId: String): Boolean {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        return currentRetries >= MAX_RETRY_PER_SONG
    }

    private fun incrementRetryCount(mediaId: String) {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        currentMediaIdRetryCount[mediaId] = currentRetries + 1
    }

    private fun resetRetryCount(mediaId: String) {
        currentMediaIdRetryCount.remove(mediaId)
        recentlyFailedSongs.remove(mediaId)
    }

    private fun markSongAsFailed(mediaId: String) {
        recentlyFailedSongs.add(mediaId)
        currentMediaIdRetryCount.remove(mediaId)

        failedSongsClearJob?.cancel()
        failedSongsClearJob = scope.launch {
            delay(5 * 60 * 1000L)
            recentlyFailedSongs.clear()
        }
    }

    private fun handleAudioRendererError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            try {
                player.pause()
                delay(RETRY_DELAY_MS * 3)

                if (!playerInitialized.value) return@launch

                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    val currentPosition = player.currentPosition
                    player.seekTo(currentIndex, currentPosition)
                    player.prepare()

                    if (wasPlayingBeforeAudioFocusLoss) {
                        delay(500)
                        if (hasAudioFocus && playerInitialized.value) {
                            if (castConnectionHandler?.isCasting?.value != true) {
                                player.play()
                            }
                        }
                    }
                } else {
                    handleFinalFailure()
                }
            } catch (e: Exception) {
                handleFinalFailure()
            }
        }
    }

    private fun handleRangeNotSatisfiableError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)
            delay(RETRY_DELAY_MS)
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, 0)
            player.prepare()
        }
    }

    private fun handlePageReloadError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        poTokenProvider.invalidatePoTokens(mediaId)

        incrementRetryCount(mediaId)
        val retryAttempt = currentMediaIdRetryCount[mediaId] ?: 1

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)
            val delayMs = RETRY_DELAY_MS * (1L shl (retryAttempt + 1))
            delay(delayMs)

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
        }
    }

    private fun handleRateLimitError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        poTokenProvider.invalidatePoTokens(mediaId)
        incrementRetryCount(mediaId)
        val retryAttempt = currentMediaIdRetryCount[mediaId] ?: 1

        songUrlCache.remove(mediaId)

        val delayMs = minOf(15_000L shl (retryAttempt - 1), 60_000L)

        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)

            if (!playerInitialized.value) return@launch
            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
        }
    }

    private fun handleExpiredUrlError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        poTokenProvider.invalidatePoTokens(mediaId)

        incrementRetryCount(mediaId)
        val retryAttempt = currentMediaIdRetryCount[mediaId] ?: 1

        songUrlCache.remove(mediaId)

        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (_: Exception) {
        }

        val delayMs = RETRY_DELAY_MS * (1L shl retryAttempt)

        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
        }
    }

    private fun handleGenericIOError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        poTokenProvider.invalidatePoTokens(mediaId)

        incrementRetryCount(mediaId)
        val retryAttempt = currentMediaIdRetryCount[mediaId] ?: 1

        val delayMs = RETRY_DELAY_MS * (1L shl retryAttempt)

        retryJob?.cancel()
        retryJob = scope.launch {
            performAggressiveCacheClear(mediaId)
            delay(delayMs)

            val currentPosition = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            player.seekTo(currentIndex, currentPosition)
            player.prepare()
        }
    }

    private fun handleFinalFailure() {
        if (dataStore.get(AutoSkipNextOnErrorKey, true)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        super.onDeviceVolumeChanged(volume, muted)
        val pauseOnMute = dataStore.get(PauseOnMute, false)

        if ((volume == 0 || muted) && pauseOnMute) {
            if (player.isPlaying) {
                wasPlayingBeforeVolumeMute = true
                isPausedByVolumeMute = true
                player.pause()
            }
        } else if (volume > 0 && !muted && pauseOnMute) {
            if (wasPlayingBeforeVolumeMute && !player.isPlaying && castConnectionHandler?.isCasting?.value != true) {
                wasPlayingBeforeVolumeMute = false
                isPausedByVolumeMute = false
                player.play()
            }
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory {
        val isTvLocal = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_LEANBACK
        )
        val playerCacheBuilder = CacheDataSource
            .Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this,
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .proxy(YouTube.proxy)
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
            )
        if (isTvLocal) {
            playerCacheBuilder.setCacheWriteDataSinkFactory(null)
        }
        return CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playerCacheBuilder)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private var isSilenceSkipping = false

    private fun handleLongSilenceDetected() {
        if (!instantSilenceSkipEnabled.value) return
        if (silenceSkipJob?.isActive == true) return

        silenceSkipJob = scope.launch {
            delay(200)
            performInstantSilenceSkip()
        }
    }

    private suspend fun performInstantSilenceSkip() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        if (duration <= INSTANT_SILENCE_SKIP_STEP_MS) return

        isSilenceSkipping = true
        try {
            var hops = 0
            val silenceProcessor = playerSilenceProcessors[player] ?: return
            while (coroutineContext.isActive && instantSilenceSkipEnabled.value && silenceProcessor.isCurrentlySilent()) {
                val current = player.currentPosition
                val target = (current + INSTANT_SILENCE_SKIP_STEP_MS).coerceAtMost(duration - 500)

                if (target <= current) break

                silenceProcessor.resetTracking()
                try {
                    player.seekTo(target)
                } catch (e: Exception) {
                    break
                }
                hops++

                if (hops >= 80 || target >= duration - 500) break

                delay(INSTANT_SILENCE_SKIP_SETTLE_MS)
            }
        } finally {
            isSilenceSkipping = false
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = (dataSpec.key ?: error("No media id")).removeSuffix("_video")

            val requestedChunkLength = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                CHUNK_LENGTH
            } else {
                minOf(CHUNK_LENGTH, dataSpec.length)
            }

            val uri = dataSpec.uri.toString()
            if (uri.startsWith("https://") && (uri.contains("googlevideo.com") || uri.contains("youtube.com/videoplayback"))) {
                return@Factory dataSpec.subrange(0, requestedChunkLength)
            }
            if (uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("file://")) {
                return@Factory dataSpec
            }

            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    requestedChunkLength
                ) ||
                playerCache.isCached(mediaId, dataSpec.position, requestedChunkLength)
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.subrange(0, requestedChunkLength)
            }

            songUrlCache[mediaId]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { entry ->
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                val cachedUri = entry.url.toUri()
                if (cachedUri.host == "127.0.0.1" || cachedUri.host == "localhost" || cachedUri.scheme == "file") {
                    return@Factory dataSpec
                        .withUri(cachedUri)
                        .withRequestHeaders(entry.headers)
                }
                return@Factory dataSpec
                    .withUri(cachedUri)
                    .withRequestHeaders(entry.headers)
                    .subrange(0, requestedChunkLength)
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    poTokenProvider = poTokenProvider,
                )
            }.getOrElse { throwable ->
                when (throwable) {
                    is PlaybackException -> throw throwable

                    is java.net.ConnectException, is java.net.UnknownHostException -> {
                        throw PlaybackException(
                            getString(R.string.error_no_internet),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        )
                    }

                    is java.net.SocketTimeoutException -> {
                        throw PlaybackException(
                            getString(R.string.error_timeout),
                            throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        )
                    }

                    else -> throw PlaybackException(
                        getString(R.string.error_unknown),
                        throwable,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }

            val nonNullPlayback = requireNotNull(playbackData) {
                getString(R.string.error_unknown)
            }
            val streamUrl = nonNullPlayback.streamUrl
            val streamUri = streamUrl.toUri()
            val urlExpiresAt = streamUri.getQueryParameter("expire")
                ?.toLongOrNull()
                ?.times(1000L)
            val responseExpiresAt = System.currentTimeMillis() + (nonNullPlayback.streamExpiresInSeconds * 1000L)
            val expiresAt = minOf(urlExpiresAt ?: Long.MAX_VALUE, responseExpiresAt) - 5 * 60_000L
            songUrlCache[mediaId] = StreamUrlEntry(
                url = streamUrl,
                expiresAtMs = expiresAt,
                headers = nonNullPlayback.streamHeaders,
            )

            scope.launch(Dispatchers.IO) {
                val format = nonNullPlayback.format
                val loudnessDb = nonNullPlayback.audioConfig?.loudnessDb
                val perceptualLoudnessDb = nonNullPlayback.audioConfig?.perceptualLoudnessDb

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.substringBefore(";"),
                            codecs = format.mimeType.substringAfter("codecs=", "").removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength ?: 0L,
                            loudnessDb = loudnessDb,
                            perceptualLoudnessDb = perceptualLoudnessDb,
                            playbackUrl = nonNullPlayback.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        )
                    )
                }
                recoverSong(mediaId, nonNullPlayback)
            }

            if (streamUri.host == "127.0.0.1" || streamUri.host == "localhost" || streamUri.scheme == "file") {
                return@Factory dataSpec
                    .withUri(streamUri)
                    .withRequestHeaders(nonNullPlayback.streamHeaders)
            }

            return@Factory dataSpec
                .withUri(streamUri)
                .withRequestHeaders(nonNullPlayback.streamHeaders)
                .subrange(0, requestedChunkLength)
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor(), Mp3Extractor())
            },
        )

    private fun createRenderersFactory(
        eqProcessor: CustomEqualizerAudioProcessor,
        vocalSuppressionProcessor: VocalSuppressionAudioProcessor,
        dynamicRangeCompressionProcessor: DynamicRangeCompressionAudioProcessor,
        silenceProcessor: SilenceDetectorAudioProcessor,
        reverbProcessor: ReverbAudioProcessor,
    ) =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    StudioAudioProcessorChain(
                        eqProcessor,
                        vocalSuppressionProcessor,
                        dynamicRangeCompressionProcessor,
                        silenceProcessor,
                        reverbProcessor = reverbProcessor,
                    ),
                ).build()

            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: android.os.Handler,
                eventListener: AudioRendererEventListener,
                out: ArrayList<Renderer>,
            ) {
                out.add(
                    MediaCodecAudioRenderer(
                        context,
                        codecAdapterFactory,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        audioSink,
                        null,
                    )
                )
            }
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        val historyDurationMs = dataStore[HistoryDuration]?.times(1000f) ?: 30000f

        if (playbackStats.totalPlayTimeMs >= historyDurationMs &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            val meta = mediaItem.metadata
            com.ikyvortex.vortexmusic.streak.MusicJourneyManager.onSongPlayed(
                context = applicationContext,
                songId = mediaItem.mediaId,
                title = meta?.title ?: mediaItem.mediaMetadata.title?.toString().orEmpty(),
                artist = meta?.artists?.joinToString(", ") { it.name }
                    ?: mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                thumbnailUrl = meta?.thumbnailUrl ?: mediaItem.mediaMetadata.artworkUri?.toString(),
                listenedMs = playbackStats.totalPlayTimeMs,
            )
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
        }

        if (playbackStats.totalPlayTimeMs >= historyDurationMs) {
            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                    ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                        .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                playbackUrl?.let {
                    YouTube.registerPlayback(null, playbackUrl)
                        .onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.mediaItemCount == 0) return

        try {
            val persistQueue = currentQueue.toPersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex,
                position = player.currentPosition
            )

            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            val persistPlayerState = PersistPlayerState(
                playWhenReady = player.playWhenReady,
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = playerVolume.value,
                currentPosition = player.currentPosition,
                currentMediaItemIndex = player.currentMediaItemIndex,
                playbackState = player.playbackState
            )

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistPlayerState)
                    }
                }
            }.onFailure {
                reportException(it)
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    override fun onDestroy() {
        isRunning = false

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
        }
        dynamicIslandSystemOverlayManager?.release()
        dynamicIslandSystemOverlayManager = null
        castConnectionHandler?.release()
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        if (DiscordRpcManager.isReady()) {
            DiscordRpcManager.disconnect()
        }
        DiscordRpcManager.destroy()
        connectivityObserver.unregister()
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        playerInitialized.value = false
        _playerFlow.value = null
        crossfadeTriggerJob?.cancel()
        crossfadeJob?.cancel()
        retryJob?.cancel()
        videoSwitchJob?.cancel()
        widgetUpdateJob?.cancel()
        discordUpdateJob?.cancel()
        scope.cancel()
        mediaSession.release()
        listOfNotNull(secondaryPlayer, fadingPlayer, player).distinct().forEach { playerToRelease ->
            playerToRelease.removeListener(this)
            playerToRelease.removeListener(sleepTimer)
            playerSilenceProcessors.remove(playerToRelease)
            playerToRelease.stop()
            playerToRelease.release()
        }
        releaseExternalStemPlayers()
        secondaryPlayer = null
        fadingPlayer = null
        isCrossfading = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        com.ikyvortex.vortexmusic.ui.component.DynamicIslandOverlayState.resetOnAppExit(this)
        if (!player.playWhenReady || !player.isPlaying) {
            player.pause()
            if (isVideoMode) {
                player.stop()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            handleForegroundServiceStartNotAllowed(e)
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e.javaClass.name == ForegroundServiceStartNotAllowedException::class.java.name
            ) {
                handleForegroundServiceStartNotAllowed(e)
            } else {
                throw e
            }
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "onUpdateNotification: suppressed SecurityException from FGS path")
        }
    }

    private fun handleForegroundServiceStartNotAllowed(error: Throwable?) {
        Timber.tag(TAG).w(error, "Foreground service start denied during notification update")
        promoteToForegroundWithLatestNotification()
    }

    private fun promoteToForegroundWithLatestNotification() {
        val notification = latestMediaNotification ?: foregroundNotification
        startForegroundSafely(notification)
    }

    private fun startForegroundSafely(notification: Notification): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundWithLatestNotification()

        when (intent?.action) {
            ACTION_PLAY_ALARM -> {
                handleAlarmPlay()
            }
            MusicWidgetReceiver.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                updateWidgetUI(player.isPlaying)
            }
            LockscreenLyricsBridge.ACTION_SEEK -> {
                val pos = intent?.getLongExtra(LockscreenLyricsBridge.EXTRA_POSITION_MS, -1L) ?: -1L
                if (pos >= 0L) {
                    player.seekTo(pos)
                    updateWidgetUI(player.isPlaying)
                }
            }
            MusicWidgetReceiver.ACTION_LIKE -> {
                toggleLike()
            }
            MusicWidgetReceiver.ACTION_NEXT -> {
                player.seekToNext()
                updateWidgetUI(player.isPlaying)
            }
            MusicWidgetReceiver.ACTION_PREVIOUS -> {
                player.seekToPrevious()
                updateWidgetUI(player.isPlaying)
            }
            MusicWidgetReceiver.ACTION_UPDATE_WIDGET -> {
                updateWidgetUI(player.isPlaying)
            }
            CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_UPDATE -> {
                updateWidgetUI(player.isPlaying)
            }
            CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                updateWidgetUI(player.isPlaying)
            }
            CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_PREVIOUS -> {
                player.seekToPrevious()
                updateWidgetUI(player.isPlaying)
            }
            CompactSquareWidgetReceiver.ACTION_COMPACT_SQUARE_NEXT -> {
                player.seekToNext()
                updateWidgetUI(player.isPlaying)
            }
            CompactWideWidgetReceiver.ACTION_COMPACT_WIDE_UPDATE -> {
                updateWidgetUI(player.isPlaying)
            }
            CompactWideWidgetReceiver.ACTION_COMPACT_WIDE_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                updateWidgetUI(player.isPlaying)
            }
            CompactWideWidgetReceiver.ACTION_COMPACT_WIDE_LIKE -> {
                toggleLike()
            }
            LiquidGlassWidgetReceiver.ACTION_LIQUID_GLASS_UPDATE,
            LiquidGlassWideWidgetReceiver.ACTION_LIQUID_GLASS_WIDE_UPDATE -> {
                updateWidgetUI(player.isPlaying)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /** Sinkronkan posisi/kecepatan ke lirik layar kunci tanpa memperbarui widget (ringan). */
    private fun refreshLockscreenSnapshot() {
        try {
            if (!::player.isInitialized) return
            val songData = currentSong.value
            val song = songData?.song
            val meta = currentMediaMetadata.value
            LockscreenLyricsBridge.update(
                mediaId = song?.id ?: player.currentMediaItem?.mediaId ?: meta?.id,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                lyricsOffsetMs = (song?.lyricsOffset ?: 0).toLong(),
                title = song?.title ?: meta?.title,
                artist = songData?.artists?.joinToString(", ") { it.name }
                    ?: meta?.artists?.joinToString(", ") { it.name },
                durationSec = song?.duration ?: meta?.duration ?: -1,
                thumbnailUrl = song?.thumbnailUrl ?: meta?.thumbnailUrl,
                durationMs = player.duration.takeIf { it > 0L } ?: -1L,
                speed = player.playbackParameters.speed,
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "refreshLockscreenSnapshot failed")
        }
    }

    private fun updateWidgetUI(isPlaying: Boolean) {
        scope.launch {
            widgetUpdateMutex.withLock {
                try {
                    val songData = currentSong.value
                    val song = songData?.song
                    val songTitle = song?.title ?: getString(R.string.no_song_playing)
                    val artistName = songData?.artists?.joinToString(", ") { it.name } ?: getString(R.string.tap_to_open)
                    val isLiked = songData?.song?.liked == true

                    val meta = currentMediaMetadata.value
                    LockscreenLyricsBridge.update(
                        mediaId = song?.id ?: player.currentMediaItem?.mediaId ?: meta?.id,
                        isPlaying = isPlaying,
                        positionMs = player.currentPosition,
                        lyricsOffsetMs = (song?.lyricsOffset ?: 0).toLong(),
                        title = song?.title ?: meta?.title,
                        artist = songData?.artists?.joinToString(", ") { it.name }
                            ?: meta?.artists?.joinToString(", ") { it.name },
                        durationSec = song?.duration ?: meta?.duration ?: -1,
                        thumbnailUrl = song?.thumbnailUrl ?: meta?.thumbnailUrl,
                        durationMs = player.duration.takeIf { it > 0L } ?: -1L,
                        speed = player.playbackParameters.speed,
                    )

                    widgetManager.updateWidgets(
                        title = songTitle,
                        artist = artistName,
                        artworkUri = song?.thumbnailUrl,
                        isPlaying = isPlaying,
                        isLiked = isLiked,
                        duration = if (player.duration != C.TIME_UNSET) player.duration else 0,
                        currentPosition = player.currentPosition
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    private val widgetUpdateMutex = Mutex()
    private var widgetUpdateJob: Job? = null

    private fun startWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    updateWidgetUI(true)
                }
                delay(200)
            }
        }
    }

    private fun stopWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = null
    }

    private fun shareSong() {
        val songData = currentSong.value
        val songId = songData?.song?.id ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$songId")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    suspend fun getStreamUrl(mediaId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    poTokenProvider = poTokenProvider,
                ).getOrNull()
                playbackData?.streamUrl
            } catch (e: Exception) {
                null
            }
        }
    }

    private var isVideoMode = false
    private var currentVideoUrl: String? = null
    private var currentVideoSourceMediaId: String? = null
    private var originalAudioMediaItem: MediaItem? = null
    private var videoSwitchJob: Job? = null
    private var isInjectingVideoSource = false
    val isInjectingVideoSourcePublic: Boolean get() = isInjectingVideoSource
    private var videoFirstFrameListener: AnalyticsListener? = null

    private val isTvDevice: Boolean by lazy {
        packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }

    private var lastTransitionMediaId: String? = null
    private val _videoModeEnabled = MutableStateFlow(false)
    val videoModeEnabled: StateFlow<Boolean> = _videoModeEnabled.asStateFlow()
    private val _isVideoSwitching = MutableStateFlow(false)
    val isVideoSwitching: StateFlow<Boolean> = _isVideoSwitching.asStateFlow()
    private val _videoFetchError = MutableStateFlow<String?>(null)
    val videoFetchError: StateFlow<String?> = _videoFetchError.asStateFlow()
    private val _videoModeMessage = MutableStateFlow<String?>(null)
    val videoModeMessage: StateFlow<String?> = _videoModeMessage.asStateFlow()
    private val _isVideoAvailable = MutableStateFlow(false)
    val isVideoAvailable: StateFlow<Boolean> = _isVideoAvailable.asStateFlow()
    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()
    private val videoSearchCache = java.util.concurrent.ConcurrentHashMap<String, com.ikyvortex.auravideo.VortexVideo.VideoSearchResult>()
    val captionCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    val captionAttemptedIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val _availableSubtitles = MutableStateFlow<List<com.ikyvortex.vortexmusic.subtitles.SubtitleInfo>>(emptyList())
    val availableSubtitles: StateFlow<List<com.ikyvortex.vortexmusic.subtitles.SubtitleInfo>> = _availableSubtitles.asStateFlow()
    private val _selectedSubtitleIndex = MutableStateFlow(-1)
    val selectedSubtitleIndex: StateFlow<Int> = _selectedSubtitleIndex.asStateFlow()

    private val _currentPositionFlow = MutableStateFlow(0L)
    val currentPositionFlow: StateFlow<Long> = _currentPositionFlow.asStateFlow()

    private fun currentPlaybackDurationMs(): Long = if (player.duration > 0) player.duration else 0L

    private fun currentSponsorBlockVideoId(): String? {
        if (_isVideoSwitching.value) return null
        currentVideoId.value?.let { return it }
        if (isVideoMode) return null
        return player.currentMediaItem?.mediaId
    }

    fun selectSubtitle(index: Int) {
        _selectedSubtitleIndex.value = index
    }

    private fun resetVideoMode() {
        videoSwitchJob?.cancel()
        videoFirstFrameListener?.let { matchingListener ->
            runCatching { player.removeAnalyticsListener(matchingListener) }
        }
        videoFirstFrameListener = null
        _videoModeEnabled.value = false
        _isVideoSwitching.value = false
        isVideoMode = false
        currentVideoUrl = null
        currentVideoSourceMediaId = null
        _currentVideoId.value = null
        originalAudioMediaItem = null
        _videoFetchError.value = null
        _videoModeMessage.value = null
        _availableSubtitles.value = emptyList()
        _selectedSubtitleIndex.value = -1
    }

    private suspend fun waitForFirstVideoFrameThenResume(wasPlaying: Boolean) {
        val firstFrame = CompletableDeferred<Unit>()
        val listener = object : AnalyticsListener {
            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                surface: Any,
                renderTimeMs: Long,
            ) {
                if (!firstFrame.isCompleted) {
                    firstFrame.complete(Unit)
                }
            }
        }
        videoFirstFrameListener = listener
        runCatching { player.addAnalyticsListener(listener) }
        try {
            withTimeout(10_000L) {
                firstFrame.await()
            }
        } catch (_: TimeoutCancellationException) {
        } finally {
            runCatching { player.removeAnalyticsListener(listener) }
            if (videoFirstFrameListener === listener) {
                videoFirstFrameListener = null
            }
        }
        player.playWhenReady = wasPlaying
    }

    private fun restoreTvPlaybackIfPaused(wasPlaying: Boolean) {
        if (isTvDevice && wasPlaying && !player.playWhenReady) {
            player.playWhenReady = true
        }
    }

    private suspend fun switchToAudioMode() {
        if (!isVideoMode) return

        try {
            val index = player.currentMediaItemIndex
            val position = player.currentPosition
            val wasPlaying = player.playWhenReady

            val original = originalAudioMediaItem
            if (original != null) {
                player.replaceMediaItem(index, original)
                player.prepare()
                if (position > 0) {
                    player.seekTo(index, position)
                }
                player.playWhenReady = wasPlaying
            }
        } catch (_: Exception) {
        } finally {
            resetVideoMode()
        }
    }

    fun setVideoMode(enabled: Boolean) {
        val mediaId = currentMediaMetadata.value?.id
            ?: player.currentMediaItem?.mediaId
            ?: return

        if (enabled == isVideoMode && (!enabled || currentVideoSourceMediaId == mediaId)) {
            return
        }

        videoSwitchJob?.cancel()
        _isVideoSwitching.value = true

        videoSwitchJob = scope.launch {
            try {
                val index = player.currentMediaItemIndex
                val position = player.currentPosition
                val wasPlaying = player.playWhenReady

                if (enabled) {
                    if (isTvDevice && wasPlaying) {
                        player.playWhenReady = false
                        _isVideoSwitching.value = true
                    }

                    originalAudioMediaItem = player.getMediaItemAt(index)

                    val metadata = currentMediaMetadata.value
                        ?: player.currentMediaItem?.metadata
                    val songTitle = metadata?.title ?: ""
                    val artistName = metadata?.artists?.firstOrNull()?.name ?: ""
                    val isVideoSong = metadata?.isVideoSong == true

                    val cachedSearchResult = videoSearchCache[mediaId]
                    val searchResult = if (cachedSearchResult != null) {
                        Result.success(cachedSearchResult)
                    } else {
                        val timeoutResult = withContext(Dispatchers.IO) {
                            kotlinx.coroutines.withTimeoutOrNull(VIDEO_SEARCH_TIMEOUT_MS) {
                                VortexPlayerUtils.getVideoStreamUrlWithFallback(songTitle, artistName, mediaId, isVideoSong)
                            }
                        }
                        timeoutResult ?: Result.failure(Exception("Video resolution timed out"))
                    }

                    if (searchResult.isSuccess) {
                        val videoData = searchResult.getOrNull()
                        if (videoData != null && cachedSearchResult == null) {
                            videoSearchCache[mediaId] = videoData
                        }

                        if (videoData != null) {
                            var videoId = videoData.videoId
                            val ccEnabled = dataStore.get(com.ikyvortex.vortexmusic.constants.SubtitlesEnabledKey, true)

                            val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()
                            val subtitleInfos = mutableListOf<SubtitleInfo>()
                            val subtitleVideoId = videoId

                            val subtitleJob = if (ccEnabled) {
                                scope.launch {
                                    try {
                                        val captionResult = withContext(Dispatchers.IO) {
                                            YouTube.getCaptionTracksWithDuration(subtitleVideoId).getOrNull()
                                        }
                                        val captionTracks = captionResult?.first
                                        val videoDurationMs = captionResult?.second
                                        if (!captionTracks.isNullOrEmpty()) {
                                            for (track in captionTracks) {
                                                try {
                                                    val subtitleText = withContext(Dispatchers.IO) {
                                                        YouTube.fetchSubtitleFromCaptionTrack(track.baseUrl).getOrNull()
                                                    }
                                                    if (!subtitleText.isNullOrBlank()) {
                                                        val vttText = YouTube.convertTimedTextToVttWithDuration(subtitleText, videoDurationMs)
                                                        val tempFile = withContext(Dispatchers.IO) {
                                                            File(cacheDir, "subtitle_${track.vssId}.vtt").apply {
                                                                writeText(vttText)
                                                            }
                                                        }
                                                        val uri = android.net.Uri.fromFile(tempFile)
                                                        val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                                                            .setMimeType(MimeTypes.TEXT_VTT)
                                                            .setLanguage(track.languageCode)
                                                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                                            .build()
                                                        synchronized(subtitleConfigs) {
                                                            subtitleConfigs.add(subConfig)
                                                            subtitleInfos.add(SubtitleInfo(
                                                                url = track.baseUrl,
                                                                format = "vtt",
                                                                language = java.util.Locale(track.languageCode).displayLanguage,
                                                                languageCode = track.languageCode,
                                                                isAutoGenerated = track.kind == "asr",
                                                                cachedContent = vttText,
                                                                cachedFilePath = tempFile.absolutePath
                                                            ))
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                        _availableSubtitles.value = subtitleInfos.toList()
                                        if (subtitleInfos.isNotEmpty()) {
                                            val preferredLang = dataStore.get(com.ikyvortex.vortexmusic.constants.SubtitleLanguageKey, "en")
                                            val preferredIndex = subtitleInfos.indexOfFirst { it.languageCode == preferredLang }
                                            selectSubtitle(if (preferredIndex >= 0) preferredIndex else 0)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            } else null

                            var sourceResult = withContext(Dispatchers.IO) {
                                VortexPlayerUtils.getVideoStreamSource(videoId)
                            }

                            if (sourceResult.isFailure && isVideoSong && videoId == mediaId) {
                                subtitleJob?.cancel()
                                val fallbackVideo = withContext(Dispatchers.IO) {
                                    VortexPlayerUtils.getVideoStreamUrlWithFallback(
                                        songTitle,
                                        artistName,
                                        mediaId,
                                        isVideoSong = false
                                    ).getOrNull()
                                }
                                if (fallbackVideo != null && fallbackVideo.videoId != videoId) {
                                    videoId = fallbackVideo.videoId
                                    sourceResult = withContext(Dispatchers.IO) {
                                        VortexPlayerUtils.getVideoStreamSource(videoId)
                                    }
                                }
                            }

                            if (sourceResult.isSuccess) {
                                val streamSource = sourceResult.getOrNull()
                                val primaryVideoUrl = when (streamSource) {
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Single -> streamSource.url
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Merged -> streamSource.videoUrl
                                    null -> ""
                                }
                                val primaryMimeType = when (streamSource) {
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Single -> streamSource.mimeType
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Merged -> streamSource.videoMimeType
                                    null -> "video/mp4"
                                }
                                currentVideoUrl = primaryVideoUrl

                                if (primaryVideoUrl.isBlank() || streamSource == null) {
                                    _videoFetchError.value = "URL video kosong - Lagu ini mungkin tidak memiliki video"
                                    _videoModeMessage.value = "Tidak ada video yang tersedia untuk lagu ini"
                                    restoreTvPlaybackIfPaused(wasPlaying)
                                    resetVideoMode()
                                    return@launch
                                }

                                val currentItem = player.getMediaItemAt(index)
                                val readySubtitleConfigs = synchronized(subtitleConfigs) {
                                    subtitleConfigs.toList()
                                }

                                val videoMediaItemBuilder = currentItem.buildUpon()
                                    .setUri(primaryVideoUrl)
                                    .setMimeType(primaryMimeType)
                                    .setCustomCacheKey(mediaId + "_video")

                                if (ccEnabled && readySubtitleConfigs.isNotEmpty()) {
                                    videoMediaItemBuilder.setSubtitleConfigurations(readySubtitleConfigs)
                                }

                                val videoMediaItem = videoMediaItemBuilder.build()

                                if (ccEnabled) {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredTextLanguage("en")
                                        .build()
                                } else {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredTextLanguage(null)
                                        .build()
                                }

                                player.playWhenReady = false

                                when (streamSource) {
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Single -> {
                                        player.replaceMediaItem(index, videoMediaItem)
                                    }
                                    is com.ikyvortex.auravideo.VortexVideo.VideoStreamSource.Merged -> {
                                        val factory = ProgressiveMediaSource.Factory(
                                            createDataSourceFactory(),
                                            ExtractorsFactory {
                                                arrayOf(
                                                    MatroskaExtractor(),
                                                    FragmentedMp4Extractor(),
                                                    Mp4Extractor()
                                                )
                                            }
                                        )
                                        val videoSource = factory.createMediaSource(videoMediaItem)
                                        val audioMediaItem = MediaItem.Builder()
                                            .setUri(streamSource.audioUrl)
                                            .setMimeType(streamSource.audioMimeType)
                                            .setCustomCacheKey(mediaId + "_video_audio")
                                            .build()
                                        val audioSource = factory.createMediaSource(audioMediaItem)
                                        val merged = MergingMediaSource(true, true, videoSource, audioSource)

                                        val insertIndex = (index + 1).coerceAtMost(player.mediaItemCount)
                                        isInjectingVideoSource = true
                                        try {
                                            player.addMediaSource(insertIndex, merged)
                                            player.removeMediaItem(index)
                                        } finally {
                                            isInjectingVideoSource = false
                                        }
                                    }
                                }
                                player.prepare()

                                if (position > 0) {
                                    player.seekTo(index, position)
                                }
                                if (isTvDevice && wasPlaying) {
                                    waitForFirstVideoFrameThenResume(wasPlaying)
                                } else {
                                    player.playWhenReady = wasPlaying
                                }
                                isVideoMode = true
                                _videoModeEnabled.value = true
                                currentVideoSourceMediaId = mediaId
                                _currentVideoId.value = videoId
                                if (sponsorBlockManager.enabled.value) {
                                    sponsorBlockManager.forceReload(videoId, currentPlaybackDurationMs())
                                }
                            } else {
                                _videoFetchError.value = "Gagal memuat streaming video"
                                _videoModeMessage.value = "Tidak dapat memuat video"
                                restoreTvPlaybackIfPaused(wasPlaying)
                                resetVideoMode()
                            }
                        } else {
                            _videoFetchError.value = "Video tidak ditemukan untuk lagu ini"
                            _videoModeMessage.value = "Tidak ada video yang tersedia untuk lagu ini"
                            restoreTvPlaybackIfPaused(wasPlaying)
                            resetVideoMode()
                        }
                    } else {
                        val errorMsg = searchResult.exceptionOrNull()?.message ?: "Kesalahan tidak diketahui"
                        _videoFetchError.value = "Tidak ada video yang tersedia: $errorMsg"
                        _videoModeMessage.value = "Video tidak ditemukan untuk lagu ini"
                        restoreTvPlaybackIfPaused(wasPlaying)
                        resetVideoMode()
                    }
                } else {
                    val original = originalAudioMediaItem
                    if (original != null) {
                        player.replaceMediaItem(index, original)
                        player.seekTo(index, position)
                        player.playWhenReady = wasPlaying
                    }
                    isVideoMode = false
                    _videoModeEnabled.value = false
                    currentVideoUrl = null
                    currentVideoSourceMediaId = null
                    originalAudioMediaItem = null
                }
            } catch (_: Exception) {
                isVideoMode = false
                _videoModeEnabled.value = false
                originalAudioMediaItem = null
                if (isTvDevice && !player.playWhenReady) {
                    player.playWhenReady = true
                }
            } finally {
                _isVideoSwitching.value = false
            }
        }
    }

    suspend fun checkVideoAvailability(mediaId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val available = VortexPlayerUtils.hasVideoPlayback(mediaId)
                _isVideoAvailable.value = available
                available
            } catch (_: Exception) {
                _isVideoAvailable.value = false
                false
            }
        }
    }

    fun clearVideoError() {
        _videoFetchError.value = null
    }

    fun clearVideoModeMessage() {
        _videoModeMessage.value = null
    }

    private fun initializeCast() {
        if (dataStore.get(com.ikyvortex.vortexmusic.constants.EnableGoogleCastKey, true)) {
            try {
                castConnectionHandler = CastConnectionHandler(this, scope, this)
                castConnectionHandler?.initialize()
            } catch (_: Exception) {
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            scheduleCrossfade()
            refreshLockscreenSnapshot()
            stemRefPositionMs = newPosition.positionMs.coerceAtLeast(0L)
            stemRefElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime()
        } else if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && RemoveMusicVocalRemover.state.value.active) {
            deactivateVocalInstrumentSeparation()
        }
    }

    private fun scheduleCrossfade() {
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        if (!crossfadeEnabled) return
        if (isVideoMode) return
        if (player.repeatMode == REPEAT_MODE_ONE) return
        if (player.duration == C.TIME_UNSET || player.duration <= crossfadeDuration) {
            if (player.duration == C.TIME_UNSET && player.playWhenReady) {
                crossfadeTriggerJob = scope.launch {
                    delay(500)
                    if (player.playWhenReady && player.currentMediaItem != null) {
                        scheduleCrossfade()
                    }
                }
            }
            return
        }
        if (crossfadeGapless && isNextItemGapless()) return
        val nextCrossfadeIndex = nextCrossfadeMediaItemIndex()
        if (nextCrossfadeIndex == C.INDEX_UNSET) return
        if (player.getMediaItemAt(nextCrossfadeIndex).metadata?.isVideoSong == true) return

        val triggerPercent = if (automixEnabled) automixBlendPercent / 100f else 0f
        val triggerOffset = if (automixEnabled) {
            (player.duration * triggerPercent).toLong()
        } else {
            player.duration - crossfadeDuration.toLong()
        }
        val triggerTime = if (automixEnabled) {
            minOf(triggerOffset, player.duration - crossfadeDuration.toLong())
        } else {
            triggerOffset
        }
        val remainingTrackMs = player.duration - player.currentPosition
        if (remainingTrackMs <= 1200L) return
        val delayMs = (triggerTime - player.currentPosition).coerceAtLeast(0L)
        val preWarmLeadMs = 1500L
        val preWarmDelayMs = (delayMs - preWarmLeadMs).coerceAtLeast(0L)

        val targetMediaId = player.currentMediaItem?.mediaId

        crossfadeTriggerJob = scope.launch {
            delay(preWarmDelayMs)
            if (isActive && player.isPlaying && player.currentMediaItem?.mediaId == targetMediaId) {
                startCrossfade(triggerTimeMs = triggerTime)
            }
        }
    }

    private fun isNextItemGapless(): Boolean {
        val current = player.currentMediaItem?.mediaMetadata ?: return false
        val nextIndex = nextCrossfadeMediaItemIndex()
        if (nextIndex == C.INDEX_UNSET) return false
        val next = player.getMediaItemAt(nextIndex).mediaMetadata
        return current.albumTitle != null && current.albumTitle == next.albumTitle
    }

    private fun nextCrossfadeMediaItemIndex(): Int {
        if (player.repeatMode == REPEAT_MODE_ONE || player.mediaItemCount < 2) return C.INDEX_UNSET
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex != C.INDEX_UNSET) return nextIndex
        return if (player.repeatMode == REPEAT_MODE_ALL) 0 else C.INDEX_UNSET
    }

    private fun recordCurrentSongToHistory() {
        val mediaItem = player.currentMediaItem ?: return
        val playTimeMs = player.currentPosition
        val historyDurationMs = dataStore[HistoryDuration]?.times(1000f) ?: 30000f

        if (playTimeMs >= historyDurationMs && !dataStore.get(PauseListenHistoryKey, false)) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
        }

        if (playTimeMs >= historyDurationMs) {
            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                    ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                        .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                playbackUrl?.let {
                    YouTube.registerPlayback(null, playbackUrl)
                        .onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    private fun startCrossfade(triggerTimeMs: Long = 0L) {
        if (isCrossfading) return
        if (isVideoMode) return
        val nextIndex = nextCrossfadeMediaItemIndex()
        if (nextIndex == C.INDEX_UNSET) return
        val outgoingMediaId = player.currentMediaItem?.mediaId ?: return

        secondaryPlayer?.let { stale ->
            stale.removeListener(secondaryPlayerListener)
            stale.stop()
            stale.clearMediaItems()
            playerSilenceProcessors.remove(stale)
            stale.release()
        }
        secondaryPlayer = null

        recordCurrentSongToHistory()

        secondaryPlayer = createExoPlayer(publishPlayer = false)
        val secPlayer = secondaryPlayer!!
        secPlayer.addListener(secondaryPlayerListener)

        val itemCount = player.mediaItemCount
        val items = mutableListOf<MediaItem>()
        for (i in 0 until itemCount) {
            items.add(player.getMediaItemAt(i))
        }

        secPlayer.setMediaItems(items)
        secPlayer.repeatMode = player.repeatMode
        secPlayer.shuffleModeEnabled = player.shuffleModeEnabled
        secPlayer.seekTo(nextIndex, 0)
        secPlayer.volume = 0f
        val readyListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_READY || secondaryPlayer !== secPlayer) return
                secPlayer.removeListener(this)

                scope.launch {
                    val waitMs = (triggerTimeMs - player.currentPosition).coerceAtLeast(0L)
                    if (waitMs > 0L) {
                        secPlayer.playWhenReady = false
                        delay(waitMs)
                    }

                    if (secondaryPlayer !== secPlayer ||
                        player.currentMediaItem?.mediaId != outgoingMediaId ||
                        player.repeatMode == REPEAT_MODE_ONE || !crossfadeEnabled
                    ) {
                        secPlayer.removeListener(secondaryPlayerListener)
                        secPlayer.stop()
                        secPlayer.clearMediaItems()
                        playerSilenceProcessors.remove(secPlayer)
                        secPlayer.release()
                        if (secondaryPlayer === secPlayer) secondaryPlayer = null
                        scheduleCrossfade()
                        return@launch
                    }
                    secPlayer.playWhenReady = true
                    performCrossfadeSwap()
                }
            }
        }
        secPlayer.addListener(readyListener)
        secPlayer.prepare()
        secPlayer.playWhenReady = true
    }

    private fun performCrossfadeSwap() {
        if (sleepTimer.pauseWhenSongEnd) {
            sleepTimer.notifySongTransition()
            secondaryPlayer?.let { pendingPlayer ->
                pendingPlayer.removeListener(secondaryPlayerListener)
                pendingPlayer.stop()
                pendingPlayer.clearMediaItems()
                playerSilenceProcessors.remove(pendingPlayer)
                pendingPlayer.release()
            }
            secondaryPlayer = null
            return
        }

        val nextPlayer = secondaryPlayer ?: return
        val oldPlayer = player
        isCrossfading = true

        nextPlayer.removeListener(secondaryPlayerListener)
        nextPlayer.addListener(this)
        nextPlayer.addListener(sleepTimer)
        nextPlayer.addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))

        try {
            mediaSession.player = nextPlayer
        } catch (e: Exception) {
            nextPlayer.removeListener(this)
            nextPlayer.removeListener(sleepTimer)
            nextPlayer.stop()
            nextPlayer.clearMediaItems()
            playerSilenceProcessors.remove(nextPlayer)
            nextPlayer.release()
            secondaryPlayer = null
            isCrossfading = false
            scheduleCrossfade()
            return
        }

        fadingPlayer = oldPlayer
        player = nextPlayer
        secondaryPlayer = null
        sleepTimer.player = nextPlayer
        currentMediaMetadata.value = nextPlayer.currentMetadata

        oldPlayer.removeListener(this)
        oldPlayer.removeListener(sleepTimer)

        val playbackBridge = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isCrossfading && fadingPlayer === oldPlayer) {
                    if (isPlaying) {
                        oldPlayer.play()
                    } else {
                        oldPlayer.pause()
                    }
                } else {
                    nextPlayer.removeListener(this)
                }
            }
        }
        nextPlayer.addListener(playbackBridge)

        _playerFlow.value = nextPlayer

        crossfadeJob = scope.launch {
            val remainingOutgoingMs = (oldPlayer.duration - oldPlayer.currentPosition).takeIf {
                oldPlayer.duration != C.TIME_UNSET && it > 500L
            } ?: crossfadeDuration.toLong()
            val duration = minOf(crossfadeDuration.toLong(), remainingOutgoingMs).coerceAtLeast(500L)
            val steps = 40
            val stepTime = (duration / steps).coerceAtLeast(15L)
            val targetVolume = if (isMuted.value) 0f else (playerVolume.value * sleepTimer.fadeVolumeMultiplierFlow.value).coerceIn(0f, 1f)
            val outgoingStartVolume = oldPlayer.volume
            nextPlayer.volume = 0f

            for (i in 1..steps) {
                if (!isActive) break
                while (!nextPlayer.isPlaying && isActive) {
                    delay(100)
                }

                val progress = i / steps.toFloat()
                val fadeIn: Float
                val fadeOut: Float
                when {
                    automixEnabled || crossfadeCurve == CrossfadeCurve.LINEAR -> {
                        fadeIn = progress
                        fadeOut = 1.0f - progress
                    }
                    crossfadeCurve == CrossfadeCurve.EQUAL_POWER -> {
                        val angle = progress * (PI.toFloat() / 2f)
                        fadeIn = sin(angle)
                        fadeOut = cos(angle)
                    }
                    else -> {
                        fadeIn = 1.0f - (1.0f - progress) * (1.0f - progress)
                        fadeOut = (1.0f - progress) * (1.0f - progress)
                    }
                }

                try {
                    nextPlayer.volume = targetVolume * fadeIn
                    oldPlayer.volume = outgoingStartVolume * fadeOut
                } catch (_: Exception) {
                    break
                }

                delay(stepTime)
            }

            try {
                oldPlayer.volume = 0f
                nextPlayer.volume = targetVolume
                nextPlayer.removeListener(playbackBridge)
                delay(100)
                closeAudioEffectSession()
                cleanupCrossfade()
                openAudioEffectSession()
                scheduleCrossfade()
            } catch (_: Exception) {
                cleanupCrossfade()
                scheduleCrossfade()
            }
        }
    }

    private fun cleanupCrossfade() {
        fadingPlayer?.let { oldPlayer ->
            oldPlayer.stop()
            oldPlayer.clearMediaItems()
            playerSilenceProcessors.remove(oldPlayer)
            oldPlayer.release()
        }
        fadingPlayer = null
        isCrossfading = false
    }

    private fun handleAlarmPlay() {
        val ds = dataStore
        val csv = ds.get(com.ikyvortex.vortexmusic.constants.AlarmSongIdsKey, "")
        val songIds = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (songIds.isEmpty()) return
        val shuffle = ds.get(com.ikyvortex.vortexmusic.constants.AlarmShuffleKey, false)
        val volume = ds.get(com.ikyvortex.vortexmusic.constants.AlarmVolumeKey, 0.85f)

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        scope.launch {
            val songs = withContext(Dispatchers.IO) { database.getSongsByIds(songIds) }
            val ordered = if (shuffle) songs.shuffled() else songs
            val mediaItems = ordered.map { it.toMediaItem() }
            if (mediaItems.isEmpty()) return@launch
            setMuted(false)
            if (::playerVolume.isInitialized) {
                playerVolume.value = volume.coerceIn(0.05f, 1f)
            }
            playQueue(
                queue = com.ikyvortex.vortexmusic.playback.queues.ListQueue(
                    title = getString(R.string.alarm_title),
                    items = mediaItems,
                    startIndex = 0,
                    position = 0L,
                ),
                playWhenReady = true,
            )
        }
    }

    companion object {
        val desiredSubtitleLanguages = setOf("en", "sw", "es", "fr", "la")

        const val ACTION_PLAY_ALARM = "com.ikyvortex.vortexmusic.playback.ACTION_PLAY_ALARM"

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val YOUTUBE_PLAYLIST = "youtube_playlist"
        const val SEARCH = "search"
        const val SHUFFLE_ACTION = "__shuffle__"

        const val CHANNEL_ID = "music_channel_01"
        const val TV_CHANNEL_ID = "tv_now_playing"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val MAX_RETRY_COUNT = 10
        const val MAX_GAIN_MB = 300

        private const val VIDEO_SEARCH_TIMEOUT_MS = 20_000L
        const val MIN_GAIN_MB = -1500

        const val TAG = "MusicService"

        @Volatile
        var isRunning = false
            private set
    }
}
