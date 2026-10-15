package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.BatchMetadataResult
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.metadata.MetadataEditResult
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import java.io.File

class PlayerViewModel(
    val playbackStateHolder: PlaybackStateHolder,
    val libraryStateHolder: LibraryStateHolder,
    val themeStateHolder: ThemeStateHolder
) {

    val playerInfo = playbackStateHolder.playerInfo
    val currentPositionMs = playbackStateHolder.currentPositionMs
    val currentSong = playbackStateHolder.currentSong
    val queue = playbackStateHolder.queue
    val playbackState = playbackStateHolder.playbackState
    val lyricsSearchUiState = playbackStateHolder.lyricsSearchUiState

    fun searchLyrics(query: String) = playbackStateHolder.searchLyrics(query)
    fun applyLyrics(lyrics: com.kayoi.pixelplayerdesktop.data.model.Lyrics) = playbackStateHolder.applyLyrics(lyrics)
    fun importLyricsFromFile(file: java.io.File) = playbackStateHolder.importLyricsFromFile(file)
    fun resetLyricsSearchState() = playbackStateHolder.resetLyricsSearchState()
    fun resetCurrentSongLyrics() = playbackStateHolder.resetCurrentSongLyrics()
    fun saveLyricsBesideCurrentSong(preferSynced: Boolean = true) = playbackStateHolder.saveLyricsBesideCurrentSong(preferSynced)
    fun exportLyricsToFile(targetFile: java.io.File, preferSynced: Boolean = true) = playbackStateHolder.exportLyricsToFile(targetFile, preferSynced)
    fun setLyricsSyncOffset(offsetMs: Int) = playbackStateHolder.setLyricsSyncOffset(offsetMs)

    fun playSong(song: Song) = playbackStateHolder.playSong(song)
    fun playSongs(songs: List<Song>, startIndex: Int = 0) = playbackStateHolder.playSongs(songs, startIndex)
    fun shuffleSongs(songs: List<Song>) = playbackStateHolder.shuffleSongs(songs)
    fun togglePlayPause() = playbackStateHolder.togglePlayPause()
    fun playNext() = playbackStateHolder.playNext()
    fun playPrevious() = playbackStateHolder.playPrevious()
    fun seekTo(positionMs: Long) = playbackStateHolder.seekTo(positionMs)
    fun setVolume(vol: Float) = playbackStateHolder.setVolume(vol)
    fun toggleMute() = playbackStateHolder.toggleMute()
    fun toggleShuffle() = playbackStateHolder.toggleShuffle()
    fun cycleRepeatMode() = playbackStateHolder.cycleRepeatMode()
    fun toggleFavoriteCurrentSong() = playbackStateHolder.toggleFavoriteCurrentSong()
    fun stopPlayback() = playbackStateHolder.stopPlayback()
    fun undoStopPlayback() = playbackStateHolder.undoStopPlayback()
    fun removeFromQueue(index: Int) = playbackStateHolder.removeFromQueue(index)
    fun removeSongFromQueue(songId: String) = playbackStateHolder.removeSongFromQueue(songId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackStateHolder.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = playbackStateHolder.clearQueue()
    fun playSongAtIndex(index: Int) = playbackStateHolder.playSongAtIndex(index)
    fun addSongToQueue(song: Song) = playbackStateHolder.addSongToQueue(song)
    fun playSongNext(song: Song) = playbackStateHolder.playSongNext(song)

    fun shuffleRandomAlbum(availableAlbums: List<Album>? = null) {
        val targetList = availableAlbums ?: albums.value
        if (targetList.isNotEmpty()) {
            val randomAlbum = targetList.random()
            val albumSongs = songs.value.filter { it.album == randomAlbum.title }
            if (albumSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(albumSongs)
            }
        }
    }

    fun shuffleRandomArtist(availableArtists: List<Artist>? = null) {
        val targetList = availableArtists ?: artists.value
        if (targetList.isNotEmpty()) {
            val randomArtist = targetList.random()
            val artistSongs = songs.value.filter { song ->
                song.artistId == randomArtist.id ||
                song.artists.any { it.id == randomArtist.id || it.name.equals(randomArtist.name, ignoreCase = true) } ||
                song.artist.contains(randomArtist.name, ignoreCase = true)
            }
            if (artistSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(artistSongs)
            }
        }
    }

    val songs = libraryStateHolder.songs
    val albums = libraryStateHolder.albums
    val artists = libraryStateHolder.artists
    val playlists = libraryStateHolder.playlists
    val favoriteSongs = libraryStateHolder.favoriteSongs
    val recentlyPlayedSongs = libraryStateHolder.recentlyPlayedSongs
    val scanProgress = libraryStateHolder.scanProgress
    val searchQuery = libraryStateHolder.searchQuery
    val searchResults = libraryStateHolder.searchResults
    val fetchDeezerArtistImages = libraryStateHolder.fetchDeezerArtistImages
    val musicDirectories = libraryStateHolder.musicDirectories
    val excludedDirectories = libraryStateHolder.excludedDirectories
    val folderSortOption = libraryStateHolder.folderSortOption
    val artistDelimiters = libraryStateHolder.artistDelimiters
    val wordDelimiters = libraryStateHolder.wordDelimiters
    val extractArtistFromTitle = libraryStateHolder.extractArtistFromTitle
    val rescanRequired = libraryStateHolder.rescanRequired
    val isAlbumsListView = libraryStateHolder.isAlbumsListView
    val isArtistsListView = libraryStateHolder.isArtistsListView
    val isFoldersPlaylistView = libraryStateHolder.isFoldersPlaylistView
    val minSongDurationMs = libraryStateHolder.minSongDurationMs
    val minTracksPerAlbum = libraryStateHolder.minTracksPerAlbum
    val albumArtCacheLimitMb = libraryStateHolder.albumArtCacheLimitMb
    val autoScanLrcFiles = libraryStateHolder.autoScanLrcFiles
    val lyricsSourcePreference = libraryStateHolder.lyricsSourcePreference

    fun setMinSongDuration(durationMs: Int) = libraryStateHolder.setMinSongDuration(durationMs)
    fun setMinTracksPerAlbum(minTracks: Int) = libraryStateHolder.setMinTracksPerAlbum(minTracks)
    fun setAlbumArtCacheLimitMb(limitMb: Int) = libraryStateHolder.setAlbumArtCacheLimitMb(limitMb)
    fun setAutoScanLrcFiles(enabled: Boolean) = libraryStateHolder.setAutoScanLrcFiles(enabled)
    fun setLyricsSourcePreference(preference: LyricsSourcePreference) = libraryStateHolder.setLyricsSourcePreference(preference)
    fun resetAllLyrics() = libraryStateHolder.resetAllLyrics()

    fun setSongSortOption(sort: SortOption) = libraryStateHolder.setSongSortOption(sort)
    fun setAlbumSortOption(sort: SortOption) = libraryStateHolder.setAlbumSortOption(sort)
    fun setArtistSortOption(sort: SortOption) = libraryStateHolder.setArtistSortOption(sort)
    fun setPlaylistSortOption(sort: SortOption) = libraryStateHolder.setPlaylistSortOption(sort)
    fun setFolderSortOption(sort: SortOption) = libraryStateHolder.setFolderSortOption(sort)
    fun setAlbumsListView(isListView: Boolean) = libraryStateHolder.setAlbumsListView(isListView)
    fun setArtistsListView(isListView: Boolean) = libraryStateHolder.setArtistsListView(isListView)
    fun setFoldersPlaylistView(isPlaylistView: Boolean) = libraryStateHolder.setFoldersPlaylistView(isPlaylistView)
    fun setFetchDeezerArtistImages(enabled: Boolean) = libraryStateHolder.setFetchDeezerArtistImages(enabled)
    fun toggleFetchDeezerArtistImages() = libraryStateHolder.toggleFetchDeezerArtistImages()
    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) = libraryStateHolder.scanLibrary(directories, forceRescan)
    fun rescanLibrary() = libraryStateHolder.scanLibrary(forceRescan = true)
    fun rebuildDatabase() = libraryStateHolder.rebuildDatabase()
    fun addMusicDirectory(path: String) = libraryStateHolder.addMusicDirectory(path)
    fun removeMusicDirectory(path: String) = libraryStateHolder.removeMusicDirectory(path)
    fun addExcludedDirectory(path: String) = libraryStateHolder.addExcludedDirectory(path)
    fun removeExcludedDirectory(path: String) = libraryStateHolder.removeExcludedDirectory(path)
    fun addArtistDelimiter(delimiter: String) = libraryStateHolder.addArtistDelimiter(delimiter)
    fun removeArtistDelimiter(delimiter: String) = libraryStateHolder.removeArtistDelimiter(delimiter)
    fun resetArtistDelimiters() = libraryStateHolder.resetArtistDelimiters()
    fun addWordDelimiter(delimiter: String) = libraryStateHolder.addWordDelimiter(delimiter)
    fun removeWordDelimiter(delimiter: String) = libraryStateHolder.removeWordDelimiter(delimiter)
    fun resetWordDelimiters() = libraryStateHolder.resetWordDelimiters()
    fun setExtractArtistFromTitle(enabled: Boolean) = libraryStateHolder.setExtractArtistFromTitle(enabled)
    fun onSearchQueryChanged(query: String) = libraryStateHolder.onSearchQueryChanged(query)
    fun createPlaylist(name: String, songIds: List<String> = emptyList()) = libraryStateHolder.createPlaylist(name, songIds)
    fun deletePlaylist(id: String) = libraryStateHolder.deletePlaylist(id)
    fun deleteSong(song: Song) {
        playbackStateHolder.removeSongFromQueue(song.id)
        libraryStateHolder.deleteSong(song)
    }
    fun renamePlaylist(id: String, newName: String) = libraryStateHolder.renamePlaylist(id, newName)
    fun updatePlaylist(playlist: Playlist) = libraryStateHolder.updatePlaylist(playlist)
    fun addSongToPlaylist(playlistId: String, songId: String) = libraryStateHolder.addSongToPlaylist(playlistId, songId)
    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) = libraryStateHolder.addSongsToPlaylist(playlistId, songIds)
    fun removeSongFromPlaylist(playlistId: String, songId: String) = libraryStateHolder.removeSongFromPlaylist(playlistId, songId)
    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) = libraryStateHolder.reorderPlaylistSongs(playlistId, songIds)
    fun toggleFavorite(song: Song) = playbackStateHolder.toggleFavorite(song)

    val isDarkMode = themeStateHolder.isDarkMode
    val isDynamicColorEnabled = themeStateHolder.isDynamicColorEnabled
    val albumArtPaletteStyle = themeStateHolder.albumArtPaletteStyle
    val albumArtColorAccuracy = themeStateHolder.albumArtColorAccuracy
    val showPlayerFileInfo = themeStateHolder.showPlayerFileInfo
    val navBarCompactMode = themeStateHolder.navBarCompactMode
    val defaultTab = themeStateHolder.defaultTab
    fun toggleDarkMode() = themeStateHolder.toggleDarkMode()
    fun toggleDynamicColor() = themeStateHolder.toggleDynamicColor()
    fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) = themeStateHolder.setAlbumArtPaletteStyle(style)
    fun setAlbumArtColorAccuracy(accuracy: Int) = themeStateHolder.setAlbumArtColorAccuracy(accuracy)
    fun setShowPlayerFileInfo(show: Boolean) = themeStateHolder.setShowPlayerFileInfo(show)
    fun toggleShowPlayerFileInfo() = themeStateHolder.toggleShowPlayerFileInfo()
    fun setNavBarCompactMode(enabled: Boolean) = themeStateHolder.setNavBarCompactMode(enabled)
    fun toggleNavBarCompactMode() = themeStateHolder.toggleNavBarCompactMode()
    fun setDefaultTab(tab: String) = themeStateHolder.setDefaultTab(tab)
    val lyricsAlignment = themeStateHolder.lyricsAlignment
    fun setLyricsAlignment(alignment: String) = themeStateHolder.setLyricsAlignment(alignment)
    val showLyricsRomanization = themeStateHolder.showLyricsRomanization
    fun setShowLyricsRomanization(enabled: Boolean) = themeStateHolder.setShowLyricsRomanization(enabled)

    suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata
    ): MetadataEditResult {
        val result = libraryStateHolder.updateSongMetadata(song, metadata)
        if (result.success && result.updatedSong != null) {
            playbackStateHolder.onSongMetadataUpdated(result.updatedSong)
        }
        return result
    }

    suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails> {
        return libraryStateHolder.readSongMetadataDetails(file)
    }

    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult {
        val result = libraryStateHolder.batchUpdateMetadata(songs, update, onProgress)
        if (result.updatedSongs.isNotEmpty()) {
            playbackStateHolder.onSongsMetadataUpdated(result.updatedSongs)
        }
        return result
    }

    suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata> {
        return libraryStateHolder.searchOnlineMetadata(title, artist)
    }

    suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata> {
        return libraryStateHolder.searchOnlineMetadataByQuery(query)
    }

    suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata> {
        return libraryStateHolder.searchOnlineReleases(query)
    }

    suspend fun fetchOnlineCoverArt(releaseId: String): ByteArray? {
        return libraryStateHolder.fetchOnlineCoverArt(releaseId)
    }
}
