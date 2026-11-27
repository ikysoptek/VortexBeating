/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.models

import com.ikyvortex.innertube.models.YTItem
import com.ikyvortex.vortexmusic.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
