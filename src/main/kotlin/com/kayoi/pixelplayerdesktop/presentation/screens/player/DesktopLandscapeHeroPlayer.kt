package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.WavySliderExpressive
import java.io.File

internal fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

internal fun formatAudioMetaLabel(song: Song): String {
    val ext = File(song.path).extension.uppercase()
    return when (ext) {
        "FLAC" -> "FLAC LOSSLESS"
        "OPUS" -> "OPUS"
        "OGG" -> "VORBIS"
        "WAV" -> "WAV"
        "M4A", "AAC" -> "AAC"
        "MP3" -> "MP3"
        else -> ext.ifBlank { "AUDIO" }
    }
}

@Composable
fun DesktopLandscapeHeroPlayer(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    progress: Float,
    isShuffle: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    volume: Float,
    isMuted: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        val heroWidth = maxWidth
        val isStackedMode = heroWidth < 660.dp

        if (isStackedMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(12.dp, shape = RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        AsyncArtworkImage(
                            artworkUri = song.albumArtUriString,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 48.dp,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            contentDescription = "${song.title} artwork"
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (song.album.isNotBlank() && song.album != song.title) {
                                        "${song.displayArtist} • ${song.album}"
                                    } else {
                                        song.displayArtist
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.65f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

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
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        val metaLabel = formatAudioMetaLabel(song)
                        if (metaLabel.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = colors.onPrimaryContainer.copy(alpha = 0.15f),
                                contentColor = colors.onPrimaryContainer
                            ) {
                                Text(
                                    text = metaLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    WavySliderExpressive(
                        value = progress,
                        onValueChange = onSeek,
                        isPlaying = isPlaying,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.22f),
                        thumbColor = colors.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else song.duration
                        Text(
                            text = formatTime(effectiveDuration),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                PlaybackControlsRow(
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onPlayPause = onPlayPause,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    buttonHeight = 52.dp,
                    shuffleRepeatSize = 40.dp,
                    colors = colors
                )
            }
        } else {
            val artworkSize = if (heroWidth < 800.dp) 210.dp else 260.dp
            val gap = if (heroWidth < 800.dp) 18.dp else 24.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(artworkSize)
                            .shadow(20.dp, shape = RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        AsyncArtworkImage(
                            artworkUri = song.albumArtUriString,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 72.dp,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            contentDescription = "${song.title} artwork"
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp,
                                        lineHeight = 32.sp
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (song.album.isNotBlank() && song.album != song.title) {
                                        "${song.displayArtist} • ${song.album}"
                                    } else {
                                        song.displayArtist
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WavySliderExpressive(
                            value = progress,
                            onValueChange = onSeek,
                            isPlaying = isPlaying,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.onPrimaryContainer.copy(alpha = 0.22f),
                            thumbColor = colors.primary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTime(currentPositionMs),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                                )

                                val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else song.duration
                                Text(
                                    text = formatTime(effectiveDuration),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }

                            val metaLabel = formatAudioMetaLabel(song)
                            if (metaLabel.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.15f),
                                    contentColor = colors.onPrimaryContainer
                                ) {
                                    Text(
                                        text = metaLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        PlaybackControlsRow(
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            onPlayPause = onPlayPause,
                            onPrev = onPrev,
                            onNext = onNext,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat,
                            buttonHeight = 56.dp,
                            shuffleRepeatSize = 42.dp,
                            colors = colors
                        )
                    }

                    VerticalVolumeBar(
                        volume = volume,
                        isMuted = isMuted,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .height(180.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalVolumeBar(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isDraggingVolume by remember { mutableStateOf(false) }
    var localVolume by remember(volume) { mutableFloatStateOf(volume) }
    val displayVolume = if (isMuted) 0f else if (isDraggingVolume) localVolume else volume

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(44.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val sliderLength = maxHeight

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
                modifier = Modifier
                    .requiredWidth(sliderLength)
                    .graphicsLayer {
                        rotationZ = 270f
                    }
            )
        }

        IconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(36.dp)
        ) {
            val volIcon = when {
                isMuted || displayVolume == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
                displayVolume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                else -> Icons.AutoMirrored.Rounded.VolumeUp
            }
            Icon(
                imageVector = volIcon,
                contentDescription = "Volume",
                tint = if (isMuted) colors.error else colors.onPrimaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun PlaybackControlsRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    buttonHeight: Dp = 56.dp,
    shuffleRepeatSize: Dp = 42.dp,
    colors: ColorScheme,
    modifier: Modifier = Modifier
) {
    var lastClickedButton by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lastClickedButton) {
        if (lastClickedButton != null) {
            kotlinx.coroutines.delay(220L)
            lastClickedButton = null
        }
    }

    fun weightFor(button: String): Float = when (lastClickedButton) {
        button -> 1.25f
        null -> 1.0f
        else -> 0.72f
    }

    val prevWeight by animateFloatAsState(
        targetValue = weightFor("PREVIOUS"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "prevWeightDesk"
    )
    val playWeight by animateFloatAsState(
        targetValue = weightFor("PLAY_PAUSE"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "playWeightDesk"
    )
    val nextWeight by animateFloatAsState(
        targetValue = weightFor("NEXT"),
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "nextWeightDesk"
    )

    val playPauseCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 38.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "playCornerDesk"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier
                .size(shuffleRepeatSize)
                .clip(CircleShape)
                .background(if (isShuffle) colors.primary else colors.onPrimary.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) colors.onPrimary else colors.primary,
                modifier = Modifier.size(if (shuffleRepeatSize < 40.dp) 18.dp else 20.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.onPrimary)
                    .clickable {
                        lastClickedButton = "PREVIOUS"
                        onPrev()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = colors.primary,
                    modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(playWeight * 1.35f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(playPauseCornerRadius))
                    .background(colors.primary)
                    .clickable {
                        lastClickedButton = "PLAY_PAUSE"
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(if (buttonHeight < 50.dp) 28.dp else 34.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.onPrimary)
                    .clickable {
                        lastClickedButton = "NEXT"
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Track",
                    tint = colors.primary,
                    modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                )
            }
        }

        val repIcon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier
                .size(shuffleRepeatSize)
                .clip(CircleShape)
                .background(if (repeatMode > 0) colors.primary else colors.onPrimary.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = repIcon,
                contentDescription = "Repeat",
                tint = if (repeatMode > 0) colors.onPrimary else colors.primary,
                modifier = Modifier.size(if (shuffleRepeatSize < 40.dp) 18.dp else 20.dp)
            )
        }
    }
}
