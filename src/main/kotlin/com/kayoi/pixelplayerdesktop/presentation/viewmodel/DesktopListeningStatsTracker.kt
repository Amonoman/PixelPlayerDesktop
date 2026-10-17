package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.ScrobbleManager
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class DesktopListeningStatsTracker(
    private val musicRepository: MusicRepository,
    private val scrobbleManager: ScrobbleManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private var currentSession: ActiveSession? = null

    init {
        runCatching {
            Runtime.getRuntime().addShutdownHook(Thread {
                finalizeCurrentSession(forceSynchronous = true)
            })
        }
    }

    @Synchronized
    fun onSongChanged(song: Song?, isPlaying: Boolean) {
        finalizeCurrentSession()
        if (song == null) return

        val now = System.currentTimeMillis()
        currentSession = ActiveSession(
            song = song,
            startedAtEpochMs = now,
            accumulatedListeningMs = 0L,
            lastRecordedEpochMs = now,
            isPlaying = isPlaying
        )

        if (isPlaying) {
            scrobbleManager?.onPlayingNow(song)
        }
    }

    @Synchronized
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        val session = currentSession ?: return
        val now = System.currentTimeMillis()
        accumulateTime(session, now)
        val wasPlaying = session.isPlaying
        session.isPlaying = isPlaying
        session.lastRecordedEpochMs = now

        if (!wasPlaying && isPlaying) {
            scrobbleManager?.onPlayingNow(session.song)
        }
    }

    @Synchronized
    fun onPlaybackStopped() {
        finalizeCurrentSession()
    }

    @Synchronized
    fun finalizeCurrentSession(forceSynchronous: Boolean = false) {
        val session = currentSession ?: return
        val now = System.currentTimeMillis()
        accumulateTime(session, now)

        val listenedMs = session.accumulatedListeningMs
        val song = session.song
        if (listenedMs >= MIN_SESSION_LISTEN_MS) {
            val songId = song.id
            if (forceSynchronous) {
                runCatching {
                    runBlocking {
                        musicRepository.recordSongPlayed(songId, listenedMs)
                    }
                }
            } else {
                coroutineScope.launch {
                    musicRepository.recordSongPlayed(songId, listenedMs)
                }
            }
        }

        scrobbleManager?.onSessionFinalized(
            song = song,
            startedAtEpochMs = session.startedAtEpochMs,
            listenedMs = listenedMs,
            trackDurationMs = song.duration
        )

        currentSession = null
    }

    private fun accumulateTime(session: ActiveSession, now: Long) {
        if (!session.isPlaying) return
        val delta = (now - session.lastRecordedEpochMs).coerceAtLeast(0L)
        if (delta > 0L) {
            session.accumulatedListeningMs += delta
        }
    }

    private data class ActiveSession(
        val song: Song,
        val startedAtEpochMs: Long,
        var accumulatedListeningMs: Long,
        var lastRecordedEpochMs: Long,
        var isPlaying: Boolean
    )

    companion object {
        private val MIN_SESSION_LISTEN_MS = TimeUnit.SECONDS.toMillis(5)
    }
}
