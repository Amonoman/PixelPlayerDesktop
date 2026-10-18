package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Album
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AlbumSearchStateHolder(
    private val albumsFlow: StateFlow<List<Album>>,
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

    private val searchableAlbumsFlow: StateFlow<List<SearchableAlbum>> = combine(
        albumsFlow,
        songsFlow
    ) { albums, songs ->
        val onlineAlbumIds = HashSet<Long>()
        val onlineAlbumTitles = HashSet<String>()
        val offlineAlbumIds = HashSet<Long>()
        val offlineAlbumTitles = HashSet<String>()

        for (song in songs) {
            val isOnline = song.navidromeId != null || song.jellyfinId != null
            val isOffline = song.navidromeId == null && song.jellyfinId == null
            if (isOnline) {
                if (song.albumId != 0L) onlineAlbumIds.add(song.albumId)
                if (song.album.isNotBlank()) onlineAlbumTitles.add(song.album.lowercase().trim())
            }
            if (isOffline) {
                if (song.albumId != 0L) offlineAlbumIds.add(song.albumId)
                if (song.album.isNotBlank()) offlineAlbumTitles.add(song.album.lowercase().trim())
            }
        }

        albums.map { album ->
            val titleTrimmed = album.title.lowercase().trim()
            val isOnline = (album.id != 0L && album.id in onlineAlbumIds) || titleTrimmed in onlineAlbumTitles
            val isOffline = (album.id != 0L && album.id in offlineAlbumIds) || titleTrimmed in offlineAlbumTitles
            SearchableAlbum(
                album = album,
                normalizedKey = normalizeSearchKey(album.title, album.artist),
                isOnline = isOnline,
                isOffline = isOffline,
                isAll = isOnline || isOffline
            )
        }
    }.flowOn(dispatcher)
    .stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val debouncedQueryFlow: Flow<String> = querySource
        .debounce { query -> if (query.isEmpty()) 0L else debounceMs }
        .distinctUntilChanged()

    val filteredAlbums: StateFlow<List<Album>> = combine(
        debouncedQueryFlow,
        searchableAlbumsFlow,
        storageFilterFlow
    ) { query, searchable, storageFilter ->
        SearchParameters(query, searchable, storageFilter)
    }.mapLatest { params ->
        val storageFiltered = when (params.storageFilter) {
            StorageFilter.ALL -> params.searchable.filter { it.isAll }
            StorageFilter.ONLINE -> params.searchable.filter { it.isOnline }
            StorageFilter.OFFLINE -> params.searchable.filter { it.isOffline }
        }
        if (params.query.isBlank()) {
            storageFiltered.map { it.album }
        } else {
            val queryLower = params.query.lowercase()
            val result = ArrayList<Album>(storageFiltered.size.coerceAtMost(512))
            for (i in storageFiltered.indices) {
                if ((i and 127) == 0) {
                    currentCoroutineContext().ensureActive()
                }
                val item = storageFiltered[i]
                if (item.normalizedKey.contains(queryLower)) {
                    result.add(item.album)
                }
            }
            result
        }
    }.flowOn(dispatcher)
    .stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = albumsFlow.value
    )

    private data class SearchParameters(
        val query: String,
        val searchable: List<SearchableAlbum>,
        val storageFilter: StorageFilter
    )

    internal class SearchableAlbum(
        val album: Album,
        val normalizedKey: String,
        val isOnline: Boolean,
        val isOffline: Boolean,
        val isAll: Boolean
    )

    companion object {
        fun normalizeSearchKey(title: String, artist: String): String {
            return "$title\u0000$artist".lowercase()
        }
    }
}
