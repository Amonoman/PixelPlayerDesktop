package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.lifecycle.AppShutdownCoordinator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VlcjAudioEngineReleaseTest {

    @Test
    fun testInitialStateIsNotClosed() {
        val engine = VlcjAudioEngine()
        assertFalse(engine.isClosed())
        engine.release()
        assertTrue(engine.isClosed())
    }

    @Test
    fun testReleaseIsIdempotent() {
        val engine = VlcjAudioEngine()
        engine.release()
        assertTrue(engine.isClosed())
        engine.release()
        assertTrue(engine.isClosed())
        engine.release()
        assertTrue(engine.isClosed())
    }

    @Test
    fun testConcurrentRelease() {
        val engine = VlcjAudioEngine()
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val errorCount = AtomicInteger(0)

        repeat(threadCount) {
            thread {
                try {
                    engine.release()
                } catch (_: Throwable) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals(0, errorCount.get())
        assertTrue(engine.isClosed())
    }

    @Test
    fun testOperationsAfterReleaseAreSafeNoOps() {
        val engine = VlcjAudioEngine()
        engine.release()
        assertTrue(engine.isClosed())

        engine.play("/dummy/nonexistent.mp3")
        engine.play()
        engine.pause()
        engine.stop()
        engine.seekTo(5000L)
        engine.setVolume(0.8f)
        engine.setMute(true)
        engine.setEqualizer(true, listOf(0f, 1f, 2f))
        engine.crossfadeTo("/dummy/track2.mp3", 1000L)

        assertTrue(engine.isClosed())
    }

    @Test
    fun testAppShutdownCoordinatorIdempotent() {
        AppShutdownCoordinator.resetForTesting()
        val engine = VlcjAudioEngine()
        assertFalse(engine.isClosed())

        AppShutdownCoordinator.performTeardown(audioEngine = engine, timeoutMs = 2000L)
        assertTrue(engine.isClosed())

        AppShutdownCoordinator.performTeardown(audioEngine = engine, timeoutMs = 2000L)
        assertTrue(engine.isClosed())
    }

    @Test
    fun testAppShutdownCoordinatorBoundedTimeoutOnHang() {
        AppShutdownCoordinator.resetForTesting()
        val hangingEngine = object : AudioEngine {
            override val playbackState: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState.IDLE)
            override val currentPositionMs: StateFlow<Long> = MutableStateFlow(0L)
            override val durationMs: StateFlow<Long> = MutableStateFlow(0L)
            override val volume: StateFlow<Float> = MutableStateFlow(1f)
            override val isMuted: StateFlow<Boolean> = MutableStateFlow(false)
            override val errorEvents: SharedFlow<String> = MutableSharedFlow()
            override val completionEvents: SharedFlow<Unit> = MutableSharedFlow()

            override fun play(mediaPathOrUri: String) {}
            override fun play() {}
            override fun pause() {}
            override fun stop() {}
            override fun seekTo(positionMs: Long) {}
            override fun setVolume(volume: Float) {}
            override fun setMute(muted: Boolean) {}
            override fun setReplayGain(enabled: Boolean, mode: String) {}

            override fun release() {
                Thread.sleep(5000L)
            }
        }

        val startTime = System.currentTimeMillis()
        AppShutdownCoordinator.performTeardown(audioEngine = hangingEngine, timeoutMs = 500L)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue(elapsed < 2000L, "Teardown must not hang; took ${elapsed}ms instead of ~500ms")
    }
}
