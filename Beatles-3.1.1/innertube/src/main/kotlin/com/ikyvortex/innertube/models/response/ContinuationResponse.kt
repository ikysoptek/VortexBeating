package com.ikyvortex.innertube.models.response

import com.ikyvortex.innertube.models.Continuation
import com.ikyvortex.innertube.models.MusicShelfRenderer
import kotlinx.serialization.Serializable

@Serializable
data class ContinuationResponse(
    val onResponseReceivedActions: List<ResponseAction>?,
    val continuationContents: ContinuationContents?,
) {

    @Serializable
    data class ResponseAction(
        val appendContinuationItemsAction: ContinuationItems?,
    )

    @Serializable
    data class ContinuationItems(
        val continuationItems: List<MusicShelfRenderer.Content>?,
    )

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation?,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfContinuation?,
        val gridContinuation: GridContinuation?,
        val musicShelfContinuation: MusicShelfRenderer?,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<MusicShelfRenderer.Content> = emptyList(),
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class MusicPlaylistShelfContinuation(
            val contents: List<MusicShelfRenderer.Content> = emptyList(),
            val continuations: List<Continuation>?,
        )

        @Serializable
        data class GridContinuation(
            val items: List<com.ikyvortex.innertube.models.GridRenderer.Item> = emptyList(),
            val continuations: List<Continuation>?,
        )
    }
}
