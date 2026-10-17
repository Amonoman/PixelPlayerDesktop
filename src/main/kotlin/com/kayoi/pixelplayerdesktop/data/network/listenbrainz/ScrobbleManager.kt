package com.kayoi.pixelplayerdesktop.data.network.listenbrainz

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import com.kayoi.pixelplayerdesktop.domain.repository.ListenBrainzRepository
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ScrobbleManager(
    private val listenBrainzRepository: ListenBrainzRepository,
    private val navidromeRepository: NavidromeRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private var lastPlayingNowSongId: String? = null
    private var lastPlayingNowEpochMs: Long = 0L

    fun onPlayingNow(song: Song) {
        if (song.navidromeId != null) {
            coroutineScope.launch {
                navidromeRepository.scrobble(song.navidromeId, submission = false)
            }
        }

        if (!listenBrainzRepository.hasToken()) return
        if (song.title.isBlank() || song.artist.isBlank()) return
        if (!isSourceEnabled(song)) return

        val now = System.currentTimeMillis()
        synchronized(this) {
            if (song.id == lastPlayingNowSongId && now - lastPlayingNowEpochMs < PLAYING_NOW_DEBOUNCE_MS) {
                return
            }
            lastPlayingNowSongId = song.id
            lastPlayingNowEpochMs = now
        }

        coroutineScope.launch {
            listenBrainzRepository.submitPlayingNow(
                trackName = song.title,
                artistName = song.displayArtist,
                releaseName = song.album.takeIf { it.isNotBlank() },
                durationMs = song.duration.takeIf { it > 0 },
                recordingMbid = null
            )
        }
    }

    fun onSessionFinalized(
        song: Song,
        startedAtEpochMs: Long,
        listenedMs: Long,
        trackDurationMs: Long
    ) {
        if (song.navidromeId != null && meetsListenThreshold(listenedMs, trackDurationMs)) {
            coroutineScope.launch {
                navidromeRepository.scrobble(song.navidromeId, submission = true)
            }
        }

        if (!listenBrainzRepository.hasToken()) return
        if (song.title.isBlank() || song.artist.isBlank()) return
        if (!isSourceEnabled(song)) return
        if (!meetsListenThreshold(listenedMs, trackDurationMs)) return

        coroutineScope.launch {
            val listen = ListenBrainzPendingListen(
                listenedAtMs = startedAtEpochMs,
                trackName = song.title,
                artistName = song.displayArtist,
                releaseName = song.album.takeIf { it.isNotBlank() },
                durationMs = song.duration.takeIf { it > 0 },
                recordingMbid = null,
                source = resolveSource(song),
                createdAtMs = System.currentTimeMillis()
            )
            listenBrainzRepository.enqueueListen(listen)
        }
    }

    private fun isSourceEnabled(song: Song): Boolean {
        val settings = settingsRepository.settingsFlow.value
        if (!settings.listenBrainzEnabled) return false
        return when {
            song.navidromeId != null -> settings.listenBrainzScrobbleNavidrome
            song.jellyfinId != null -> settings.listenBrainzScrobbleJellyfin
            else -> settings.listenBrainzScrobbleLocal
        }
    }

    private fun resolveSource(song: Song): String {
        return when {
            song.navidromeId != null -> "NAVIDROME"
            song.jellyfinId != null -> "JELLYFIN"
            else -> "LOCAL"
        }
    }

    companion object {
        private const val PLAYING_NOW_DEBOUNCE_MS = 30_000L
        private val FOUR_MINUTES_MS = TimeUnit.MINUTES.toMillis(4)

        fun meetsListenThreshold(listenedMs: Long, trackDurationMs: Long): Boolean {
            val required = if (trackDurationMs > 0) {
                minOf(FOUR_MINUTES_MS, trackDurationMs / 2)
            } else {
                FOUR_MINUTES_MS
            }
            return listenedMs >= required
        }
    }
}
