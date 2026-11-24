/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.innertube.models.AccountInfo
import com.nanzbeatles.nanas.constants.ArtistSongSortType
import com.nanzbeatles.nanas.db.DatabaseDao
import com.nanzbeatles.nanas.db.entities.Artist
import com.nanzbeatles.nanas.db.entities.PlaylistEntity
import com.nanzbeatles.nanas.db.entities.SongWithStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import java.util.UUID

sealed class PlaylistCreationState {
    object Idle : PlaylistCreationState()
    object Creating : PlaylistCreationState()
    object Success : PlaylistCreationState()
}

class WrappedManager(
    private val databaseDao: DatabaseDao,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(WrappedState())
    val state = _state.asStateFlow()

    private var activeFromTimestamp: Long = 0L
    private var activeToTimestamp: Long = System.currentTimeMillis()

    private fun monthRange(date: LocalDate): Pair<Long, Long> {
        val year = date.year
        val month = date.monthValue - 1
        val from = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val to = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return from to to
    }

    private suspend fun resolveActiveDateRange(): Pair<Long, Long> {
        val now = LocalDate.now()
        val (curFrom, curTo) = monthRange(now)
        val currentPlayTime = databaseDao.getTotalPlayTimeInRange(curFrom, curTo).first() ?: 0L
        val currentSongCount = databaseDao.getUniqueSongCountInRange(curFrom, curTo).first()

        if (currentPlayTime > 0L || currentSongCount > 0) {
            return curFrom to curTo
        }

        val (prevFrom, prevTo) = monthRange(now.minusMonths(1))
        val prevPlayTime = databaseDao.getTotalPlayTimeInRange(prevFrom, prevTo).first() ?: 0L
        if (prevPlayTime > 0L) {
            return prevFrom to prevTo
        }

        return curFrom to curTo
    }

    fun createPlaylist(imageResName: String) {
        if (_state.value.playlistCreationState != PlaylistCreationState.Idle) return

        _state.update { it.copy(playlistCreationState = PlaylistCreationState.Creating) }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val (fromTimestamp, toTimestamp) = if (activeFromTimestamp > 0L) {
                        activeFromTimestamp to activeToTimestamp
                    } else {
                        resolveActiveDateRange()
                    }
                    val allSongs = databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = -1).first()

                    val playlistId = UUID.randomUUID().toString()

                    val drawableId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
                    val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
                    val file = File(context.cacheDir, "$playlistId.png")
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }

                    val newPlaylist = PlaylistEntity(
                        id = playlistId,
                        name = WrappedConstants.PLAYLIST_NAME,
                        thumbnailUrl = file.toURI().toString(),
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true
                    )
                    databaseDao.insert(newPlaylist)

                    val createdPlaylist = databaseDao.playlist(playlistId).first()
                    if (createdPlaylist != null) {
                        val songIds = allSongs.map { it.id }
                        databaseDao.addSongToPlaylist(createdPlaylist, songIds)
                    } else {
                        Timber.tag("WrappedManager")
                            .e("Failed to retrieve created playlist with id: $playlistId")
                    }
                }
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Success) }
            } catch (e: Exception) {
                Timber.tag("WrappedManager").e(e, "Error saving wrapped playlist")
                _state.update { it.copy(playlistCreationState = PlaylistCreationState.Idle) }
            }
        }
    }

    private suspend fun generatePlaylistMap() {
        val topSongs = _state.value.topSongs
        val topArtists = _state.value.topArtists
        if (topSongs.isEmpty()) {
            Timber.tag("WrappedManager").w("Cannot generate playlist map, top songs list is empty.")
            _state.update { it.copy(trackMap = emptyMap()) }
            return
        }

        withContext(Dispatchers.IO) {
            val playlistMap = mutableMapOf<WrappedScreenType, String>()

            // Intro Part: Safe pool even when fewer than 6 songs exist
            val introSongPool = if (topSongs.size > 5) topSongs.subList(5, topSongs.size) else topSongs
            val introSong = introSongPool.randomOrNull()?.id ?: topSongs.last().id
            playlistMap[WrappedScreenType.Welcome] = introSong
            playlistMap[WrappedScreenType.MinutesTease] = introSong
            playlistMap[WrappedScreenType.MinutesReveal] = introSong

            // Music Part: Top 1 song
            val topSong = topSongs.first()
            playlistMap[WrappedScreenType.TotalSongs] = topSong.id
            playlistMap[WrappedScreenType.TopSongReveal] = topSong.id
            playlistMap[WrappedScreenType.Top5Songs] = topSong.id

            // Album Part: Random song from top album
            val topAlbum = _state.value.topAlbum
            val albumSong = topAlbum?.let { album ->
                val albumSongs = databaseDao.albumSongs(album.id).first()
                albumSongs.randomOrNull()?.id
            } ?: topSong.id
            playlistMap[WrappedScreenType.TotalAlbums] = albumSong
            playlistMap[WrappedScreenType.TopAlbumReveal] = albumSong
            playlistMap[WrappedScreenType.Top5Albums] = albumSong

            // Artist Part
            val topArtist = topArtists.firstOrNull()
            val fromTimestamp = activeFromTimestamp
            val toTimestamp = activeToTimestamp

            val artistSong = topArtist?.let { artist ->
                val artistTopSongs = databaseDao.artistSongs(
                    artistId = artist.id,
                    sortType = ArtistSongSortType.PLAY_TIME,
                    descending = true,
                    fromTimeStamp = fromTimestamp,
                    toTimeStamp = toTimestamp
                ).first()
                if (artistTopSongs.isNotEmpty()) {
                    val artistTopSong = artistTopSongs.first()
                    if (artistTopSong.id == topSong.id) {
                        artistTopSongs.getOrNull(1)?.id
                            ?: artistTopSongs.filter { it.id != topSong.id }.randomOrNull()?.id
                            ?: artistTopSong.id
                    } else {
                        artistTopSong.id
                    }
                } else {
                    topSong.id
                }
            } ?: topSong.id
            playlistMap[WrappedScreenType.TotalArtists] = artistSong
            playlistMap[WrappedScreenType.TopArtistReveal] = artistSong
            playlistMap[WrappedScreenType.Top5Artists] = artistSong

            // Top Artist Albums Part
            val topArtistAlbumSong = _state.value.topArtistAlbums.firstOrNull()?.let { album ->
                val albumSongs = databaseDao.albumSongs(album.id).first()
                albumSongs.randomOrNull()?.id
            } ?: artistSong
            playlistMap[WrappedScreenType.TopArtistAlbums] = topArtistAlbumSong

            // End Part: Safe bounds even when fewer than 5 songs exist
            val endSongPool = when {
                topSongs.size >= 5 -> topSongs.subList(2, 5)
                topSongs.size > 2 -> topSongs.subList(1, topSongs.size)
                else -> topSongs
            }
            val endSong = endSongPool.randomOrNull()?.id ?: topSongs.last().id
            playlistMap[WrappedScreenType.DayOfWeek] = endSong
            playlistMap[WrappedScreenType.TimeOfDay] = endSong
            playlistMap[WrappedScreenType.RepeatOffender] = topSong.id
            playlistMap[WrappedScreenType.DiscoveryScore] = endSong
            playlistMap[WrappedScreenType.Comparison] = endSong
            playlistMap[WrappedScreenType.ShareCard] = endSong
            playlistMap[WrappedScreenType.Playlist] = endSong
            playlistMap[WrappedScreenType.Conclusion] = topSong.id

            Timber.tag("WrappedManager").d("Generated Playlist Map: $playlistMap")
            _state.update { it.copy(trackMap = playlistMap) }
        }
    }

    suspend fun prepare() {
        Timber.tag("WrappedManager").d("Starting Wrapped data preparation")

        withContext(Dispatchers.IO) {
            val (fromTimestamp, toTimestamp) = resolveActiveDateRange()
            activeFromTimestamp = fromTimestamp
            activeToTimestamp = toTimestamp

            val accountInfoDeferred = async { YouTube.accountInfo().getOrNull() }
            val topSongsDeferred = async { databaseDao.mostPlayedSongsStats(fromTimestamp, toTimeStamp = toTimestamp, limit = 30).first() }
            val topArtistsDeferred = async { databaseDao.mostPlayedArtists(fromTimestamp, toTimeStamp = toTimestamp, limit = 5).first() }
            val topAlbumsDeferred = async { databaseDao.mostPlayedAlbums(fromTimestamp, toTimeStamp = toTimestamp, limit = 5).first() }
            val uniqueSongCountDeferred = async { databaseDao.getUniqueSongCountInRange(fromTimestamp, toTimestamp).first() }
            val uniqueArtistCountDeferred = async { databaseDao.getUniqueArtistCountInRange(fromTimestamp, toTimestamp).first() }
            val uniqueAlbumCountDeferred = async { databaseDao.getUniqueAlbumCountInRange(fromTimestamp, toTimestamp).first() }
            val totalPlayTimeMsDeferred = async { databaseDao.getTotalPlayTimeInRange(fromTimestamp, toTimestamp).first() ?: 0L }

            val eventsDeferred = async { databaseDao.getEventsInRange(fromTimestamp, toTimestamp).first() }
            val mostReplayedSongIdDeferred = async { databaseDao.getMostReplayedSongInRange(fromTimestamp, toTimestamp).first() }
            val newArtistCountDeferred = async { databaseDao.getNewArtistCountInRange(fromTimestamp, toTimestamp).first() }

            // Comparison against previous month relative to active window
            val activeStartDate = LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(fromTimestamp),
                java.time.ZoneId.systemDefault()
            )
            val (prevFromTimestamp, prevToTimestamp) = monthRange(activeStartDate.minusMonths(1))
            val prevMonthMinutesDeferred = async {
                (databaseDao.getTotalPlayTimeInRange(prevFromTimestamp, prevToTimestamp).first() ?: 0L) / 1000 / 60
            }
            val prevMonthUniqueSongsDeferred = async { databaseDao.getUniqueSongCountInRange(prevFromTimestamp, prevToTimestamp).first() }
            val prevMonthUniqueArtistsDeferred = async { databaseDao.getUniqueArtistCountInRange(prevFromTimestamp, prevToTimestamp).first() }

            val results = awaitAll(
                accountInfoDeferred,
                topSongsDeferred,
                topArtistsDeferred,
                topAlbumsDeferred,
                uniqueSongCountDeferred,
                uniqueArtistCountDeferred,
                uniqueAlbumCountDeferred,
                totalPlayTimeMsDeferred
            )

            val events = eventsDeferred.await()
            val mostReplayedSongId = mostReplayedSongIdDeferred.await()
            val newArtistCount = newArtistCountDeferred.await()
            val prevMonthMinutes = prevMonthMinutesDeferred.await()
            val prevMonthUniqueSongs = prevMonthUniqueSongsDeferred.await()
            val prevMonthUniqueArtists = prevMonthUniqueArtistsDeferred.await()

            @Suppress("UNCHECKED_CAST")
            val topSongsResult = results[1] as List<SongWithStats>
            @Suppress("UNCHECKED_CAST")
            val topAlbumsResult = results[3] as List<com.nanzbeatles.nanas.db.entities.Album>
            @Suppress("UNCHECKED_CAST")
            val topArtistsResult = results[2] as List<Artist>

            val dayOfWeekMinutes = mutableMapOf<Int, Long>()
            events.forEach { event ->
                val dayOfWeek = event.timestamp.dayOfWeek.value
                dayOfWeekMinutes[dayOfWeek] = (dayOfWeekMinutes[dayOfWeek] ?: 0L) + event.playTime
            }
            val listeningByDayOfWeek = dayOfWeekMinutes.mapValues { it.value / 1000 / 60 }

            val timeOfDayMinutes = mutableMapOf<String, Long>()
            events.forEach { event ->
                val hour = event.timestamp.hour
                val timeOfDay = when (hour) {
                    in 5..11 -> "Morning"
                    in 12..16 -> "Afternoon"
                    in 17..21 -> "Evening"
                    else -> "Night"
                }
                timeOfDayMinutes[timeOfDay] = (timeOfDayMinutes[timeOfDay] ?: 0L) + event.playTime
            }
            val listeningByTimeOfDay = timeOfDayMinutes.mapValues { it.value / 1000 / 60 }

            val repeatOffenderSong = mostReplayedSongId?.let { songId ->
                topSongsResult.find { it.id == songId }
            } ?: topSongsResult.firstOrNull()

            val rawPlayTimeMs = results[7] as Long
            val currentMinutes = if (rawPlayTimeMs in 1..59_999L) 1L else (rawPlayTimeMs / 1000L / 60L)
            val monthOverMonthChange = if (prevMonthMinutes > 0) {
                ((currentMinutes - prevMonthMinutes).toFloat() / prevMonthMinutes) * 100
            } else if (currentMinutes > 0) {
                100f
            } else {
                0f
            }

            val topArtistId = topArtistsResult.firstOrNull()?.id
            val topArtistAlbumsDeferred = async {
                if (topArtistId != null) {
                    databaseDao.getAlbumsPlayedByArtist(topArtistId, fromTimestamp, toTimestamp, 10).first()
                } else {
                    emptyList()
                }
            }
            val topArtistAlbumsResult = topArtistAlbumsDeferred.await()

            _state.update {
                it.copy(
                    accountInfo = results[0] as AccountInfo?,
                    topSongs = topSongsResult,
                    topArtists = topArtistsResult,
                    top5Albums = topAlbumsResult,
                    topAlbum = topAlbumsResult.firstOrNull(),
                    uniqueSongCount = results[4] as Int,
                    uniqueArtistCount = results[5] as Int,
                    totalAlbums = results[6] as Int,
                    totalMinutes = currentMinutes,
                    topArtistAlbums = topArtistAlbumsResult,
                    listeningByDayOfWeek = listeningByDayOfWeek,
                    listeningByTimeOfDay = listeningByTimeOfDay,
                    repeatOffenderSong = repeatOffenderSong,
                    discoveryScore = newArtistCount,
                    totalPlayCount = events.size,
                    previousMonthMinutes = prevMonthMinutes,
                    previousMonthUniqueSongs = prevMonthUniqueSongs,
                    previousMonthUniqueArtists = prevMonthUniqueArtists,
                    monthOverMonthChange = monthOverMonthChange
                )
            }
        }

        generatePlaylistMap()
        _state.update { it.copy(isDataReady = true) }
        Timber.tag("WrappedManager").d("Wrapped data preparation finished")
    }
}
