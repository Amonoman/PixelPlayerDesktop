package com.kayoi.pixelplayerdesktop.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.artistFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.ArtistGridItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.ArtistListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@Composable
fun ArtistsTab(
    artists: List<Artist>,
    songs: List<Song>,
    searchQuery: String,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val currentArtistSortOption by viewModel.libraryStateHolder.artistSortOption.collectAsState()
    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
    var isArtistsListView by remember { mutableStateOf(false) }
    val artistsGridState = rememberLazyGridState()
    val artistsListState = rememberLazyListState()
    var lastHandledArtistSortKey by remember { mutableStateOf(currentArtistSortOption.storageKey) }
    var pendingArtistSortScrollReset by remember { mutableStateOf(false) }

    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isBlank()) artists
        else artists.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val artistFastScrollLabelProvider = remember(filteredArtists, currentArtistSortOption) {
        { index: Int ->
            artistFastScrollLabel(
                artist = filteredArtists.getOrNull(index),
                sortOption = currentArtistSortOption
            )
        }
    }

    LaunchedEffect(currentArtistSortOption) {
        val currentSortKey = currentArtistSortOption.storageKey
        if (currentSortKey != lastHandledArtistSortKey) {
            lastHandledArtistSortKey = currentSortKey
            pendingArtistSortScrollReset = true
            if (isArtistsListView) artistsListState.scrollToItem(0) else artistsGridState.scrollToItem(0)
        }
    }

    LaunchedEffect(filteredArtists, pendingArtistSortScrollReset) {
        if (pendingArtistSortScrollReset) {
            if (isArtistsListView) artistsListState.scrollToItem(0) else artistsGridState.scrollToItem(0)
            pendingArtistSortScrollReset = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryActionRow(
            itemCount = filteredArtists.size,
            itemCountLabel = "artists",
            currentSortOption = currentArtistSortOption,
            availableSortOptions = SortOption.ARTISTS,
            showViewModeToggle = true,
            isListView = isArtistsListView,
            onToggleViewMode = { isArtistsListView = !isArtistsListView },
            onSortOptionSelect = { option -> viewModel.setArtistSortOption(option) },
            onShuffleClick = {
                viewModel.shuffleRandomArtist(filteredArtists)
            }
        )

        if (filteredArtists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = PixelPlayerIcons.Artist,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No artists matching \"$searchQuery\"" else "No artists in library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isArtistsListView) {
                    LazyColumn(
                        state = artistsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredArtists,
                            key = { it.name },
                            contentType = { "artist_list_item" }
                        ) { artist ->
                            val representativeArtwork = remember(artist.name, songs) {
                                songs.firstOrNull { it.artist.contains(artist.name, ignoreCase = true) }?.albumArtUriString
                            }
                            ArtistListItem(
                                artist = artist,
                                fallbackArtworkUri = representativeArtwork,
                                fetchDeezerImages = fetchDeezerImages,
                                onClick = {
                                    val artistSongs = songs.filter { it.artist.contains(artist.name, ignoreCase = true) }
                                    if (artistSongs.isNotEmpty()) {
                                        viewModel.playSongs(artistSongs)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = artistsGridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredArtists,
                            key = { it.name },
                            contentType = { "artist_grid_item" }
                        ) { artist ->
                            val representativeArtwork = remember(artist.name, songs) {
                                songs.firstOrNull { it.artist.contains(artist.name, ignoreCase = true) }?.albumArtUriString
                            }
                            ArtistGridItem(
                                artist = artist,
                                fallbackArtworkUri = representativeArtwork,
                                fetchDeezerImages = fetchDeezerImages,
                                onClick = {
                                    val artistSongs = songs.filter { it.artist.contains(artist.name, ignoreCase = true) }
                                    if (artistSongs.isNotEmpty()) {
                                        viewModel.playSongs(artistSongs)
                                    }
                                }
                            )
                        }
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                    listState = if (isArtistsListView) artistsListState else null,
                    gridState = if (!isArtistsListView) artistsGridState else null,
                    dragLabelProvider = artistFastScrollLabelProvider
                )
            }
        }
    }
}
