package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
