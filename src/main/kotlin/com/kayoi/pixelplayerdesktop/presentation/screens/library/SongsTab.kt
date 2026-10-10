package com.kayoi.pixelplayerdesktop.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.songFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SongsTab(
    filteredSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    searchQuery: String,
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val currentSongSortOption by viewModel.libraryStateHolder.songSortOption.collectAsState()
    val songsListState = rememberLazyListState()
    var lastHandledSongSortKey by remember { mutableStateOf(currentSongSortOption.storageKey) }
    var pendingSongSortScrollReset by remember { mutableStateOf(false) }

    val currentSongIndex = remember(filteredSongs, currentSong) {
        if (currentSong == null) -1 else filteredSongs.indexOfFirst { it.id == currentSong.id }
    }

    val songFastScrollLabelProvider = remember(filteredSongs, currentSongSortOption) {
        { index: Int ->
            songFastScrollLabel(
                song = filteredSongs.getOrNull(index),
                sortOption = currentSongSortOption
            )
        }
    }

    LaunchedEffect(currentSongSortOption) {
        val currentSortKey = currentSongSortOption.storageKey
        if (currentSortKey != lastHandledSongSortKey) {
            lastHandledSongSortKey = currentSortKey
            pendingSongSortScrollReset = true
            songsListState.scrollToItem(0)
        }
    }

    LaunchedEffect(filteredSongs, pendingSongSortScrollReset) {
        if (pendingSongSortScrollReset) {
            songsListState.scrollToItem(0)
            pendingSongSortScrollReset = false
        }
    }

    val isCurrentSongVisible by remember(currentSongIndex, songsListState) {
        derivedStateOf {
            if (currentSongIndex < 0) false
            else {
                val visible = songsListState.layoutInfo.visibleItemsInfo
                if (visible.isEmpty()) false
                else currentSongIndex in visible.first().index..visible.last().index
            }
        }
    }
    val showLocateButton = currentSongIndex >= 0 && !isCurrentSongVisible

    Column(modifier = modifier.fillMaxSize()) {
        LibraryActionRow(
            itemCount = filteredSongs.size,
            itemCountLabel = "songs",
            currentSortOption = currentSongSortOption,
            showLocateButton = showLocateButton,
            onLocateClick = {
                if (currentSongIndex >= 0) {
                    scope.launch {
                        val firstVisible = songsListState.firstVisibleItemIndex
                        val distance = abs(currentSongIndex - firstVisible)
                        if (distance > 15) {
                            val preScrollIndex = if (currentSongIndex > firstVisible) {
                                (currentSongIndex - 8).coerceAtLeast(0)
                            } else {
                                (currentSongIndex + 8).coerceAtMost(filteredSongs.size - 1)
                            }
                            songsListState.scrollToItem(preScrollIndex)
                        }
                        songsListState.animateScrollToItem(currentSongIndex)
                    }
                }
            },
            onSortOptionSelect = { option -> viewModel.setSongSortOption(option) },
            onShuffleClick = { viewModel.shuffleSongs(filteredSongs) }
        )

        if (filteredSongs.isEmpty()) {
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
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No songs matching \"$searchQuery\"" else "No songs in library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Try searching with another keyword" else "Add music directories in Settings to scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = songsListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> "song_item" }
                    ) { index, song ->
                        val isSelected = currentSong?.id == song.id
                        EnhancedSongListItem(
                            song = song,
                            isCurrentSong = isSelected,
                            isPlaying = isPlaying && isSelected,
                            onPlayNext = { viewModel.playSongNext(song) },
                            onAddToQueue = { viewModel.addSongToQueue(song) },
                            onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                            onClick = { viewModel.playSongs(filteredSongs.drop(index), 0) }
                        )
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                    listState = songsListState,
                    dragLabelProvider = songFastScrollLabelProvider
                )
            }
        }
    }
}
