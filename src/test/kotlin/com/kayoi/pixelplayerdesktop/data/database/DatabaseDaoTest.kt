package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseDaoTest {

    @Test
    fun testMusicDaoAndFavoritesAndPlaylists() = runTest {
        val tempDb = File.createTempFile("test_pixelplayer", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)
        val favoritesDao = FavoritesDao(db)
        val playlistDao = LocalPlaylistDao(db)

        val song1 = Song(
            id = "1",
            title = "Alpha Song",
            artist = "Artist A",
            artistId = 101L,
            album = "Album 1",
            albumId = 201L,
            path = "/music/alpha.mp3",
            contentUriString = "file:///music/alpha.mp3",
            duration = 180000L,
            genre = "Rock",
            year = 2022,
            dateAdded = 1000L
        )

        val song2 = Song(
            id = "2",
            title = "Beta Song",
            artist = "Artist B",
            artistId = 102L,
            album = "Album 2",
            albumId = 202L,
            path = "/music/beta.mp3",
            contentUriString = "file:///music/beta.mp3",
            duration = 240000L,
            genre = "Pop",
            year = 2023,
            dateAdded = 2000L
        )

        // 1. Test upsert songs
        musicDao.upsertSongs(listOf(song1, song2))

        val allSongs = musicDao.getAllSongs(SortOption.SongTitleAZ)
        assertEquals(2, allSongs.size)
        assertEquals("Alpha Song", allSongs[0].title)
        assertEquals("Beta Song", allSongs[1].title)

        // 2. Test sorting
        val descSongs = musicDao.getAllSongs(SortOption.SongTitleZA)
        assertEquals("Beta Song", descSongs[0].title)

        // 3. Test albums and artists aggregation
        val albums = musicDao.getAllAlbums()
        assertEquals(2, albums.size)

        val artists = musicDao.getAllArtists()
        assertEquals(2, artists.size)

        // 4. Test Favorites
        favoritesDao.addFavorite("1")
        assertTrue(favoritesDao.isFavorite("1"))
        assertEquals(listOf("1"), favoritesDao.getFavoriteSongIds())

        val updatedSong1 = musicDao.getSongById("1")
        assertNotNull(updatedSong1)
        assertTrue(updatedSong1.isFavorite)

        // 5. Test Playlists
        val playlist = Playlist(
            id = "pl_1",
            name = "My Favorites",
            songIds = listOf("1", "2")
        )
        playlistDao.insertPlaylist(playlist)

        val playlists = playlistDao.getAllPlaylists()
        assertEquals(1, playlists.size)
        assertEquals("My Favorites", playlists[0].name)
        assertEquals(listOf("1", "2"), playlists[0].songIds)

        // 6. Test Search
        val searchResults = musicDao.search("Alpha")
        assertEquals(1, searchResults.size)
    }

    @Test
    fun testAlbumsAndArtistsMinDurationFiltering() = runTest {
        val tempDb = File.createTempFile("test_duration_filter_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val validSong = Song(
            id = "valid_1",
            title = "Valid Song",
            artist = "Unknown Artist",
            artistId = 101L,
            album = "Unknown Album",
            albumId = 201L,
            path = "/music/valid.mp3",
            contentUriString = "file:///music/valid.mp3",
            duration = 180000L,
            dateAdded = 1000L
        )

        val shortSong1 = Song(
            id = "short_1",
            title = "Sound Effect 1",
            artist = "Unknown Artist",
            artistId = 101L,
            album = "Unknown Album",
            albumId = 201L,
            path = "/music/se1.mp3",
            contentUriString = "file:///music/se1.mp3",
            duration = 3000L,
            dateAdded = 1001L
        )

        val shortSong2 = Song(
            id = "short_2",
            title = "Sound Effect 2",
            artist = "Unknown Artist",
            artistId = 101L,
            album = "Unknown Album",
            albumId = 201L,
            path = "/music/se2.mp3",
            contentUriString = "file:///music/se2.mp3",
            duration = 5000L,
            dateAdded = 1002L
        )

        musicDao.upsertSongs(listOf(validSong, shortSong1, shortSong2))

        val unfilteredAlbums = musicDao.getAllAlbums(minDurationMs = 0L)
        assertEquals(3, unfilteredAlbums[0].songCount)

        val filteredAlbums = musicDao.getAllAlbums(minDurationMs = 10000L)
        assertEquals(1, filteredAlbums.size)
        assertEquals(1, filteredAlbums[0].songCount)

        val unfilteredArtists = musicDao.getAllArtists(minDurationMs = 0L)
        assertEquals(3, unfilteredArtists[0].songCount)

        val filteredArtists = musicDao.getAllArtists(minDurationMs = 10000L)
        assertEquals(1, filteredArtists.size)
        assertEquals(1, filteredArtists[0].songCount)
    }
}
