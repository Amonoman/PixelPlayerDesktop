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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
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
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.albumFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AlbumGridItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AlbumListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow
import com.kayoi.pixelplayerdesktop.presentation.screens.AlbumDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@Composable
fun AlbumsTab(
    albums: List<Album>,
    songs: List<Song>,
    isDarkTheme: Boolean,
    searchQuery: String,
    selectedAlbum: Album? = null,
    onSelectAlbum: (Album?) -> Unit = {},
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    onBatchEditMetadata: ((List<Song>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (selectedAlbum != null) {
        AlbumDetailScreen(
            album = selectedAlbum,
            songs = songs,
            viewModel = viewModel,
            onBack = { onSelectAlbum(null) },
            onAddToPlaylist = onAddToPlaylist,
            onDelete = onDelete,
            onEditMetadata = onEditMetadata,
            onBatchEditMetadata = onBatchEditMetadata,
            modifier = modifier
        )
    } else {
        val colors = MaterialTheme.colorScheme
        val currentAlbumSortOption by viewModel.libraryStateHolder.albumSortOption.collectAsState()
        val isAlbumsListView by viewModel.isAlbumsListView.collectAsState()
        val albumsGridState = rememberLazyGridState()
        val albumsListState = rememberLazyListState()
        var lastHandledAlbumSortKey by remember { mutableStateOf(currentAlbumSortOption.storageKey) }
        var pendingAlbumSortScrollReset by remember { mutableStateOf(false) }

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums
        else albums.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val albumFastScrollLabelProvider = remember(filteredAlbums, currentAlbumSortOption) {
        { index: Int ->
            albumFastScrollLabel(
                album = filteredAlbums.getOrNull(index),
                sortOption = currentAlbumSortOption
            )
        }
    }

    LaunchedEffect(currentAlbumSortOption) {
        val currentSortKey = currentAlbumSortOption.storageKey
        if (currentSortKey != lastHandledAlbumSortKey) {
            lastHandledAlbumSortKey = currentSortKey
            pendingAlbumSortScrollReset = true
            if (isAlbumsListView) albumsListState.scrollToItem(0) else albumsGridState.scrollToItem(0)
        }
    }

    LaunchedEffect(filteredAlbums, pendingAlbumSortScrollReset) {
        if (pendingAlbumSortScrollReset) {
            if (isAlbumsListView) albumsListState.scrollToItem(0) else albumsGridState.scrollToItem(0)
            pendingAlbumSortScrollReset = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryActionRow(
            itemCount = filteredAlbums.size,
            itemCountLabel = "albums",
            currentSortOption = currentAlbumSortOption,
            availableSortOptions = SortOption.ALBUMS,
            showViewModeToggle = true,
            isListView = isAlbumsListView,
            onToggleViewMode = { viewModel.setAlbumsListView(!isAlbumsListView) },
            onSortOptionSelect = { option -> viewModel.setAlbumSortOption(option) },
            onShuffleClick = {
                viewModel.shuffleRandomAlbum(filteredAlbums)
            }
        )

        if (filteredAlbums.isEmpty()) {
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
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No albums matching \"$searchQuery\"" else "No albums in library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isAlbumsListView) {
                    LazyColumn(
                        state = albumsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredAlbums,
                            key = { it.title + it.artist },
                            contentType = { "album_list_item" }
                        ) { album ->
                            AlbumListItem(
                                album = album,
                                isDarkTheme = isDarkTheme,
                                onClick = { onSelectAlbum(album) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = albumsGridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredAlbums,
                            key = { it.title + it.artist },
                            contentType = { "album_grid_item" }
                        ) { album ->
                            AlbumGridItem(
                                album = album,
                                isDarkTheme = isDarkTheme,
                                onClick = { onSelectAlbum(album) }
                            )
                        }
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                    listState = if (isAlbumsListView) albumsListState else null,
                    gridState = if (!isAlbumsListView) albumsGridState else null,
                    dragLabelProvider = albumFastScrollLabelProvider
                )
            }
        }
    }
    }
}
