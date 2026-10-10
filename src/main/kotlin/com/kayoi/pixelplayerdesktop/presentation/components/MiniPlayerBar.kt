package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    progress: Float = 0f,
    isShuffle: Boolean = false,
    repeatMode: Int = 0,
    isFavorite: Boolean = false,
    volume: Float = 1.0f,
    isMuted: Boolean = false,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onExpandClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    if (currentSong == null) return

    val colors = MaterialTheme.colorScheme

    val miniPlayerShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

    val playPauseCorner by animateDpAsState(
        targetValue = if (isPlaying) 12.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "MiniPlayerPlayPauseCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
            .shadow(elevation = 16.dp, shape = miniPlayerShape)
            .clip(miniPlayerShape),
        color = colors.primaryContainer,
        shape = miniPlayerShape
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val isNarrow = maxWidth < 580.dp
            val isWide = maxWidth >= 820.dp
            val centerMaxWidth = when {
                maxWidth >= 1100.dp -> 480.dp
                maxWidth >= 820.dp -> 380.dp
                else -> 280.dp
            }

            if (isNarrow) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncArtworkImage(
                            artworkUri = currentSong.albumArtUriString,
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 26.dp,
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurfaceVariant,
                            contentDescription = "${currentSong.title} cover"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = currentSong.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentSong.displayArtist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = onPreviousClick,
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.onPrimary,
                                contentColor = colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(playPauseCorner),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = onNextClick,
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.onPrimary,
                                contentColor = colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Stop & Close",
                                tint = colors.onPrimaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AsyncArtworkImage(
                            artworkUri = currentSong.albumArtUriString,
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 30.dp,
                            containerColor = colors.surfaceVariant,
                            contentColor = colors.onSurfaceVariant,
                            contentDescription = "${currentSong.title} cover"
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = currentSong.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentSong.displayArtist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                                color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(min = 240.dp, max = centerMaxWidth)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isWide) {
                                FilledIconButton(
                                    onClick = onToggleShuffle,
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isShuffle) colors.primary else Color.Transparent,
                                        contentColor = if (isShuffle) colors.onPrimary else colors.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            FilledIconButton(
                                onClick = onPreviousClick,
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.onPrimary,
                                    contentColor = colors.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onPlayPauseClick,
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(playPauseCorner),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = onNextClick,
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colors.onPrimary,
                                    contentColor = colors.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (isWide) {
                                FilledIconButton(
                                    onClick = onCycleRepeat,
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (repeatMode > 0) colors.primary else Color.Transparent,
                                        contentColor = if (repeatMode > 0) colors.onPrimary else colors.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                ) {
                                    val repIcon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                                    Icon(
                                        imageVector = repIcon,
                                        contentDescription = "Repeat",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            WavySliderExpressive(
                                value = progress,
                                onValueChange = onSeek,
                                isPlaying = isPlaying,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.20f),
                                thumbColor = colors.primary,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else currentSong.duration
                            Text(
                                text = formatTime(effectiveDuration),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onQueueClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Queue",
                                tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onLyricsClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = PixelPlayerIcons.Lyrics,
                                contentDescription = "Lyrics",
                                tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(38.dp)
                        ) {
                            val volIcon = when {
                                isMuted || volume == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
                                volume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                                else -> Icons.AutoMirrored.Rounded.VolumeUp
                            }
                            Icon(
                                imageVector = volIcon,
                                contentDescription = "Volume",
                                tint = if (isMuted) colors.error else colors.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (isWide) {
                            var isDraggingVolume by remember { mutableStateOf(false) }
                            var localVolume by remember(volume) { mutableFloatStateOf(volume) }
                            val displayVolume = if (isMuted) 0f else if (isDraggingVolume) localVolume else volume

                            Slider(
                                value = displayVolume.coerceIn(0f, 1f),
                                onValueChange = {
                                    isDraggingVolume = true
                                    localVolume = it
                                    onVolumeChange(it)
                                },
                                onValueChangeFinished = {
                                    isDraggingVolume = false
                                    onVolumeChange(localVolume)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.primary,
                                    activeTrackColor = colors.primary,
                                    inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.20f)
                                ),
                                modifier = Modifier.width(96.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onExpandClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Expand Player",
                                tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Stop & Close",
                                tint = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
