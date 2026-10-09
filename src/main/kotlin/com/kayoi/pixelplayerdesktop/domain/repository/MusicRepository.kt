package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.model.*
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongsFlow(sortOption: SortOption = SortOption.SongTitleAZ): Flow<List<Song>>
    suspend fun getAllSongs(sortOption: SortOption = SortOption.SongTitleAZ): List<Song>
    suspend fun getSongById(id: String): Song?
    suspend fun getSongsByIds(ids: List<String>): List<Song>
    suspend fun getSongByPath(path: String): Song?

    fun getAllAlbumsFlow(sortOption: SortOption = SortOption.AlbumTitleAZ): Flow<List<Album>>
    suspend fun getAlbumById(id: Long): Album?
    suspend fun getSongsByAlbum(albumId: Long): List<Song>

    fun getAllArtistsFlow(sortOption: SortOption = SortOption.ArtistNameAZ): Flow<List<Artist>>
    suspend fun getArtistById(id: Long): Artist?
    suspend fun getSongsByArtist(artistId: Long): List<Song>

    fun getAllGenresFlow(): Flow<List<Genre>>
    suspend fun getSongsByGenre(genreName: String): List<Song>

    fun getAllPlaylistsFlow(sortOption: SortOption = SortOption.PlaylistNameAZ): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: String): Playlist?
    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): Playlist
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(id: String)
    suspend fun addSongToPlaylist(playlistId: String, songId: String)
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    suspend fun reorderPlaylistSongs(playlistId: String, songIds: List<String>)

    fun getFavoriteSongsFlow(): Flow<List<Song>>
    suspend fun isFavorite(songId: String): Boolean
    suspend fun toggleFavorite(songId: String): Boolean
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    fun getRecentlyPlayedSongsFlow(limit: Int = 50): Flow<List<Song>>
    suspend fun recordSongPlayed(songId: String)

    suspend fun search(query: String, filter: SearchFilterType = SearchFilterType.ALL): List<SearchResultItem>
    fun getSearchHistoryFlow(): Flow<List<SearchHistoryItem>>
    suspend fun addSearchHistory(query: String)
    suspend fun clearSearchHistory()
    suspend fun deleteSearchHistoryItem(id: Long)
}
