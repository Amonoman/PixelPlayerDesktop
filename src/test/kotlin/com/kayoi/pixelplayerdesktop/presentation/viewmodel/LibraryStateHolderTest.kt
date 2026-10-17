package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryStateHolderTest {

    @Test
    fun `songs and favorites filter out songs in excluded directories`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_lib_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_lib_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val allowedSong = Song(
            id = "song_allowed",
            title = "Allowed Track",
            artist = "Artist A",
            album = "Album A",
            path = File("/home/user/music/allowed/song.mp3").absolutePath,
            duration = 180000L,
            isFavorite = true
        )
        val excludedSong = Song(
            id = "song_excluded",
            title = "Excluded Track",
            artist = "Unknown Artist",
            album = "Unknown Album",
            path = File("/home/user/music/excluded/song.mp3").absolutePath,
            duration = 180000L,
            isFavorite = true
        )

        musicDao.upsertSongs(listOf(allowedSong, excludedSong))
        favDao.toggleFavorite("song_allowed")
        favDao.toggleFavorite("song_excluded")

        val directSongs = musicDao.getAllSongs()
        val directFavs = favDao.getFavoriteSongIds()
        println("DIRECT: songs=${directSongs.size}, favs=${directFavs.size}")

        val libraryStateHolder = LibraryStateHolder(
            musicRepository = musicRepo,
            settingsRepository = settingsRepo,
            scanner = scanner,
            coroutineScope = backgroundScope
        )

        val initialSongs = libraryStateHolder.songs.first { it.isNotEmpty() }
        val initialFavs = libraryStateHolder.favoriteSongs.first { it.isNotEmpty() }

        assertEquals(2, initialSongs.size)
        assertEquals(2, initialFavs.size)

        settingsRepo.addExcludedDirectory(File("/home/user/music/excluded").absolutePath)

        val filteredSongs = libraryStateHolder.songs.first { it.size == 1 }
        val filteredFavs = libraryStateHolder.favoriteSongs.first { it.size == 1 }

        assertEquals("Allowed Track", filteredSongs.first().title)
        assertEquals("Allowed Track", filteredFavs.first().title)
    }
}
