package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.PlaybackCurrentTimeText
import com.kayoi.pixelplayerdesktop.presentation.components.ToggleSegmentButton
import com.kayoi.pixelplayerdesktop.presentation.components.WavySliderExpressive
import com.kayoi.pixelplayerdesktop.presentation.components.formatTime
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HeroTrackControlSection(
    song: Song,
    isPlaying: Boolean,
    totalDurationMs: Long,
    currentPositionFlow: StateFlow<Long>? = null,
    currentPositionMs: Long = 0L,
    progress: Float = 0f,
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
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val artSize = if (isCompact) 200.dp else 270.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncArtworkImage(
            artworkUri = song.albumArtUriString,
            modifier = Modifier
                .size(artSize)
                .shadow(24.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            placeholderIcon = Icons.Rounded.MusicNote,
            placeholderIconSize = 80.dp,
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            contentDescription = "${song.title} artwork"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.displayArtist,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val positionState = currentPositionFlow?.collectAsState()
        val effectiveProgress = if (currentPositionFlow != null && totalDurationMs > 0) {
            val pos = positionState?.value ?: 0L
            (pos.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else progress

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            WavySliderExpressive(
                value = effectiveProgress,
                onValueChange = onSeek,
                isPlaying = isPlaying,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceContainerHighest,
                thumbColor = colors.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                if (currentPositionFlow != null) {
                    PlaybackCurrentTimeText(
                        positionMsState = positionState,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                } else {
                    PlaybackCurrentTimeText(
                        positionProvider = { currentPositionMs },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(999.dp),
                    color = colors.surfaceContainerHighest,
                    contentColor = colors.onSurfaceVariant
                ) {
                    Text(
                        text = formatAudioMetaLabel(song),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                val effectiveDuration = if (totalDurationMs > 0) totalDurationMs else song.duration
                Text(
                    text = formatTime(effectiveDuration),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            label = "prevWeight"
        )
        val playWeight by animateFloatAsState(
            targetValue = weightFor("PLAY_PAUSE"),
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "playWeight"
        )
        val nextWeight by animateFloatAsState(
            targetValue = weightFor("NEXT"),
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "nextWeight"
        )

        val playPauseCornerRadius by animateDpAsState(
            targetValue = if (isPlaying) 24.dp else 60.dp,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "playCorner"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest)
                    .clickable {
                        lastClickedButton = "PREVIOUS"
                        onPrev()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Song",
                    tint = colors.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(playWeight)
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
                    modifier = Modifier.size(36.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest)
                    .clickable {
                        lastClickedButton = "NEXT"
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Song",
                    tint = colors.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(60.dp),
            color = colors.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleSegmentButton(
                    modifier = Modifier.weight(1f),
                    active = isShuffle,
                    text = "Shuffle",
                    imageVector = Icons.Rounded.Shuffle,
                    activeColor = colors.primary,
                    inactiveColor = Color.Transparent,
                    activeContentColor = colors.onPrimary,
                    inactiveContentColor = colors.onSurfaceVariant,
                    activeCornerRadius = 60.dp,
                    inactiveCornerRadius = 14.dp,
                    onClick = onToggleShuffle
                )

                val repIcon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                val repText = when (repeatMode) {
                    1 -> "All"
                    2 -> "One"
                    else -> "Off"
                }
                ToggleSegmentButton(
                    modifier = Modifier.weight(1f),
                    active = repeatMode > 0,
                    text = repText,
                    imageVector = repIcon,
                    activeColor = colors.primary,
                    inactiveColor = Color.Transparent,
                    activeContentColor = colors.onPrimary,
                    inactiveContentColor = colors.onSurfaceVariant,
                    activeCornerRadius = 60.dp,
                    inactiveCornerRadius = 14.dp,
                    onClick = onCycleRepeat
                )

                ToggleSegmentButton(
                    modifier = Modifier.weight(1f),
                    active = isFavorite,
                    text = if (isFavorite) "Liked" else "Like",
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    activeColor = colors.primary,
                    inactiveColor = Color.Transparent,
                    activeContentColor = colors.onPrimary,
                    inactiveContentColor = colors.onSurfaceVariant,
                    activeCornerRadius = 60.dp,
                    inactiveCornerRadius = 14.dp,
                    onClick = onToggleFavorite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.widthIn(max = 340.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onToggleMute, modifier = Modifier.size(34.dp)) {
                val volIcon = when {
                    isMuted || volume == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
                    volume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeUp
                }
                Icon(
                    imageVector = volIcon,
                    contentDescription = "Volume Mute Toggle",
                    tint = if (isMuted) colors.error else colors.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

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
                onValueChangeFinished = { isDraggingVolume = false },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceContainerHighest
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
