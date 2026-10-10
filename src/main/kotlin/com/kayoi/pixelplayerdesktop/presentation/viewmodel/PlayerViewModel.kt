package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle

class PlayerViewModel(
    val playbackStateHolder: PlaybackStateHolder,
    val libraryStateHolder: LibraryStateHolder,
    val themeStateHolder: ThemeStateHolder
) {

    val playerInfo = playbackStateHolder.playerInfo
    val currentSong = playbackStateHolder.currentSong
    val queue = playbackStateHolder.queue
    val playbackState = playbackStateHolder.playbackState

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
            val artistSongs = songs.value.filter { it.artist.contains(randomArtist.name, ignoreCase = true) }
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

    fun setSongSortOption(sort: SortOption) = libraryStateHolder.setSongSortOption(sort)
    fun setAlbumSortOption(sort: SortOption) = libraryStateHolder.setAlbumSortOption(sort)
    fun setArtistSortOption(sort: SortOption) = libraryStateHolder.setArtistSortOption(sort)
    fun setPlaylistSortOption(sort: SortOption) = libraryStateHolder.setPlaylistSortOption(sort)
    fun setFolderSortOption(sort: SortOption) = libraryStateHolder.setFolderSortOption(sort)
    fun setFetchDeezerArtistImages(enabled: Boolean) = libraryStateHolder.setFetchDeezerArtistImages(enabled)
    fun toggleFetchDeezerArtistImages() = libraryStateHolder.toggleFetchDeezerArtistImages()
    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) = libraryStateHolder.scanLibrary(directories, forceRescan)
    fun rescanLibrary() = libraryStateHolder.scanLibrary(forceRescan = true)
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
    fun toggleDarkMode() = themeStateHolder.toggleDarkMode()
    fun toggleDynamicColor() = themeStateHolder.toggleDynamicColor()
    fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) = themeStateHolder.setAlbumArtPaletteStyle(style)
    fun setAlbumArtColorAccuracy(accuracy: Int) = themeStateHolder.setAlbumArtColorAccuracy(accuracy)
}
