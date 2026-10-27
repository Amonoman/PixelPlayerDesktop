package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.presentation.components.MiniPlayerBar
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

@Composable
fun PaletteStyleDetailScreen(
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4 }
    }

    val currentSong by viewModel.currentSong.collectAsState()
    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentPaletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val currentColorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()

    val effectiveSong = currentSong ?: remember {
        Song(
            id = "preview_track",
            title = "Preview Track",
            artist = "Pixel Player",
            album = "Material You Live Preview",
            duration = 214000L,
            path = "",
            albumArtUriString = null
        )
    }

    val previewScheme by produceState(
        initialValue = DynamicColorProcessor.getCachedColorScheme(
            pathOrUri = effectiveSong.albumArtUriString,
            paletteStyle = currentPaletteStyle,
            colorAccuracyLevel = currentColorAccuracy
        )?.let { if (isDarkTheme) it.dark else it.light } ?: colors,
        effectiveSong.albumArtUriString,
        currentPaletteStyle,
        currentColorAccuracy,
        isDarkTheme
    ) {
        val pair = if (effectiveSong.albumArtUriString != null) {
            DynamicColorProcessor.extractColorScheme(
                pathOrUri = effectiveSong.albumArtUriString,
                paletteStyle = currentPaletteStyle,
                colorAccuracyLevel = currentColorAccuracy
            )
        } else {
            DynamicColorProcessor.generateColorSchemePair(
                seed = colors.primary,
                paletteStyle = currentPaletteStyle
            )
        }
        value = if (isDarkTheme) pair?.dark ?: colors else pair?.light ?: colors
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Album Art Palette Style",
                subtitle = "Choose the album colors for the player UI",
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSubsection(title = "PREVIEW") {
                        MaterialTheme(colorScheme = previewScheme) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = previewScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp)
                                ) {
                                    val previewPositionFlow = remember(currentSong) {
                                        if (currentSong != null) viewModel.currentPositionMs else MutableStateFlow(75000L)
                                    }
                                    MiniPlayerBar(
                                        currentSong = effectiveSong,
                                        isPlaying = playerInfo.isPlaying,
                                        totalDurationMs = if (currentSong != null) playerInfo.totalDurationMs else 214000L,
                                        currentPositionFlow = previewPositionFlow,
                                        isShuffle = playerInfo.isShuffleEnabled,
                                        repeatMode = playerInfo.repeatMode,
                                        isFavorite = playerInfo.isFavorite,
                                        volume = playerInfo.volume,
                                        isMuted = playerInfo.isMuted,
                                        onPlayPauseClick = { if (currentSong != null) viewModel.togglePlayPause() },
                                        onPreviousClick = { if (currentSong != null) viewModel.playPrevious() },
                                        onNextClick = { if (currentSong != null) viewModel.playNext() },
                                        onSeek = { fraction ->
                                            if (currentSong != null && playerInfo.totalDurationMs > 0) {
                                                viewModel.seekTo((fraction * playerInfo.totalDurationMs).toLong())
                                            }
                                        },
                                        onToggleShuffle = { viewModel.toggleShuffle() },
                                        onCycleRepeat = { viewModel.cycleRepeatMode() },
                                        onToggleFavorite = { if (currentSong != null) viewModel.toggleFavoriteCurrentSong() },
                                        onVolumeChange = { viewModel.setVolume(it) },
                                        onToggleMute = { viewModel.toggleMute() }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSubsection(title = "PALETTE STYLE") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Palette style",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "Choose the album colors for the player UI.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }

                                if (isCompact) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val chunkedStyles = remember { AlbumArtPaletteStyle.entries.chunked(2) }
                                        chunkedStyles.forEach { rowStyles ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowStyles.forEach { style ->
                                                    PaletteStyleChip(
                                                        style = style,
                                                        isSelected = style == currentPaletteStyle,
                                                        onClick = { viewModel.setAlbumArtPaletteStyle(style) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AlbumArtPaletteStyle.entries.forEach { style ->
                                            PaletteStyleChip(
                                                style = style,
                                                isSelected = style == currentPaletteStyle,
                                                onClick = { viewModel.setAlbumArtPaletteStyle(style) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = currentPaletteStyle.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = previewScheme.tertiaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when (currentPaletteStyle) {
                                            AlbumArtPaletteStyle.TONAL_SPOT -> "Balanced and calm."
                                            AlbumArtPaletteStyle.VIBRANT -> "High saturation accents."
                                            AlbumArtPaletteStyle.EXPRESSIVE -> "Bold hue shifts and contrast."
                                            AlbumArtPaletteStyle.FRUIT_SALAD -> "Playful rotated accents."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = previewScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSubsection(title = "COLOR ACCURACY") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = "Color accuracy",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "0 keeps the current tuning. Higher values stay closer to the album art's dominant hue.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }

                                    val clamped = AlbumArtColorAccuracy.clamp(currentColorAccuracy)
                                    val badgeText = when {
                                        clamped == 0 -> "0 • Current"
                                        clamped in 1..3 -> "$clamped • Subtle"
                                        clamped in 4..7 -> "$clamped • Balanced"
                                        else -> "$clamped • Precise"
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = currentColorAccuracy.toFloat(),
                                    onValueChange = { viewModel.setAlbumArtColorAccuracy(it.roundToInt()) },
                                    valueRange = AlbumArtColorAccuracy.MIN.toFloat()..AlbumArtColorAccuracy.MAX.toFloat(),
                                    steps = AlbumArtColorAccuracy.STEPS,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primary,
                                        activeTrackColor = colors.primary,
                                        inactiveTrackColor = colors.surfaceContainerHighest
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Current",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                    Text(
                                        text = "More accurate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteStyleChip(
    style: AlbumArtPaletteStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.surfaceContainerHighest,
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        label = "chipText"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = style.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
