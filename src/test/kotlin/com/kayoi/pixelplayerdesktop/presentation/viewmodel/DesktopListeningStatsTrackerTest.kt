package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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

    @Test
    fun testConcurrentSessionEndsRecordExactlyOnce() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        var currentTime = 1_000_000L

        val testMusicDao = MusicDao(database, ioDispatcher = testDispatcher)
        val testFavDao = FavoritesDao(database, ioDispatcher = testDispatcher)
        val testPlaylistDao = LocalPlaylistDao(database, ioDispatcher = testDispatcher)
        val testRepo = MusicRepositoryImpl(database, testMusicDao, testFavDao, testPlaylistDao, ioDispatcher = testDispatcher)

        val tracker = DesktopListeningStatsTracker(
            musicRepository = testRepo,
            coroutineScope = testScope,
            timeProvider = { currentTime }
        )

        val song = Song(id = "song_concurrent", title = "Concurrent Song", artist = "Artist", duration = 180000L)
        testMusicDao.upsertSongs(listOf(song))

        tracker.onSongChanged(song, isPlaying = true)
        currentTime += 10_000L

        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                tracker.finalizeCurrentSession()
            }
        }
        jobs.joinAll()
        testDispatcher.scheduler.advanceUntilIdle()

        val events = testRepo.getPlaybackEvents().filter { it.songId == song.id }
        assertEquals(1, events.size)

        val updatedSong = testRepo.getSongById(song.id)
        assertEquals(1, updatedSong?.playCount)
    }

    @Test
    fun testSessionFlushUnderScopeCancellationFlushesWithoutDataLoss() = runTest {
        var currentTime = 1_000_000L
        val parentJob = Job()
        val cancellableScope = kotlinx.coroutines.CoroutineScope(parentJob + Dispatchers.Default)

        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = cancellableScope,
            timeProvider = { currentTime }
        )

        val song = Song(id = "song_cancellation", title = "Cancellation Song", artist = "Artist", duration = 180000L)
        musicDao.upsertSongs(listOf(song))

        tracker.onSongChanged(song, isPlaying = true)
        currentTime += 10_000L

        parentJob.cancel()
        tracker.finalizeCurrentSession()

        val events = repository.getPlaybackEvents().filter { it.songId == song.id }
        assertEquals(1, events.size)

        val updatedSong = repository.getSongById(song.id)
        assertEquals(1, updatedSong?.playCount)
    }
}
