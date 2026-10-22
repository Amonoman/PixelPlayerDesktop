package com.kayoi.pixelplayerdesktop.presentation.screens


import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_auto_graph
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_bolt
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_calendar_month
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_hearing
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_history
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_play_circle_outline
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.PlaybackStatsSummary
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.data.model.TimelineEntry
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache
import com.kayoi.pixelplayerdesktop.utils.formatListeningDurationCompact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

enum class TimelineMetric(val displayName: String, val description: String) {
    ListeningTime("Listening Time", "Track playback minutes"),
    PlayCount("Play Count", "Track play counts")
}

enum class CategoryDimension(
    val displayName: String,
    val topLabel: String
) {
    Song("Song", "Top Song"),
    Album("Album", "Top Album"),
    Artist("Artist", "Top Artist"),
    Genre("Genre", "Top Genre")
}

@Composable
fun StatsScreen(
    viewModel: PlayerViewModel,
    songs: List<Song>,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statsHolder = viewModel.statsStateHolder
    val selectedRange by statsHolder.selectedRange.collectAsState()
    val summary by statsHolder.currentSummary.collectAsState()

    val colors = MaterialTheme.colorScheme
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 4 }
    }

    var selectedTimelineMetric by remember { mutableStateOf(TimelineMetric.ListeningTime) }
    var selectedCategoryDimension by remember { mutableStateOf(CategoryDimension.Song) }
    var isRefreshing by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        val screenWidth = maxWidth
        val isVeryCompact = screenWidth < 460.dp
        val isCompact = screenWidth < 600.dp
        val horizontalPadding = if (isVeryCompact) 16.dp else 24.dp

        Column(modifier = Modifier.fillMaxSize()) {
            val refreshRotation by animateFloatAsState(
                targetValue = if (isRefreshing) 360f else 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "statsTopBarRefreshRotation"
            )

            val currentSummary = summary
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = 40.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "stats_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Listening Stats",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = colors.onSurface
                        )

                        FilledTonalIconButton(
                            onClick = {
                                if (!isRefreshing) {
                                    coroutineScope.launch {
                                        isRefreshing = true
                                        statsHolder.refreshStats()
                                        delay(500)
                                        isRefreshing = false
                                    }
                                }
                            },
                            enabled = !isRefreshing,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surfaceContainerLow,
                                contentColor = colors.onSurface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = "Refresh Stats",
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = refreshRotation }
                            )
                        }
                    }
                }

                item(key = "stats_range_tabs") {
                    RangeTabsHeader(
                        ranges = StatsTimeRange.entries,
                        selected = selectedRange,
                        onRangeSelected = { statsHolder.selectRange(it) },
                        horizontalPadding = 0.dp
                    )
                }

                if (currentSummary == null || !currentSummary.hasListeningActivity()) {
                    item(key = "stats_empty") {
                        StatsEmptyState(
                            range = selectedRange,
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth()
                        )
                    }
                } else {
                    item(key = "hero_section") {
                        StatsHeroSection(summary = currentSummary)
                    }

                    item(key = "listening_timeline") {
                        ListeningTimelineSection(
                            summary = currentSummary,
                            selectedMetric = selectedTimelineMetric,
                            onMetricSelected = { selectedTimelineMetric = it }
                        )
                    }

                    item(key = "category_metrics") {
                        CategoryMetricsSection(
                            summary = currentSummary,
                            selectedDimension = selectedCategoryDimension,
                            onDimensionSelected = { selectedCategoryDimension = it },
                            songs = songs,
                            onPlaySong = { songId ->
                                val song = songs.firstOrNull { it.id == songId }
                                if (song != null) {
                                    viewModel.playSong(song)
                                }
                            }
                        )
                    }

                    item(key = "listening_habits") {
                        ListeningHabitsCard(summary = currentSummary)
                    }

                    item(key = "track_concentration") {
                        TrackConcentrationCard(summary = currentSummary)
                    }
                }
            }
        }
    }
}


