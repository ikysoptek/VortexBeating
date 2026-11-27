/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ikyvortex.innertube.models.AlbumItem
import com.ikyvortex.innertube.models.ArtistItem
import com.ikyvortex.innertube.models.EpisodeItem
import com.ikyvortex.innertube.models.PlaylistItem
import com.ikyvortex.innertube.models.PodcastItem
import com.ikyvortex.innertube.models.SongItem
import com.ikyvortex.vortexmusic.LocalPlayerAwareWindowInsets
import com.ikyvortex.vortexmusic.LocalPlayerConnection
import com.ikyvortex.vortexmusic.R
import com.ikyvortex.vortexmusic.constants.GridItemSize
import com.ikyvortex.vortexmusic.constants.GridItemsSizeKey
import com.ikyvortex.vortexmusic.constants.GridThumbnailHeight
import com.ikyvortex.vortexmusic.models.toMediaMetadata
import com.ikyvortex.vortexmusic.playback.queues.YouTubeQueue
import com.ikyvortex.vortexmusic.ui.component.IconButton
import com.ikyvortex.vortexmusic.ui.component.LocalMenuState
import com.ikyvortex.vortexmusic.ui.component.YouTubeGridItem
import com.ikyvortex.vortexmusic.ui.component.shimmer.GridItemPlaceHolder
import com.ikyvortex.vortexmusic.ui.component.shimmer.ShimmerHost
import com.ikyvortex.vortexmusic.ui.menu.YouTubeAlbumMenu
import com.ikyvortex.vortexmusic.ui.menu.YouTubeArtistMenu
import com.ikyvortex.vortexmusic.ui.menu.YouTubePlaylistMenu
import com.ikyvortex.vortexmusic.ui.menu.YouTubeSongMenu
import com.ikyvortex.vortexmusic.ui.utils.backToMain
import com.ikyvortex.vortexmusic.utils.rememberEnumPreference
import com.ikyvortex.vortexmusic.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val allItems = browseResult?.items?.flatMap { it.items } ?: emptyList()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        if (browseResult == null) {
            items(8) {
                ShimmerHost {
                    GridItemPlaceHolder(fillMaxWidth = true)
                }
            }
        }

        items(
            items = allItems.distinctBy { it.id },
            key = { it.id }
        ) { item ->
            YouTubeGridItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                fillMaxWidth = true,
                coroutineScope = coroutineScope,
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(item.toMediaMetadata())
                                        )
                                    }
                                }
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                is EpisodeItem -> playerConnection.playQueue(
                                    YouTubeQueue.radio(item.toMediaMetadata())
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem ->
                                        YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    is AlbumItem ->
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    is ArtistItem ->
                                        YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = menuState::dismiss,
                                        )
                                    is PlaylistItem ->
                                        YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    is PodcastItem ->
                                        YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    is EpisodeItem ->
                                        YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                }
                            }
                        }
                    )
                    .animateItem()
            )
        }
    }

    TopAppBar(
        title = { Text(browseResult?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
