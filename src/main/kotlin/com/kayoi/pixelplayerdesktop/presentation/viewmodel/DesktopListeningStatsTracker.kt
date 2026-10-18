package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.ScrobbleManager
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DesktopListeningStatsTracker(
    private val musicRepository: MusicRepository,
    private val scrobbleManager: ScrobbleManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val currentSession = AtomicReference<ActiveSession?>(null)

    init {
        runCatching {
            Runtime.getRuntime().addShutdownHook(Thread {
                finalizeCurrentSession(forceSynchronous = true)
            })
        }
    }

    fun onSongChanged(song: Song?, isPlaying: Boolean) {
        finalizeCurrentSession()
        if (song == null) return

        val now = timeProvider()
        val newSession = ActiveSession(
            song = song,
            startedAtEpochMs = now,
            accumulatedListeningMs = 0L,
            lastRecordedEpochMs = now,
            isPlaying = isPlaying
        )
        val prev = currentSession.getAndSet(newSession)
        if (prev != null) {
            finalizeSessionInternal(prev, now, forceSynchronous = false)
        }

        if (isPlaying) {
            scrobbleManager?.onPlayingNow(song)
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        val now = timeProvider()
        var newlyStartedPlayingSong: Song? = null

        currentSession.updateAndGet { session ->
            if (session == null) return@updateAndGet null
            val accumulated = accumulateTime(session, now)
            val wasPlaying = session.isPlaying
            if (!wasPlaying && isPlaying) {
                newlyStartedPlayingSong = session.song
            }
            accumulated.copy(
                isPlaying = isPlaying,
                lastRecordedEpochMs = now
            )
        }

        newlyStartedPlayingSong?.let { scrobbleManager?.onPlayingNow(it) }
    }

    fun onPlaybackStopped() {
        finalizeCurrentSession()
    }

    fun finalizeCurrentSession(forceSynchronous: Boolean = false) {
        val session = currentSession.getAndUpdate { null } ?: return
        val now = timeProvider()
        finalizeSessionInternal(session, now, forceSynchronous)
    }

    fun flush(forceSynchronous: Boolean = false) {
        finalizeCurrentSession(forceSynchronous)
    }

    private fun finalizeSessionInternal(session: ActiveSession, now: Long, forceSynchronous: Boolean) {
        val finalSession = accumulateTime(session, now)
        val listenedMs = finalSession.accumulatedListeningMs
        val song = finalSession.song

        if (listenedMs >= MIN_SESSION_LISTEN_MS && !finalSession.isCountRecorded) {
            val songId = song.id
            if (forceSynchronous || !coroutineScope.isActive) {
                runCatching {
                    runBlocking {
                        withContext(NonCancellable) {
                            musicRepository.recordSongPlayed(songId, listenedMs)
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    withContext(NonCancellable) {
                        musicRepository.recordSongPlayed(songId, listenedMs)
                    }
                }
            }
        }

        scrobbleManager?.onSessionFinalized(
            song = song,
            startedAtEpochMs = finalSession.startedAtEpochMs,
            listenedMs = listenedMs,
            trackDurationMs = song.duration
        )
    }

    private fun accumulateTime(session: ActiveSession, now: Long): ActiveSession {
        if (!session.isPlaying) {
            return session.copy(lastRecordedEpochMs = now)
        }
        val delta = (now - session.lastRecordedEpochMs).coerceAtLeast(0L)
        return session.copy(
            accumulatedListeningMs = session.accumulatedListeningMs + delta,
            lastRecordedEpochMs = now
        )
    }

    private data class ActiveSession(
        val song: Song,
        val startedAtEpochMs: Long,
        val accumulatedListeningMs: Long,
        val lastRecordedEpochMs: Long,
        val isPlaying: Boolean,
        val isCountRecorded: Boolean = false
    )

    companion object {
        private val MIN_SESSION_LISTEN_MS = TimeUnit.SECONDS.toMillis(5)
    }
}
