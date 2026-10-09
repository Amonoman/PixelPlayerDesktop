package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor

private val HomeRecentlyPlayedPillHeight = 68.dp
private val HomeRecentlyPlayedPillSpacing = 12.dp
private const val HomeRecentlyPlayedPillsLimit = 15
private const val HomeRecentlyPlayedPillsPerColumn = 3
internal const val RecentlyPlayedSectionMinSongsToShow = 3
private val HomeRecentlyPlayedPillArtSize = 50.dp
private val HomeRecentlyPlayedWidthSteps = listOf(190.dp, 220.dp, 250.dp, 280.dp, 310.dp)

private data class RecentlyPlayedPillCell(
    val song: Song,
    val width: Dp
)

private data class RecentlyPlayedPillRow(
    val pills: List<RecentlyPlayedPillCell>,
    val contentWidth: Dp
)

@Composable
fun RecentlyPlayedSection(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSongs = remember(songs) { songs.take(HomeRecentlyPlayedPillsLimit) }
    if (visibleSongs.size < RecentlyPlayedSectionMinSongsToShow) return

    val colors = MaterialTheme.colorScheme
    val songRows = remember(visibleSongs) {
        buildRecentlyPlayedPillRows(visibleSongs)
    }
    val maxRowContentWidth = remember(songRows) {
        songRows.maxOfOrNull { it.contentWidth } ?: 0.dp
    }
    val sharedScrollState = rememberScrollState()

    val sectionHeight = HomeRecentlyPlayedPillHeight * HomeRecentlyPlayedPillsPerColumn +
            HomeRecentlyPlayedPillSpacing * (HomeRecentlyPlayedPillsPerColumn - 1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            FilledIconButton(
                modifier = Modifier
                    .height(44.dp)
                    .width(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurface
                ),
                onClick = onSeeAllClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "See all recently played",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sectionHeight)
                .horizontalScroll(state = sharedScrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(maxRowContentWidth)
                    .height(sectionHeight),
                verticalArrangement = Arrangement.spacedBy(HomeRecentlyPlayedPillSpacing),
                horizontalAlignment = Alignment.Start
            ) {
                songRows.forEach { row ->
                    if (row.pills.isEmpty()) {
                        Spacer(modifier = Modifier.height(HomeRecentlyPlayedPillHeight))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HomeRecentlyPlayedPillHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(HomeRecentlyPlayedPillSpacing)
                        ) {
                            row.pills.forEach { cell ->
                                key(cell.song.id) {
                                    RecentlyPlayedPill(
                                        song = cell.song,
                                        isCurrentSong = currentSong?.id == cell.song.id,
                                        modifier = Modifier.width(cell.width),
                                        onClick = { onSongClick(cell.song) }
                                    )
                                }
                            }
                            val trailingGap = (maxRowContentWidth - row.contentWidth).coerceAtLeast(0.dp)
                            if (trailingGap > 0.dp) {
                                Spacer(modifier = Modifier.width(trailingGap))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedPill(
    song: Song,
    isCurrentSong: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = com.kayoi.pixelplayerdesktop.ui.theme.LocalPixelPlayerDarkTheme.current
    val colors = MaterialTheme.colorScheme

    val albumColorScheme = remember(
        song.albumArtUriString,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        DynamicColorProcessor.extractColorScheme(song.albumArtUriString)
    }

    val targetContainerColor = remember(albumColorScheme, isDark, colors) {
        if (albumColorScheme != null) {
            if (isDark) albumColorScheme.dark.primaryContainer else albumColorScheme.light.primaryContainer
        } else {
            colors.primaryContainer
        }
    }

    val targetTitleColor = remember(albumColorScheme, isDark, colors) {
        if (albumColorScheme != null) {
            if (isDark) albumColorScheme.dark.onPrimaryContainer else albumColorScheme.light.onPrimaryContainer
        } else {
            colors.onPrimaryContainer
        }
    }
    val targetArtistColor = remember(targetTitleColor) {
        targetTitleColor.copy(alpha = 0.80f)
    }

    val animatedCorner by animateDpAsState(
        targetValue = if (isCurrentSong) 18.dp else (HomeRecentlyPlayedPillHeight / 2),
        animationSpec = tween(durationMillis = 280),
        label = "pillCorner"
    )
    val animatedContainer by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 280),
        label = "pillContainer"
    )
    val titleColor by animateColorAsState(
        targetValue = targetTitleColor,
        animationSpec = tween(durationMillis = 280),
        label = "pillTitleColor"
    )
    val artistColor by animateColorAsState(
        targetValue = targetArtistColor,
        animationSpec = tween(durationMillis = 280),
        label = "pillArtistColor"
    )
    val shape = RoundedCornerShape(animatedCorner)
    val artStartPadding = (HomeRecentlyPlayedPillHeight - HomeRecentlyPlayedPillArtSize) / 2

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = animatedContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(HomeRecentlyPlayedPillHeight)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = artStartPadding, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(HomeRecentlyPlayedPillArtSize)
                    .clip(CircleShape)
            ) {
                AsyncArtworkImage(
                    artworkUri = song.albumArtUriString,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    placeholderIcon = Icons.Rounded.MusicNote,
                    placeholderIconSize = 26.dp,
                    contentDescription = song.title
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )
                Text(
                    text = song.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = artistColor
                )
            }
        }
    }
}

private fun buildRecentlyPlayedPillRows(
    visibleSongs: List<Song>
): List<RecentlyPlayedPillRow> {
    val rows = MutableList(HomeRecentlyPlayedPillsPerColumn) { mutableListOf<RecentlyPlayedPillCell>() }
    val rowPillsWidths = MutableList(HomeRecentlyPlayedPillsPerColumn) { 0.dp }
    val rowTargets = resolveRecentlyPlayedRowTargets(visibleSongs.size)

    var itemIndex = 0
    for (columnIndex in 0 until rowTargets[0]) {
        for (rowIndex in 0 until HomeRecentlyPlayedPillsPerColumn) {
            if (itemIndex >= visibleSongs.size) break
            if (columnIndex >= rowTargets[rowIndex]) continue

            val song = visibleSongs[itemIndex++]
            val cellWidth = resolveRecentlyPlayedPillWidth(song = song)
            val spacingBefore = if (rows[rowIndex].isNotEmpty()) HomeRecentlyPlayedPillSpacing else 0.dp
            rows[rowIndex] += RecentlyPlayedPillCell(
                song = song,
                width = cellWidth
            )
            rowPillsWidths[rowIndex] = rowPillsWidths[rowIndex] + spacingBefore + cellWidth
        }
    }

    return rows.mapIndexed { rowIndex, pills ->
        val rowWidth = rowPillsWidths[rowIndex]
        RecentlyPlayedPillRow(
            pills = pills,
            contentWidth = rowWidth
        )
    }
}

private fun resolveRecentlyPlayedRowTargets(totalItems: Int): IntArray {
    val base = totalItems / HomeRecentlyPlayedPillsPerColumn
    val remainder = totalItems % HomeRecentlyPlayedPillsPerColumn
    return intArrayOf(
        base + if (remainder > 0) 1 else 0,
        base + if (remainder > 1) 1 else 0,
        base
    )
}

private fun resolveRecentlyPlayedPillWidth(
    song: Song
): Dp {
    val titleLength = song.title.trim().length
    val artistLength = song.displayArtist.trim().length
    val weightedTextLength = titleLength + (artistLength * 0.55f)

    val baseStep = when {
        weightedTextLength < 18f -> 0
        weightedTextLength < 28f -> 1
        weightedTextLength < 40f -> 2
        weightedTextLength < 54f -> 3
        else -> 4
    }

    val resolvedStep = baseStep.coerceIn(0, HomeRecentlyPlayedWidthSteps.lastIndex)
    return HomeRecentlyPlayedWidthSteps[resolvedStep]
}
