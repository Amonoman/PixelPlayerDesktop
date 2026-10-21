package com.kayoi.pixelplayerdesktop.platform.mediasession

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.data.player.mpv.MpvAudioEngine
import com.kayoi.pixelplayerdesktop.data.repository.LyricsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlaybackStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MediaSessionBridgeTest {

    private class RecordingController : MediaSessionController {
        val lastMetadata = AtomicReference<MediaMetadata?>(null)
        val lastPlaybackStatus = AtomicReference<MediaPlaybackStatus?>(null)
        val lastPositionMs = AtomicReference<Long?>(null)
        val updateMetadataCount = AtomicInteger(0)
        val updateStatusCount = AtomicInteger(0)
        val isReleased = AtomicBoolean(false)

        override fun updateMetadata(metadata: MediaMetadata) {
            lastMetadata.set(metadata)
            updateMetadataCount.incrementAndGet()
        }

        override fun updatePlaybackStatus(status: MediaPlaybackStatus) {
            lastPlaybackStatus.set(status)
            updateStatusCount.incrementAndGet()
        }

        override fun updatePosition(positionMs: Long) {
            lastPositionMs.set(positionMs)
        }

        override fun updateCapabilities(
            canGoNext: Boolean,
            canGoPrevious: Boolean,
            canPlay: Boolean,
            canPause: Boolean,
            canSeek: Boolean
        ) {}

        override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {}
        override fun updateVolume(volume: Double) {}

        override fun release() {
            isReleased.set(true)
        }
    }

    @Test
    fun testBridgeFlowObservationAndCommands() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        val tempDb = File.createTempFile("test_bridge_db", ".db")
        tempDb.deleteOnExit()
        val tempSettings = File.createTempFile("test_bridge_settings", ".json")
        tempSettings.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db, ioDispatcher = testDispatcher)
        val favDao = FavoritesDao(db, ioDispatcher = testDispatcher)
        val playlistDao = LocalPlaylistDao(db, ioDispatcher = testDispatcher)
        val lyricsDao = LyricsDao(db, ioDispatcher = testDispatcher)

        val musicRepo = MusicRepositoryImpl(db, musicDao, favDao, playlistDao, ioDispatcher = testDispatcher)
        val settingsRepo = SettingsRepositoryImpl(settingsFile = tempSettings, ioDispatcher = testDispatcher)
        val lyricsRepo = LyricsRepositoryImpl(lyricsDao, settingsRepository = settingsRepo, ioDispatcher = testDispatcher)

        val audioEngine = MpvAudioEngine(coroutineScope = backgroundScope)
        val queueManager = QueueManager()

        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)

        val playbackStateHolder = PlaybackStateHolder(
            audioEngine = audioEngine,
            queueManager = queueManager,
            musicRepository = musicRepo,
            lyricsRepository = lyricsRepo,
            settingsRepository = settingsRepo,
            coroutineScope = testScope
        )

        val recordingController = RecordingController()
        val bridge = MediaSessionBridge(
            playbackStateHolder = playbackStateHolder,
            coroutineScope = testScope,
            controllerFactory = { _, _ -> recordingController }
        )

        val song1 = Song(id = "1", title = "Track One", artist = "Artist One", path = "/tmp/1.mp3", duration = 180000L)
        val song2 = Song(id = "2", title = "Track Two", artist = "Artist Two", path = "/tmp/2.mp3", duration = 210000L)

        playbackStateHolder.playSongs(listOf(song1, song2), 0)
        testScheduler.advanceUntilIdle()

        assertEquals("Track One", recordingController.lastMetadata.get()?.title)
        assertEquals("Artist One", recordingController.lastMetadata.get()?.artist)

        bridge.next()
        testScheduler.advanceUntilIdle()
        assertEquals("Track Two", playbackStateHolder.currentSong.value?.title)

        bridge.previous()
        testScheduler.advanceUntilIdle()
        assertEquals("Track One", playbackStateHolder.currentSong.value?.title)

        bridge.release()
        assertTrue(recordingController.isReleased.get(), "Controller should be released")
    }

    @Test
    fun testFactoryGracefulFallback() {
        val noOp = NoOpMediaSessionController
        noOp.updateMetadata(MediaMetadata())
        noOp.updatePlaybackStatus(MediaPlaybackStatus.STOPPED)
        noOp.updatePosition(0L)
        noOp.updateCapabilities(false, false, false, false, false)
        noOp.updateLoopAndShuffle("None", false)
        noOp.updateVolume(1.0)
        noOp.release()
        assertTrue(true)
    }
}
