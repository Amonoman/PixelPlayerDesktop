package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.PlayerInfo
import com.kayoi.pixelplayerdesktop.data.model.QueueItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaybackStateHolder(
    private val audioEngine: AudioEngine,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val _playerInfo = MutableStateFlow(PlayerInfo())
    val playerInfo: StateFlow<PlayerInfo> = _playerInfo.asStateFlow()

    init {

        coroutineScope.launch {
            queueManager.currentSong.collect { song ->
                val isFav = if (song != null) musicRepository.isFavorite(song.id) else false
                _playerInfo.update {
                    it.copy(
                        songTitle = song?.title ?: "",
                        artistName = song?.displayArtist ?: "",
                        albumArtUri = song?.albumArtUriString,
                        totalDurationMs = song?.duration ?: 0L,
                        isFavorite = isFav
                    )
                }
                if (song != null) {
                    musicRepository.recordSongPlayed(song.id)
                    val lyrics = lyricsRepository.getLyrics(song.id, song.path, song.title, song.artist)
                    _playerInfo.update { it.copy(lyrics = lyrics) }
                }
            }
        }

        coroutineScope.launch {
            audioEngine.playbackState.collect { state ->
                _playerInfo.update { it.copy(isPlaying = state == PlaybackState.PLAYING) }
            }
        }

        coroutineScope.launch {
            audioEngine.currentPositionMs.collect { pos ->
                _playerInfo.update { it.copy(currentPositionMs = pos) }
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
            audioEngine.completionEvents.collect {
                val next = queueManager.getNextSong()
                if (next != null) {
                    audioEngine.play(next.path)
                } else {
                    audioEngine.stop()
                }
            }
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
            audioEngine.play(next.path)
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
