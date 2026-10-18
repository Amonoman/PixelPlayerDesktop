package com.kayoi.pixelplayerdesktop.platform.mediasession

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AsyncMediaSessionController(
    coroutineScope: CoroutineScope,
    private val timeoutMs: Long = 3000L,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    factory: () -> MediaSessionController
) : MediaSessionController {

    private val lock = Any()
    private var activeController: MediaSessionController = NoOpMediaSessionController
    private var isReleased = false

    private var currentMetadata = MediaMetadata()
    private var currentPlaybackStatus = MediaPlaybackStatus.STOPPED
    private var currentPositionMs = 0L
    private var currentCapabilities: Capabilities? = null
    private var currentLoopStatus = "None"
    private var isShuffle = false
    private var currentVolume = 1.0

    private val initJob: Job = coroutineScope.launch(ioDispatcher) {
        val realController = try {
            withTimeoutOrNull(timeoutMs) {
                factory()
            } ?: run {
                System.err.println("Platform media session initialization timed out after ${timeoutMs}ms. Falling back to NoOp.")
                NoOpMediaSessionController
            }
        } catch (t: Throwable) {
            System.err.println("Failed to initialize platform media session (${t.javaClass.simpleName}: ${t.message}). Falling back to NoOp.")
            NoOpMediaSessionController
        }

        val snapshot = synchronized(lock) {
            if (isReleased) {
                realController.release()
                return@launch
            }
            activeController = realController
            StateSnapshot(
                metadata = currentMetadata,
                status = currentPlaybackStatus,
                positionMs = currentPositionMs,
                capabilities = currentCapabilities,
                loopStatus = currentLoopStatus,
                isShuffle = isShuffle,
                volume = currentVolume
            )
        }

        if (realController !== NoOpMediaSessionController) {
            realController.updateMetadata(snapshot.metadata)
            realController.updatePlaybackStatus(snapshot.status)
            realController.updatePosition(snapshot.positionMs)
            snapshot.capabilities?.let { caps ->
                realController.updateCapabilities(
                    canGoNext = caps.canGoNext,
                    canGoPrevious = caps.canGoPrevious,
                    canPlay = caps.canPlay,
                    canPause = caps.canPause,
                    canSeek = caps.canSeek
                )
            }
            realController.updateLoopAndShuffle(snapshot.loopStatus, snapshot.isShuffle)
            realController.updateVolume(snapshot.volume)
        }
    }

    override fun updateMetadata(metadata: MediaMetadata) {
        val delegate = synchronized(lock) {
            currentMetadata = metadata
            activeController
        }
        delegate.updateMetadata(metadata)
    }

    override fun updatePlaybackStatus(status: MediaPlaybackStatus) {
        val delegate = synchronized(lock) {
            currentPlaybackStatus = status
            activeController
        }
        delegate.updatePlaybackStatus(status)
    }

    override fun updatePosition(positionMs: Long) {
        val delegate = synchronized(lock) {
            currentPositionMs = positionMs
            activeController
        }
        delegate.updatePosition(positionMs)
    }

    override fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    ) {
        val delegate = synchronized(lock) {
            currentCapabilities = Capabilities(canGoNext, canGoPrevious, canPlay, canPause, canSeek)
            activeController
        }
        delegate.updateCapabilities(canGoNext, canGoPrevious, canPlay, canPause, canSeek)
    }

    override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {
        val delegate = synchronized(lock) {
            currentLoopStatus = loopStatus
            isShuffle = shuffle
            activeController
        }
        delegate.updateLoopAndShuffle(loopStatus, shuffle)
    }

    override fun updateVolume(volume: Double) {
        val delegate = synchronized(lock) {
            currentVolume = volume
            activeController
        }
        delegate.updateVolume(volume)
    }

    override fun release() {
        val delegate = synchronized(lock) {
            isReleased = true
            initJob.cancel()
            val target = activeController
            activeController = NoOpMediaSessionController
            target
        }
        delegate.release()
    }

    private data class Capabilities(
        val canGoNext: Boolean,
        val canGoPrevious: Boolean,
        val canPlay: Boolean,
        val canPause: Boolean,
        val canSeek: Boolean
    )

    private data class StateSnapshot(
        val metadata: MediaMetadata,
        val status: MediaPlaybackStatus,
        val positionMs: Long,
        val capabilities: Capabilities?,
        val loopStatus: String,
        val isShuffle: Boolean,
        val volume: Double
    )
}
