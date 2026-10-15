package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.metadata.AudioMetadataDetails
import com.kayoi.pixelplayerdesktop.data.metadata.BatchMetadataResult
import com.kayoi.pixelplayerdesktop.data.metadata.BatchSongMetadataUpdate
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.metadata.MetadataEditResult
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Genre
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SearchFilterType
import com.kayoi.pixelplayerdesktop.data.model.SearchResultItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MusicRepository {
    fun getAllSongsFlow(sortOption: SortOption = SortOption.SongTitleAZ, minDurationMs: Long = 0L): Flow<List<Song>>
    suspend fun getAllSongs(sortOption: SortOption = SortOption.SongTitleAZ, minDurationMs: Long = 0L): List<Song>
    suspend fun getSongById(id: String): Song?
    suspend fun getSongsByIds(ids: List<String>): List<Song>
    suspend fun getSongByPath(path: String): Song?
    suspend fun deleteSong(song: Song)
    suspend fun clearLibrary()

    fun getAllAlbumsFlow(sortOption: SortOption = SortOption.AlbumTitleAZ, minTracks: Int = 1): Flow<List<Album>>
    suspend fun getAllAlbums(sortOption: SortOption = SortOption.AlbumTitleAZ, minTracks: Int = 1): List<Album>
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

    suspend fun search(
        query: String,
        filter: SearchFilterType = SearchFilterType.ALL,
        minDurationMs: Long = 0L,
        minTracks: Int = 1
    ): List<SearchResultItem>

    suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true
    ): MetadataEditResult

    suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails>
    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult

    suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata>
    suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata>
    suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata>
    suspend fun fetchOnlineCoverArt(releaseId: String): ByteArray?
}
