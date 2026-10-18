package com.kayoi.pixelplayerdesktop.platform.mediasession

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncMediaSessionControllerTest {

    private class TestController : MediaSessionController {
        val lastMetadata = AtomicReference<MediaMetadata?>(null)
        val lastPlaybackStatus = AtomicReference<MediaPlaybackStatus?>(null)
        val lastPositionMs = AtomicReference<Long?>(null)
        val isReleased = AtomicBoolean(false)

        override fun updateMetadata(metadata: MediaMetadata) {
            lastMetadata.set(metadata)
        }

        override fun updatePlaybackStatus(status: MediaPlaybackStatus) {
            lastPlaybackStatus.set(status)
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
    fun testSuccessfulAsyncInitReplaysCachedState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testScheduler)

        val targetController = TestController()
        val asyncController = AsyncMediaSessionController(
            coroutineScope = testScope,
            timeoutMs = 1000L,
            ioDispatcher = testDispatcher
        ) {
            targetController
        }

        val meta = MediaMetadata(title = "Async Song", artist = "Async Artist")
        asyncController.updateMetadata(meta)
        asyncController.updatePlaybackStatus(MediaPlaybackStatus.PLAYING)
        asyncController.updatePosition(42000L)

        assertEquals(null, targetController.lastMetadata.get())

        testScheduler.advanceUntilIdle()

        assertEquals("Async Song", targetController.lastMetadata.get()?.title)
        assertEquals(MediaPlaybackStatus.PLAYING, targetController.lastPlaybackStatus.get())
        assertEquals(42000L, targetController.lastPositionMs.get())

        asyncController.release()
        assertTrue(targetController.isReleased.get())
    }

    @Test
    fun testTimeoutFallsBackToNoOpWithoutCrashing() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testScheduler)

        val asyncController = AsyncMediaSessionController(
            coroutineScope = testScope,
            timeoutMs = 500L,
            ioDispatcher = testDispatcher
        ) {
            Thread.sleep(1000L)
            TestController()
        }

        asyncController.updateMetadata(MediaMetadata(title = "Test"))
        testScheduler.advanceTimeBy(600L)
        testScheduler.advanceUntilIdle()

        asyncController.updatePlaybackStatus(MediaPlaybackStatus.PAUSED)
        asyncController.release()
    }

    @Test
    fun testFactoryExceptionFallsBackToNoOpWithoutCrashing() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testScheduler)

        val asyncController = AsyncMediaSessionController(
            coroutineScope = testScope,
            timeoutMs = 1000L,
            ioDispatcher = testDispatcher
        ) {
            throw NoClassDefFoundError("com/sun/security/auth/module/UnixSystem")
        }

        asyncController.updateMetadata(MediaMetadata(title = "Test"))
        testScheduler.advanceUntilIdle()

        asyncController.updatePlaybackStatus(MediaPlaybackStatus.STOPPED)
        asyncController.release()
    }
}
