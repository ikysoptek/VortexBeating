package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.vortexmusic.constants.EnableMusixmatchKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get
import com.ikyvortex.musixmatch.Musixmatch

object MusixmatchLyricsProvider : LyricsProvider {
    override val name = "Musixmatch"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableMusixmatchKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = Musixmatch.getLyrics(title, artist, duration, album)
}
