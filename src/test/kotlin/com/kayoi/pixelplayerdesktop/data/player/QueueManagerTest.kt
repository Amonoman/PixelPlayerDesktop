package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueManagerTest {

    private val sampleSongs = listOf(
        Song(id = "1", title = "Song 1", artist = "Artist 1", path = "/path/1.mp3"),
        Song(id = "2", title = "Song 2", artist = "Artist 2", path = "/path/2.mp3"),
        Song(id = "3", title = "Song 3", artist = "Artist 3", path = "/path/3.mp3")
    )

    @Test
    fun testQueueSettingAndSequentialPlayback() {
        val qm = QueueManager()
        val first = qm.setQueue(sampleSongs, startIndex = 0)
        assertNotNull(first)
        assertEquals("Song 1", first.title)
        assertEquals(0, qm.currentIndex.value)

        // Next song
        val second = qm.getNextSong()
        assertNotNull(second)
        assertEquals("Song 2", second.title)
        assertEquals(1, qm.currentIndex.value)

        // Next song
        val third = qm.getNextSong()
        assertNotNull(third)
        assertEquals("Song 3", third.title)
        assertEquals(2, qm.currentIndex.value)

        // End of queue with repeat OFF
        val end = qm.getNextSong()
        assertNull(end)
    }

    @Test
    fun testRepeatModes() {
        val qm = QueueManager(initialRepeatMode = 1) // REPEAT ALL
        qm.setQueue(sampleSongs, startIndex = 2) // Start at last song

        val looped = qm.getNextSong()
        assertNotNull(looped)
        assertEquals("Song 1", looped.title)
        assertEquals(0, qm.currentIndex.value)

        // Switch to REPEAT ONE
        qm.setRepeatMode(2)
        val repeatedOne = qm.getNextSong()
        assertNotNull(repeatedOne)
        assertEquals("Song 1", repeatedOne.title)
    }

    @Test
    fun testShuffleToggle() {
        val qm = QueueManager()
        qm.setQueue(sampleSongs, startIndex = 1) // Start at Song 2

        assertEquals("Song 2", qm.currentSong.value?.title)

        // Toggle shuffle ON
        val isShuffled = qm.toggleShuffle()
        assertTrue(isShuffled)
        // Current song should remain at index 0 of the shuffled queue
        assertEquals("Song 2", qm.queue.value[0].title)
        assertEquals(0, qm.currentIndex.value)
        assertEquals(3, qm.queue.value.size)

        // Toggle shuffle OFF
        qm.toggleShuffle()
        assertEquals(sampleSongs, qm.queue.value)
        assertEquals(1, qm.currentIndex.value)
    }

    @Test
    fun testQueueItemManipulation() {
        val qm = QueueManager()
        qm.setQueue(sampleSongs, startIndex = 0)

        // Add next
        val newSong = Song(id = "4", title = "Song 4", artist = "Artist 4", path = "/path/4.mp3")
        qm.playNextInQueue(newSong)

        assertEquals("Song 4", qm.queue.value[1].title)

        // Remove item
        qm.removeFromQueue(1)
        assertEquals(3, qm.queue.value.size)
        assertEquals("Song 2", qm.queue.value[1].title)
    }

    @Test
    fun testUpdateSongInQueue() {
        val qm = QueueManager()
        qm.setQueue(sampleSongs, startIndex = 1)

        val updated = sampleSongs[1].copy(title = "Song 2 Updated", artist = "New Artist")
        qm.updateSong(updated)

        assertEquals("Song 2 Updated", qm.currentSong.value?.title)
        assertEquals("New Artist", qm.currentSong.value?.artist)
        assertEquals("Song 2 Updated", qm.queue.value[1].title)
    }
}
