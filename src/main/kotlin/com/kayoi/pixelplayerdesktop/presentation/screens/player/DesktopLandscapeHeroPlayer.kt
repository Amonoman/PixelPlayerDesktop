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
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
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
        val isStackedMode = heroWidth < 460.dp

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
                            .size(72.dp)
                            .shadow(10.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncArtworkImage(
                            artworkUri = song.albumArtUriString,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = 36.dp,
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        lineHeight = 23.sp
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.displayArtist,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                AppTooltip(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                                    IconButton(
                                        onClick = onToggleFavorite,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.65f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                AppTooltip(if (isMuted) "Unmute" else "Mute") {
                                    IconButton(
                                        onClick = onToggleMute,
                                        modifier = Modifier.size(32.dp)
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
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
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
            val isCompactHero = heroWidth < 700.dp
            val artworkSize = when {
                heroWidth < 500.dp -> 105.dp
                heroWidth < 650.dp -> 135.dp
                heroWidth < 850.dp -> 175.dp
                else -> 215.dp
            }
            val gap = if (isCompactHero) 12.dp else 20.dp
            val titleFontSize = if (isCompactHero) 19.sp else 26.sp
            val titleLineHeight = if (isCompactHero) 24.sp else 32.sp
            val controlButtonHeight = if (isCompactHero) 48.dp else 56.dp
            val shuffleRepeatSize = if (isCompactHero) 36.dp else 42.dp
            val volumeBarHeight = if (isCompactHero) 140.dp else 180.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactHero) 16.dp else 22.dp,
                        vertical = if (isCompactHero) 14.dp else 18.dp
                    ),
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
                            .shadow(16.dp, shape = RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        AsyncArtworkImage(
                            artworkUri = song.albumArtUriString,
                            modifier = Modifier.fillMaxSize(),
                            placeholderIcon = Icons.Rounded.MusicNote,
                            placeholderIconSize = if (isCompactHero) 40.dp else 56.dp,
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = titleFontSize,
                                        lineHeight = titleLineHeight
                                    ),
                                    color = colors.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.displayArtist,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = if (isCompactHero) 13.5.sp else 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            AppTooltip(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.size(if (isCompactHero) 36.dp else 42.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) colors.primary else colors.onPrimaryContainer.copy(alpha = 0.65f),
                                        modifier = Modifier.size(if (isCompactHero) 22.dp else 26.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isCompactHero) 8.dp else 14.dp))

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
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onPrimaryContainer.copy(alpha = 0.85f)
                                )

                                val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else song.duration
                                Text(
                                    text = formatTime(effectiveDuration),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
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

                        Spacer(modifier = Modifier.height(if (isCompactHero) 8.dp else 14.dp))

                        PlaybackControlsRow(
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            onPlayPause = onPlayPause,
                            onPrev = onPrev,
                            onNext = onNext,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat,
                            buttonHeight = controlButtonHeight,
                            shuffleRepeatSize = shuffleRepeatSize,
                            colors = colors
                        )
                    }

                    VerticalVolumeBar(
                        volume = volume,
                        isMuted = isMuted,
                        onVolumeChange = onVolumeChange,
                        onToggleMute = onToggleMute,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .height(volumeBarHeight)
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

        AppTooltip(if (isMuted) "Unmute" else "Mute") {
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
        AppTooltip(if (isShuffle) "Shuffle: On" else "Shuffle: Off") {
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
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTooltip("Previous", modifier = Modifier.weight(prevWeight).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                        contentDescription = "Previous Song",
                        tint = colors.primary,
                        modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                    )
                }
            }

            AppTooltip(if (isPlaying) "Pause" else "Play", modifier = Modifier.weight(playWeight * 1.35f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
            }

            AppTooltip("Next", modifier = Modifier.weight(nextWeight).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                        contentDescription = "Next Song",
                        tint = colors.primary,
                        modifier = Modifier.size(if (buttonHeight < 50.dp) 24.dp else 28.dp)
                    )
                }
            }
        }

        val repIcon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        val repLabel = when (repeatMode) {
            1 -> "Repeat: All"
            2 -> "Repeat: One"
            else -> "Repeat: Off"
        }
        AppTooltip(repLabel) {
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
}
