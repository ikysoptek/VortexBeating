package com.ikyvortex.innertube.pages

import com.ikyvortex.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
