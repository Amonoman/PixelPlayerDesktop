package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.domain.player.SleepTimerSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerStateHolderTest {

    private class FakeAudioEngine : AudioEngine {
        val paused = MutableStateFlow(false)
        override val playbackState: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState.IDLE)
        override val currentPositionMs: StateFlow<Long> = MutableStateFlow(0L)
        override val durationMs: StateFlow<Long> = MutableStateFlow(10000L)
        override val volume: StateFlow<Float> = MutableStateFlow(1.0f)
        override val isMuted: StateFlow<Boolean> = MutableStateFlow(false)
        override val errorEvents: SharedFlow<String> = MutableSharedFlow()
        override val completionEvents: SharedFlow<Unit> = MutableSharedFlow()

        override fun play(mediaPathOrUri: String) {}
        override fun play() { paused.value = false }
        override fun pause() { paused.value = true }
        override fun stop() {}
        override fun seekTo(positionMs: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setMute(muted: Boolean) {}
        override fun setReplayGain(enabled: Boolean, mode: String) {}
        override fun release() {}
    }

    private fun testSong(id: String) = Song(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        path = "/music/$id.mp3",
        dateAdded = 0L
    )

    @Test
    fun testDurationTimerActivationAndExpiry() = runTest {
        val session = SleepTimerSession()
        val audioEngine = FakeAudioEngine()
        val queueManager = QueueManager()
        val holder = SleepTimerStateHolder(session, audioEngine, queueManager, backgroundScope)

        holder.setSleepTimer(1)
        testScheduler.runCurrent()
        assertTrue(session.timer.value is SleepTimerSession.Timer.Duration)
        assertEquals(1, holder.activeTimerDurationMinutes.value)
        assertEquals("1 min", holder.activeTimerValueDisplay.value)

        holder.cancelTimer()
        testScheduler.runCurrent()
        assertNull(session.timer.value)
        assertNull(holder.activeTimerDurationMinutes.value)
    }

    @Test
    fun testEndOfTrackTimer() = runTest {
        val session = SleepTimerSession()
        val audioEngine = FakeAudioEngine()
        val queueManager = QueueManager()
        val holder = SleepTimerStateHolder(session, audioEngine, queueManager, backgroundScope)

        val song = testSong("song-1")
        queueManager.setQueue(listOf(song), 0)

        holder.setEndOfTrackTimer(true)
        testScheduler.runCurrent()
        assertTrue(holder.isEndOfTrackTimerActive.value)
        assertEquals(SleepTimerSession.Timer.EndOfTrack("song-1"), session.timer.value)

        holder.setEndOfTrackTimer(false)
        testScheduler.runCurrent()
        assertFalse(holder.isEndOfTrackTimerActive.value)
        assertNull(session.timer.value)
    }

    @Test
    fun testCountedPlay() = runTest {
        val session = SleepTimerSession()
        val audioEngine = FakeAudioEngine()
        val queueManager = QueueManager(initialRepeatMode = 0)
        val holder = SleepTimerStateHolder(session, audioEngine, queueManager, backgroundScope)

        val song = testSong("song-2")
        queueManager.setQueue(listOf(song), 0)

        holder.playCounted(3)
        testScheduler.runCurrent()
        assertEquals(3f, holder.playCount.value)
        assertEquals(2, queueManager.repeatMode.value)
        assertEquals("song-2", session.countedPlay.value?.songId)
        assertEquals(3, session.countedPlay.value?.total)

        holder.stopCountedPlay(restoreRepeatMode = true)
        testScheduler.runCurrent()
        assertNull(session.countedPlay.value)
        assertEquals(0, queueManager.repeatMode.value)
    }
}
