package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.Album
import com.ikyvortex.innertube.models.AlbumItem
import com.ikyvortex.innertube.models.Artist
import com.ikyvortex.innertube.models.ArtistItem
import com.ikyvortex.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE
import com.ikyvortex.innertube.models.EpisodeItem
import com.ikyvortex.innertube.models.MusicResponsiveListItemRenderer
import com.ikyvortex.innertube.models.PlaylistItem
import com.ikyvortex.innertube.models.PodcastItem
import com.ikyvortex.innertube.models.SongItem
import com.ikyvortex.innertube.models.YTItem
import com.ikyvortex.innertube.models.oddElements
import com.ikyvortex.innertube.models.splitBySeparator
import com.ikyvortex.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val secondaryLine =
            renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()
                ?: return null
        return when {
            renderer.isEpisode -> {
                val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

                val podcastSectionIndex = secondaryLine.indexOfFirst { section ->
                    section.any { run ->
                        run.navigationEndpoint?.browseEndpoint
                            ?.browseEndpointContextSupportedConfigs
                            ?.browseEndpointContextMusicConfig
                            ?.pageType == MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE
                    }
                }

                val podcast = if (podcastSectionIndex >= 0) {
                    secondaryLine[podcastSectionIndex].firstOrNull()?.let { run ->
                        Album(
                            name = run.text,
                            id = run.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        )
                    }
                } else null

                val publishDateText = if (podcastSectionIndex > 0)
                    secondaryLine.getOrNull(podcastSectionIndex - 1)?.firstOrNull()?.text
                else null

                EpisodeItem(
                    id = renderer.playlistItemData?.videoId
                        ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                        ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    author = null,
                    podcast = podcast,
                    duration =
                        secondaryLine
                            .lastOrNull()
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime(),
                    publishDateText = publishDateText,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                    endpoint = renderer.overlay
                        ?.musicItemThumbnailOverlayRenderer
                        ?.content
                        ?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint
                        ?.watchEndpoint
                        ?: renderer.navigationEndpoint?.watchEndpoint,
                    libraryAddToken = libraryTokens.addToken,
                    libraryRemoveToken = libraryTokens.removeToken,
                )
            }
            renderer.isSong -> {
                val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
                val watchEndpoint = PageHelper.watchEndpoint(renderer)

                SongItem(
                    id = PageHelper.videoId(renderer) ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                    album =
                        secondaryLine.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                            )
                        },
                    duration =
                        secondaryLine
                            .lastOrNull()
                            ?.firstOrNull()
                            ?.text
                            ?.parseTime(),
                    musicVideoType = renderer.musicVideoType,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                    endpoint = watchEndpoint,
                    libraryAddToken = libraryTokens.addToken,
                    libraryRemoveToken = libraryTokens.removeToken,
                    isEpisode = false
                )
            }
            renderer.isArtist -> {
                ArtistItem(
                    id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                            ?: return null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                        ?: renderer.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                        ?: "",
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.anyWatchEndpoint,
                    radioEndpoint =
                        renderer.menu?.menuRenderer?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.anyWatchEndpoint,
                )
            }
            renderer.isAlbum -> {
                val browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null
                AlbumItem(
                    browseId = browseId,
                    playlistId =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.anyWatchEndpoint
                            ?.playlistId
                            ?: browseId,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    artists =
                        secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: secondaryLine.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: emptyList(),
                    year =
                        secondaryLine
                            .getOrNull(2)
                            ?.firstOrNull()
                            ?.text
                            ?.toIntOrNull()
                            ?: secondaryLine.lastOrNull()?.firstOrNull()?.text?.toIntOrNull(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                        ?: renderer.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                        ?: "",
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                )
            }
            renderer.isPlaylist -> {
                val playlistId = renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("VL")
                    ?: renderer.navigationEndpoint?.watchPlaylistEndpoint?.playlistId?.removePrefix("VL")
                    ?: renderer.navigationEndpoint?.watchEndpoint?.playlistId?.removePrefix("VL")
                    ?: return null
                PlaylistItem(
                    id = playlistId,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    author =
                        secondaryLine.firstOrNull()?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        },
                    songCountText =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.lastOrNull()
                            ?.text,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                        ?: renderer.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                        ?: "",
                    playEndpoint =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.anyWatchEndpoint
                            ?: renderer.navigationEndpoint?.anyWatchEndpoint,
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.anyWatchEndpoint,
                    radioEndpoint =
                        renderer.menu?.menuRenderer?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.anyWatchEndpoint,
                )
            }
            renderer.isPodcast -> {
                PodcastItem(
                    id =
                        renderer.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId
                            ?: return null,
                    title =
                        renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    author =
                        secondaryLine.firstOrNull()?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        },
                    episodeCountText =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.lastOrNull()
                            ?.text,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint =
                        renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint,
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                )
            }
            else -> null
        }
    }
}
