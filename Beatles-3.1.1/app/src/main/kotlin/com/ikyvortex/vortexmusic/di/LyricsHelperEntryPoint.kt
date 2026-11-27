/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.di

import com.ikyvortex.vortexmusic.lyrics.LyricsHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsHelperEntryPoint {
    fun lyricsHelper(): LyricsHelper
}
