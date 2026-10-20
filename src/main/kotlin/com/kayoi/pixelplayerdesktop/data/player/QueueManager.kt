package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages playback queue order, track indexing, and shuffle/repeat states.
 *
 * @property initialShuffle Initial shuffle state.
 * @property initialRepeatMode Initial repeat mode (0: OFF, 1: REPEAT_ALL, 2: REPEAT_ONE).
 */
class QueueManager(
    initialShuffle: Boolean = false,
    initialRepeatMode: Int = 0
) {

    private val lock = Any()
    private var originalQueue: List<Song> = emptyList()

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

    fun setQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = _isShuffleEnabled.value): Song? = synchronized(lock) {
        if (songs.isEmpty()) {
            clearQueue()
            return null
        }

        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        val targetSong = songs[clampedIndex]

        originalQueue = songs.toList()

        if (shuffle) {
            val shuffled = songs.toMutableList()
            shuffled.removeAt(clampedIndex)
            shuffled.shuffle()
            shuffled.add(0, targetSong)
            _queue.value = shuffled.toList()
            _currentIndex.value = 0
            _currentSong.value = targetSong
        } else {
            _queue.value = songs.toList()
            _currentIndex.value = clampedIndex
            _currentSong.value = targetSong
        }

        targetSong
    }

    fun playSong(song: Song): Song = synchronized(lock) {
        val currentList = _queue.value
        val index = currentList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            _currentIndex.value = index
            _currentSong.value = currentList[index]
            currentList[index]
        } else {
            setQueue(listOf(song), 0)!!
        }
    }

    fun playAtIndex(index: Int): Song? = synchronized(lock) {
        val currentList = _queue.value
        if (index in currentList.indices) {
            _currentIndex.value = index
            _currentSong.value = currentList[index]
            currentList[index]
        } else {
            null
        }
    }

    fun getNextSong(): Song? = synchronized(lock) {
        val currentList = _queue.value
        if (currentList.isEmpty()) return null

        val currIdx = _currentIndex.value
        val repMode = _repeatMode.value

        if (repMode == 2) {
            return _currentSong.value
        }

        val nextIdx = currIdx + 1
        if (nextIdx < currentList.size) {
            _currentIndex.value = nextIdx
            _currentSong.value = currentList[nextIdx]
            currentList[nextIdx]
        } else if (repMode == 1) {
            _currentIndex.value = 0
            _currentSong.value = currentList[0]
            currentList[0]
        } else {
            null
        }
    }

    fun peekNextSong(): Song? = synchronized(lock) {
        val currentList = _queue.value
        if (currentList.isEmpty()) return null

        val currIdx = _currentIndex.value
        val repMode = _repeatMode.value

        if (repMode == 2) {
            return _currentSong.value
        }

        val nextIdx = currIdx + 1
        if (nextIdx < currentList.size) {
            currentList[nextIdx]
        } else if (repMode == 1) {
            currentList[0]
        } else {
            null
        }
    }

    fun getPreviousSong(currentPositionMs: Long = 0L): Song? = synchronized(lock) {
        val currentList = _queue.value
        if (currentList.isEmpty()) return null

        val currIdx = _currentIndex.value

        if (currentPositionMs > 3000L && _currentSong.value != null) {
            return _currentSong.value
        }

        val prevIdx = currIdx - 1
        if (prevIdx >= 0) {
            _currentIndex.value = prevIdx
            _currentSong.value = currentList[prevIdx]
            currentList[prevIdx]
        } else if (_repeatMode.value == 1) {
            val lastIdx = currentList.size - 1
            _currentIndex.value = lastIdx
            _currentSong.value = currentList[lastIdx]
            currentList[lastIdx]
        } else {
            _currentSong.value
        }
    }

    fun toggleShuffle(): Boolean = synchronized(lock) {
        val newShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newShuffle

        val curr = _currentSong.value
        if (curr == null || originalQueue.isEmpty()) {
            return newShuffle
        }

        if (newShuffle) {
            val remaining = originalQueue.filter { it.id != curr.id }.shuffled()
            val newQueue = listOf(curr) + remaining
            _queue.value = newQueue
            _currentIndex.value = 0
        } else {
            val origIndex = originalQueue.indexOfFirst { it.id == curr.id }.coerceAtLeast(0)
            _queue.value = originalQueue
            _currentIndex.value = origIndex
        }

        newShuffle
    }

    fun cycleRepeatMode(): Int = synchronized(lock) {
        val next = when (_repeatMode.value) {
            0 -> 1
            1 -> 2
            else -> 0
        }
        _repeatMode.value = next
        next
    }

    fun setRepeatMode(mode: Int) {
        synchronized(lock) {
            _repeatMode.value = mode.coerceIn(0, 2)
        }
    }

    fun addToQueue(songs: List<Song>) {
        synchronized(lock) {
            if (songs.isEmpty()) return
            if (_queue.value.isEmpty()) {
                setQueue(songs)
                return
            }
            originalQueue = originalQueue + songs
            _queue.value = _queue.value + songs
        }
    }

    fun playNextInQueue(song: Song) {
        synchronized(lock) {
            val currentList = _queue.value.toMutableList()
            val currIdx = _currentIndex.value
            if (currIdx in currentList.indices) {
                currentList.add(currIdx + 1, song)
            } else {
                currentList.add(song)
            }
            _queue.value = currentList.toList()
            originalQueue = originalQueue + song
        }
    }

    fun playNextInQueue(songs: List<Song>) {
        synchronized(lock) {
            if (songs.isEmpty()) return
            val currentList = _queue.value.toMutableList()
            val currIdx = _currentIndex.value
            val insertIdx = if (currIdx in currentList.indices) currIdx + 1 else currentList.size
            currentList.addAll(insertIdx, songs)
            _queue.value = currentList.toList()
            val origIdx = originalQueue.indexOfFirst { it.id == _currentSong.value?.id }
            val origInsert = if (origIdx >= 0) origIdx + 1 else originalQueue.size
            val mutableOrig = originalQueue.toMutableList()
            mutableOrig.addAll(origInsert, songs)
            originalQueue = mutableOrig.toList()
        }
    }

    fun removeFromQueue(index: Int): Song? = synchronized(lock) {
        val currentList = _queue.value.toMutableList()
        if (index !in currentList.indices) return _currentSong.value

        val removed = currentList.removeAt(index)
        val origIndex = originalQueue.indexOf(removed)
        if (origIndex != -1) {
            originalQueue = originalQueue.toMutableList().apply { removeAt(origIndex) }.toList()
        }
        _queue.value = currentList.toList()

        val currIdx = _currentIndex.value
        when {
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
        synchronized(lock) {
            val currentList = _queue.value.toMutableList()
            if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) return

            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _queue.value = currentList.toList()

            val curr = _currentSong.value
            if (curr != null) {
                _currentIndex.value = currentList.indexOfFirst { it.id == curr.id }
            }
        }
    }

    fun clearQueue() {
        synchronized(lock) {
            originalQueue = emptyList()
            _queue.value = emptyList()
            _currentIndex.value = -1
            _currentSong.value = null
        }
    }

    fun updateSongFavorite(songId: String, isFav: Boolean) {
        synchronized(lock) {
            _queue.value = _queue.value.map {
                if (it.id == songId) it.copy(isFavorite = isFav) else it
            }
            originalQueue = originalQueue.map {
                if (it.id == songId) it.copy(isFavorite = isFav) else it
            }
            val curr = _currentSong.value
            if (curr != null && curr.id == songId) {
                _currentSong.value = curr.copy(isFavorite = isFav)
            }
        }
    }

    fun updateSong(updatedSong: Song) {
        synchronized(lock) {
            _queue.value = _queue.value.map {
                if (it.id == updatedSong.id) updatedSong else it
            }
            originalQueue = originalQueue.map {
                if (it.id == updatedSong.id) updatedSong else it
            }
            val curr = _currentSong.value
            if (curr != null && curr.id == updatedSong.id) {
                _currentSong.value = updatedSong
            }
        }
    }
}
