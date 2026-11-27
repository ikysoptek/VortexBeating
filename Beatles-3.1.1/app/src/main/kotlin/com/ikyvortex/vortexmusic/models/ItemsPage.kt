/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.models

import com.ikyvortex.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
