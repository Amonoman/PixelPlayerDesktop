package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumAndArtistSearchStateHolderTest {

    @Test
    fun `album search holder filters by title and artist case-insensitively with storage filter`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val albums = listOf(
            Album(id = 1L, title = "A Night at the Opera", artist = "Queen", year = 1975),
            Album(id = 2L, title = "Heroes", artist = "David Bowie", year = 1977),
            Album(id = 3L, title = "The Dark Side of the Moon", artist = "Pink Floyd", year = 1973)
        )
        val songs = listOf(
            Song(id = "s1", title = "Bohemian Rhapsody", artist = "Queen", album = "A Night at the Opera", albumId = 1L, navidromeId = "online_1"),
            Song(id = "s2", title = "Heroes", artist = "David Bowie", album = "Heroes", albumId = 2L),
            Song(id = "s3", title = "Money", artist = "Pink Floyd", album = "The Dark Side of the Moon", albumId = 3L)
        )

        val albumsFlow = MutableStateFlow(albums)
        val songsFlow = MutableStateFlow(songs)
        val storageFilterFlow = MutableStateFlow(StorageFilter.ALL)

        val holder = AlbumSearchStateHolder(
            albumsFlow = albumsFlow,
            songsFlow = songsFlow,
            storageFilterFlow = storageFilterFlow,
            coroutineScope = backgroundScope,
            debounceMs = 150L,
            dispatcher = testDispatcher
        )

        runCurrent()
        assertEquals(3, holder.filteredAlbums.value.size)

        // Title match
        holder.setSearchQuery("opera")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, holder.filteredAlbums.value.size)
        assertEquals("A Night at the Opera", holder.filteredAlbums.value.first().title)

        // Artist match
        holder.setSearchQuery("BOWIE")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, holder.filteredAlbums.value.size)
        assertEquals("Heroes", holder.filteredAlbums.value.first().title)

        // Storage filter ONLINE (only Album 1 has online song)
        holder.setSearchQuery("")
        advanceTimeBy(150)
        storageFilterFlow.value = StorageFilter.ONLINE
        runCurrent()
        assertEquals(1, holder.filteredAlbums.value.size)
        assertEquals(1L, holder.filteredAlbums.value.first().id)

        // Storage filter OFFLINE (Albums 2 & 3)
        storageFilterFlow.value = StorageFilter.OFFLINE
        runCurrent()
        assertEquals(2, holder.filteredAlbums.value.size)
    }

    @Test
    fun `artist search holder filters by name case-insensitively with storage filter`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val artists = listOf(
            Artist(id = 10L, name = "Queen", songCount = 1),
            Artist(id = 20L, name = "David Bowie", songCount = 1),
            Artist(id = 30L, name = "Pink Floyd", songCount = 1)
        )
        val songs = listOf(
            Song(id = "s1", title = "Song 1", artist = "Queen", artistId = 10L, navidromeId = "online_1"),
            Song(id = "s2", title = "Song 2", artist = "David Bowie", artistId = 20L),
            Song(id = "s3", title = "Song 3", artist = "Pink Floyd", artistId = 30L)
        )

        val artistsFlow = MutableStateFlow(artists)
        val songsFlow = MutableStateFlow(songs)
        val storageFilterFlow = MutableStateFlow(StorageFilter.ALL)

        val holder = ArtistSearchStateHolder(
            artistsFlow = artistsFlow,
            songsFlow = songsFlow,
            storageFilterFlow = storageFilterFlow,
            coroutineScope = backgroundScope,
            debounceMs = 150L,
            dispatcher = testDispatcher
        )

        runCurrent()
        assertEquals(3, holder.filteredArtists.value.size)

        // Name match
        holder.setSearchQuery("pink")
        advanceTimeBy(150)
        runCurrent()
        assertEquals(1, holder.filteredArtists.value.size)
        assertEquals("Pink Floyd", holder.filteredArtists.value.first().name)

        // Storage filter ONLINE (only Queen has online song)
        holder.setSearchQuery("")
        advanceTimeBy(150)
        storageFilterFlow.value = StorageFilter.ONLINE
        runCurrent()
        assertEquals(1, holder.filteredArtists.value.size)
        assertEquals(10L, holder.filteredArtists.value.first().id)

        // Storage filter OFFLINE (David Bowie & Pink Floyd)
        storageFilterFlow.value = StorageFilter.OFFLINE
        runCurrent()
        assertEquals(2, holder.filteredArtists.value.size)
    }
}
