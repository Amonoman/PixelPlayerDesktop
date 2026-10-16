package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopListeningStatsTrackerTest {

    private lateinit var tempDbFile: File
    private lateinit var database: PixelPlayerDatabase
    private lateinit var musicDao: MusicDao
    private lateinit var repository: MusicRepositoryImpl

    @BeforeTest
    fun setup() {
        tempDbFile = File.createTempFile("test_tracker_db", ".db")
        tempDbFile.deleteOnExit()
        database = PixelPlayerDatabase(tempDbFile)
        musicDao = MusicDao(database)
        val favoritesDao = FavoritesDao(database)
        val playlistDao = LocalPlaylistDao(database)
        repository = MusicRepositoryImpl(database, musicDao, favoritesDao, playlistDao)
    }

    @AfterTest
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun testShortListeningUnderThresholdNotRecorded() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = testScope
        )

        val song = Song(id = "song_short", title = "Short Song", artist = "Artist", duration = 180000L)
        tracker.onSongChanged(song, isPlaying = true)
        tracker.finalizeCurrentSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val events = repository.getPlaybackEvents()
        assertTrue(events.isEmpty())
    }

    @Test
    fun testSongChangedWithNoSongClearsSession() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = testScope
        )

        tracker.onSongChanged(null, isPlaying = false)
        tracker.finalizeCurrentSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val events = repository.getPlaybackEvents()
        assertTrue(events.isEmpty())
    }
}
