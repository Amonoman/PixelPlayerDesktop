package com.kayoi.pixelplayerdesktop.presentation.viewmodel

class PlayerViewModel(
    val playbackStateHolder: PlaybackStateHolder,
    val libraryStateHolder: LibraryStateHolder,
    val themeStateHolder: ThemeStateHolder
) {

    val playerInfo = playbackStateHolder.playerInfo
    val currentSong = playbackStateHolder.currentSong
    val queue = playbackStateHolder.queue
    val playbackState = playbackStateHolder.playbackState

    fun playSong(song: com.kayoi.pixelplayerdesktop.data.model.Song) = playbackStateHolder.playSong(song)
    fun playSongs(songs: List<com.kayoi.pixelplayerdesktop.data.model.Song>, startIndex: Int = 0) = playbackStateHolder.playSongs(songs, startIndex)
    fun shuffleSongs(songs: List<com.kayoi.pixelplayerdesktop.data.model.Song>) = playbackStateHolder.shuffleSongs(songs)
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
    fun addSongToQueue(song: com.kayoi.pixelplayerdesktop.data.model.Song) = playbackStateHolder.addSongToQueue(song)
    fun playSongNext(song: com.kayoi.pixelplayerdesktop.data.model.Song) = playbackStateHolder.playSongNext(song)

    fun shuffleRandomAlbum(availableAlbums: List<com.kayoi.pixelplayerdesktop.data.model.Album>? = null) {
        val targetList = availableAlbums ?: albums.value
        if (targetList.isNotEmpty()) {
            val randomAlbum = targetList.random()
            val albumSongs = songs.value.filter { it.album == randomAlbum.title }
            if (albumSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(albumSongs)
            }
        }
    }

    fun shuffleRandomArtist(availableArtists: List<com.kayoi.pixelplayerdesktop.data.model.Artist>? = null) {
        val targetList = availableArtists ?: artists.value
        if (targetList.isNotEmpty()) {
            val randomArtist = targetList.random()
            val artistSongs = songs.value.filter { it.artist.contains(randomArtist.name, ignoreCase = true) }
            if (artistSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(artistSongs)
            }
        }
    }

    val selectedTab = libraryStateHolder.selectedTab
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
    val folderSortOption = libraryStateHolder.folderSortOption

    fun selectTab(tab: com.kayoi.pixelplayerdesktop.data.model.LibraryTabId) = libraryStateHolder.selectTab(tab)
    fun setSongSortOption(sort: com.kayoi.pixelplayerdesktop.data.model.SortOption) = libraryStateHolder.setSongSortOption(sort)
    fun setAlbumSortOption(sort: com.kayoi.pixelplayerdesktop.data.model.SortOption) = libraryStateHolder.setAlbumSortOption(sort)
    fun setArtistSortOption(sort: com.kayoi.pixelplayerdesktop.data.model.SortOption) = libraryStateHolder.setArtistSortOption(sort)
    fun setPlaylistSortOption(sort: com.kayoi.pixelplayerdesktop.data.model.SortOption) = libraryStateHolder.setPlaylistSortOption(sort)
    fun setFolderSortOption(sort: com.kayoi.pixelplayerdesktop.data.model.SortOption) = libraryStateHolder.setFolderSortOption(sort)
    fun setFetchDeezerArtistImages(enabled: Boolean) = libraryStateHolder.setFetchDeezerArtistImages(enabled)
    fun toggleFetchDeezerArtistImages() = libraryStateHolder.toggleFetchDeezerArtistImages()
    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) = libraryStateHolder.scanLibrary(directories, forceRescan)
    fun rescanLibrary() = libraryStateHolder.scanLibrary(forceRescan = true)
    fun onSearchQueryChanged(query: String) = libraryStateHolder.onSearchQueryChanged(query)
    fun createPlaylist(name: String, songIds: List<String> = emptyList()) = libraryStateHolder.createPlaylist(name, songIds)
    fun deletePlaylist(id: String) = libraryStateHolder.deletePlaylist(id)
    fun renamePlaylist(id: String, newName: String) = libraryStateHolder.renamePlaylist(id, newName)
    fun updatePlaylist(playlist: com.kayoi.pixelplayerdesktop.data.model.Playlist) = libraryStateHolder.updatePlaylist(playlist)
    fun addSongToPlaylist(playlistId: String, songId: String) = libraryStateHolder.addSongToPlaylist(playlistId, songId)
    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) = libraryStateHolder.addSongsToPlaylist(playlistId, songIds)
    fun removeSongFromPlaylist(playlistId: String, songId: String) = libraryStateHolder.removeSongFromPlaylist(playlistId, songId)
    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) = libraryStateHolder.reorderPlaylistSongs(playlistId, songIds)
    fun toggleFavorite(song: com.kayoi.pixelplayerdesktop.data.model.Song) = playbackStateHolder.toggleFavorite(song)

    val isDarkMode = themeStateHolder.isDarkMode
    val isDynamicColorEnabled = themeStateHolder.isDynamicColorEnabled
    val albumArtPaletteStyle = themeStateHolder.albumArtPaletteStyle
    val albumArtColorAccuracy = themeStateHolder.albumArtColorAccuracy
    fun toggleDarkMode() = themeStateHolder.toggleDarkMode()
    fun toggleDynamicColor() = themeStateHolder.toggleDynamicColor()
    fun setAlbumArtPaletteStyle(style: com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle) = themeStateHolder.setAlbumArtPaletteStyle(style)
    fun setAlbumArtColorAccuracy(accuracy: Int) = themeStateHolder.setAlbumArtColorAccuracy(accuracy)
}
