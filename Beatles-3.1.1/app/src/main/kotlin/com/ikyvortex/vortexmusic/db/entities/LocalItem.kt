/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
