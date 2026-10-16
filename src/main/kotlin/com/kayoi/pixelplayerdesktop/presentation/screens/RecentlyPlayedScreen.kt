package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecentlyPlayedScreen(
    recentlyPlayedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedRange by rememberSaveable { mutableStateOf(StatsTimeRange.WEEK) }

    val filteredSongs = remember(recentlyPlayedSongs, selectedRange) {
        val (startBound, endBound) = selectedRange.resolveBounds()
        recentlyPlayedSongs.filter { song ->
            val timestamp = song.lastPlayedTimestamp
            timestamp > 0 && timestamp <= endBound && (startBound == null || timestamp >= startBound)
        }
    }

    val groupedSongs = remember(filteredSongs, selectedRange) {
        groupRecentlyPlayedSongs(filteredSongs, selectedRange)
    }

    val colors = MaterialTheme.colorScheme
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

        val isScrolled by remember {
            derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 4 }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            RecentlyPlayedTopBar(
                isScrolled = isScrolled,
                onBack = onBack,
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "recently_played_hero") {
                    RecentlyPlayedHeroSection(
                        songCount = filteredSongs.size,
                        isCompact = isCompact,
                        isVeryCompact = isVeryCompact,
                        horizontalPadding = horizontalPadding
                    )
                }

                item(key = "recently_played_range_selector") {
                    RecentlyPlayedRangeSelector(
                        selected = selectedRange,
                        onRangeSelected = { selectedRange = it },
                        modifier = Modifier.padding(horizontal = horizontalPadding)
                    )
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "recently_played_actions") {
                        RecentlyPlayedActions(
                            onPlay = { viewModel.playSongs(filteredSongs, 0) },
                            onShuffle = { viewModel.shuffleSongs(filteredSongs) },
                            isVeryCompact = isVeryCompact,
                            isCompact = isCompact,
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)
                        )
                    }

                    groupedSongs.forEachIndexed { groupIndex, group ->
                        item(key = "recently_played_divider_${groupIndex}_${group.key}") {
                            RecentlyPlayedTimestampDivider(
                                label = group.label,
                                isHourBucket = group.isHourBucket,
                                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 2.dp)
                            )
                        }

                        items(group.songs, key = { it.id }) { song ->
                            val isCurrentPlaying = currentSong?.id == song.id
                            EnhancedSongListItem(
                                modifier = Modifier.padding(horizontal = horizontalPadding),
                                song = song,
                                isCurrentSong = isCurrentPlaying,
                                isPlaying = isCurrentPlaying && isPlaying,
                                showAlbumArt = true,
                                showDuration = !isVeryCompact,
                                isFavorite = song.isFavorite,
                                onPlayNext = { viewModel.playSongNext(song) },
                                onAddToQueue = { viewModel.addSongToQueue(song) },
                                onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                                onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                                onClick = {
                                    val idx = filteredSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    viewModel.playSongs(filteredSongs, idx)
                                }
                            )
                        }
                    }
                } else {
                    item(key = "recently_played_empty") {
                        RecentlyPlayedEmptyState(
                            range = selectedRange,
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedTopBar(
    isScrolled: Boolean,
    onBack: () -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val animatedBgColor by animateColorAsState(
        targetValue = if (isScrolled) colors.surfaceContainerHigh else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "rpTopBarBg"
    )

    val animatedVerticalPadding by animateDpAsState(
        targetValue = if (isScrolled) 8.dp else 12.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "rpTopBarPadding"
    )

    val collapsedTitleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "rpCollapsedTitleAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(animatedBgColor)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = animatedVerticalPadding,
                bottom = animatedVerticalPadding
            )
    ) {
        FilledTonalIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = colors.surfaceContainerLow,
                contentColor = colors.onSurface
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }

        if (collapsedTitleAlpha > 0.01f) {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface.copy(alpha = collapsedTitleAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentlyPlayedHeroSection(
    songCount: Int,
    isCompact: Boolean = false,
    isVeryCompact: Boolean = false,
    horizontalPadding: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val titleFontSize = when {
        isVeryCompact -> 28.sp
        isCompact -> 32.sp
        else -> 36.sp
    }
    val titleStyle = rememberRecentlyPlayedTitleStyle(fontSize = titleFontSize)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 4.dp,
                bottom = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Recently Played",
            style = titleStyle,
            color = colors.onSurface
        )

        val tracksText = if (songCount == 1) "1 track played" else "$songCount tracks played"
        Text(
            text = tracksText,
            style = if (isVeryCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberRecentlyPlayedTitleStyle(fontSize: TextUnit = 36.sp): TextStyle {
    return remember(fontSize) {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    "font/gflex_variable.ttf",
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(560),
                        FontVariation.width(122f),
                        FontVariation.grade(40),
                        FontVariation.Setting("ROND", 100f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(560),
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.15f).sp,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun RecentlyPlayedRangeSelector(
    selected: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsTimeRange.entries.forEach { range ->
            RecentlyPlayedRangeChip(
                label = range.displayName,
                selected = selected == range,
                onClick = { onRangeSelected(range) }
            )
        }
    }
}

@Composable
private fun RecentlyPlayedRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.tertiary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "chipContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onTertiary else colors.tertiary,
        animationSpec = tween(durationMillis = 150),
        label = "chipContent"
    )
    val iconSlotWidth by animateDpAsState(
        targetValue = if (selected) 26.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "chipIconSlotWidth"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "chipIconAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.8f,
        animationSpec = tween(durationMillis = 200),
        label = "chipIconScale"
    )

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Tab },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(width = 2.dp, color = colors.tertiary),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .height(40.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(iconSlotWidth)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            alpha = iconAlpha
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun RecentlyPlayedActions(
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
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(if (isVeryCompact) 18.dp else 22.dp)
            )
            Spacer(Modifier.width(if (isVeryCompact) 4.dp else 8.dp))
            Text(
                text = "Play Latest",
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
                imageVector = Icons.Rounded.Shuffle,
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

@Composable
private fun RecentlyPlayedTimestampDivider(
    label: String,
    isHourBucket: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val railColor = if (isHourBucket) colors.primary else colors.secondary
    val chipContainer = if (isHourBucket) {
        colors.primaryContainer.copy(alpha = 0.78f)
    } else {
        colors.secondaryContainer.copy(alpha = 0.78f)
    }
    val chipContent = if (isHourBucket) colors.onPrimaryContainer else colors.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            railColor.copy(alpha = 0f),
                            railColor.copy(alpha = 0.50f)
                        )
                    )
                )
        )

        Surface(
            color = chipContainer,
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(railColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chipContent
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            railColor.copy(alpha = 0.50f),
                            railColor.copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun RecentlyPlayedEmptyState(
    range: StatsTimeRange,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No tracks played in ${range.displayName.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Songs you listen to will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private data class TimestampGroup(
    val key: String,
    val label: String,
    val isHourBucket: Boolean,
    val songs: List<Song>
)

private fun groupRecentlyPlayedSongs(
    songs: List<Song>,
    range: StatsTimeRange
): List<TimestampGroup> {
    if (songs.isEmpty()) return emptyList()
    val zoneId = ZoneId.systemDefault()
    val sorted = songs.sortedByDescending { it.lastPlayedTimestamp }

    val groups = mutableListOf<TimestampGroup>()
    var currentBucketKey: String? = null
    var currentLabel: String? = null
    var currentIsHourBucket = false
    var bucket = mutableListOf<Song>()

    sorted.forEach { item ->
        val timeBucket = resolveTimestampBucket(
            timestamp = item.lastPlayedTimestamp,
            range = range,
            zoneId = zoneId
        )
        if (currentBucketKey == null || currentBucketKey == timeBucket.key) {
            currentBucketKey = timeBucket.key
            currentLabel = timeBucket.label
            currentIsHourBucket = timeBucket.isHourBucket
            bucket += item
        } else {
            groups += TimestampGroup(
                key = currentBucketKey,
                label = currentLabel ?: "",
                isHourBucket = currentIsHourBucket,
                songs = bucket
            )
            currentBucketKey = timeBucket.key
            currentLabel = timeBucket.label
            currentIsHourBucket = timeBucket.isHourBucket
            bucket = mutableListOf(item)
        }
    }

    if (bucket.isNotEmpty() && currentLabel != null && currentBucketKey != null) {
        groups += TimestampGroup(
            key = currentBucketKey,
            label = currentLabel,
            isHourBucket = currentIsHourBucket,
            songs = bucket
        )
    }

    return groups
}

private data class TimestampBucket(
    val key: String,
    val label: String,
    val isHourBucket: Boolean
)

private fun resolveTimestampBucket(
    timestamp: Long,
    range: StatsTimeRange,
    zoneId: ZoneId
): TimestampBucket {
    val safeTimestamp = timestamp.coerceAtLeast(0L)
    val safeNow = System.currentTimeMillis().coerceAtLeast(0L)
    val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
    val date = zonedDateTime.toLocalDate()
    val nowDate = Instant.ofEpochMilli(safeNow).atZone(zoneId).toLocalDate()

    if (range == StatsTimeRange.DAY) {
        val hourStart = zonedDateTime
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return TimestampBucket(
            key = hourStart.toInstant().toEpochMilli().toString(),
            label = hourStart.format(DateTimeFormatter.ofPattern("h a", Locale.getDefault())),
            isHourBucket = true
        )
    }

    return when {
        date == nowDate -> {
            TimestampBucket(
                key = date.toString(),
                label = "Today",
                isHourBucket = false
            )
        }
        date == nowDate.minusDays(1) -> {
            TimestampBucket(
                key = date.toString(),
                label = "Yesterday",
                isHourBucket = false
            )
        }
        range == StatsTimeRange.YEAR || range == StatsTimeRange.ALL -> {
            TimestampBucket(
                key = date.toString(),
                label = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())),
                isHourBucket = false
            )
        }
        else -> {
            TimestampBucket(
                key = date.toString(),
                label = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                isHourBucket = false
            )
        }
    }
}
