package com.kayoi.pixelplayerdesktop.presentation.screens

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_shuffle
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

@Composable
fun RecentlyAddedScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    viewModel: PlayerViewModel,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val filteredSongs = remember(songs) {
        songs.sortedByDescending { it.dateAdded }.take(QuickAccessItem.MAX_RECENTLY_ADDED_SONGS)
    }

    val lazyListState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        val screenWidth = maxWidth
        val isVeryCompact = screenWidth < 400.dp
        val isCompact = screenWidth < 560.dp
        val horizontalPadding = when {
            isVeryCompact -> 12.dp
            isCompact -> 16.dp
            else -> 24.dp
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "recently_added_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPadding, end = horizontalPadding, top = 20.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Recently Added",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = colors.onSurface
                    )
                    val subtitle = if (filteredSongs.size == 1) "1 song" else "${filteredSongs.size} songs"
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (filteredSongs.isNotEmpty()) {
                item(key = "recently_added_actions") {
                    RecentlyAddedActions(
                        onPlay = { viewModel.playSongs(filteredSongs, 0) },
                        onShuffle = { viewModel.shuffleSongs(filteredSongs) },
                        isVeryCompact = isVeryCompact,
                        isCompact = isCompact,
                        modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)
                    )
                }

                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                    val isCurrentPlaying = currentSong?.id == song.id
                    val dateAddedText = remember(song.dateAdded) { formatRelativeDateAdded(song.dateAdded) }
                    EnhancedSongListItem(
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        song = song,
                        customShape = ShapeCache.ConnectedGroup.getShape(index, filteredSongs.size),
                        isCurrentSong = isCurrentPlaying,
                        isPlaying = isCurrentPlaying && isPlaying,
                        showAlbumArt = true,
                        showDuration = !isVeryCompact,
                        customDurationText = if (!isVeryCompact) dateAddedText else null,
                        customDurationColor = colors.primary,
                        isFavorite = song.isFavorite,
                        onPlayNext = { viewModel.playSongNext(song) },
                        onAddToQueue = { viewModel.addSongToQueue(song) },
                        onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                        onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                        onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                        onClick = {
                            viewModel.playSongs(filteredSongs, index)
                        }
                    )
                }
            } else {
                item(key = "recently_added_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyAddedActions(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    isVeryCompact: Boolean = false,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val buttonHeight = when {
        isVeryCompact -> 48.dp
        isCompact -> 52.dp
        else -> 56.dp
    }
    val cornerSize = when {
        isVeryCompact -> 36.dp
        isCompact -> 44.dp
        else -> 52.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPlay,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            shape = RoundedCornerShape(
                topStart = cornerSize,
                topEnd = 14.dp,
                bottomStart = cornerSize,
                bottomEnd = 14.dp
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(if (isVeryCompact) 18.dp else 22.dp)
            )
            Spacer(Modifier.width(if (isVeryCompact) 4.dp else 8.dp))
            Text(
                text = "Play All",
                style = if (isVeryCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        FilledTonalButton(
            onClick = onShuffle,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = cornerSize,
                bottomStart = 14.dp,
                bottomEnd = cornerSize
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_shuffle),
                contentDescription = null,
                modifier = Modifier.size(if (isVeryCompact) 18.dp else 22.dp)
            )
            Spacer(Modifier.width(if (isVeryCompact) 4.dp else 8.dp))
            Text(
                text = "Shuffle",
                style = if (isVeryCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatRelativeDateAdded(dateAdded: Long): String {
    if (dateAdded <= 0L) return "Recently"
    val millis = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
    val zoneId = ZoneId.systemDefault()
    val addedDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    val nowDate = LocalDate.now(zoneId)
    val daysBetween = ChronoUnit.DAYS.between(addedDate, nowDate)
    return when {
        daysBetween <= 0L -> "Today"
        daysBetween == 1L -> "Yesterday"
        daysBetween < 7L -> "$daysBetween days ago"
        daysBetween < 14L -> "1 week ago"
        daysBetween < 30L -> "${daysBetween / 7} weeks ago"
        daysBetween < 60L -> "1 month ago"
        daysBetween < 365L -> "${daysBetween / 30} months ago"
        daysBetween < 730L -> "1 year ago"
        else -> "${daysBetween / 365} years ago"
    }
}

