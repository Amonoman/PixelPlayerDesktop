package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp as lerpDp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.PlayingEqIcon
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class QueueUiItem(val stableKey: String, val song: Song)

@Composable
fun QueueSidePanel(
    queue: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    repeatMode: Int = 0,
    onSongClick: (Song, Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveItem: ((Int) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    favoriteSongIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    var keyCounter by remember { mutableStateOf(0L) }
    var localQueue by remember { mutableStateOf<List<QueueUiItem>>(emptyList()) }

    val currentSongIndex = remember(localQueue, currentSong) {
        if (currentSong == null) -1 else localQueue.indexOfFirst { it.song.id == currentSong.id }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            localQueue = localQueue.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            onMoveItem(from.index, to.index)
        }
    )

    LaunchedEffect(queue) {
        if (!reorderableState.isAnyItemDragging) {
            val newItems = mutableListOf<QueueUiItem>()
            val oldPool = localQueue.toMutableList()
            for (song in queue) {
                val matchIdx = oldPool.indexOfFirst { it.song.id == song.id }
                if (matchIdx >= 0) {
                    val matched = oldPool.removeAt(matchIdx)
                    newItems.add(matched.copy(song = song))
                } else {
                    newItems.add(QueueUiItem(stableKey = "q_${keyCounter++}_${song.id}", song = song))
                }
            }
            localQueue = newItems
        }
    }

    // Keep currently playing song in view when not user-dragging
    LaunchedEffect(currentSongIndex) {
        if (!reorderableState.isAnyItemDragging && currentSongIndex >= 0 && currentSongIndex < localQueue.size) {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == currentSongIndex }
            if (!isVisible) {
                val firstVisible = listState.firstVisibleItemIndex
                val distance = kotlin.math.abs(currentSongIndex - firstVisible)
                if (distance > 15) {
                    val preScrollIndex = if (currentSongIndex > firstVisible) {
                        (currentSongIndex - 8).coerceAtLeast(0)
                    } else {
                        (currentSongIndex + 8).coerceAtMost(queue.size - 1)
                    }
                    listState.scrollToItem(preScrollIndex)
                }
                listState.animateScrollToItem(currentSongIndex)
            }
        }
    }

    val isCurrentSongVisible by remember(currentSongIndex, listState) {
        derivedStateOf {
            if (currentSongIndex < 0) true
            else {
                val visible = listState.layoutInfo.visibleItemsInfo
                if (visible.isEmpty()) true
                else currentSongIndex in visible.first().index..visible.last().index
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .width(420.dp)
    ) {

        VerticalDivider(
            color = colors.outlineVariant.copy(alpha = 0.25f),
            thickness = 1.dp,
            modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Next Up",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "${queue.size} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Smooth pre-scroll to minimize UI jumping across large index gaps
                    AnimatedVisibility(
                        visible = !isCurrentSongVisible && currentSongIndex >= 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (currentSongIndex >= 0) {
                                    coroutineScope.launch {
                                        val firstVisible = listState.firstVisibleItemIndex
                                        val distance = kotlin.math.abs(currentSongIndex - firstVisible)
                                        if (distance > 15) {
                                            val preScrollIndex = if (currentSongIndex > firstVisible) {
                                                (currentSongIndex - 8).coerceAtLeast(0)
                                            } else {
                                                (currentSongIndex + 8).coerceAtMost(queue.size - 1)
                                            }
                                            listState.scrollToItem(preScrollIndex)
                                        }
                                        listState.animateScrollToItem(currentSongIndex)
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                                contentColor = colors.primary
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Locate playing song",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val shuffleContainerColor = if (isShuffleOn) colors.primary else colors.surfaceContainerHighest.copy(alpha = 0.6f)
                    val shuffleContentColor = if (isShuffleOn) colors.onPrimary else colors.onSurfaceVariant

                    FilledIconButton(
                        onClick = onToggleShuffle,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = shuffleContainerColor,
                            contentColor = shuffleContentColor
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val repeatContainerColor = if (repeatMode > 0) colors.primary else colors.surfaceContainerHighest.copy(alpha = 0.6f)
                    val repeatContentColor = if (repeatMode > 0) colors.onPrimary else colors.onSurfaceVariant
                    val repeatIcon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat

                    FilledIconButton(
                        onClick = onCycleRepeat,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = repeatContainerColor,
                            contentColor = repeatContentColor
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat Mode",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = { showClearDialog = true },
                        enabled = queue.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                            contentColor = colors.onSurfaceVariant,
                            disabledContainerColor = colors.surfaceContainerHighest.copy(alpha = 0.25f),
                            disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear Queue",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No songs in queue",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                val isReordering = reorderableState.isAnyItemDragging

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp, end = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = localQueue.size,
                            key = { index -> localQueue[index].stableKey },
                            contentType = { "queue_song" }
                        ) { index ->
                            val song = localQueue[index].song
                            val isSelected = currentSong?.id == song.id && (index == currentSongIndex || currentSongIndex == -1)

                            ReorderableItem(
                                state = reorderableState,
                                key = localQueue[index].stableKey,
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
                                    isPlaying = isPlaying && isSelected,
                                    showAlbum = false,
                                    showDuration = false,
                                    isDragging = isDragging,
                                    isDragHandleVisible = true,
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
                                    isFavorite = if (favoriteSongIds.isNotEmpty()) favoriteSongIds.contains(song.id) else song.isFavorite,
                                    onPlayNext = if (onPlayNext != null) { { onPlayNext(song) } } else null,
                                    onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                    onToggleFavorite = if (onToggleFavorite != null) { { onToggleFavorite(song) } } else null,
                                    onRemoveFromQueue = if (onRemoveItem != null) { { onRemoveItem(index) } } else null,
                                    onClick = { onSongClick(song, index) }
                                )
                            }
                        }
                    }

                    ExpressiveScrollBar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp, top = 4.dp, bottom = 16.dp),
                        listState = listState
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Queue?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all ${queue.size} tracks from the queue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearQueue()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
