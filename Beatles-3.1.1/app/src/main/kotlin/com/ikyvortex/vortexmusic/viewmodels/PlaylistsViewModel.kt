/**
 * NanzBeatles Project (C) 2026
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.ikyvortex.vortexmusic.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikyvortex.vortexmusic.constants.AddToPlaylistSortDescendingKey
import com.ikyvortex.vortexmusic.constants.AddToPlaylistSortTypeKey
import com.ikyvortex.vortexmusic.constants.PlaylistSortType
import com.ikyvortex.vortexmusic.db.MusicDatabase
import com.ikyvortex.vortexmusic.extensions.toEnum
import com.ikyvortex.vortexmusic.utils.SyncUtils
import com.ikyvortex.vortexmusic.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allPlaylists =
        context.dataStore.data
            .map {
                it[AddToPlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[AddToPlaylistSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.playlists(sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Suspend function that waits for sync to complete
    suspend fun sync() {
        syncUtils.syncSavedPlaylists()
    }
}
