/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikyvortex.innertube.YouTube
import com.ikyvortex.innertube.models.YTItem
import com.ikyvortex.innertube.models.filterExplicit
import com.ikyvortex.innertube.models.filterVideoSongs
import com.ikyvortex.vortexmusic.constants.HideExplicitKey
import com.ikyvortex.vortexmusic.constants.HideVideoSongsKey
import com.ikyvortex.vortexmusic.db.SearchHistoryRepository
import com.ikyvortex.vortexmusic.db.entities.SearchHistory
import com.ikyvortex.vortexmusic.utils.dataStore
import com.ikyvortex.vortexmusic.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        searchHistoryRepository.getRecentQueries().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                            )
                        }
                    } else {
                        val result = YouTube.searchSuggestions(query).getOrNull()
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                        searchHistoryRepository
                            .getRecentQueries(query)
                            .map { it.take(3) }
                            .map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                    suggestions =
                                    result
                                        ?.queries
                                        ?.filter { suggestionQuery ->
                                            history.none { it.query == suggestionQuery }
                                        }.orEmpty(),
                                    items =
                                    result
                                        ?.recommendedItems
                                        ?.distinctBy { it.id }
                                        ?.filterExplicit(hideExplicit)
                                        ?.filterVideoSongs(hideVideoSongs)
                                        .orEmpty(),
                                )
                            }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }

    fun saveQuery(searchQuery: String) {
        viewModelScope.launch {
            searchHistoryRepository.insertQuery(searchQuery)
        }
    }

    fun deleteHistoryItem(item: SearchHistory) {
        viewModelScope.launch {
            searchHistoryRepository.deleteQuery(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
