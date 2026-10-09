package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class MusicDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAllSongs(sortOption: SortOption = SortOption.SongTitleAZ): List<Song> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.SongTitleAZ -> "title COLLATE NOCASE ASC"
            SortOption.SongTitleZA -> "title COLLATE NOCASE DESC"
            SortOption.SongArtist -> "artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongArtistDesc -> "artist COLLATE NOCASE DESC, album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongAlbum -> "album COLLATE NOCASE ASC, track_number ASC"
            SortOption.SongAlbumDesc -> "album COLLATE NOCASE DESC, track_number ASC"
            SortOption.SongDateAdded -> "date_added DESC"
            SortOption.SongDateAddedAsc -> "date_added ASC"
            SortOption.SongDuration -> "duration DESC"
            SortOption.SongDurationAsc -> "duration ASC"
            else -> "title COLLATE NOCASE ASC"
        }

        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs ORDER BY $orderByClause"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong())
                    }
                    songs
                }
            }
        }
    }

    suspend fun getSongById(id: String): Song? = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSong() else null
                }
            }
        }
    }

    suspend fun getSongByPath(path: String): Song? = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE path = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, path)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSong() else null
                }
            }
        }
    }

    suspend fun getSongsByIds(ids: List<String>): List<Song> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE id IN ($placeholders)"
            conn.prepareStatement(sql).use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.setString(index + 1, id)
                }
                val resultMap = mutableMapOf<String, Song>()
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val song = rs.toSong()
                        resultMap[song.id] = song
                    }
                }
                // Maintain the requested order
                ids.mapNotNull { resultMap[it] }
            }
        }
    }

    suspend fun upsertSongs(songs: List<Song>) = withContext(ioDispatcher) {
        if (songs.isEmpty()) return@withContext
        database.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val sql = """
                    INSERT INTO songs (
                        id, title, artist, artist_id, album, album_id, album_artist,
                        path, content_uri_string, album_art_uri_string, duration, genre,
                        lyrics, is_favorite, track_number, disc_number, year, date_added,
                        date_modified, mime_type, bitrate, sample_rate, navidrome_id, jellyfin_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(path) DO UPDATE SET
                        title = excluded.title,
                        artist = excluded.artist,
                        album = excluded.album,
                        album_artist = excluded.album_artist,
                        album_art_uri_string = excluded.album_art_uri_string,
                        duration = excluded.duration,
                        genre = excluded.genre,
                        lyrics = excluded.lyrics,
                        track_number = excluded.track_number,
                        disc_number = excluded.disc_number,
                        year = excluded.year,
                        date_modified = excluded.date_modified,
                        mime_type = excluded.mime_type,
                        bitrate = excluded.bitrate,
                        sample_rate = excluded.sample_rate;
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    for (song in songs) {
                        stmt.setString(1, song.id)
                        stmt.setString(2, song.title)
                        stmt.setString(3, song.artist)
                        stmt.setLong(4, song.artistId)
                        stmt.setString(5, song.album)
                        stmt.setLong(6, song.albumId)
                        stmt.setString(7, song.albumArtist)
                        stmt.setString(8, song.path)
                        stmt.setString(9, song.contentUriString)
                        stmt.setString(10, song.albumArtUriString)
                        stmt.setLong(11, song.duration)
                        stmt.setString(12, song.genre)
                        stmt.setString(13, song.lyrics)
                        stmt.setInt(14, if (song.isFavorite) 1 else 0)
                        stmt.setInt(15, song.trackNumber)
                        if (song.discNumber != null) stmt.setInt(16, song.discNumber) else stmt.setNull(16, java.sql.Types.INTEGER)
                        stmt.setInt(17, song.year)
                        stmt.setLong(18, song.dateAdded)
                        stmt.setLong(19, song.dateModified)
                        stmt.setString(20, song.mimeType)
                        if (song.bitrate != null) stmt.setInt(21, song.bitrate) else stmt.setNull(21, java.sql.Types.INTEGER)
                        if (song.sampleRate != null) stmt.setInt(22, song.sampleRate) else stmt.setNull(22, java.sql.Types.INTEGER)
                        stmt.setString(23, song.navidromeId)
                        stmt.setString(24, song.jellyfinId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.commit()
                database.notifyTableChanged("songs")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun deleteSongsNotInPaths(existingPaths: Set<String>) = withContext(ioDispatcher) {
        if (existingPaths.isEmpty()) {
            database.getConnection().use { conn ->
                conn.createStatement().use { it.executeUpdate("DELETE FROM songs") }
                database.notifyTableChanged("songs")
            }
            return@withContext
        }
        database.getConnection().use { conn ->
            val allPaths = mutableSetOf<String>()
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT path FROM songs").use { rs ->
                    while (rs.next()) {
                        allPaths.add(rs.getString("path"))
                    }
                }
            }
            val pathsToDelete = allPaths - existingPaths
            if (pathsToDelete.isNotEmpty()) {
                conn.autoCommit = false
                try {
                    conn.prepareStatement("DELETE FROM songs WHERE path = ?").use { stmt ->
                        for (path in pathsToDelete) {
                            stmt.setString(1, path)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                    conn.commit()
                    database.notifyTableChanged("songs")
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }
    }

    suspend fun getAllAlbums(sortOption: SortOption = SortOption.AlbumTitleAZ): List<Album> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.AlbumTitleAZ -> "album COLLATE NOCASE ASC"
            SortOption.AlbumTitleZA -> "album COLLATE NOCASE DESC"
            SortOption.AlbumArtist -> "artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC"
            SortOption.AlbumArtistDesc -> "artist COLLATE NOCASE DESC, album COLLATE NOCASE ASC"
            SortOption.AlbumReleaseYear -> "max_year DESC"
            SortOption.AlbumReleaseYearAsc -> "max_year ASC"
            SortOption.AlbumDateAdded -> "max_date_added DESC"
            SortOption.AlbumSizeAsc -> "song_count ASC"
            SortOption.AlbumSizeDesc -> "song_count DESC"
            else -> "album COLLATE NOCASE ASC"
        }

        database.getConnection().use { conn ->
            val sql = """
                SELECT
                    album_id,
                    album,
                    artist,
                    album_artist,
                    MAX(year) as max_year,
                    MAX(date_added) as max_date_added,
                    MAX(album_art_uri_string) as album_art_uri_string,
                    COUNT(*) as song_count
                FROM songs
                WHERE album != ''
                GROUP BY album, artist
                ORDER BY $orderByClause
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val albums = mutableListOf<Album>()
                    while (rs.next()) {
                        albums.add(
                            Album(
                                id = rs.getLong("album_id"),
                                title = rs.getString("album"),
                                artist = rs.getString("artist"),
                                albumArtist = rs.getString("album_artist"),
                                year = rs.getInt("max_year"),
                                dateAdded = rs.getLong("max_date_added"),
                                albumArtUriString = rs.getString("album_art_uri_string"),
                                songCount = rs.getInt("song_count")
                            )
                        )
                    }
                    albums
                }
            }
        }
    }

    suspend fun getSongsByAlbum(albumId: Long): List<Song> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE album_id = ? ORDER BY disc_number ASC, track_number ASC, title COLLATE NOCASE ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, albumId)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong())
                    }
                    songs
                }
            }
        }
    }

    suspend fun getAllArtists(sortOption: SortOption = SortOption.ArtistNameAZ): List<Artist> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.ArtistNameAZ -> "artist COLLATE NOCASE ASC"
            SortOption.ArtistNameZA -> "artist COLLATE NOCASE DESC"
            SortOption.ArtistNumSongsDesc -> "song_count DESC"
            SortOption.ArtistNumSongsAsc -> "song_count ASC"
            else -> "artist COLLATE NOCASE ASC"
        }

        database.getConnection().use { conn ->
            val sql = """
                SELECT
                    artist_id,
                    artist,
                    COUNT(*) as song_count
                FROM songs
                WHERE artist != ''
                GROUP BY artist
                ORDER BY $orderByClause
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val artists = mutableListOf<Artist>()
                    while (rs.next()) {
                        artists.add(
                            Artist(
                                id = rs.getLong("artist_id"),
                                name = rs.getString("artist"),
                                songCount = rs.getInt("song_count")
                            )
                        )
                    }
                    artists
                }
            }
        }
    }

    suspend fun getSongsByArtist(artistId: Long): List<Song> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE artist_id = ? ORDER BY year DESC, album COLLATE NOCASE ASC, track_number ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, artistId)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong())
                    }
                    songs
                }
            }
        }
    }

    suspend fun getAllGenres(): List<Genre> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = """
                SELECT genre, COUNT(*) as count
                FROM songs
                WHERE genre IS NOT NULL AND genre != ''
                GROUP BY genre
                ORDER BY genre COLLATE NOCASE ASC
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val genres = mutableListOf<Genre>()
                    while (rs.next()) {
                        val name = rs.getString("genre")
                        genres.add(Genre(id = name, name = name))
                    }
                    genres
                }
            }
        }
    }

    suspend fun getSongsByGenre(genreName: String): List<Song> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE genre = ? ORDER BY title COLLATE NOCASE ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, genreName)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong())
                    }
                    songs
                }
            }
        }
    }

    suspend fun recordSongPlayed(songId: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "UPDATE songs SET play_count = play_count + 1, last_played_timestamp = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, System.currentTimeMillis())
                stmt.setString(2, songId)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("songs")
        }
    }

    suspend fun getRecentlyPlayedSongs(limit: Int = 50): List<Song> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM songs WHERE last_played_timestamp > 0 ORDER BY last_played_timestamp DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val songs = mutableListOf<Song>()
                    while (rs.next()) {
                        songs.add(rs.toSong())
                    }
                    songs
                }
            }
        }
    }

    suspend fun search(query: String, filter: SearchFilterType = SearchFilterType.ALL): List<SearchResultItem> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val q = "%$query%"
        val results = mutableListOf<SearchResultItem>()

        database.getConnection().use { conn ->
            if (filter == SearchFilterType.ALL || filter == SearchFilterType.SONGS) {
                val songSql = "SELECT * FROM songs WHERE title LIKE ? OR artist LIKE ? OR album LIKE ? LIMIT 50"
                conn.prepareStatement(songSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.setString(2, q)
                    stmt.setString(3, q)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(SearchResultItem.SongItem(rs.toSong()))
                        }
                    }
                }
            }

            if (filter == SearchFilterType.ALL || filter == SearchFilterType.ALBUMS) {
                val albumSql = """
                    SELECT album_id, album, artist, album_artist, MAX(year) as max_year, MAX(date_added) as max_date_added, album_art_uri_string, COUNT(*) as song_count
                    FROM songs WHERE album LIKE ? GROUP BY album, artist LIMIT 20
                """.trimIndent()
                conn.prepareStatement(albumSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(
                                SearchResultItem.AlbumItem(
                                    Album(
                                        id = rs.getLong("album_id"),
                                        title = rs.getString("album"),
                                        artist = rs.getString("artist"),
                                        albumArtist = rs.getString("album_artist"),
                                        year = rs.getInt("max_year"),
                                        dateAdded = rs.getLong("max_date_added"),
                                        albumArtUriString = rs.getString("album_art_uri_string"),
                                        songCount = rs.getInt("song_count")
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (filter == SearchFilterType.ALL || filter == SearchFilterType.ARTISTS) {
                val artistSql = "SELECT artist_id, artist, COUNT(*) as song_count FROM songs WHERE artist LIKE ? GROUP BY artist LIMIT 20"
                conn.prepareStatement(artistSql).use { stmt ->
                    stmt.setString(1, q)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            results.add(
                                SearchResultItem.ArtistItem(
                                    Artist(
                                        id = rs.getLong("artist_id"),
                                        name = rs.getString("artist"),
                                        songCount = rs.getInt("song_count")
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
        results
    }
}

private fun ResultSet.toSong(): Song {
    val discNumber = getInt("disc_number")
    val wasNullDisc = wasNull()
    val bitrate = getInt("bitrate")
    val wasNullBitrate = wasNull()
    val sampleRate = getInt("sample_rate")
    val wasNullSampleRate = wasNull()

    return Song(
        id = getString("id"),
        title = getString("title"),
        artist = getString("artist"),
        artistId = getLong("artist_id"),
        album = getString("album"),
        albumId = getLong("album_id"),
        albumArtist = getString("album_artist"),
        path = getString("path"),
        contentUriString = getString("content_uri_string"),
        albumArtUriString = getString("album_art_uri_string"),
        duration = getLong("duration"),
        genre = getString("genre"),
        lyrics = getString("lyrics"),
        isFavorite = getInt("is_favorite") == 1,
        trackNumber = getInt("track_number"),
        discNumber = if (wasNullDisc) null else discNumber,
        year = getInt("year"),
        dateAdded = getLong("date_added"),
        dateModified = getLong("date_modified"),
        mimeType = getString("mime_type"),
        bitrate = if (wasNullBitrate) null else bitrate,
        sampleRate = if (wasNullSampleRate) null else sampleRate,
        navidromeId = getString("navidrome_id"),
        jellyfinId = getString("jellyfin_id")
    )
}
