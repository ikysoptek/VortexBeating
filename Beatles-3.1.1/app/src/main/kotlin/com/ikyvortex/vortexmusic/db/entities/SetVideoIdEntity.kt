/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_video_id")
data class SetVideoIdEntity(
    @PrimaryKey(autoGenerate = false)
    val videoId: String = "",
    val setVideoId: String? = null,
)
