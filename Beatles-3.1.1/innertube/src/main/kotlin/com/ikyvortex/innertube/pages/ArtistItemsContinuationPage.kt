package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
