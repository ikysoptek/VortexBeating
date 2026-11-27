/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.lyrics

import android.content.Context
import com.ikyvortex.kugou.KuGou
import com.ikyvortex.vortexmusic.constants.EnableKugouKey
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "KuGou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> =
        KuGou.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, album, callback)
    }
}
