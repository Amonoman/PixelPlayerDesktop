package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.screens.player.DesktopLandscapeHeroPlayer
import com.kayoi.pixelplayerdesktop.presentation.screens.player.HeroTrackControlSection
import com.kayoi.pixelplayerdesktop.presentation.screens.player.SyncedLyricsView
import com.kayoi.pixelplayerdesktop.presentation.screens.player.WindowControlButton
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor

enum class ExpandedTab {
    LYRICS,
    QUEUE
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
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()

    val songColorScheme = remember(currentSong?.albumArtUriString, isDarkTheme, isDynamicColor, paletteStyle, colorAccuracy) {
        if (isDynamicColor && currentSong?.albumArtUriString != null) {
            val pair = DynamicColorProcessor.extractColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )
            if (isDarkTheme) pair?.dark else pair?.light
        } else null
    }

    val activeTab by remember(initialTab) { mutableStateOf(initialTab) }
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
                                    .width(340.dp)
                                    .fillMaxHeight(),
                                color = colors.surfaceContainerLow,
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                QueueSidePanel(
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
