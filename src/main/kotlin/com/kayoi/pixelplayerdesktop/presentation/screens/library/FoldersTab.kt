package com.kayoi.pixelplayerdesktop.presentation.screens.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.folderFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.FolderListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.FolderPlaylistItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow
import com.kayoi.pixelplayerdesktop.presentation.screens.FolderDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.utils.FolderTreeBuilder

@Composable
fun FoldersTab(
    songs: List<Song>,
    musicDirectories: List<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteSongs: List<Song>,
    searchQuery: String,
    selectedFolderDetail: MusicFolder?,
    onSelectFolderDetail: (MusicFolder?) -> Unit,
    currentFolderTreePath: String?,
    onFolderTreePathChange: (String?) -> Unit,
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFoldersPlaylistView by viewModel.isFoldersPlaylistView.collectAsState()

    if (selectedFolderDetail != null) {
        FolderDetailScreen(
            folder = selectedFolderDetail,
            viewModel = viewModel,
            onBack = { onSelectFolderDetail(null) },
            onAddToPlaylist = onAddToPlaylist,
            onDelete = onDelete
        )
    } else {
        val colors = MaterialTheme.colorScheme
        val currentFolderSortOption by viewModel.folderSortOption.collectAsState()
        val foldersListState = rememberLazyListState()
        var lastHandledFolderSortKey by remember { mutableStateOf(currentFolderSortOption.storageKey) }
        var pendingFolderSortScrollReset by remember { mutableStateOf(false) }

        val fullFolderTree = remember(songs, musicDirectories) {
            FolderTreeBuilder.buildFolderTree(songs, musicDirectories)
        }

        val activeTreeFolder = remember(fullFolderTree, currentFolderTreePath) {
            findFolderByPath(currentFolderTreePath, fullFolderTree)
        }

        val flattenedFolders = remember(fullFolderTree) {
            FolderTreeBuilder.flattenFolders(fullFolderTree)
        }

        val filteredFlattenedFolders = remember(flattenedFolders, searchQuery) {
            if (searchQuery.isBlank()) flattenedFolders
            else flattenedFolders.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.path.contains(searchQuery, ignoreCase = true)
            }
        }

        val sortedFlattenedFolders = remember(filteredFlattenedFolders, currentFolderSortOption) {
            FolderTreeBuilder.sortMusicFolders(filteredFlattenedFolders, currentFolderSortOption)
        }

        val currentTreeSubFolders = remember(activeTreeFolder, fullFolderTree, currentFolderSortOption, searchQuery) {
            val list = activeTreeFolder?.subFolders ?: fullFolderTree
            val filtered = if (searchQuery.isBlank()) list else list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.path.contains(searchQuery, ignoreCase = true)
            }
            FolderTreeBuilder.sortMusicFolders(filtered, currentFolderSortOption)
        }

        val currentTreeSongs = remember(activeTreeFolder, currentFolderSortOption, searchQuery) {
            val list = activeTreeFolder?.songs ?: emptyList()
            val filtered = if (searchQuery.isBlank()) list else list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
            FolderTreeBuilder.sortSongsForFolder(filtered, currentFolderSortOption)
        }

        val folderFastScrollLabelProvider = remember(isFoldersPlaylistView, sortedFlattenedFolders, currentTreeSubFolders, currentFolderSortOption) {
            { index: Int ->
                val folder = if (isFoldersPlaylistView) {
                    sortedFlattenedFolders.getOrNull(index)
                } else {
                    currentTreeSubFolders.getOrNull(index)
                }
                folderFastScrollLabel(
                    folder = folder,
                    sortOption = currentFolderSortOption
                )
            }
        }

        LaunchedEffect(currentFolderSortOption) {
            val currentSortKey = currentFolderSortOption.storageKey
            if (currentSortKey != lastHandledFolderSortKey) {
                lastHandledFolderSortKey = currentSortKey
                pendingFolderSortScrollReset = true
                foldersListState.scrollToItem(0)
            }
        }

        LaunchedEffect(sortedFlattenedFolders, currentTreeSubFolders, pendingFolderSortScrollReset) {
            if (pendingFolderSortScrollReset) {
                foldersListState.scrollToItem(0)
                pendingFolderSortScrollReset = false
            }
        }

        val totalItemsCount = if (isFoldersPlaylistView) {
            sortedFlattenedFolders.size
        } else {
            currentTreeSubFolders.size + currentTreeSongs.size
        }

        val breadcrumbs = remember(currentFolderTreePath, fullFolderTree) {
            if (currentFolderTreePath == null) emptyList()
            else getFolderAncestors(currentFolderTreePath, fullFolderTree)
        }

        val favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }

        Column(modifier = modifier.fillMaxSize()) {
            LibraryActionRow(
                itemCount = totalItemsCount,
                itemCountLabel = if (isFoldersPlaylistView) "folders" else if (activeTreeFolder != null) "items" else "folders",
                currentSortOption = currentFolderSortOption,
                availableSortOptions = SortOption.FOLDERS,
                showViewModeToggle = true,
                isListView = !isFoldersPlaylistView,
                onToggleViewMode = { viewModel.setFoldersPlaylistView(!isFoldersPlaylistView) },
                onSortOptionSelect = { option -> viewModel.setFolderSortOption(option) },
                showShuffleButton = false,
                customLeadingContent = if (!isFoldersPlaylistView && breadcrumbs.isNotEmpty()) {
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Surface(
                                onClick = { onFolderTreePathChange(null) },
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceContainerLow,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Root",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                }
                            }

                            breadcrumbs.forEachIndexed { index, crumb ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 4.dp).size(16.dp)
                                )
                                val isLast = index == breadcrumbs.lastIndex
                                Surface(
                                    onClick = { onFolderTreePathChange(crumb.path) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isLast) colors.primaryContainer else colors.surfaceContainerLow,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = crumb.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isLast) colors.onPrimaryContainer else colors.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else null
            )

            if ((isFoldersPlaylistView && sortedFlattenedFolders.isEmpty()) ||
                (!isFoldersPlaylistView && currentTreeSubFolders.isEmpty() && currentTreeSongs.isEmpty())
            ) {
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
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No folders matching \"$searchQuery\"" else "No folders found in library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = foldersListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isFoldersPlaylistView) {
                            items(
                                items = sortedFlattenedFolders,
                                key = { it.path },
                                contentType = { "folder_playlist_item" }
                            ) { folder ->
                                FolderPlaylistItem(
                                    folder = folder,
                                    onClick = { onSelectFolderDetail(folder) },
                                    onPlayClick = {
                                        val fSongs = folder.collectAllSongs()
                                        if (fSongs.isNotEmpty()) {
                                            viewModel.playSongs(fSongs, 0)
                                        }
                                    }
                                )
                            }
                        } else {
                            items(
                                items = currentTreeSubFolders,
                                key = { "subfolder_${it.path}" },
                                contentType = { "folder_tree_item" }
                            ) { folder ->
                                FolderListItem(
                                    folder = folder,
                                    onClick = { onFolderTreePathChange(folder.path) },
                                    onPlayClick = {
                                        val fSongs = folder.collectAllSongs()
                                        if (fSongs.isNotEmpty()) {
                                            viewModel.playSongs(fSongs, 0)
                                        }
                                    }
                                )
                            }

                            if (currentTreeSongs.isNotEmpty()) {
                                itemsIndexed(
                                    items = currentTreeSongs,
                                    key = { _, song -> "song_${song.id}" },
                                    contentType = { _, _ -> "folder_tree_song" }
                                ) { index, song ->
                                    val isCurrentPlaying = currentSong?.id == song.id
                                    EnhancedSongListItem(
                                        song = song,
                                        isPlaying = isCurrentPlaying && isPlaying,
                                        isCurrentSong = isCurrentPlaying,
                                        isFavorite = favoriteSongIds.contains(song.id),
                                        onPlayNext = { viewModel.playSongNext(song) },
                                        onAddToQueue = { viewModel.addSongToQueue(song) },
                                        onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                                        onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                                        onClick = { viewModel.playSongs(currentTreeSongs.drop(index), 0) }
                                    )
                                }
                            }
                        }
                    }

                    ExpressiveScrollBar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                        listState = foldersListState,
                        dragLabelProvider = folderFastScrollLabelProvider
                    )
                }
            }
        }
    }
}

private fun findFolderByPath(path: String?, folders: List<MusicFolder>): MusicFolder? {
    if (path == null) return null
    for (folder in folders) {
        if (folder.path == path) return folder
        val found = findFolderByPath(path, folder.subFolders)
        if (found != null) return found
    }
    return null
}

private fun getFolderAncestors(targetPath: String, folders: List<MusicFolder>): List<MusicFolder> {
    val result = mutableListOf<MusicFolder>()
    fun traverse(current: MusicFolder): Boolean {
        result.add(current)
        if (current.path == targetPath) return true
        for (sub in current.subFolders) {
            if (traverse(sub)) return true
        }
        result.removeAt(result.size - 1)
        return false
    }
    for (root in folders) {
        if (traverse(root)) return result
    }
    return emptyList()
}
