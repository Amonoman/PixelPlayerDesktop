package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AddSongsToPlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.CreatePlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.PlaylistCover
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState

private data class PlaylistSongUiItem(val stableKey: String, val song: Song)

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val playlists by viewModel.playlists.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val playerInfo by viewModel.playerInfo.collectAsState()

    val playlist = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }
    }

    if (playlist == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Playlist not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(onClick = onBack) {
                    Text("Back to Playlists")
                }
            }
        }
        return
    }

    val songMap = remember(allSongs) {
        allSongs.associateBy { it.id }
    }

    var localSongs by remember(playlist.songIds, songMap) {
        var counter = 0
        mutableStateOf(
            playlist.songIds.mapNotNull { songMap[it] }.map { song ->
                PlaylistSongUiItem(stableKey = "pl_${counter++}_${song.id}", song = song)
            }
        )
    }

    val plainSongs = remember(localSongs) {
        localSongs.map { it.song }
    }

    val totalDurationMs = remember(plainSongs) {
        plainSongs.sumOf { it.duration }
    }

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isRemoveModeEnabled by remember { mutableStateOf(false) }
    var isReorderModeEnabled by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var lastMovedFrom by remember { mutableStateOf<Int?>(null) }
    var lastMovedTo by remember { mutableStateOf<Int?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localSongs = localSongs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            if (lastMovedFrom == null) {
                lastMovedFrom = from.index
            }
            lastMovedTo = to.index
        }
    )

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && (lastMovedFrom != null || lastMovedTo != null)) {
            val newSongIds = localSongs.map { it.song.id }
            viewModel.reorderPlaylistSongs(playlist.id, newSongIds)
            lastMovedFrom = null
            lastMovedTo = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isWide = maxWidth >= 860.dp
            val isVeryCompact = maxWidth < 460.dp
            val isCompact = maxWidth < 680.dp
            val coverSize = if (isWide) 110.dp else if (isVeryCompact) 56.dp else if (isCompact) 72.dp else 90.dp
            val horizontalPad = if (isVeryCompact) 14.dp else 24.dp

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPad, end = horizontalPad, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.surfaceContainerHigh,
                            contentColor = colors.onSurface
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    PlaylistCover(
                        playlist = playlist,
                        playlistSongs = plainSongs,
                        size = coverSize,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PLAYLIST",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val countText = if (plainSongs.size == 1) "1 track" else "${plainSongs.size} tracks"
                        val durationText = formatDurationHuman(totalDurationMs)
                        Text(
                            text = if (durationText.isNotBlank()) "$countText • $durationText" else countText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    PlaylistHeaderActions(
                        localSongs = plainSongs,
                        isRemoveModeEnabled = isRemoveModeEnabled,
                        onToggleRemoveMode = {
                            isRemoveModeEnabled = !isRemoveModeEnabled
                            if (isRemoveModeEnabled) isReorderModeEnabled = false
                        },
                        isReorderModeEnabled = isReorderModeEnabled,
                        onToggleReorderMode = {
                            isReorderModeEnabled = !isReorderModeEnabled
                            if (isReorderModeEnabled) isRemoveModeEnabled = false
                        },
                        onPlay = { viewModel.playSongs(plainSongs, 0) },
                        onShuffle = { viewModel.shuffleSongs(plainSongs) },
                        onAddTracks = { showAddSongsDialog = true },
                        onRename = { showRenameDialog = true },
                        onDelete = { showDeleteConfirmDialog = true },
                        isCompact = false
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPad, end = horizontalPad, top = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surfaceContainerHigh,
                                contentColor = colors.onSurface
                            ),
                            modifier = Modifier.size(if (isVeryCompact) 34.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(if (isVeryCompact) 16.dp else 18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isVeryCompact) 10.dp else 14.dp))

                        PlaylistCover(
                            playlist = playlist,
                            playlistSongs = plainSongs,
                            size = coverSize,
                            modifier = Modifier.clip(RoundedCornerShape(if (isVeryCompact) 12.dp else 16.dp))
                        )

                        Spacer(modifier = Modifier.width(if (isVeryCompact) 12.dp else 16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "PLAYLIST",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = playlist.name,
                                style = if (isVeryCompact) MaterialTheme.typography.titleMedium else if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val countText = if (plainSongs.size == 1) "1 track" else "${plainSongs.size} tracks"
                            val durationText = formatDurationHuman(totalDurationMs)
                            Text(
                                text = if (durationText.isNotBlank()) "$countText • $durationText" else countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    PlaylistHeaderActions(
                        localSongs = plainSongs,
                        isRemoveModeEnabled = isRemoveModeEnabled,
                        onToggleRemoveMode = {
                            isRemoveModeEnabled = !isRemoveModeEnabled
                            if (isRemoveModeEnabled) isReorderModeEnabled = false
                        },
                        isReorderModeEnabled = isReorderModeEnabled,
                        onToggleReorderMode = {
                            isReorderModeEnabled = !isReorderModeEnabled
                            if (isReorderModeEnabled) isRemoveModeEnabled = false
                        },
                        onPlay = { viewModel.playSongs(plainSongs, 0) },
                        onShuffle = { viewModel.shuffleSongs(plainSongs) },
                        onAddTracks = { showAddSongsDialog = true },
                        onRename = { showRenameDialog = true },
                        onDelete = { showDeleteConfirmDialog = true },
                        isCompact = isCompact
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (localSongs.isEmpty()) {
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
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Playlist is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddSongsDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Tracks", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val isReordering = reorderableState.isAnyItemDragging

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = localSongs.size,
                        key = { index -> localSongs[index].stableKey },
                        contentType = { "playlist_song_item" }
                    ) { index ->
                        val song = localSongs[index].song
                        val isSelected = currentSong?.id == song.id

                        ReorderableItem(
                            state = reorderableState,
                            key = localSongs[index].stableKey,
                            animateItemModifier = if (isReordering) {
                                Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            } else {
                                Modifier
                            }
                        ) { isDragging ->
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.025f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "scaleAnimation"
                            )

                            EnhancedSongListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                song = song,
                                isCurrentSong = isSelected,
                                isPlaying = playerInfo.isPlaying && isSelected,
                                isDragging = isDragging,
                                isDragHandleVisible = isReorderModeEnabled,
                                dragHandle = {
                                    Box(
                                        modifier = Modifier
                                            .draggableHandle()
                                            .size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DragIndicator,
                                            contentDescription = "Reorder track",
                                            tint = if (isSelected || isDragging) colors.onPrimaryContainer.copy(alpha = 0.85f) else colors.onSurfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                isRemoveButtonVisible = isRemoveModeEnabled,
                                onRemoveClick = {
                                    viewModel.removeSongFromPlaylist(playlist.id, song.id)
                                },
                                showMoreOptionsButton = !isRemoveModeEnabled && !isReorderModeEnabled,
                                onPlayNext = { viewModel.playSongNext(song) },
                                onAddToQueue = { viewModel.addSongToQueue(song) },
                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                                onClick = {
                                    if (!isDragging) {
                                        viewModel.playSongs(plainSongs, index)
                                    }
                                }
                            )
                        }
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = 20.dp),
                    listState = listState
                )
            }
        }
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            allSongs = allSongs,
            existingSongIds = playlist.songIds.toSet(),
            onDismiss = { showAddSongsDialog = false },
            onAddSongs = { newSongIds ->
                viewModel.addSongsToPlaylist(playlist.id, newSongIds)
                showAddSongsDialog = false
            }
        )
    }

    if (showRenameDialog) {
        CreatePlaylistDialog(
            title = "Rename Playlist",
            initialName = playlist.name,
            confirmButtonText = "Save",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"${playlist.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteConfirmDialog = false
                        onBack()
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
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun formatDurationHuman(durationMs: Long): String {
    if (durationMs <= 0) return ""
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

@Composable
private fun PlaylistHeaderActions(
    localSongs: List<com.kayoi.pixelplayerdesktop.data.model.Song>,
    isRemoveModeEnabled: Boolean,
    onToggleRemoveMode: () -> Unit,
    isReorderModeEnabled: Boolean,
    onToggleReorderMode: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddTracks: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (localSongs.isNotEmpty()) {
            FilledTonalButton(
                onClick = onPlay,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = if (isCompact) 14.dp else 18.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play", fontWeight = FontWeight.Bold)
            }

            FilledTonalIconButton(
                onClick = onShuffle,
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        FilledTonalButton(
            onClick = onAddTracks,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Tracks", fontWeight = FontWeight.SemiBold)
        }

        if (localSongs.isNotEmpty()) {
            val removeBtnBg by animateColorAsState(
                targetValue = if (isRemoveModeEnabled) colors.tertiary else colors.surfaceContainerHigh,
                label = "removeBtnBg"
            )
            val removeBtnContent by animateColorAsState(
                targetValue = if (isRemoveModeEnabled) colors.onTertiary else colors.onSurface,
                label = "removeBtnContent"
            )
            val removeCornerRadius by animateDpAsState(
                targetValue = if (isRemoveModeEnabled) 24.dp else 12.dp,
                label = "removeCornerRadius"
            )

            FilledTonalButton(
                onClick = onToggleRemoveMode,
                shape = RoundedCornerShape(removeCornerRadius),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = removeBtnBg,
                    contentColor = removeBtnContent
                ),
                contentPadding = PaddingValues(horizontal = if (isCompact) 10.dp else 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircleOutline,
                    contentDescription = "Toggle Remove Mode",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove", fontWeight = FontWeight.SemiBold)
            }

            val reorderBtnBg by animateColorAsState(
                targetValue = if (isReorderModeEnabled) colors.tertiary else colors.surfaceContainerHigh,
                label = "reorderBtnBg"
            )
            val reorderBtnContent by animateColorAsState(
                targetValue = if (isReorderModeEnabled) colors.onTertiary else colors.onSurface,
                label = "reorderBtnContent"
            )
            val reorderCornerRadius by animateDpAsState(
                targetValue = if (isReorderModeEnabled) 24.dp else 12.dp,
                label = "reorderCornerRadius"
            )

            FilledTonalButton(
                onClick = onToggleReorderMode,
                shape = RoundedCornerShape(reorderCornerRadius),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = reorderBtnBg,
                    contentColor = reorderBtnContent
                ),
                contentPadding = PaddingValues(horizontal = if (isCompact) 10.dp else 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Toggle Reorder Mode",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reorder", fontWeight = FontWeight.SemiBold)
            }
        }

        Box {
            FilledTonalIconButton(
                onClick = { isMenuExpanded = true },
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurfaceVariant
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename Playlist") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        isMenuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Playlist", color = colors.error) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        isMenuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
