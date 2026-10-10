package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.PlaylistArtCollage
import com.kayoi.pixelplayerdesktop.presentation.components.songFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

private fun formatTotalDuration(songs: List<Song>): String {
    val totalMs = songs.sumOf { it.duration }
    val totalSeconds = (totalMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours} hr ${remainingMinutes} min"
    } else {
        "${minutes} min"
    }
}

@Composable
fun FolderDetailScreen(
    folder: MusicFolder,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val currentSong by viewModel.currentSong.collectAsState()
    val playerInfo by viewModel.playerInfo.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    val folderSongs = remember(folder) { folder.collectAllSongs() }
    val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }

    val listState = rememberLazyListState()

    val songFastScrollLabelProvider = remember(folderSongs) {
        { index: Int ->
            songFastScrollLabel(
                song = folderSongs.getOrNull(index),
                sortOption = SortOption.SongTitleAZ
            )
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

                    Box(
                        modifier = Modifier
                            .size(coverSize)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        PlaylistArtCollage(
                            songs = folderSongs.take(9),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FOLDER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = folder.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${folderSongs.size} tracks • ${formatTotalDuration(folderSongs)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (folderSongs.isNotEmpty()) {
                                    viewModel.playSongs(folderSongs, 0)
                                }
                            },
                            enabled = folderSongs.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (folderSongs.isNotEmpty()) {
                                    viewModel.shuffleSongs(folderSongs)
                                }
                            },
                            enabled = folderSongs.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = 12.dp)
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Box(
                            modifier = Modifier
                                .size(coverSize)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            PlaylistArtCollage(
                                songs = folderSongs.take(9),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "FOLDER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${folderSongs.size} tracks • ${formatTotalDuration(folderSongs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (folderSongs.isNotEmpty()) {
                                    viewModel.playSongs(folderSongs, 0)
                                }
                            },
                            enabled = folderSongs.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (folderSongs.isNotEmpty()) {
                                    viewModel.shuffleSongs(folderSongs)
                                }
                            },
                            enabled = folderSongs.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = colors.outlineVariant.copy(alpha = 0.35f)
        )

        if (folderSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
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
                        text = "No songs found in this folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = folderSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> "folder_detail_song" }
                    ) { index, song ->
                        val isCurrentPlaying = currentSong?.id == song.id
                        EnhancedSongListItem(
                            song = song,
                            isPlaying = isCurrentPlaying && playerInfo.isPlaying,
                            isCurrentSong = isCurrentPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            onPlayNext = { viewModel.playSongNext(song) },
                            onAddToQueue = { viewModel.addSongToQueue(song) },
                            onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                            onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                            onClick = { viewModel.playSongs(folderSongs.drop(index), 0) }
                        )
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = 24.dp),
                    listState = listState,
                    dragLabelProvider = songFastScrollLabelProvider
                )
            }
        }
    }
}
