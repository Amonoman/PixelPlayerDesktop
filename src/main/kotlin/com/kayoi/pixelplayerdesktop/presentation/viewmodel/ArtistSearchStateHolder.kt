package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.collectArtistNames
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
class ArtistSearchStateHolder(
    private val artistsFlow: StateFlow<List<Artist>>,
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

    private val searchableArtistsFlow: StateFlow<List<SearchableArtist>> = combine(
        artistsFlow,
        songsFlow
    ) { artists, songs ->
        val onlineArtistIds = HashSet<Long>()
        val onlineArtistNames = HashSet<String>()
        val offlineArtistIds = HashSet<Long>()
        val offlineArtistNames = HashSet<String>()

        for (song in songs) {
            val isOnline = song.navidromeId != null || song.jellyfinId != null
            val isOffline = song.navidromeId == null && song.jellyfinId == null

            if (isOnline) {
                onlineArtistIds.add(song.artistId)
                onlineArtistNames.add(song.artist.lowercase().trim())
                for (ref in song.artists) {
                    onlineArtistIds.add(ref.id)
                    onlineArtistNames.add(ref.name.lowercase().trim())
                }
                val names = collectArtistNames(
                    rawArtistName = song.artist,
                    title = song.title,
                    artistDelimiters = DEFAULT_ARTIST_DELIMITERS,
                    wordDelimiters = DEFAULT_WORD_DELIMITERS,
                    extractFromTitle = true
                )
                for (name in names) {
                    onlineArtistNames.add(name.lowercase().trim())
                }
            }

            if (isOffline) {
                offlineArtistIds.add(song.artistId)
                offlineArtistNames.add(song.artist.lowercase().trim())
                for (ref in song.artists) {
                    offlineArtistIds.add(ref.id)
                    offlineArtistNames.add(ref.name.lowercase().trim())
                }
                val names = collectArtistNames(
                    rawArtistName = song.artist,
                    title = song.title,
                    artistDelimiters = DEFAULT_ARTIST_DELIMITERS,
                    wordDelimiters = DEFAULT_WORD_DELIMITERS,
                    extractFromTitle = true
                )
                for (name in names) {
                    offlineArtistNames.add(name.lowercase().trim())
                }
            }
        }

        artists.map { artist ->
            val nameTrimmed = artist.name.lowercase().trim()
            val isOnline = (artist.id != 0L && artist.id in onlineArtistIds) || nameTrimmed in onlineArtistNames
            val isOffline = (artist.id != 0L && artist.id in offlineArtistIds) || nameTrimmed in offlineArtistNames
            SearchableArtist(
                artist = artist,
                normalizedKey = artist.name.lowercase(),
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

    val filteredArtists: StateFlow<List<Artist>> = combine(
        debouncedQueryFlow,
        searchableArtistsFlow,
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
            storageFiltered.map { it.artist }
        } else {
            val queryLower = params.query.lowercase()
            val result = ArrayList<Artist>(storageFiltered.size.coerceAtMost(512))
            for (i in storageFiltered.indices) {
                if ((i and 127) == 0) {
                    currentCoroutineContext().ensureActive()
                }
                val item = storageFiltered[i]
                if (item.normalizedKey.contains(queryLower)) {
                    result.add(item.artist)
                }
            }
            result
        }
    }.flowOn(dispatcher)
    .stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = artistsFlow.value
    )

    private data class SearchParameters(
        val query: String,
        val searchable: List<SearchableArtist>,
        val storageFilter: StorageFilter
    )

    internal class SearchableArtist(
        val artist: Artist,
        val normalizedKey: String,
        val isOnline: Boolean,
        val isOffline: Boolean,
        val isAll: Boolean
    )
}
