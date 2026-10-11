package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.data.player.VlcjAudioEngine
import com.kayoi.pixelplayerdesktop.data.repository.LyricsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @Test
    fun testPlayerViewModelOrchestration() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_vm_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_vm_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)
        val lyricsDao = LyricsDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val lyricsRepo = LyricsRepositoryImpl(lyricsDao, settingsRepository = settingsRepo, ioDispatcher = testDispatcher)
        val scanner = DesktopMusicScanner(musicDao, ioDispatcher = testDispatcher)

        val audioEngine = VlcjAudioEngine(coroutineScope = backgroundScope)
        val queueManager = QueueManager()

        val playbackStateHolder = PlaybackStateHolder(audioEngine, queueManager, musicRepo, lyricsRepo, settingsRepo, coroutineScope = backgroundScope)
        val libraryStateHolder = LibraryStateHolder(musicRepo, settingsRepo, scanner, coroutineScope = backgroundScope)
        val themeStateHolder = ThemeStateHolder(settingsRepo, coroutineScope = backgroundScope)

        val viewModel = PlayerViewModel(playbackStateHolder, libraryStateHolder, themeStateHolder)

        // 1. Play song through viewModel
        val song = Song(
            id = "s1",
            title = "Test Song",
            artist = "Artist",
            path = "/tmp/test.mp3",
            duration = 200000L
        )

        musicDao.upsertSongs(listOf(song))
        viewModel.playSong(song)

        assertEquals("Test Song", viewModel.currentSong.value?.title)

        // 2. Search query
        viewModel.onSearchQueryChanged("Test")
        assertEquals("Test", viewModel.searchQuery.value)
    }
}
