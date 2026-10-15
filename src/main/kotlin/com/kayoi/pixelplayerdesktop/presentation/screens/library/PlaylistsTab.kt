package com.kayoi.pixelplayerdesktop.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.CreatePlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.components.playlistFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.PlaylistListItem
import com.kayoi.pixelplayerdesktop.presentation.screens.PlaylistDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    songs: List<Song>,
    searchQuery: String,
    selectedPlaylistId: String?,
    onSelectPlaylist: (String?) -> Unit,
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (selectedPlaylistId != null) {
        PlaylistDetailScreen(
            playlistId = selectedPlaylistId,
            viewModel = viewModel,
            onBack = { onSelectPlaylist(null) },
            onAddToPlaylist = onAddToPlaylist,
            onDelete = onDelete,
            onEditMetadata = onEditMetadata
        )
    } else {
        val colors = MaterialTheme.colorScheme
        val currentPlaylistSortOption by viewModel.libraryStateHolder.playlistSortOption.collectAsState()
        val playlistsListState = rememberLazyListState()
        var lastHandledPlaylistSortKey by remember { mutableStateOf(currentPlaylistSortOption.storageKey) }
        var pendingPlaylistSortScrollReset by remember { mutableStateOf(false) }

        var showCreatePlaylistDialog by remember { mutableStateOf(false) }
        var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
        var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

        val filteredPlaylists = remember(playlists, searchQuery) {
            if (searchQuery.isBlank()) playlists
            else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val playlistFastScrollLabelProvider = remember(filteredPlaylists, currentPlaylistSortOption) {
            { index: Int ->
                playlistFastScrollLabel(
                    playlist = filteredPlaylists.getOrNull(index),
                    sortOption = currentPlaylistSortOption
                )
            }
        }

        LaunchedEffect(currentPlaylistSortOption) {
            val currentSortKey = currentPlaylistSortOption.storageKey
            if (currentSortKey != lastHandledPlaylistSortKey) {
                lastHandledPlaylistSortKey = currentSortKey
                pendingPlaylistSortScrollReset = true
                playlistsListState.scrollToItem(0)
            }
        }

        LaunchedEffect(filteredPlaylists, pendingPlaylistSortScrollReset) {
            if (pendingPlaylistSortScrollReset) {
                playlistsListState.scrollToItem(0)
                pendingPlaylistSortScrollReset = false
            }
        }

        Column(modifier = modifier.fillMaxSize()) {
            LibraryActionRow(
                itemCount = filteredPlaylists.size,
                itemCountLabel = "playlists",
                currentSortOption = currentPlaylistSortOption,
                availableSortOptions = SortOption.PLAYLISTS,
                showCreateButton = true,
                createButtonLabel = "New Playlist",
                onCreateClick = { showCreatePlaylistDialog = true },
                showShuffleButton = false,
                onSortOptionSelect = { option -> viewModel.setPlaylistSortOption(option) }
            )

            if (filteredPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No playlists matching \"$searchQuery\"" else "No playlists created yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                )
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Playlist", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = playlistsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredPlaylists,
                            key = { it.id },
                            contentType = { "playlist_list_item" }
                        ) { pl ->
                            val plSongs = remember(pl.songIds, songs) {
                                songs.filter { pl.songIds.contains(it.id) }
                            }
                            PlaylistListItem(
                                playlist = pl,
                                playlistSongs = plSongs,
                                onClick = { onSelectPlaylist(pl.id) },
                                onPlayClick = {
                                    if (plSongs.isNotEmpty()) {
                                        viewModel.playSongs(plSongs, 0)
                                    }
                                },
                                onRenameClick = { playlistToRename = pl },
                                onDeleteClick = { playlistToDelete = pl }
                            )
                        }
                    }

                    ExpressiveScrollBar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                        listState = playlistsListState,
                        dragLabelProvider = playlistFastScrollLabelProvider
                    )
                }
            }
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                title = "New Playlist",
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name ->
                    viewModel.createPlaylist(name)
                    showCreatePlaylistDialog = false
                }
            )
        }

        playlistToRename?.let { pl ->
            CreatePlaylistDialog(
                title = "Rename Playlist",
                initialName = pl.name,
                confirmButtonText = "Save",
                onDismiss = { playlistToRename = null },
                onConfirm = { newName ->
                    viewModel.renamePlaylist(pl.id, newName)
                    playlistToRename = null
                }
            )
        }

        playlistToDelete?.let { pl ->
            TrackDialogInput()
            AlertDialog(
                onDismissRequest = { playlistToDelete = null },
                title = { Text("Delete Playlist") },
                text = { Text("Are you sure you want to delete \"${pl.name}\"? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePlaylist(pl.id)
                            playlistToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { playlistToDelete = null },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
