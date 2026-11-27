package com.ikyvortex.innertube.models.body

import com.ikyvortex.innertube.models.Context
import com.ikyvortex.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
