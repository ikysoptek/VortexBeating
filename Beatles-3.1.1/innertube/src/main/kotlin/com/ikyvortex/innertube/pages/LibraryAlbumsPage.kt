package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.Album
import com.ikyvortex.innertube.models.AlbumItem
import com.ikyvortex.innertube.models.Artist
import com.ikyvortex.innertube.models.ArtistItem
import com.ikyvortex.innertube.models.MusicResponsiveListItemRenderer
import com.ikyvortex.innertube.models.MusicTwoRowItemRenderer
import com.ikyvortex.innertube.models.PlaylistItem
import com.ikyvortex.innertube.models.SongItem
import com.ikyvortex.innertube.models.YTItem
import com.ikyvortex.innertube.models.oddElements
import com.ikyvortex.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            return AlbumItem(
                browseId = browseId,
                playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                    ?.musicPlayButtonRenderer?.playNavigationEndpoint
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
    }
}
