package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.ToggleSegmentButton
import com.kayoi.pixelplayerdesktop.presentation.components.WavySliderExpressive
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.BubblesLine
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import java.io.File

enum class ExpandedTab {
    LYRICS,
    QUEUE
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatAudioMetaLabel(song: Song): String {
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
fun ExpandedPlayerScreen(
    viewModel: PlayerViewModel,
    initialTab: ExpandedTab = ExpandedTab.LYRICS,
    onClose: () -> Unit,
    isMaximized: Boolean = false,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onCloseWindow: () -> Unit = {},
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    modifier: Modifier = Modifier
) {
    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()

    val songColorScheme = remember(currentSong?.albumArtUriString, isDarkTheme, isDynamicColor) {
        if (isDynamicColor && currentSong?.albumArtUriString != null) {
            val pair = DynamicColorProcessor.extractColorScheme(currentSong?.albumArtUriString)
            if (isDarkTheme) pair?.dark else pair?.light
        } else null
    }

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(activeTab, currentSong?.id, queue.size) {
        focusRequester.requestFocus()
    }

    if (currentSong == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val progressFraction = remember(playerInfo.currentPositionMs, playerInfo.totalDurationMs) {
        if (playerInfo.totalDurationMs > 0) {
            (playerInfo.currentPositionMs.toFloat() / playerInfo.totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    PixelPlayerTheme(
        darkTheme = isDarkTheme,
        colorSchemeOverride = songColorScheme
    ) {
        val colors = MaterialTheme.colorScheme

        Surface(
            modifier = modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Escape) {
                        if (event.type == KeyEventType.KeyDown) {
                            onClose()
                        }
                        true
                    } else false
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() },
            color = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse Player (Esc)",
                        tint = colors.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Box(modifier = Modifier.weight(1f).height(42.dp)) {
                    topBarWrapper {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WindowControlButton(
                        onClick = onMinimize,
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Minimize",
                        iconSize = 22.dp
                    )

                    WindowControlButton(
                        onClick = onMaximizeRestore,
                        icon = if (isMaximized) Icons.Rounded.FullscreenExit else Icons.Rounded.CropSquare,
                        contentDescription = if (isMaximized) "Restore" else "Maximize",
                        iconSize = 20.dp
                    )

                    WindowControlButton(
                        onClick = onCloseWindow,
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close",
                        iconSize = 22.dp,
                        isClose = true
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
                val isWideScreen = maxWidth >= 860.dp

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = colors.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                DesktopLandscapeHeroPlayer(
                                    song = currentSong!!,
                                    isPlaying = playerInfo.isPlaying,
                                    currentPositionMs = playerInfo.currentPositionMs,
                                    totalDurationMs = playerInfo.totalDurationMs,
                                    progress = progressFraction,
                                    isShuffle = playerInfo.isShuffleEnabled,
                                    repeatMode = playerInfo.repeatMode,
                                    isFavorite = playerInfo.isFavorite,
                                    volume = playerInfo.volume,
                                    isMuted = playerInfo.isMuted,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onPrev = { viewModel.playPrevious() },
                                    onNext = { viewModel.playNext() },
                                    onSeek = { fraction ->
                                        val targetMs = (fraction * playerInfo.totalDurationMs).toLong()
                                        viewModel.seekTo(targetMs)
                                    },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                                    onVolumeChange = { viewModel.setVolume(it) },
                                    onToggleMute = { viewModel.toggleMute() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                color = colors.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 22.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = PixelPlayerIcons.Lyrics,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Lyrics",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = colors.onSurface
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = colors.outlineVariant.copy(alpha = 0.25f)
                                    )

                                    SyncedLyricsView(
                                        lyrics = playerInfo.lyrics,
                                        currentPositionMs = playerInfo.currentPositionMs,
                                        onSeekToMs = { viewModel.seekTo(it) }
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .width(460.dp)
                                .fillMaxHeight(),
                            color = colors.surfaceContainerLow,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel(
                                queue = queue,
                                currentSong = currentSong,
                                isPlaying = playerInfo.isPlaying,
                                isShuffleOn = playerInfo.isShuffleEnabled,
                                repeatMode = playerInfo.repeatMode,
                                onSongClick = { _, index ->
                                    viewModel.playSongAtIndex(index)
                                },
                                onToggleShuffle = {
                                    viewModel.toggleShuffle()
                                },
                                onCycleRepeat = {
                                    viewModel.cycleRepeatMode()
                                },
                                onClearQueue = {
                                    viewModel.clearQueue()
                                },
                                onMoveItem = { from, to ->
                                    viewModel.moveQueueItem(from, to)
                                },
                                onRemoveItem = { index ->
                                    viewModel.removeFromQueue(index)
                                },
                                onPlayNext = { song ->
                                    viewModel.playSongNext(song)
                                },
                                onToggleFavorite = { song ->
                                    viewModel.toggleFavorite(song)
                                },
                                favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroTrackControlSection(
                            song = currentSong!!,
                            isPlaying = playerInfo.isPlaying,
                            currentPositionMs = playerInfo.currentPositionMs,
                            totalDurationMs = playerInfo.totalDurationMs,
                            progress = progressFraction,
                            isShuffle = playerInfo.isShuffleEnabled,
                            repeatMode = playerInfo.repeatMode,
                            isFavorite = playerInfo.isFavorite,
                            volume = playerInfo.volume,
                            isMuted = playerInfo.isMuted,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onPrev = { viewModel.playPrevious() },
                            onNext = { viewModel.playNext() },
                            onSeek = { fraction ->
                                val targetMs = (fraction * playerInfo.totalDurationMs).toLong()
                                viewModel.seekTo(targetMs)
                            },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            isCompact = true
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun VerticalVolumeBar(
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
private fun WindowControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    isClose: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme

    val bgColor by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> colors.errorContainer
            isHovered -> colors.surfaceContainerHighest.copy(alpha = 0.85f)
            else -> Color.Transparent
        },
        animationSpec = tween(120)
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> colors.onErrorContainer
            isHovered -> colors.onSurface
            else -> colors.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = tween(120)
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun DesktopLandscapeHeroPlayer(
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
private fun PlaybackControlsRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp = 56.dp,
    shuffleRepeatSize: androidx.compose.ui.unit.Dp = 42.dp,
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

@Composable
private fun HeroTrackControlSection(
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
    isCompact: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val artSize = if (isCompact) 200.dp else 270.dp

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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        WavySliderExpressive(
            value = progress,
            onValueChange = onSeek,
            isPlaying = isPlaying,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.surfaceContainerHighest,
            thumbColor = colors.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatTime(currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterStart)
            )

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
                contentDescription = "Previous Track",
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
                contentDescription = "Next Track",
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

@Composable
private fun SyncedLyricsView(
    lyrics: com.kayoi.pixelplayerdesktop.data.model.Lyrics?,
    currentPositionMs: Long,
    onSeekToMs: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    if (lyrics == null || (lyrics.synced.isNullOrEmpty() && lyrics.plain.isNullOrEmpty())) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = PixelPlayerIcons.Lyrics,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No lyrics found for this track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    if (!lyrics.synced.isNullOrEmpty()) {
        val syncedLines = lyrics.synced
        val activeIndex = remember(syncedLines, currentPositionMs) {
            syncedLines.indexOfLast { it.time <= currentPositionMs }
        }

        LaunchedEffect(activeIndex) {
            if (activeIndex in syncedLines.indices) {
                listState.animateScrollToItem(
                    index = (activeIndex - 2).coerceAtLeast(0)
                )
            } else if (activeIndex < 0) {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 36.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(syncedLines) { index, line ->
                val isActive = activeIndex >= 0 && index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex

                if (line.line.isBlank()) {
                    BubblesLine(
                        isActive = isActive,
                        color = colors.primary,
                        onSeek = { onSeekToMs(line.time.toLong()) }
                    )
                } else {
                    val fontSize by animateFloatAsState(
                        targetValue = if (isActive) 23f else 17f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "lyricFontSize"
                    )

                    val textColor = when {
                        isActive -> colors.primary
                        isPast -> colors.onSurface.copy(alpha = 0.85f)
                        else -> colors.onSurfaceVariant.copy(alpha = 0.38f)
                    }

                    Text(
                        text = line.line,
                        fontSize = fontSize.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSeekToMs(line.time.toLong()) }
                            .padding(vertical = 6.dp, horizontal = 10.dp)
                    )
                }
            }
        }
    } else if (!lyrics.plain.isNullOrEmpty()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lyrics.plain) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

