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
import com.kayoi.pixelplayerdesktop.presentation.screens.ArtistDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@Composable
fun ArtistsTab(
    artists: List<Artist>,
    songs: List<Song>,
    searchQuery: String,
    selectedArtist: Artist? = null,
    onSelectArtist: (Artist?) -> Unit = {},
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (selectedArtist != null) {
        ArtistDetailScreen(
            artist = selectedArtist,
            songs = songs,
            viewModel = viewModel,
            onBack = { onSelectArtist(null) },
            onAddToPlaylist = onAddToPlaylist,
            onDelete = onDelete,
            modifier = modifier
        )
    } else {
        val colors = MaterialTheme.colorScheme
        val currentArtistSortOption by viewModel.libraryStateHolder.artistSortOption.collectAsState()
        val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
        val isArtistsListView by viewModel.isArtistsListView.collectAsState()
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
                onToggleViewMode = { viewModel.setArtistsListView(!isArtistsListView) },
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
                                ArtistListItem(
                                    artist = artist,
                                    fetchDeezerImages = fetchDeezerImages,
                                    onClick = { onSelectArtist(artist) }
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
                                ArtistGridItem(
                                    artist = artist,
                                    fetchDeezerImages = fetchDeezerImages,
                                    onClick = { onSelectArtist(artist) }
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
}