@Composable
private fun RangeTabsHeader(
    ranges: List<StatsTimeRange>,
    selected: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ranges.forEach { range ->
            val isSelected = range == selected
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.surfaceContainerLowest,
                animationSpec = tween(durationMillis = 180),
                label = "rangeTabBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180),
                label = "rangeTabContent"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(containerColor)
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun StatsHeroSection(
    summary: PlaybackStatsSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroCard(
            title = "Listening",
            value = formatListeningDurationCompact(summary.totalDurationMs),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )

        HeroCard(
            title = "Plays",
            value = summary.totalPlayCount.toString(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeroCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeningTimelineSection(
    summary: PlaybackStatsSummary,
    selectedMetric: TimelineMetric,
    onMetricSelected: (TimelineMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val range = summary.range
    val timeline = summary.timeline

    val cardColor = when (range) {
        StatsTimeRange.DAY, StatsTimeRange.WEEK -> colors.primaryContainer.copy(alpha = 0.45f)
        StatsTimeRange.MONTH, StatsTimeRange.YEAR -> colors.secondaryContainer.copy(alpha = 0.42f)
        StatsTimeRange.ALL -> colors.tertiaryContainer.copy(alpha = 0.40f)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Listening Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = getTimelineSupportingCopy(selectedMetric, range),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TimelineMetric.entries.forEach { metric ->
                val isSelected = metric == selectedMetric
                val containerColor = if (isSelected) colors.primary else colors.surfaceContainer
                val contentColor = if (isSelected) colors.onPrimary else colors.onSurface

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(containerColor)
                        .clickable { onMetricSelected(metric) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = metric.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = getRhythmTitle(range),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = getGroupingSubtitle(range),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.surfaceContainerHighest.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedMetric.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                }

                TimelineBarChart(
                    entries = timeline,
                    metric = selectedMetric,
                    range = range
                )
            }
        }
    }
}

@Composable
private fun TimelineBarChart(
    entries: List<TimelineEntry>,
    metric: TimelineMetric,
    range: StatsTimeRange,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val maxValue = remember(entries, metric) {
        val rawMax = when (metric) {
            TimelineMetric.ListeningTime -> entries.maxOfOrNull { it.totalDurationMs } ?: 1L
            TimelineMetric.PlayCount -> entries.maxOfOrNull { it.playCount.toLong() } ?: 1L
        }
        max(1L, rawMax)
    }

    val highlightColor = when (range) {
        StatsTimeRange.DAY, StatsTimeRange.WEEK -> colors.primary
        StatsTimeRange.MONTH, StatsTimeRange.YEAR -> colors.secondary
        StatsTimeRange.ALL -> colors.tertiary
    }
    val regularColor = highlightColor.copy(alpha = 0.72f)
    val trackColor = colors.onSurface.copy(alpha = 0.10f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val currentValue = when (metric) {
                TimelineMetric.ListeningTime -> entry.totalDurationMs
                TimelineMetric.PlayCount -> entry.playCount.toLong()
            }
            val progress = if (maxValue > 0L && currentValue > 0L) {
                (currentValue.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val isPeak = currentValue > 0L && currentValue == maxValue

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTimelineEntryValue(metric, entry),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .widthIn(min = 18.dp, max = 34.dp)
                        .fillMaxWidth(0.65f)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(trackColor),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(progress)
                                .clip(CircleShape)
                                .background(if (isPeak) highlightColor else regularColor)
                        )
                    }
                }

                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatTimelineEntryValue(metric: TimelineMetric, entry: TimelineEntry): String = when (metric) {
    TimelineMetric.ListeningTime -> {
        if (entry.totalDurationMs > 0L) formatListeningDurationCompact(entry.totalDurationMs) else "-"
    }
    TimelineMetric.PlayCount -> {
        if (entry.playCount > 0) "${entry.playCount}" else "-"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryMetricsSection(
    summary: PlaybackStatsSummary,
    selectedDimension: CategoryDimension,
    onDimensionSelected: (CategoryDimension) -> Unit,
    songs: List<Song> = emptyList(),
    onPlaySong: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Top Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = "Compare how you listen across genres, artists, albums, and songs.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CategoryDimension.entries.forEach { dimension ->
                val isSelected = dimension == selectedDimension
                val containerColor = if (isSelected) colors.primary else colors.surfaceContainer
                val contentColor = if (isSelected) colors.onPrimary else colors.onSurface

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(containerColor)
                        .clickable { onDimensionSelected(dimension) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = dimension.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }

        val entries: List<UnifiedCategoryItem> = when (selectedDimension) {
            CategoryDimension.Song -> summary.topSongs.take(5).map { song ->
                UnifiedCategoryItem(
                    id = song.songId,
                    title = song.title,
                    subtitle = song.artist,
                    artworkUri = song.albumArtUri ?: songs.firstOrNull { it.id == song.songId }?.albumArtUriString,
                    durationMs = song.totalDurationMs,
                    playCount = song.playCount,
                    isSong = true,
                    isArtist = false
                )
            }
            CategoryDimension.Album -> summary.topAlbums.take(5).map { album ->
                UnifiedCategoryItem(
                    id = album.album,
                    title = album.album,
                    subtitle = if (album.uniqueSongs == 1) "1 song" else "${album.uniqueSongs} songs",
                    artworkUri = album.albumArtUri ?: songs.firstOrNull { it.album.equals(album.album, ignoreCase = true) }?.albumArtUriString,
                    durationMs = album.totalDurationMs,
                    playCount = album.playCount,
                    isSong = false,
                    isArtist = false
                )
            }
            CategoryDimension.Artist -> summary.topArtists.take(5).map { artist ->
                UnifiedCategoryItem(
                    id = artist.artist,
                    title = artist.artist,
                    subtitle = if (artist.uniqueSongs == 1) "1 song" else "${artist.uniqueSongs} songs",
                    artworkUri = songs.firstOrNull { it.artist.equals(artist.artist, ignoreCase = true) && !it.albumArtUriString.isNullOrBlank() }?.albumArtUriString,
                    durationMs = artist.totalDurationMs,
                    playCount = artist.playCount,
                    isSong = false,
                    isArtist = true
                )
            }
            CategoryDimension.Genre -> summary.topGenres.take(5).map { genre ->
                UnifiedCategoryItem(
                    id = genre.genre,
                    title = genre.genre,
                    subtitle = if (genre.uniqueArtists == 1) "1 artist" else "${genre.uniqueArtists} artists",
                    artworkUri = null,
                    durationMs = genre.totalDurationMs,
                    playCount = genre.playCount,
                    isSong = false,
                    isArtist = false
                )
            }
        }.filter { it.durationMs > 0L || it.playCount > 0 }

        if (entries.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No listening activity for ${selectedDimension.displayName.lowercase()}s in this range",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val topItem = entries.first()
            val otherItems = entries.drop(1)

            TopSpotlightCard(
                item = topItem,
                dimension = selectedDimension,
                onSongClick = onPlaySong
            )

            if (otherItems.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        otherItems.forEachIndexed { index, item ->
                            val rank = index + 2
                            CategoryRankItem(
                                rank = rank,
                                item = item,
                                dimension = selectedDimension,
                                onSongClick = onPlaySong
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSpotlightCard(
    item: UnifiedCategoryItem,
    dimension: CategoryDimension,
    onSongClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (item.isSong && onSongClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { onSongClick(item.id) }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#1",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = dimension.topLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatListeningDurationCompact(item.durationMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    val playCountText = if (item.playCount == 1) "1 play" else "${item.playCount} plays"
                    Text(
                        text = playCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (dimension == CategoryDimension.Genre) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val artShape = if (item.isArtist) ShapeCache.ExpressiveAvatar else RoundedCornerShape(18.dp)
                    AsyncArtworkImage(
                        artworkUri = item.artworkUri,
                        shape = artShape,
                        placeholderIcon = if (item.isArtist) Res.drawable.ic_artist else Res.drawable.ic_outlined_music_note,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(artShape)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.subtitle.isNotBlank()) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRankItem(
    rank: Int,
    item: UnifiedCategoryItem,
    dimension: CategoryDimension,
    onSongClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (item.isSong && onSongClick != null) {
                    Modifier.clickable { onSongClick(item.id) }
                } else Modifier
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.width(22.dp),
            textAlign = TextAlign.Center
        )

        if (dimension != CategoryDimension.Genre) {
            val artShape = if (item.isArtist) ShapeCache.ExpressiveAvatar else RoundedCornerShape(12.dp)
            AsyncArtworkImage(
                artworkUri = item.artworkUri,
                shape = artShape,
                placeholderIcon = if (item.isArtist) Res.drawable.ic_artist else Res.drawable.ic_outlined_music_note,
                modifier = Modifier
                    .size(46.dp)
                    .clip(artShape)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formatListeningDurationCompact(item.durationMs),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            val playCountText = if (item.playCount == 1) "1 play" else "${item.playCount} plays"
            Text(
                text = playCountText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

private data class UnifiedCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUri: String?,
    val durationMs: Long,
    val playCount: Int,
    val isSong: Boolean = false,
    val isArtist: Boolean = false
)

@Composable
private fun ListeningHabitsCard(
    summary: PlaybackStatsSummary,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Listening Habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitTile(
                    icon = Res.drawable.ic_outlined_history,
                    label = "Sessions",
                    value = summary.totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                HabitTile(
                    icon = Res.drawable.ic_outlined_hearing,
                    label = "Avg Session",
                    value = formatListeningDurationCompact(summary.averageSessionDurationMs),
                    modifier = Modifier.weight(1f)
                )
                HabitTile(
                    icon = Res.drawable.ic_outlined_bolt,
                    label = "Longest",
                    value = formatListeningDurationCompact(summary.longestSessionDurationMs),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.35f))

            if (!summary.peakDayLabel.isNullOrBlank()) {
                HabitHighlightRow(
                    icon = Res.drawable.ic_outlined_calendar_month,
                    title = "Most Active Day",
                    value = summary.peakDayLabel,
                    supporting = formatListeningDurationCompact(summary.peakDayDurationMs)
                )
            }

            HabitHighlightRow(
                icon = Res.drawable.ic_outlined_auto_graph,
                title = "Active Days & Streak",
                value = "${summary.activeDays} days",
                supporting = "Longest streak: ${summary.longestStreakDays} days"
            )
        }
    }
}

@Composable
private fun HabitTile(
    icon: DrawableResource,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerLowest)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
    }
}

@Composable
private fun HabitHighlightRow(
    icon: DrawableResource,
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
        }

        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = colors.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TrackConcentrationCard(
    summary: PlaybackStatsSummary,
    modifier: Modifier = Modifier
) {
    val songs = summary.songs
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Track Concentration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Distribution of your listening catalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (songs.isNotEmpty()) {
                val totalDuration = summary.totalDurationMs.coerceAtLeast(1L)
                val topOneDuration = songs.firstOrNull()?.totalDurationMs ?: 0L
                val topThreeDuration = songs.take(3).sumOf { it.totalDurationMs }
                val topThreeShare = (topThreeDuration.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your top 3 tracks account for ${(topThreeShare * 100).toInt()}% of your total listening time.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val topOneFraction = (topOneDuration.toFloat() / totalDuration.toFloat()).coerceIn(0.04f, 1f)
                        val topTwoToThree = (topThreeDuration - topOneDuration).coerceAtLeast(0L)
                        val topTwoThreeFraction = (topTwoToThree.toFloat() / totalDuration.toFloat()).coerceIn(0.04f, 1f)
                        val othersFraction = (1f - topOneFraction - topTwoThreeFraction).coerceAtLeast(0.04f)

                        Box(modifier = Modifier.weight(topOneFraction).fillMaxHeight().background(colors.primary))
                        Box(modifier = Modifier.weight(topTwoThreeFraction).fillMaxHeight().background(colors.secondary))
                        Box(modifier = Modifier.weight(othersFraction).fillMaxHeight().background(colors.outlineVariant))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ConcentrationLegend(color = colors.primary, label = "Top 1")
                        ConcentrationLegend(color = colors.secondary, label = "Top 2–3")
                        ConcentrationLegend(color = colors.outlineVariant, label = "Other Tracks")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConcentrationLegend(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsEmptyState(
    range: StatsTimeRange,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 44.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outlined_play_circle_outline),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "No Activity for ${range.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Text(
                    text = "Play songs in PixelPlayer to track your listening statistics and habits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getRhythmTitle(range: StatsTimeRange): String = when (range) {
    StatsTimeRange.DAY -> "Daily Rhythm"
    StatsTimeRange.WEEK -> "Weekly Rhythm"
    StatsTimeRange.MONTH -> "Monthly Rhythm"
    StatsTimeRange.YEAR -> "Yearly Rhythm"
    StatsTimeRange.ALL -> "All-Time Rhythm"
}

private fun getGroupingSubtitle(range: StatsTimeRange): String = when (range) {
    StatsTimeRange.DAY -> "Divided into 4-hour windows"
    StatsTimeRange.WEEK -> "Daily bars by day of week"
    StatsTimeRange.MONTH -> "Grouped by week of month"
    StatsTimeRange.YEAR -> "Grouped by month"
    StatsTimeRange.ALL -> "Grouped by year"
}

private fun getTimelineSupportingCopy(metric: TimelineMetric, range: StatsTimeRange): String {
    val rangeText = when (range) {
        StatsTimeRange.DAY -> "Split into 4-hour windows to reveal your daily rhythm."
        StatsTimeRange.WEEK -> "Daily bars make week-to-week habits easy to compare."
        StatsTimeRange.MONTH -> "Weekly bars show how the month is trending."
        StatsTimeRange.YEAR -> "Monthly bars show seasonality across the year."
        StatsTimeRange.ALL -> "Yearly bars summarize your full history."
    }
    return "${metric.description}. $rangeText"
}
