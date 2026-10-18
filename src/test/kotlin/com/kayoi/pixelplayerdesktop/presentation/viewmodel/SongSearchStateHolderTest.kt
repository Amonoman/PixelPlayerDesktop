package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongSearchStateHolderTest {

    private fun sampleSongs(): List<Song> {
        return listOf(
            Song(
                id = "s1",
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera"
            ),
            Song(
                id = "s2",
                title = "Starman",
                artist = "David Bowie",
                album = "Ziggy Stardust"
            ),
            Song(
                id = "s3",
                title = "Under Pressure",
                artist = "Queen & David Bowie",
                album = "Hot Space",
                navidromeId = "navi_1"
            ),
            Song(
                id = "s4",
                title = "Heroes",
                artist = "David Bowie",
                album = "Heroes",
                jellyfinId = "jelly_1"
            )
        )
    }

    @Test
    fun `matching semantics match case-insensitively on title, artist, album and blank query returns full list`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val songsFlow = MutableStateFlow(sampleSongs())
        val holder = SongSearchStateHolder(
            songsFlow = songsFlow,
            coroutineScope = backgroundScope,
            debounceMs = 150L,
            dispatcher = testDispatcher
        )

        // Initial blank query
        runCurrent()
        assertEquals(4, holder.filteredSongs.value.size)

        // Case-insensitive title match
        holder.setSearchQuery("bohemian")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, holder.filteredSongs.value.size)
        assertEquals("s1", holder.filteredSongs.value.first().id)

        // Case-insensitive artist match
        holder.setSearchQuery("BOWIE")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(3, holder.filteredSongs.value.size)

        // Case-insensitive album match
        holder.setSearchQuery("opera")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, holder.filteredSongs.value.size)
        assertEquals("s1", holder.filteredSongs.value.first().id)

        // Non-matching query
        holder.setSearchQuery("nonexistent")
        advanceTimeBy(150)
        runCurrent()
        assertTrue(holder.filteredSongs.value.isEmpty())

        // Blank query restores full list
        holder.setSearchQuery("   ")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(4, holder.filteredSongs.value.size)
    }

    @Test
    fun `storage filter correctly filters online and offline candidates with search`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val songsFlow = MutableStateFlow(sampleSongs())
        val storageFilterFlow = MutableStateFlow(StorageFilter.ALL)
        val holder = SongSearchStateHolder(
            songsFlow = songsFlow,
            storageFilterFlow = storageFilterFlow,
            coroutineScope = backgroundScope,
            debounceMs = 150L,
            dispatcher = testDispatcher
        )

        runCurrent()
        holder.setSearchQuery("Bowie")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(3, holder.filteredSongs.value.size)

        // Filter ONLINE only (navidrome or jellyfin)
        storageFilterFlow.value = StorageFilter.ONLINE
        runCurrent()
        val onlineBowie = holder.filteredSongs.value
        assertEquals(2, onlineBowie.size)
        assertTrue(onlineBowie.all { it.navidromeId != null || it.jellyfinId != null })

        // Filter OFFLINE only
        storageFilterFlow.value = StorageFilter.OFFLINE
        runCurrent()
        val offlineBowie = holder.filteredSongs.value
        assertEquals(1, offlineBowie.size)
        assertEquals("s2", offlineBowie.first().id)
    }

    @Test
    fun `stale queries are cancelled when rapid keystrokes arrive and only latest query emits`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val songsFlow = MutableStateFlow(sampleSongs())
        val holder = SongSearchStateHolder(
            songsFlow = songsFlow,
            coroutineScope = backgroundScope,
            debounceMs = 150L,
            dispatcher = testDispatcher
        )

        runCurrent()

        // Fast typing: "b", "bo", "boh" each within 50ms (before 150ms debounce)
        holder.setSearchQuery("b")
        advanceTimeBy(50)
        runCurrent()
        assertEquals(4, holder.filteredSongs.value.size) // No intermediate execution

        holder.setSearchQuery("bo")
        advanceTimeBy(50)
        runCurrent()
        assertEquals(4, holder.filteredSongs.value.size) // Still suppressed

        holder.setSearchQuery("boh")
        advanceTimeBy(150)
        runCurrent()

        // Only "boh" was evaluated
        assertEquals(1, holder.filteredSongs.value.size)
        assertEquals("Bohemian Rhapsody", holder.filteredSongs.value.first().title)
    }

    @Test
    fun `mapLatest cancels ongoing search when a new query is submitted during processing`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val songs = (1..50_000).map { i ->
            Song(
                id = "song_$i",
                title = "Track Number $i",
                artist = if (i % 2 == 0) "Even Artist" else "Odd Artist",
                album = "Album ${i / 100}"
            )
        }
        val songsFlow = MutableStateFlow(songs)
        val holder = SongSearchStateHolder(
            songsFlow = songsFlow,
            coroutineScope = backgroundScope,
            debounceMs = 0L,
            dispatcher = testDispatcher
        )

        runCurrent()

        // First query
        holder.setSearchQuery("Track Number 1")
        // Before running fully, immediately submit second query
        holder.setSearchQuery("Even Artist")
        runCurrent()

        assertEquals(25_000, holder.filteredSongs.value.size)
    }
}
