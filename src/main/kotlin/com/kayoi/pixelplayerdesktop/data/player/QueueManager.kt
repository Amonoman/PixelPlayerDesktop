package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

/**
 * Manages playback queue order, track indexing, and shuffle/repeat states.
 *
 * @property initialShuffle Initial shuffle state.
 * @property initialRepeatMode Initial repeat mode (0: OFF, 1: REPEAT_ALL, 2: REPEAT_ONE).
 */
class QueueManager(
    initialShuffle: Boolean = false,
    initialRepeatMode: Int = 0 // 0: OFF, 1: ALL, 2: ONE
) {

    private val _originalQueue = mutableListOf<Song>()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(initialShuffle)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(initialRepeatMode)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    fun setQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = _isShuffleEnabled.value): Song? {
        if (songs.isEmpty()) {
            clearQueue()
            return null
        }

        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val targetSong = songs[clampedIndex]

        _originalQueue.clear()
        _originalQueue.addAll(songs)

        if (shuffle) {
            val shuffled = songs.toMutableList()
            shuffled.removeAt(clampedIndex)
            Collections.shuffle(shuffled)
            shuffled.add(0, targetSong)
            _queue.value = shuffled
            _currentIndex.value = 0
            _currentSong.value = targetSong
        } else {
            _queue.value = songs
            _currentIndex.value = clampedIndex
            _currentSong.value = targetSong
        }

        return targetSong
    }

    fun playSong(song: Song): Song {
        val currentList = _queue.value
        val index = currentList.indexOfFirst { it.id == song.id }
        return if (index != -1) {
            _currentIndex.value = index
            _currentSong.value = currentList[index]
            currentList[index]
        } else {
            setQueue(listOf(song), 0)!!
        }
    }

    fun playAtIndex(index: Int): Song? {
        val currentList = _queue.value
        if (index in currentList.indices) {
            _currentIndex.value = index
            _currentSong.value = currentList[index]
            return currentList[index]
        }
        return null
    }

    fun getNextSong(): Song? {
        val currentList = _queue.value
        if (currentList.isEmpty()) return null

        val currIdx = _currentIndex.value
        val repMode = _repeatMode.value

        if (repMode == 2) { // REPEAT ONE
            return _currentSong.value
        }

        val nextIdx = currIdx + 1
        return if (nextIdx < currentList.size) {
            _currentIndex.value = nextIdx
            _currentSong.value = currentList[nextIdx]
            currentList[nextIdx]
        } else if (repMode == 1) { // REPEAT ALL
            _currentIndex.value = 0
            _currentSong.value = currentList[0]
            currentList[0]
        } else {
            null
        }
    }

    fun getPreviousSong(currentPositionMs: Long = 0L): Song? {
        val currentList = _queue.value
        if (currentList.isEmpty()) return null

        val currIdx = _currentIndex.value

        // If song has played for more than 3 seconds, restart current song
        if (currentPositionMs > 3000L && _currentSong.value != null) {
            return _currentSong.value
        }

        val prevIdx = currIdx - 1
        return if (prevIdx >= 0) {
            _currentIndex.value = prevIdx
            _currentSong.value = currentList[prevIdx]
            currentList[prevIdx]
        } else if (_repeatMode.value == 1) { // REPEAT ALL
            val lastIdx = currentList.size - 1
            _currentIndex.value = lastIdx
            _currentSong.value = currentList[lastIdx]
            currentList[lastIdx]
        } else {
            _currentSong.value
        }
    }

    fun toggleShuffle(): Boolean {
        val newShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newShuffle

        val curr = _currentSong.value
        if (curr == null || _originalQueue.isEmpty()) {
            return newShuffle
        }

        if (newShuffle) {
            val remaining = _originalQueue.filter { it.id != curr.id }.shuffled()
            val newQueue = listOf(curr) + remaining
            _queue.value = newQueue
            _currentIndex.value = 0
        } else {
            val origIndex = _originalQueue.indexOfFirst { it.id == curr.id }.coerceAtLeast(0)
            _queue.value = _originalQueue.toList()
            _currentIndex.value = origIndex
        }

        return newShuffle
    }

    fun cycleRepeatMode(): Int {
        val next = when (_repeatMode.value) {
            0 -> 1 // ALL
            1 -> 2 // ONE
            else -> 0 // OFF
        }
        _repeatMode.value = next
        return next
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode.coerceIn(0, 2)
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (_queue.value.isEmpty()) {
            setQueue(songs)
            return
        }
        _originalQueue.addAll(songs)
        _queue.value = _queue.value + songs
    }

    fun playNextInQueue(song: Song) {
        val currentList = _queue.value.toMutableList()
        val currIdx = _currentIndex.value
        if (currIdx in currentList.indices) {
            currentList.add(currIdx + 1, song)
        } else {
            currentList.add(song)
        }
        _queue.value = currentList
        _originalQueue.add(song)
    }

    fun removeFromQueue(index: Int): Song? {
        val currentList = _queue.value.toMutableList()
        if (index !in currentList.indices) return _currentSong.value

        val removed = currentList.removeAt(index)
        _originalQueue.remove(removed)
        _queue.value = currentList

        val currIdx = _currentIndex.value
        return when {
            index < currIdx -> {
                _currentIndex.value = currIdx - 1
                _currentSong.value
            }
            index == currIdx -> {
                if (currentList.isEmpty()) {
                    clearQueue()
                    null
                } else {
                    val nextIdx = currIdx.coerceAtMost(currentList.size - 1)
                    _currentIndex.value = nextIdx
                    _currentSong.value = currentList[nextIdx]
                    currentList[nextIdx]
                }
            }
            else -> _currentSong.value
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _queue.value = currentList

        val curr = _currentSong.value
        if (curr != null) {
            _currentIndex.value = currentList.indexOfFirst { it.id == curr.id }
        }
    }

    fun clearQueue() {
        _originalQueue.clear()
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentSong.value = null
    }

    fun updateSongFavorite(songId: String, isFav: Boolean) {
        _queue.value = _queue.value.map {
            if (it.id == songId) it.copy(isFavorite = isFav) else it
        }
        _originalQueue.replaceAll {
            if (it.id == songId) it.copy(isFavorite = isFav) else it
        }
        val curr = _currentSong.value
        if (curr != null && curr.id == songId) {
            _currentSong.value = curr.copy(isFavorite = isFav)
        }
    }
}
