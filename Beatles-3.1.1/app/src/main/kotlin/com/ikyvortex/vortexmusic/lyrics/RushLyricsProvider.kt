/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.rush.RushLyrics
import com.ikyvortex.vortexmusic.constants.EnableRushLyricsKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get

object RushLyricsProvider : LyricsProvider {
    override val name = "RushLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableRushLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = RushLyrics.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        RushLyrics.getAllLyrics(title, artist, duration, album, callback)
    }
}
