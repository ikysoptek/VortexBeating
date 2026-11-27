package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.vortexmusic.constants.EnablePaxsenixKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get
import com.ikyvortex.paxsenix.Paxsenix
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnablePaxsenixKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        Timber.d("PaxsenixLyricsProvider: fetching lyrics for '$title' by '$artist'")
        return Paxsenix.getLyrics(title, artist, duration, album)
    }
}
