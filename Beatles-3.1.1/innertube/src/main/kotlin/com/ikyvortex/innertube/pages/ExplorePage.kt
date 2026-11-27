package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.MixesPage
import com.ikyvortex.innertube.PodcastsPage
import com.ikyvortex.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
    val podcasts: List<PodcastsPage.PodcastSection> = emptyList(),
    val mixes: List<MixesPage.MixSection> = emptyList(),
)
