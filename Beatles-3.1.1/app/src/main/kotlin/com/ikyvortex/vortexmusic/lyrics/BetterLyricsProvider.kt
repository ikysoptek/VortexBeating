/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.music.betterlyrics.BetterLyrics
import com.ikyvortex.vortexmusic.constants.EnableBetterLyricsKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)
}
