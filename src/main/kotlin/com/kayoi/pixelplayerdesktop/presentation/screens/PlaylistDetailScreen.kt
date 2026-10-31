package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_file_upload
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_add
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_drag_indicator
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_edit
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_more_vert
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_push_pin
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_remove_circle_outline
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import com.kayoi.pixelplayerdesktop.presentation.components.AddSongsToPlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.CreatePlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.PlaylistCover
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker

private data class PlaylistSongUiItem(val stableKey: String, val song: Song)

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: PlayerViewModel,
    onBack: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
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

    val quickAccessItems by viewModel.quickAccessItems.collectAsState()
    val isPinned = remember(quickAccessItems, playlist?.id) {
        if (playlist == null) false
        else quickAccessItems.any { it.type == QuickAccessType.PLAYLIST && it.targetKey == playlist.id }
    }
    val onTogglePin = remember(isPinned, playlist) {
        if (playlist == null) null
        else {
            { viewModel.togglePin("playlist_${playlist.id}", playlist.name, QuickAccessType.PLAYLIST, playlist.id, isPinned) }
        }
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
                FilledTonalButton(onClick = { onBack?.invoke() }) {
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
    var showEditCoverDialog by remember { mutableStateOf(false) }
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
                    PlaylistCover(
                        playlist = playlist,
                        playlistSongs = plainSongs,
                        size = coverSize,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showEditCoverDialog = true }
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            FilledTonalIconButton(
                                onClick = { showEditCoverDialog = true },
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colors.surfaceContainerHighest,
                                    contentColor = colors.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_edit),
                                    contentDescription = "Edit Playlist",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val countText = if (plainSongs.size == 1) "1 song" else "${plainSongs.size} songs"
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
                        onAddSongs = { showAddSongsDialog = true },
                        onEditPlaylist = { showEditCoverDialog = true },
                        onRename = { showRenameDialog = true },
                        onExport = {
                            DirectoryPicker.saveM3uFile(defaultFileName = "${playlist.name}.m3u") { targetFile ->
                                viewModel.exportPlaylistToM3u(playlist.id, targetFile)
                            }
                        },
                        onDelete = { showDeleteConfirmDialog = true },
                        isWide = true,
                        isPinned = isPinned,
                        onTogglePin = onTogglePin
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
                        PlaylistCover(
                            playlist = playlist,
                            playlistSongs = plainSongs,
                            size = coverSize,
                            modifier = Modifier
                                .clip(RoundedCornerShape(if (isVeryCompact) 12.dp else 16.dp))
                                .clickable { showEditCoverDialog = true }
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = if (isVeryCompact) MaterialTheme.typography.titleMedium else if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                FilledTonalIconButton(
                                    onClick = { showEditCoverDialog = true },
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = colors.surfaceContainerHighest,
                                        contentColor = colors.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_edit),
                                        contentDescription = "Edit Playlist",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            val countText = if (plainSongs.size == 1) "1 song" else "${plainSongs.size} songs"
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
                        onAddSongs = { showAddSongsDialog = true },
                        onEditPlaylist = { showEditCoverDialog = true },
                        onRename = { showRenameDialog = true },
                        onExport = {
                            DirectoryPicker.saveM3uFile(defaultFileName = "${playlist.name}.m3u") { targetFile ->
                                viewModel.exportPlaylistToM3u(playlist.id, targetFile)
                            }
                        },
                        onDelete = { showDeleteConfirmDialog = true },
                        isWide = false,
                        isPinned = isPinned,
                        onTogglePin = onTogglePin
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
                            painter = painterResource(Res.drawable.ic_rounded_music_note),
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
                            Icon(painterResource(Res.drawable.ic_rounded_add), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Songs", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val isReordering = reorderableState.isAnyItemDragging

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp, top = 6.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                customShape = ShapeCache.ConnectedGroup.getShape(index, localSongs.size),
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
                                            painter = painterResource(Res.drawable.ic_rounded_drag_indicator),
                                            contentDescription = "Reorder song",
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
                                onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                                onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(playlist.id, song.id) },
                                onDelete = if (onDelete != null) { { onDelete(song) } } else null,
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

    if (showEditCoverDialog) {
        CreatePlaylistDialog(
            title = "Edit Playlist Cover",
            initialName = playlist.name,
            confirmButtonText = "Save",
            showCoverCustomization = true,
            initialCoverImageUri = playlist.coverImageUri,
            initialCoverColor = playlist.coverColorArgb,
            initialCoverIconName = playlist.coverIconName,
            initialCoverShapeType = playlist.coverShapeType,
            onDismiss = { showEditCoverDialog = false },
            onConfirm = { name, coverImageUri, coverColorArgb, coverIconName, coverShapeType ->
                viewModel.updatePlaylist(
                    playlist.copy(
                        name = name,
                        coverImageUri = coverImageUri,
                        coverColorArgb = coverColorArgb,
                        coverIconName = coverIconName,
                        coverShapeType = coverShapeType
                    )
                )
                showEditCoverDialog = false
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
        TrackDialogInput()
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"${playlist.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteConfirmDialog = false
                        onBack?.invoke()
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
    localSongs: List<Song>,
    isRemoveModeEnabled: Boolean,
    onToggleRemoveMode: () -> Unit,
    isReorderModeEnabled: Boolean,
    onToggleReorderMode: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddSongs: () -> Unit,
    onEditPlaylist: () -> Unit = {},
    onRename: () -> Unit,
    onExport: () -> Unit = {},
    onDelete: () -> Unit,
    isWide: Boolean = true,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isMenuExpanded by remember { mutableStateOf(false) }

    val playCornerTL = 28.dp
    val playCornerBL = 28.dp
    val playCornerTR = 8.dp
    val playCornerBR = 8.dp

    val shuffleCornerTL = 8.dp
    val shuffleCornerBL = 8.dp
    val shuffleCornerTR = 28.dp
    val shuffleCornerBR = 28.dp

    val heroHeight = if (isWide) 42.dp else 46.dp
    val chipHeight = 36.dp

    val removeBtnBg by animateColorAsState(
        targetValue = if (isRemoveModeEnabled) colors.tertiary else colors.surfaceContainerHigh,
        label = "removeBtnBg"
    )
    val removeBtnContent by animateColorAsState(
        targetValue = if (isRemoveModeEnabled) colors.onTertiary else colors.onSurface,
        label = "removeBtnContent"
    )
    val removeCorner by animateDpAsState(
        targetValue = if (isRemoveModeEnabled) 18.dp else 12.dp,
        label = "removeCorner"
    )

    val reorderBtnBg by animateColorAsState(
        targetValue = if (isReorderModeEnabled) colors.tertiary else colors.surfaceContainerHigh,
        label = "reorderBtnBg"
    )
    val reorderBtnContent by animateColorAsState(
        targetValue = if (isReorderModeEnabled) colors.onTertiary else colors.onSurface,
        label = "reorderBtnContent"
    )
    val reorderCorner by animateDpAsState(
        targetValue = if (isReorderModeEnabled) 18.dp else 12.dp,
        label = "reorderCorner"
    )

    Column(
        modifier = if (isWide) modifier.width(IntrinsicSize.Max) else modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (localSongs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(
                        topStart = playCornerTL,
                        bottomStart = playCornerBL,
                        topEnd = playCornerTR,
                        bottomEnd = playCornerBR
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.weight(1f).height(heroHeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                FilledTonalButton(
                    onClick = onShuffle,
                    shape = RoundedCornerShape(
                        topStart = shuffleCornerTL,
                        bottomStart = shuffleCornerBL,
                        topEnd = shuffleCornerTR,
                        bottomEnd = shuffleCornerBR
                    ),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.weight(1f).height(heroHeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        Row(
            modifier = if (isWide) Modifier else Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddSongs,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(chipHeight)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_add),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Add Songs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            if (localSongs.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onToggleReorderMode,
                    shape = RoundedCornerShape(reorderCorner),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = reorderBtnBg,
                        contentColor = reorderBtnContent
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(chipHeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_drag_indicator),
                        contentDescription = "Toggle Reorder Mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Reorder", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = onToggleRemoveMode,
                    shape = RoundedCornerShape(removeCorner),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = removeBtnBg,
                        contentColor = removeBtnContent
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(chipHeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_remove_circle_outline),
                        contentDescription = "Toggle Remove Mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            FilledTonalButton(
                onClick = onEditPlaylist,
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(chipHeight)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_edit),
                    contentDescription = "Edit Playlist",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Edit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Box {
                FilledTonalIconButton(
                    onClick = { isMenuExpanded = true },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.surfaceContainerHigh,
                        contentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.size(chipHeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_more_vert),
                        contentDescription = "Options",
                        modifier = Modifier.size(18.dp)
                    )
                }

                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                ) {
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        offset = DpOffset(0.dp, 8.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp))
                            .border(1.dp, colors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Playlist", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_edit), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onEditPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Playlist", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_edit), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as M3U", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_file_upload), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onExport()
                            }
                        )
                        if (onTogglePin != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isPinned) "Unpin from Quick Access" else "Pin to Quick Access",
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_push_pin),
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                onClick = {
                                    isMenuExpanded = false
                                    onTogglePin()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Playlist", fontWeight = FontWeight.Medium, color = colors.error) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_delete), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
