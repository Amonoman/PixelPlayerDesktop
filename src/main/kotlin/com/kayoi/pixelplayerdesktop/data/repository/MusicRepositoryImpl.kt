package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.BatchMetadataResult
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.DesktopMetadataEditor
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.metadata.MetadataEditResult
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.MusicBrainzClient
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Genre
import com.kayoi.pixelplayerdesktop.data.model.PlaybackEvent
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SearchFilterType
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

class MusicRepositoryImpl(
    private val database: PixelPlayerDatabase,
    private val musicDao: MusicDao,
    private val favoritesDao: FavoritesDao,
    private val playlistDao: LocalPlaylistDao,
    private val metadataEditor: DesktopMetadataEditor? = null,
    private val musicBrainzClient: MusicBrainzClient? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MusicRepository {

    override fun getAllSongsFlow(sortOption: SortOption, minDurationMs: Long): Flow<List<Song>> = flow {
        emit(musicDao.getAllSongs(sortOption, minDurationMs))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs" || table == "favorites") {
                emit(musicDao.getAllSongs(sortOption, minDurationMs))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getAllSongs(sortOption: SortOption, minDurationMs: Long): List<Song> {
        return musicDao.getAllSongs(sortOption, minDurationMs)
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

    override suspend fun deleteSong(song: Song) {
        musicDao.deleteSong(song)
    }

    override suspend fun clearLibrary() {
        musicDao.clearLibrary()
    }

    override fun getAllAlbumsFlow(sortOption: SortOption, minTracks: Int): Flow<List<Album>> = flow {
        emit(musicDao.getAllAlbums(sortOption, minTracks))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs") {
                emit(musicDao.getAllAlbums(sortOption, minTracks))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getAllAlbums(sortOption: SortOption, minTracks: Int): List<Album> {
        return musicDao.getAllAlbums(sortOption, minTracks)
    }

    override suspend fun getAlbumById(id: Long): Album? {
        return musicDao.getAlbumById(id)
    }

    override suspend fun getSongsByAlbum(albumId: Long): List<Song> {
        return musicDao.getSongsByAlbum(albumId)
    }

    override fun getAllArtistsFlow(sortOption: SortOption): Flow<List<Artist>> = flow {
        emit(musicDao.getAllArtists(sortOption))
        database.tableInvalidationFlow.collect { table ->
            if (table == "songs" || table == "artists") {
                emit(musicDao.getAllArtists(sortOption))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getArtistById(id: Long): Artist? {
        return musicDao.getArtistById(id)
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
            if (table == "recently_played" || table == "songs") {
                emit(musicDao.getRecentlyPlayedSongs(limit))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun recordSongPlayed(songId: String, durationMs: Long) {
        musicDao.recordSongPlayed(songId, durationMs)
    }

    override fun getPlaybackEventsFlow(limit: Int): Flow<List<PlaybackEvent>> = flow {
        emit(musicDao.getPlaybackEvents(limit))
        database.tableInvalidationFlow.collect { table ->
            if (table == "playback_events") {
                emit(musicDao.getPlaybackEvents(limit))
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun getPlaybackEvents(limit: Int): List<PlaybackEvent> {
        return musicDao.getPlaybackEvents(limit)
    }

    override suspend fun getPlaybackEventsBetween(start: Long, end: Long): List<PlaybackEvent> {
        return musicDao.getPlaybackEventsBetween(start, end)
    }

    override suspend fun search(
        query: String,
        filter: SearchFilterType,
        minDurationMs: Long,
        minTracks: Int
    ): List<SearchResultItem> {
        return musicDao.search(query, filter, minDurationMs, minTracks)
    }

    override suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata,
        artistDelimiters: List<String>,
        wordDelimiters: List<String>,
        extractArtistFromTitle: Boolean
    ): MetadataEditResult {
        val editor = metadataEditor ?: DesktopMetadataEditor(musicDao, ioDispatcher = ioDispatcher)
        return editor.writeMetadata(
            song = song,
            metadata = metadata,
            artistDelimiters = artistDelimiters,
            wordDelimiters = wordDelimiters,
            extractArtistFromTitle = extractArtistFromTitle
        )
    }

    override suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails> {
        val editor = metadataEditor ?: DesktopMetadataEditor(musicDao, ioDispatcher = ioDispatcher)
        return editor.readMetadata(file)
    }

    override suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        artistDelimiters: List<String>,
        wordDelimiters: List<String>,
        extractArtistFromTitle: Boolean,
        onProgress: (completed: Int, total: Int) -> Unit
    ): BatchMetadataResult {
        val editor = metadataEditor ?: DesktopMetadataEditor(musicDao, ioDispatcher = ioDispatcher)
        return editor.batchUpdateMetadata(
            songs = songs,
            update = update,
            artistDelimiters = artistDelimiters,
            wordDelimiters = wordDelimiters,
            extractArtistFromTitle = extractArtistFromTitle,
            onProgress = onProgress
        )
    }

    override suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata> {
        val client = musicBrainzClient ?: MusicBrainzClient(ioDispatcher)
        return client.searchRecordings(title, artist)
    }

    override suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata> {
        val client = musicBrainzClient ?: MusicBrainzClient(ioDispatcher)
        return client.searchRecordingsByQuery(query)
    }

    override suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata> {
        val client = musicBrainzClient ?: MusicBrainzClient(ioDispatcher)
        return client.searchReleases(query)
    }

    override suspend fun fetchOnlineCoverArt(releaseId: String): ByteArray? {
        val client = musicBrainzClient ?: MusicBrainzClient(ioDispatcher)
        return client.fetchCoverArt(releaseId)
    }
}
