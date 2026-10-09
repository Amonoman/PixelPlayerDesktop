package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.database.*
import com.kayoi.pixelplayerdesktop.data.model.*
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepositoryImpl(
    private val database: PixelPlayerDatabase,
    private val musicDao: MusicDao,
    private val favoritesDao: FavoritesDao,
    private val playlistDao: LocalPlaylistDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MusicRepository {

    override fun getAllSongsFlow(sortOption: SortOption): Flow<List<Song>> = flow {
        emit(musicDao.getAllSongs(sortOption))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs" || table == "favorites") {
                emit(musicDao.getAllSongs(sortOption))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getAllSongs(sortOption: SortOption): List<Song> {
        return musicDao.getAllSongs(sortOption)
    }

    override suspend fun getSongById(id: String): Song? {
        return musicDao.getSongById(id)
    }

    override suspend fun getSongsByIds(ids: List<String>): List<Song> {
        return musicDao.getSongsByIds(ids)
    }

    override suspend fun getSongByPath(path: String): Song? {
        return musicDao.getSongByPath(path)
    }

    override fun getAllAlbumsFlow(sortOption: SortOption): Flow<List<Album>> = flow {
        emit(musicDao.getAllAlbums(sortOption))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs") {
                emit(musicDao.getAllAlbums(sortOption))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getAlbumById(id: Long): Album? {
        return musicDao.getAllAlbums().find { it.id == id }
    }

    override suspend fun getSongsByAlbum(albumId: Long): List<Song> {
        return musicDao.getSongsByAlbum(albumId)
    }

    override fun getAllArtistsFlow(sortOption: SortOption): Flow<List<Artist>> = flow {
        emit(musicDao.getAllArtists(sortOption))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs") {
                emit(musicDao.getAllArtists(sortOption))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getArtistById(id: Long): Artist? {
        return musicDao.getAllArtists().find { it.id == id }
    }

    override suspend fun getSongsByArtist(artistId: Long): List<Song> {
        return musicDao.getSongsByArtist(artistId)
    }

    override fun getAllGenresFlow(): Flow<List<Genre>> = flow {
        emit(musicDao.getAllGenres())
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs") {
                emit(musicDao.getAllGenres())
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getSongsByGenre(genreName: String): List<Song> {
        return musicDao.getSongsByGenre(genreName)
    }

    override fun getAllPlaylistsFlow(sortOption: SortOption): Flow<List<Playlist>> = flow {
        emit(playlistDao.getAllPlaylists(sortOption))
        database.tableInvalidationFlow.collect { table ->
            if (table == "playlists") {
                emit(playlistDao.getAllPlaylists(sortOption))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getPlaylistById(id: String): Playlist? {
        return playlistDao.getPlaylistById(id)
    }

    override suspend fun createPlaylist(name: String, songIds: List<String>): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            songIds = songIds,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)
        return playlist
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.insertPlaylist(playlist.copy(lastModified = System.currentTimeMillis()))
    }

    override suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylist(id)
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        addSongsToPlaylist(playlistId, listOf(songId))
    }

    override suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentSet = playlist.songIds.toSet()
        val newToAdd = songIds.filter { !currentSet.contains(it) }
        if (newToAdd.isNotEmpty()) {
            val updated = playlist.copy(
                songIds = playlist.songIds + newToAdd,
                lastModified = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(updated)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val updated = playlist.copy(
            songIds = playlist.songIds.filter { it != songId },
            lastModified = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(updated)
    }

    override suspend fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val updated = playlist.copy(
            songIds = songIds,
            lastModified = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(updated)
    }

    override fun getFavoriteSongsFlow(): Flow<List<Song>> = flow {
        val favIds = favoritesDao.getFavoriteSongIds()
        emit(musicDao.getSongsByIds(favIds))
        database.tableInvalidationFlow.collect { table ->
            if (table == "favorites" || table == "songs") {
                val currentFavIds = favoritesDao.getFavoriteSongIds()
                emit(musicDao.getSongsByIds(currentFavIds))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun isFavorite(songId: String): Boolean {
        return favoritesDao.isFavorite(songId)
    }

    override suspend fun toggleFavorite(songId: String): Boolean {
        return favoritesDao.toggleFavorite(songId)
    }

    override suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoritesDao.addFavorite(songId)
        } else {
            favoritesDao.removeFavorite(songId)
        }
    }

    override fun getRecentlyPlayedSongsFlow(limit: Int): Flow<List<Song>> = flow {
        emit(musicDao.getRecentlyPlayedSongs(limit))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs") {
                emit(musicDao.getRecentlyPlayedSongs(limit))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun recordSongPlayed(songId: String) {
        musicDao.recordSongPlayed(songId)
    }

    override suspend fun search(query: String, filter: SearchFilterType): List<SearchResultItem> {
        return musicDao.search(query, filter)
    }

    override fun getSearchHistoryFlow(): Flow<List<SearchHistoryItem>> = flow {
        emit(searchHistoryDao.getRecentSearches())
        database.tableInvalidationFlow.collect { table ->
            if (table == "search_history") {
                emit(searchHistoryDao.getRecentSearches())
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun addSearchHistory(query: String) {
        searchHistoryDao.insertSearch(query)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearHistory()
    }

    override suspend fun deleteSearchHistoryItem(id: Long) {
        searchHistoryDao.deleteSearch(id)
    }
}
