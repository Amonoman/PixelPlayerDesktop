package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape

@Composable
fun DailyMixSection(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayMix: () -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val headerSongs = remember(songs) { songs.take(3) }
    val visibleSongs = remember(songs) { songs.take(4) }
    val cornerRadius = 30.dp

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            DailyMixHeader(
                thumbnails = headerSongs,
                onHeaderClick = onPlayMix
            )

            DailyMixSongList(
                songs = visibleSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onToggleFavorite = onToggleFavorite,
                onDelete = onDelete
            )

            ViewAllDailyMixButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                onClickOpen = onSeeAllClick
            )
        }
    }
}

@Composable
private fun DailyMixHeader(
    thumbnails: List<Song>,
    onHeaderClick: () -> Unit
) {
    val titleStyle = rememberDailyMixTitleStyle()
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colors.primary,
                        colors.tertiary
                    )
                )
            )
            .clickable(onClick = onHeaderClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY MIX",
                    style = titleStyle,
                    color = colors.onPrimary
                )
                Text(
                    text = "Based on History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    color = colors.onPrimary.copy(alpha = 0.90f),
                    modifier = Modifier.padding(start = 1.dp, top = 3.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy((-24).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                thumbnails.forEachIndexed { index, song ->
                    val shape = threeShapeSwitch(index, thirdShapeCornerRadius = 22.dp)
                    val sizeModifier = when (index) {
                        0 -> Modifier.size(76.dp).padding(top = 4.dp)
                        1 -> Modifier.size(66.dp).padding(bottom = 4.dp)
                        else -> Modifier.size(72.dp)
                    }

                    Box(
                        modifier = sizeModifier
                            .clip(shape)
                            .border(3.dp, colors.surface, shape)
                    ) {
                        AsyncArtworkImage(
                            artworkUri = song.albumArtUriString,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 32.dp,
                            contentDescription = song.title
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun threeShapeSwitch(index: Int, thirdShapeCornerRadius: Dp = 22.dp): Shape {
    return when (index) {
        0 -> RoundedStarShape(
            sides = 6,
            rotation = 10f
        )
        1 -> CircleShape
        2 -> RoundedCornerShape(thirdShapeCornerRadius)
        else -> CircleShape
    }
}

@Composable
private fun DailyMixSongList(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val itemContainerColor = colors.surfaceContainerLow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .clip(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        songs.forEach { song ->
            val isSelected = currentSong?.id == song.id
            EnhancedSongListItem(
                song = song,
                isCurrentSong = isSelected,
                isPlaying = isPlaying && isSelected,
                containerColorOverride = itemContainerColor,
                customShape = RoundedCornerShape(12.dp),
                showAlbumArt = false,
                onPlayNext = if (onPlayNext != null) { { onPlayNext(song) } } else null,
                onAddToQueue = if (onAddToQueue != null) { { onAddToQueue(song) } } else null,
                onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                onToggleFavorite = if (onToggleFavorite != null) { { onToggleFavorite(song) } } else null,
                onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                onClick = { onSongClick(song) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ViewAllDailyMixButton(
    modifier: Modifier = Modifier,
    onClickOpen: () -> Unit
) {
    val buttonShape = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    Surface(
        modifier = modifier
            .clip(buttonShape)
            .clickable(onClick = onClickOpen),
        shape = buttonShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Check all of Daily Mix",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberDailyMixTitleStyle(): TextStyle {
    return remember {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    "font/gflex_variable.ttf",
                    weight = FontWeight(630),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(630),
                        FontVariation.width(136f),
                        FontVariation.grade(40),
                        FontVariation.Setting("ROND", 100f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(630),
            fontSize = 28.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.35).sp
        )
    }
}
