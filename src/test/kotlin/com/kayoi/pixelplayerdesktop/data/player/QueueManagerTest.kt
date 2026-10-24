package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    fun testMoveQueueItemWithDuplicates() {
        val qm = QueueManager()
        val duplicateList = listOf(
            Song(id = "dup_a", title = "Duplicate A", artist = "Artist", path = "/path/1.mp3"),
            Song(id = "song_b", title = "Song B", artist = "Artist", path = "/path/2.mp3"),
            Song(id = "dup_a", title = "Duplicate A", artist = "Artist", path = "/path/3.mp3"),
            Song(id = "song_c", title = "Song C", artist = "Artist", path = "/path/4.mp3")
        )
        qm.setQueue(duplicateList, startIndex = 2)
        assertEquals(2, qm.currentIndex.value)

        // Move playing duplicate from index 2 to index 0 (before the first duplicate)
        qm.moveQueueItem(2, 0)
        assertEquals(0, qm.currentIndex.value)
        assertEquals("dup_a", qm.currentSong.value?.id)

        // Move playing duplicate from index 0 to index 3 (after the other duplicate which is now at 1)
        qm.moveQueueItem(0, 3)
        assertEquals(3, qm.currentIndex.value)
        assertEquals("dup_a", qm.currentSong.value?.id)

        // Move non-playing item (index 0) to index 2
        qm.moveQueueItem(0, 2)
        // Playing item was at 3, moving 0 to 2 shifts 3 to 2? No, 0 was removed (items shifted left), then inserted at 2 (before 3), so index 3 remains 3
        assertEquals(3, qm.currentIndex.value)
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

    @Test
    fun testPeekNextSong() {
        val qm = QueueManager()
        qm.setQueue(sampleSongs, startIndex = 0)

        val peeked = qm.peekNextSong()
        assertNotNull(peeked)
        assertEquals("Song 2", peeked.title)
        assertEquals(0, qm.currentIndex.value)
    }

    @Test
    fun testConcurrentOperationsMaintainConsistency() = runBlocking {
        val qm = QueueManager()
        val initialSongs = (1..20).map { i ->
            Song(id = "init_$i", title = "Initial $i", artist = "Artist", path = "/path/$i.mp3")
        }
        qm.setQueue(initialSongs, startIndex = 0)

        val iterations = 100
        val jobs = listOf(
            launch(Dispatchers.Default) {
                repeat(iterations) { i ->
                    qm.addToQueue(listOf(Song(id = "add_$i", title = "Add $i", artist = "Artist", path = "/path/add_$i.mp3")))
                }
            },
            launch(Dispatchers.Default) {
                repeat(iterations) {
                    qm.toggleShuffle()
                }
            },
            launch(Dispatchers.Default) {
                repeat(iterations) {
                    qm.getNextSong()
                }
            },
            launch(Dispatchers.Default) {
                repeat(iterations) {
                    val currSize = qm.queue.value.size
                    if (currSize > 1) {
                        qm.removeFromQueue((0 until currSize).random())
                    }
                }
            },
            launch(Dispatchers.Default) {
                repeat(iterations) { i ->
                    qm.playNextInQueue(Song(id = "next_$i", title = "Next $i", artist = "Artist", path = "/path/next_$i.mp3"))
                }
            }
        )

        jobs.joinAll()

        val finalQueue = qm.queue.value
        val finalIndex = qm.currentIndex.value
        val finalSong = qm.currentSong.value

        if (finalQueue.isEmpty()) {
            assertEquals(-1, finalIndex)
            assertNull(finalSong)
        } else {
            assertTrue(finalIndex in finalQueue.indices)
            assertEquals(finalQueue[finalIndex].id, finalSong?.id)
        }
    }
}
