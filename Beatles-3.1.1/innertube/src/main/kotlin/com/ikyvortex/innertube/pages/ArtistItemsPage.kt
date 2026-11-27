package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.Album
import com.ikyvortex.innertube.models.AlbumItem
import com.ikyvortex.innertube.models.Artist
import com.ikyvortex.innertube.models.MusicResponsiveListItemRenderer
import com.ikyvortex.innertube.models.MusicTwoRowItemRenderer
import com.ikyvortex.innertube.models.PlaylistItem
import com.ikyvortex.innertube.models.SongItem
import com.ikyvortex.innertube.models.YTItem
import com.ikyvortex.innertube.models.oddElements
import com.ikyvortex.innertube.models.splitBySeparator
import com.ikyvortex.innertube.utils.parseTime

data class ArtistItemsPage(
    val title: String,
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            // Extract artists from flexColumns - try multiple approaches
            val artists = renderer.flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }

            // Extract album from last flexColumn (like SimpMusic does)
            val album = renderer.flexColumns.lastOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.firstOrNull()?.let {
                    if (it.navigationEndpoint?.browseEndpoint?.browseId != null) {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint.browseEndpoint.browseId
                        )
                    } else null
                }

            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val watchEndpoint = PageHelper.watchEndpoint(renderer)

            return SongItem(
                id = PageHelper.videoId(renderer) ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = artists ?: return null,
                album = album,
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()
                    ?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = watchEndpoint,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isAlbum -> {
                    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
                    AlbumItem(
                        browseId = browseId,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer
                            ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.anyWatchEndpoint?.playlistId ?: browseId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }
                // Video
                renderer.isSong -> {
                    val watchEndpoint = PageHelper.watchEndpoint(renderer)
                    SongItem(
                        id = watchEndpoint?.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = renderer.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        album = null,
                        duration = null,
                        musicVideoType = renderer.musicVideoType,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        endpoint = watchEndpoint
                    )
                }
                renderer.isPlaylist -> {
                    val playlistId = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL")
                        ?: renderer.navigationEndpoint.watchPlaylistEndpoint?.playlistId?.removePrefix("VL")
                        ?: renderer.navigationEndpoint.watchEndpoint?.playlistId?.removePrefix("VL")
                        ?: return null
                    PlaylistItem(
                        id = playlistId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = renderer.subtitle?.runs?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        },
                        songCountText = renderer.subtitle?.runs?.getOrNull(4)?.text,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                            ?: renderer.thumbnailRenderer.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                            ?: "",
                        playEndpoint = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.anyWatchEndpoint
                            ?: renderer.navigationEndpoint.anyWatchEndpoint,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.anyWatchEndpoint,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.anyWatchEndpoint
                    )
                }
                else -> null
            }
        }
    }
}
