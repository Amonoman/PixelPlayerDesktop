package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.PlayerInfo
import com.kayoi.pixelplayerdesktop.data.model.QueueItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.LrcParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlaybackStateHolder(
    private val audioEngine: AudioEngine,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    val replayGainEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.replayGainEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val replayGainMode: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.replayGainMode }
        .stateIn(coroutineScope, SharingStarted.Eagerly, "track")

    val crossfadeEnabled: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.crossfadeEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val crossfadeDurationSeconds: StateFlow<Int> = settingsRepository.settingsFlow
        .map { it.crossfadeDurationSeconds }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 3)

    val showQueueHistory: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.showQueueHistory }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val closeToTray: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.closeToTray }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    fun setReplayGainEnabled(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setReplayGainEnabled(enabled) }
    }

    fun setReplayGainMode(mode: String) {
        coroutineScope.launch { settingsRepository.setReplayGainMode(mode) }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDurationSeconds(durationSeconds: Int) {
        coroutineScope.launch { settingsRepository.setCrossfadeDurationSeconds(durationSeconds) }
    }

    fun setShowQueueHistory(show: Boolean) {
        coroutineScope.launch { settingsRepository.setShowQueueHistory(show) }
    }

    fun setCloseToTray(enabled: Boolean) {
        coroutineScope.launch { settingsRepository.setCloseToTray(enabled) }
    }

    private val _playerInfo = MutableStateFlow(PlayerInfo())
    val playerInfo: StateFlow<PlayerInfo> = _playerInfo.asStateFlow()

    val currentPositionMs: StateFlow<Long> = audioEngine.currentPositionMs

    private val _lyricsSearchUiState = MutableStateFlow<LyricsSearchUiState>(LyricsSearchUiState.Idle)
    val lyricsSearchUiState: StateFlow<LyricsSearchUiState> = _lyricsSearchUiState.asStateFlow()

    fun resetLyricsSearchState() {
        _lyricsSearchUiState.value = LyricsSearchUiState.Idle
    }

    fun searchLyrics(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            _lyricsSearchUiState.value = LyricsSearchUiState.Loading
            try {
                val results = lyricsRepository.searchOnlineLyrics(query)
                if (results.isEmpty()) {
                    _lyricsSearchUiState.value = LyricsSearchUiState.NotFound(query)
                } else {
                    _lyricsSearchUiState.value = LyricsSearchUiState.PickResult(query, results)
                }
            } catch (e: Exception) {
                _lyricsSearchUiState.value = LyricsSearchUiState.Error(e.message ?: "Failed to search lyrics")
            }
        }
    }

    fun applyLyrics(lyrics: Lyrics) {
        val currentSong = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            lyricsRepository.saveLyrics(currentSong.id, lyrics)
            _playerInfo.update { it.copy(lyrics = lyrics) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun importLyricsFromFile(file: File) {
        val currentSong = queueManager.currentSong.value ?: return
        if (!file.exists() || !file.isFile) return
        coroutineScope.launch {
            val content = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@launch
            val parsed = LrcParser.parse(content).copy(areFromRemote = false)
            lyricsRepository.saveLyrics(currentSong.id, parsed)
            _playerInfo.update { it.copy(lyrics = parsed) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun resetCurrentSongLyrics() {
        val currentSong = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            lyricsRepository.deleteLyrics(currentSong.id)
            settingsRepository.setLyricsSyncOffset(currentSong.id, 0)
            val fresh = lyricsRepository.getLyrics(
                songId = currentSong.id,
                songPath = currentSong.path,
                title = currentSong.title,
                artist = currentSong.artist
            )
            _playerInfo.update { it.copy(lyrics = fresh, lyricsSyncOffsetMs = 0) }
            _lyricsSearchUiState.value = LyricsSearchUiState.Idle
        }
    }

    fun setLyricsSyncOffset(offsetMs: Int) {
        val currentSong = queueManager.currentSong.value ?: return
        _playerInfo.update { it.copy(lyricsSyncOffsetMs = offsetMs) }
        coroutineScope.launch {
            settingsRepository.setLyricsSyncOffset(currentSong.id, offsetMs)
        }
    }

    fun saveLyricsBesideCurrentSong(preferSynced: Boolean = true): File? {
        val song = queueManager.currentSong.value ?: return null
        val lyrics = _playerInfo.value.lyrics ?: return null
        val content = LrcParser.toLrcString(lyrics, preferSynced)
        if (content.isBlank()) return null
        return runCatching {
            val songFile = File(song.path)
            val lrcFile = File(songFile.parentFile, "${songFile.nameWithoutExtension}.lrc")
            lrcFile.writeText(content, Charsets.UTF_8)
            lrcFile
        }.getOrNull()
    }

    fun exportLyricsToFile(targetFile: File, preferSynced: Boolean = true): Boolean {
        val lyrics = _playerInfo.value.lyrics ?: return false
        val content = LrcParser.toLrcString(lyrics, preferSynced)
        if (content.isBlank()) return false
        return runCatching {
            targetFile.writeText(content, Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    fun onSongMetadataUpdated(updatedSong: Song) {
        queueManager.updateSong(updatedSong)
        val current = queueManager.currentSong.value
        if (current != null && current.id == updatedSong.id) {
            _playerInfo.update {
                it.copy(
                    songTitle = updatedSong.title,
                    artistName = updatedSong.displayArtist,
                    albumArtUri = updatedSong.albumArtUriString,
                    totalDurationMs = updatedSong.duration
                )
            }
        }
    }

    fun onSongsMetadataUpdated(updatedSongs: List<Song>) {
        for (song in updatedSongs) {
            onSongMetadataUpdated(song)
        }
    }

    init {

        coroutineScope.launch {
            queueManager.currentSong.collectLatest { song ->
                val isFav = if (song != null) musicRepository.isFavorite(song.id) else false
                val offset = if (song != null) {
                    settingsRepository.settingsFlow.value.lyricsSyncOffsets[song.id] ?: 0
                } else 0
                _playerInfo.update {
                    it.copy(
                        songTitle = song?.title ?: "",
                        artistName = song?.displayArtist ?: "",
                        albumArtUri = song?.albumArtUriString,
                        totalDurationMs = song?.duration ?: 0L,
                        isFavorite = isFav,
                        lyricsSyncOffsetMs = offset,
                        lyrics = null,
                        isLoadingLyrics = song != null
                    )
                }
                if (song != null) {
                    musicRepository.recordSongPlayed(song.id)
                    scheduleTransitionFor(song)
                    val lyrics = lyricsRepository.getLyrics(song.id, song.path, song.title, song.artist)
                    _playerInfo.update {
                        it.copy(
                            lyrics = lyrics,
                            isLoadingLyrics = false
                        )
                    }
                } else {
                    transitionSchedulerJob?.cancel()
                    _playerInfo.update { it.copy(isLoadingLyrics = false) }
                }
            }
        }

        coroutineScope.launch {
            audioEngine.playbackState.collect { state ->
                _playerInfo.update { it.copy(isPlaying = state == PlaybackState.PLAYING) }
            }
        }



        coroutineScope.launch {
            audioEngine.durationMs.collect { dur ->
                if (dur > 0) {
                    _playerInfo.update { it.copy(totalDurationMs = dur) }
                }
            }
        }

        coroutineScope.launch {
            queueManager.queue.collect { q ->
                val items = q.map { QueueItem(id = it.id.hashCode().toLong(), albumArtUri = it.albumArtUriString) }
                _playerInfo.update { it.copy(queue = items) }
            }
        }

        coroutineScope.launch {
            queueManager.isShuffleEnabled.collect { shuffle ->
                _playerInfo.update { it.copy(isShuffleEnabled = shuffle) }
            }
        }

        coroutineScope.launch {
            queueManager.repeatMode.collect { rep ->
                _playerInfo.update { it.copy(repeatMode = rep) }
            }
        }

        coroutineScope.launch {
            audioEngine.volume.collect { vol ->
                _playerInfo.update { it.copy(volume = vol) }
            }
        }

        coroutineScope.launch {
            audioEngine.isMuted.collect { muted ->
                _playerInfo.update { it.copy(isMuted = muted) }
            }
        }

        coroutineScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                audioEngine.setReplayGain(settings.replayGainEnabled, settings.replayGainMode)
                val curr = queueManager.currentSong.value
                if (curr != null) {
                    if (settings.crossfadeEnabled) {
                        scheduleTransitionFor(curr)
                    } else {
                        transitionSchedulerJob?.cancel()
                    }
                }
            }
        }

        coroutineScope.launch {
            audioEngine.completionEvents.collect {
                transitionSchedulerJob?.cancel()
                val next = queueManager.getNextSong()
                if (next != null) {
                    audioEngine.play(next.path)
                } else {
                    audioEngine.stop()
                }
            }
        }
    }

    private var transitionSchedulerJob: Job? = null

    private fun scheduleTransitionFor(song: Song) {
        transitionSchedulerJob?.cancel()
        if (!crossfadeEnabled.value) return

        transitionSchedulerJob = coroutineScope.launch {
            while (isActive && audioEngine.isCrossfading.value) {
                delay(200)
            }
            if (!isActive || queueManager.currentSong.value?.id != song.id) return@launch

            delay(1500)
            if (!isActive || queueManager.currentSong.value?.id != song.id) return@launch

            var dur = audioEngine.durationMs.value
            if (dur <= 0) dur = song.duration
            var waitCount = 0
            while (isActive && dur <= 0 && waitCount < 20) {
                delay(250)
                dur = audioEngine.durationMs.value
                waitCount++
            }
            if (!isActive || dur <= 0 || queueManager.currentSong.value?.id != song.id) return@launch

            val fadeDurationMs = (crossfadeDurationSeconds.value * 1000L).coerceIn(500L, 12000L)
            val minTrackLength = fadeDurationMs + 1000L
            if (dur < minTrackLength) return@launch

            val transitionPoint = dur - fadeDurationMs

            val currentPos = audioEngine.currentPositionMs.value
            if (currentPos >= transitionPoint) {
                val remaining = (dur - currentPos).coerceAtLeast(0L)
                if (remaining > 500L) {
                    val nextSong = queueManager.peekNextSong()
                    if (nextSong != null) {
                        queueManager.getNextSong()
                        audioEngine.crossfadeTo(nextSong.path, remaining.coerceAtMost(fadeDurationMs))
                    }
                }
                return@launch
            }

            while (isActive &&
                queueManager.currentSong.value?.id == song.id &&
                audioEngine.playbackState.value == PlaybackState.PLAYING
            ) {
                val pos = audioEngine.currentPositionMs.value
                val remaining = transitionPoint - pos
                if (remaining <= 0) break

                val sleep = when {
                    remaining > 5000 -> 1000L
                    remaining > 1000 -> 250L
                    else -> 50L
                }.coerceAtMost(remaining).coerceAtLeast(20L)

                delay(sleep)
            }

            if (!isActive ||
                queueManager.currentSong.value?.id != song.id ||
                audioEngine.playbackState.value != PlaybackState.PLAYING
            ) return@launch

            val nextSong = queueManager.peekNextSong() ?: return@launch
            queueManager.getNextSong()
            audioEngine.crossfadeTo(nextSong.path, fadeDurationMs)
        }
    }

    fun playSong(song: Song) {
        queueManager.playSong(song)
        audioEngine.play(song.path)
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val target = queueManager.setQueue(songs, startIndex)
        if (target != null) {
            audioEngine.play(target.path)
        }
    }

    fun shuffleSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSongs(shuffled, 0)
    }

    fun togglePlayPause() {
        if (audioEngine.playbackState.value == PlaybackState.PLAYING) {
            audioEngine.pause()
        } else {
            val currSong = queueManager.currentSong.value
            if (currSong != null) {
                audioEngine.play()
            }
        }
    }

    fun playNext() {
        val next = queueManager.getNextSong()
        if (next != null) {
            if (crossfadeEnabled.value && audioEngine.playbackState.value == PlaybackState.PLAYING) {
                val fadeMs = (crossfadeDurationSeconds.value * 1000L).coerceIn(500L, 12000L)
                audioEngine.crossfadeTo(next.path, fadeMs)
            } else {
                audioEngine.play(next.path)
            }
        } else {
            val current = queueManager.currentSong.value
            if (current != null) {
                audioEngine.play(current.path)
            } else {
                audioEngine.stop()
            }
        }
    }

    fun playPrevious() {
        val prev = queueManager.getPreviousSong(audioEngine.currentPositionMs.value)
        if (prev != null) {
            audioEngine.play(prev.path)
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        val song = queueManager.currentSong.value
        if (song != null && crossfadeEnabled.value) {
            scheduleTransitionFor(song)
        }
    }

    fun setVolume(vol: Float) {
        audioEngine.setVolume(vol)
        coroutineScope.launch {
            settingsRepository.setVolume(vol)
        }
    }

    fun toggleMute() {
        val newMute = !audioEngine.isMuted.value
        audioEngine.setMute(newMute)
        coroutineScope.launch {
            settingsRepository.setMuted(newMute)
        }
    }

    fun toggleShuffle() {
        val newShuffle = queueManager.toggleShuffle()
        coroutineScope.launch {
            settingsRepository.setShuffleEnabled(newShuffle)
        }
    }

    fun cycleRepeatMode() {
        val newMode = queueManager.cycleRepeatMode()
        coroutineScope.launch {
            settingsRepository.setRepeatMode(newMode)
        }
    }

    fun toggleFavoriteCurrentSong() {
        val curr = queueManager.currentSong.value ?: return
        coroutineScope.launch {
            val newFav = musicRepository.toggleFavorite(curr.id)
            queueManager.updateSongFavorite(curr.id, newFav)
            _playerInfo.update { it.copy(isFavorite = newFav) }
        }
    }

    fun toggleFavorite(song: Song) {
        coroutineScope.launch {
            val newFav = musicRepository.toggleFavorite(song.id)
            queueManager.updateSongFavorite(song.id, newFav)
            if (queueManager.currentSong.value?.id == song.id) {
                _playerInfo.update { it.copy(isFavorite = newFav) }
            }
        }
    }

    private var lastDismissedQueue: List<Song> = emptyList()
    private var lastDismissedIndex: Int = 0
    private var lastDismissedPositionMs: Long = 0L

    fun stopPlayback() {
        lastDismissedQueue = queueManager.queue.value
        lastDismissedIndex = queueManager.currentIndex.value
        lastDismissedPositionMs = audioEngine.currentPositionMs.value
        audioEngine.stop()
        queueManager.clearQueue()
        _playerInfo.update { PlayerInfo() }
    }

    fun undoStopPlayback() {
        if (lastDismissedQueue.isNotEmpty()) {
            val targetIndex = lastDismissedIndex.coerceIn(0, lastDismissedQueue.size - 1)
            val pos = lastDismissedPositionMs
            playSongs(lastDismissedQueue, targetIndex)
            if (pos > 0) {
                audioEngine.seekTo(pos)
            }
            lastDismissedQueue = emptyList()
        }
    }

    fun removeFromQueue(index: Int) {
        val currIdx = queueManager.currentIndex.value
        val isRemovingCurrent = (index == currIdx)
        val nextSong = queueManager.removeFromQueue(index)

        if (isRemovingCurrent) {
            if (nextSong != null) {
                playSong(nextSong)
            } else {
                stopPlayback()
            }
        }
    }

    fun removeSongFromQueue(songId: String) {
        val idx = queueManager.queue.value.indexOfFirst { it.id == songId }
        if (idx >= 0) {
            removeFromQueue(idx)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        queueManager.moveQueueItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        audioEngine.stop()
        queueManager.clearQueue()
        _playerInfo.update { PlayerInfo() }
    }

    fun addSongToQueue(song: Song) {
        queueManager.addToQueue(listOf(song))
    }

    fun playSongNext(song: Song) {
        queueManager.playNextInQueue(song)
    }

    fun playSongAtIndex(index: Int) {
        val target = queueManager.playAtIndex(index)
        if (target != null) {
            audioEngine.play(target.path)
        }
    }

    val currentSong: StateFlow<Song?> = queueManager.currentSong
    val queue: StateFlow<List<Song>> = queueManager.queue
    val playbackState: StateFlow<PlaybackState> = audioEngine.playbackState
}
