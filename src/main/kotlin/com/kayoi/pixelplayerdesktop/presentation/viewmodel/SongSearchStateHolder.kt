package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SongSearchStateHolder(
    private val songsFlow: StateFlow<List<Song>>,
    private val storageFilterFlow: StateFlow<StorageFilter> = MutableStateFlow(StorageFilter.ALL),
    searchQueryFlow: Flow<String>? = null,
    coroutineScope: CoroutineScope,
    debounceMs: Long = 150L,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val querySource: Flow<String> = searchQueryFlow ?: _searchQuery

    private val searchableSongsFlow: StateFlow<List<SearchableSong>> = songsFlow.map { list ->
        list.map { song ->
            SearchableSong(
                song = song,
                normalizedKey = normalizeSearchKey(song.title, song.artist, song.album),
                isOnline = song.navidromeId != null || song.jellyfinId != null
            )
        }
    }.flowOn(dispatcher)
    .stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = songsFlow.value.map { song ->
            SearchableSong(
                song = song,
                normalizedKey = normalizeSearchKey(song.title, song.artist, song.album),
                isOnline = song.navidromeId != null || song.jellyfinId != null
            )
        }
    )

    private val debouncedQueryFlow: Flow<String> = querySource
        .debounce { query -> if (query.isEmpty()) 0L else debounceMs }
        .distinctUntilChanged()

    val filteredSongs: StateFlow<List<Song>> = combine(
        debouncedQueryFlow,
        searchableSongsFlow,
        storageFilterFlow
    ) { query, searchable, storageFilter ->
        SearchParameters(query, searchable, storageFilter)
    }.mapLatest { params ->
        val storageFiltered = when (params.storageFilter) {
            StorageFilter.ALL -> params.searchable
            StorageFilter.ONLINE -> params.searchable.filter { it.isOnline }
            StorageFilter.OFFLINE -> params.searchable.filter { !it.isOnline }
        }
        if (params.query.isBlank()) {
            storageFiltered.map { it.song }
        } else {
            val queryLower = params.query.lowercase()
            val result = ArrayList<Song>(storageFiltered.size.coerceAtMost(1024))
            for (i in storageFiltered.indices) {
                if ((i and 127) == 0) {
                    currentCoroutineContext().ensureActive()
                }
                val item = storageFiltered[i]
                if (item.normalizedKey.contains(queryLower)) {
                    result.add(item.song)
                }
            }
            result
        }
    }.flowOn(dispatcher)
    .stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = filterInitial(songsFlow.value, storageFilterFlow.value, _searchQuery.value)
    )

    private data class SearchParameters(
        val query: String,
        val searchable: List<SearchableSong>,
        val storageFilter: StorageFilter
    )

    internal class SearchableSong(
        val song: Song,
        val normalizedKey: String,
        val isOnline: Boolean
    )

    companion object {
        fun normalizeSearchKey(title: String, artist: String, album: String): String {
            return "$title\u0000$artist\u0000$album".lowercase()
        }

        private fun filterInitial(
            songs: List<Song>,
            storageFilter: StorageFilter,
            query: String
        ): List<Song> {
            val base = when (storageFilter) {
                StorageFilter.ALL -> songs
                StorageFilter.ONLINE -> songs.filter { it.navidromeId != null || it.jellyfinId != null }
                StorageFilter.OFFLINE -> songs.filter { it.navidromeId == null && it.jellyfinId == null }
            }
            if (query.isBlank()) return base
            val queryLower = query.lowercase()
            return base.filter {
                normalizeSearchKey(it.title, it.artist, it.album).contains(queryLower)
            }
        }
    }
}
