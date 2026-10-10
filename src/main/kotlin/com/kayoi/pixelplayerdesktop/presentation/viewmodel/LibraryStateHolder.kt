package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolder(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val scanner: DesktopMusicScanner,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val _songSortOption = MutableStateFlow<SortOption>(SortOption.SongTitleAZ)
    val songSortOption: StateFlow<SortOption> = _songSortOption.asStateFlow()

    private val _albumSortOption = MutableStateFlow<SortOption>(SortOption.AlbumTitleAZ)
    val albumSortOption: StateFlow<SortOption> = _albumSortOption.asStateFlow()

    private val _artistSortOption = MutableStateFlow<SortOption>(SortOption.ArtistNameAZ)
    val artistSortOption: StateFlow<SortOption> = _artistSortOption.asStateFlow()

    private val _playlistSortOption = MutableStateFlow<SortOption>(SortOption.PlaylistNameAZ)
    val playlistSortOption: StateFlow<SortOption> = _playlistSortOption.asStateFlow()

    private val _folderSortOption = MutableStateFlow<SortOption>(SortOption.FolderNameAZ)
    val folderSortOption: StateFlow<SortOption> = _folderSortOption.asStateFlow()

    val fetchDeezerArtistImages: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.fetchDeezerArtistImages }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val musicDirectories: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.musicDirectories }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val excludedDirectories: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.excludedDirectories }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val artistDelimiters: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.artistDelimiters }
        .stateIn(coroutineScope, SharingStarted.Eagerly, DEFAULT_ARTIST_DELIMITERS)

    val wordDelimiters: StateFlow<List<String>> = settingsRepository.settingsFlow
        .map { it.wordDelimiters }
        .stateIn(coroutineScope, SharingStarted.Eagerly, DEFAULT_WORD_DELIMITERS)

    val extractArtistFromTitle: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.extractArtistFromTitle }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val rescanRequired: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.artistSettingsRescanRequired }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val songs: StateFlow<List<Song>> = _songSortOption.flatMapLatest { sort ->
        musicRepository.getAllSongsFlow(sort)
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<List<Album>> = _albumSortOption.flatMapLatest { sort ->
        musicRepository.getAllAlbumsFlow(sort)
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val artists: StateFlow<List<Artist>> = _artistSortOption.flatMapLatest { sort ->
        musicRepository.getAllArtistsFlow(sort)
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = _playlistSortOption.flatMapLatest { sort ->
        musicRepository.getAllPlaylistsFlow(sort)
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val favoriteSongs: StateFlow<List<Song>> = musicRepository.getFavoriteSongsFlow()
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = musicRepository.getRecentlyPlayedSongsFlow()
        .stateIn(coroutineScope, SharingStarted.Lazily, emptyList())

    val scanProgress: StateFlow<ScanProgress> = scanner.scanProgress

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    fun setSongSortOption(sort: SortOption) {
        _songSortOption.value = sort
    }

    fun setAlbumSortOption(sort: SortOption) {
        _albumSortOption.value = sort
    }

    fun setArtistSortOption(sort: SortOption) {
        _artistSortOption.value = sort
    }

    fun setPlaylistSortOption(sort: SortOption) {
        _playlistSortOption.value = sort
    }

    fun setFolderSortOption(sort: SortOption) {
        _folderSortOption.value = sort
    }

    fun setFetchDeezerArtistImages(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setFetchDeezerArtistImages(enabled)
        }
    }

    fun toggleFetchDeezerArtistImages() {
        setFetchDeezerArtistImages(!fetchDeezerArtistImages.value)
    }

    fun addMusicDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.addMusicDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = false
            )
        }
    }

    fun removeMusicDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.removeMusicDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = true
            )
        }
    }

    fun addExcludedDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.addExcludedDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = true
            )
        }
    }

    fun removeExcludedDirectory(path: String) {
        coroutineScope.launch {
            settingsRepository.removeExcludedDirectory(path)
            val settings = settingsRepository.getSettings()
            scanner.scanDirectories(
                directories = settings.musicDirectories,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = false
            )
        }
    }

    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) {
        coroutineScope.launch {
            val settings = settingsRepository.getSettings()
            val dirs = directories ?: settings.musicDirectories
            scanner.scanDirectories(
                directories = dirs,
                excludedDirectories = settings.excludedDirectories,
                artistDelimiters = settings.artistDelimiters,
                wordDelimiters = settings.wordDelimiters,
                extractArtistFromTitle = settings.extractArtistFromTitle,
                pruneMissing = true,
                forceRescan = forceRescan
            )
            if (forceRescan) {
                settingsRepository.clearArtistSettingsRescanRequired()
            }
        }
    }

    fun addArtistDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.addArtistDelimiter(delimiter)
        }
    }

    fun removeArtistDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.removeArtistDelimiter(delimiter)
        }
    }

    fun resetArtistDelimiters() {
        coroutineScope.launch {
            settingsRepository.resetArtistDelimiters()
        }
    }

    fun addWordDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.addWordDelimiter(delimiter)
        }
    }

    fun removeWordDelimiter(delimiter: String) {
        coroutineScope.launch {
            settingsRepository.removeWordDelimiter(delimiter)
        }
    }

    fun resetWordDelimiters() {
        coroutineScope.launch {
            settingsRepository.resetWordDelimiters()
        }
    }

    fun setExtractArtistFromTitle(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setExtractArtistFromTitle(enabled)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        coroutineScope.launch {
            if (query.isNotBlank()) {
                val results = musicRepository.search(query)
                _searchResults.value = results
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        coroutineScope.launch {
            musicRepository.createPlaylist(name, songIds)
        }
    }

    fun deletePlaylist(id: String) {
        coroutineScope.launch {
            musicRepository.deletePlaylist(id)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        coroutineScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        coroutineScope.launch {
            musicRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        coroutineScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) {
        coroutineScope.launch {
            musicRepository.reorderPlaylistSongs(playlistId, songIds)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        coroutineScope.launch {
            val existing = musicRepository.getPlaylistById(id) ?: return@launch
            musicRepository.updatePlaylist(existing.copy(name = newName))
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        coroutineScope.launch {
            musicRepository.updatePlaylist(playlist)
        }
    }

    fun toggleFavorite(song: Song) {
        coroutineScope.launch {
            musicRepository.toggleFavorite(song.id)
        }
    }
}
