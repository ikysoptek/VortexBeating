/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.vortexmusic.constants.EnableSimpMusicKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get
import com.ikyvortex.simpmusic.SimpMusicLyrics

object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = SimpMusicLyrics.getLyrics(id, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(id, duration, callback)
    }
}
