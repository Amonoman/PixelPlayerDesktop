package com.kayoi.pixelplayerdesktop.presentation.screens


import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.PlaybackStatsSummary
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.data.model.TimelineEntry
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_auto_graph
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_bolt
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_calendar_month
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_category
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_hearing
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_play_circle_outline
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_headphones
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_info
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_refresh
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache
import com.kayoi.pixelplayerdesktop.utils.formatListeningDurationCompact
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape

enum class TimelineMetric(val displayName: String, val description: String) {
    ListeningTime("Listening time", "Track playback minutes"),
    PlayCount("Play count", "Track play counts")
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
    var selectedRotation by remember { mutableStateOf(RotationDimension.Tracks) }
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

                    item(key = "heavy_rotation") {
                        HeavyRotationSection(
                            summary = currentSummary,
                            selected = selectedRotation,
                            onSelected = { selectedRotation = it },
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
                        ListeningHabitsGrid(summary = currentSummary)
                    }

                    item(key = "listening_variety") {
                        ListeningVarietyCard(summary = currentSummary)
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
    val listeningFooter = if (summary.range == StatsTimeRange.DAY) {
        val count = summary.totalSessions
        if (count == 1) "1 session" else "$count sessions"
    } else {
        "${formatListeningDurationCompact(summary.averageDailyDurationMs)} / day"
    }
    val playsValue = NumberFormat.getIntegerInstance().format(summary.totalPlayCount)
    val tracksFooter = if (summary.uniqueSongs == 1) "1 track" else "${summary.uniqueSongs} tracks"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroVitalCard(
            label = "Listening",
            value = formatListeningDurationCompact(summary.totalDurationMs),
            footer = listeningFooter,
            icon = Res.drawable.ic_rounded_headphones,
            iconShape = RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            accentColor = MaterialTheme.colorScheme.primary,
            onAccentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f)
        )

        HeroVitalCard(
            label = "Plays",
            value = playsValue,
            footer = tracksFooter,
            icon = Res.drawable.ic_rounded_music_note,
            iconShape = ShapeCache.ExpressiveAvatar,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onAccentColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeroVitalCard(
    label: String,
    value: String,
    footer: String,
    icon: DrawableResource,
    iconShape: Shape,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(containerColor)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(iconShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = onAccentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = footer,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
    var showInfoBanner by remember { mutableStateOf(false) }

    val totalValue = remember(timeline, selectedMetric) {
        when (selectedMetric) {
            TimelineMetric.ListeningTime -> timeline.sumOf { it.totalDurationMs }
            TimelineMetric.PlayCount -> timeline.sumOf { it.playCount.toLong() }
        }
    }

    val peakIndex = remember(timeline, selectedMetric) {
        if (timeline.isEmpty()) 0
        else {
            val maxVal = when (selectedMetric) {
                TimelineMetric.ListeningTime -> timeline.maxOfOrNull { it.totalDurationMs } ?: 0L
                TimelineMetric.PlayCount -> timeline.maxOfOrNull { it.playCount.toLong() } ?: 0L
            }
            if (maxVal > 0L) {
                timeline.indexOfFirst {
                    val v = when (selectedMetric) {
                        TimelineMetric.ListeningTime -> it.totalDurationMs
                        TimelineMetric.PlayCount -> it.playCount.toLong()
                    }
                    v == maxVal
                }.coerceAtLeast(0)
            } else 0
        }
    }

    var selectedEntryIndex by remember(timeline, selectedMetric, peakIndex) { mutableStateOf(peakIndex) }

    val activeEntry = timeline.getOrNull(selectedEntryIndex) ?: timeline.getOrNull(peakIndex)
    val activeValue = when (selectedMetric) {
        TimelineMetric.ListeningTime -> activeEntry?.totalDurationMs ?: 0L
        TimelineMetric.PlayCount -> activeEntry?.playCount?.toLong() ?: 0L
    }

    val formattedActiveValue = when (selectedMetric) {
        TimelineMetric.ListeningTime -> formatListeningDurationCompact(activeValue)
        TimelineMetric.PlayCount -> if (activeValue == 1L) "1 play" else "$activeValue plays"
    }

    val percentOfTotal = if (totalValue > 0L) {
        ((activeValue.toDouble() / totalValue.toDouble()) * 100).roundToInt()
    } else 0

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
                text = getRhythmTitle(range),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            IconButton(
                onClick = { showInfoBanner = !showInfoBanner },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_info),
                    contentDescription = "Rhythm Info",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showInfoBanner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainerHighest.copy(alpha = 0.65f))
                    .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getRhythmDescription(range),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showInfoBanner = false },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_close),
                        contentDescription = "Close",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimelineMetric.entries.forEach { metric ->
                            val isSelected = metric == selectedMetric
                            val pillBackground by animateColorAsState(
                                targetValue = if (isSelected) colors.primary else Color.Transparent,
                                label = "metricPillBg"
                            )
                            val pillTextColor by animateColorAsState(
                                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                label = "metricPillText"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(pillBackground)
                                    .clickable { onMetricSelected(metric) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = metric.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = pillTextColor
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedActiveValue,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colors.onSurface
                    )

                    Text(
                        text = "$percentOfTotal% of total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }

                TimelineBarChart(
                    entries = timeline,
                    metric = selectedMetric,
                    selectedIndex = selectedEntryIndex,
                    peakIndex = peakIndex,
                    onSelectIndex = { selectedEntryIndex = it }
                )
            }
        }
    }
}

@Composable
private fun TimelineBarChart(
    entries: List<TimelineEntry>,
    metric: TimelineMetric,
    selectedIndex: Int,
    peakIndex: Int,
    onSelectIndex: (Int) -> Unit,
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

    val barColor = colors.primary
    val trackColor = colors.onSurface.copy(alpha = 0.08f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEachIndexed { index, entry ->
            val currentValue = when (metric) {
                TimelineMetric.ListeningTime -> entry.totalDurationMs
                TimelineMetric.PlayCount -> entry.playCount.toLong()
            }
            val progress = if (maxValue > 0L && currentValue > 0L) {
                (currentValue.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val isPeak = currentValue > 0L && index == peakIndex
            val isSelected = index == selectedIndex

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onSelectIndex(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 20.dp, max = 42.dp)
                        .fillMaxWidth(0.72f)
                        .clip(CircleShape)
                        .background(trackColor),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val barFillColor = if (isPeak) colors.primary else colors.primary.copy(alpha = 0.40f)
                    if (progress > 0f) {
                        if (progress < 0.12f) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(barFillColor)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress)
                                    .clip(CircleShape)
                                    .background(barFillColor)
                            ) {
                                if (isPeak) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .size(24.dp)
                                            .clip(RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f))
                                            .background(colors.surfaceContainerLowest)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_rounded_check),
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun segmentedItemShape(
    index: Int,
    count: Int,
    outer: Dp = 22.dp,
    inner: Dp = 8.dp
): RoundedCornerShape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

enum class RotationDimension(
    val label: String,
    val emptyTitle: String,
    val emptySubtitle: String,
    val emptyIcon: DrawableResource
) {
    Tracks(
        "Tracks",
        "No top tracks",
        "Listen to your favorites to see them highlighted here.",
        Res.drawable.ic_outlined_music_note
    ),
    Artists(
        "Artists",
        "No top artists",
        "Keep listening and your favorite artists will show up here.",
        Res.drawable.ic_outlined_person
    ),
    Albums(
        "Albums",
        "No top albums",
        "Albums you revisit often will appear here.",
        Res.drawable.ic_outlined_album
    ),
    Genres(
        "Genres",
        "No category data yet",
        "Press play to surface your listening highlights",
        Res.drawable.ic_outlined_category
    )
}

private data class RotationEntry(
    val key: String,
    val dimension: RotationDimension,
    val title: String,
    val artist: String?,
    val artUri: String?,
    val durationMs: Long,
    val playCount: Int,
    val childCount: Int
)

private fun buildRotationEntries(
    summary: PlaybackStatsSummary,
    dimension: RotationDimension,
    songs: List<Song>
): List<RotationEntry> {
    val entries = when (dimension) {
        RotationDimension.Tracks -> summary.topSongs.ifEmpty { summary.songs }.map {
            RotationEntry(
                key = it.songId,
                dimension = dimension,
                title = it.title,
                artist = it.artist,
                artUri = it.albumArtUri ?: songs.firstOrNull { s -> s.id == it.songId }?.albumArtUriString,
                durationMs = it.totalDurationMs,
                playCount = it.playCount,
                childCount = 1
            )
        }
        RotationDimension.Artists -> summary.topArtists.map {
            RotationEntry(
                key = it.artist,
                dimension = dimension,
                title = it.artist,
                artist = null,
                artUri = songs.firstOrNull { s -> s.artist.equals(it.artist, ignoreCase = true) && !s.albumArtUriString.isNullOrBlank() }?.albumArtUriString,
                durationMs = it.totalDurationMs,
                playCount = it.playCount,
                childCount = it.uniqueSongs
            )
        }
        RotationDimension.Albums -> summary.topAlbums.map {
            RotationEntry(
                key = it.album,
                dimension = dimension,
                title = it.album,
                artist = null,
                artUri = it.albumArtUri ?: songs.firstOrNull { s -> s.album.equals(it.album, ignoreCase = true) }?.albumArtUriString,
                durationMs = it.totalDurationMs,
                playCount = it.playCount,
                childCount = it.uniqueSongs
            )
        }
        RotationDimension.Genres -> summary.topGenres.map {
            RotationEntry(
                key = it.genre,
                dimension = dimension,
                title = it.genre,
                artist = null,
                artUri = null,
                durationMs = it.totalDurationMs,
                playCount = it.playCount,
                childCount = it.uniqueArtists
            )
        }
    }
    return entries.filter { it.durationMs > 0L || it.playCount > 0 }.take(5)
}

private fun rotationSupportingText(entry: RotationEntry): String? = when (entry.dimension) {
    RotationDimension.Tracks -> entry.artist?.takeIf { it.isNotBlank() }
    RotationDimension.Artists,
    RotationDimension.Albums -> if (entry.childCount == 1) "1 track" else "${entry.childCount} tracks"
    RotationDimension.Genres -> if (entry.childCount == 1) "1 artist" else "${entry.childCount} artists"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeavyRotationSection(
    summary: PlaybackStatsSummary,
    selected: RotationDimension,
    onSelected: (RotationDimension) -> Unit,
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
                text = "Heavy rotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = "What you kept coming back to in this range.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RotationDimension.entries.forEach { dimension ->
                val isSelected = dimension == selected
                val containerColor = if (isSelected) colors.primary else colors.surfaceContainer
                val contentColor = if (isSelected) colors.onPrimary else colors.onSurface

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(containerColor)
                        .clickable { onSelected(dimension) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = dimension.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }

        val entries = remember(summary, selected, songs) {
            buildRotationEntries(summary, selected, songs)
        }

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(selected.emptyIcon),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = selected.emptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = selected.emptySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val leader = entries.first()
            val chasers = entries.drop(1)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RotationSpotlightCard(
                    entry = leader,
                    onEntryClick = { if (it.dimension == RotationDimension.Tracks) onPlaySong?.invoke(it.key) }
                )

                if (chasers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        chasers.forEachIndexed { index, entry ->
                            RotationRankRow(
                                entry = entry,
                                rank = index + 2,
                                fractionOfLeader = (entry.durationMs.toFloat() / leader.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f),
                                shape = segmentedItemShape(index, chasers.size),
                                onEntryClick = { if (it.dimension == RotationDimension.Tracks) onPlaySong?.invoke(it.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RotationSpotlightCard(
    entry: RotationEntry,
    onEntryClick: (RotationEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = MaterialTheme.colorScheme.secondaryContainer
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    val badgeShape = ShapeCache.ExpressiveAvatar

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .clickable { onEntryClick(entry) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(106.dp)) {
            RotationArtwork(
                entry = entry,
                cornerRadius = 22.dp,
                large = true,
                modifier = Modifier
                    .size(98.dp)
                    .align(Alignment.BottomEnd)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopStart)
                    .clip(badgeShape)
                    .background(container)
                    .padding(3.dp)
                    .clip(badgeShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Top pick",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content.copy(alpha = 0.76f)
            )
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val supporting = rotationSupportingText(entry)
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpotlightStatChip(
                    text = formatListeningDurationCompact(entry.durationMs),
                    contentColor = content
                )
                val playText = if (entry.playCount == 1) "1 play" else "${entry.playCount} plays"
                SpotlightStatChip(
                    text = playText,
                    contentColor = content
                )
            }
        }
    }
}

@Composable
private fun SpotlightStatChip(text: String, contentColor: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        maxLines = 1
    )
}

@Composable
private fun RotationRankRow(
    entry: RotationEntry,
    rank: Int,
    fractionOfLeader: Float,
    shape: Shape,
    onEntryClick: (RotationEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val meterColor = MaterialTheme.colorScheme.primary
    val meterTrack = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onEntryClick(entry) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        RotationArtwork(
            entry = entry,
            cornerRadius = 12.dp,
            large = false,
            modifier = Modifier.size(48.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val supporting = rotationSupportingText(entry)
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(5.dp)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2f)
                        drawRoundRect(color = meterTrack, cornerRadius = radius)
                        val width = size.width * fractionOfLeader.coerceIn(0f, 1f)
                        if (width > 0f) {
                            drawRoundRect(
                                color = meterColor,
                                size = Size(width.coerceAtLeast(size.height), size.height),
                                cornerRadius = radius
                            )
                        }
                    }
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatListeningDurationCompact(entry.durationMs),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val playsText = if (entry.playCount == 1) "1 play" else "${entry.playCount} plays"
            Text(
                text = playsText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RotationArtwork(
    entry: RotationEntry,
    cornerRadius: Dp,
    large: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        entry.dimension == RotationDimension.Genres -> MonogramArtwork(
            text = entry.title,
            shape = RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f),
            large = large,
            modifier = modifier
        )
        entry.dimension == RotationDimension.Artists && entry.artUri.isNullOrBlank() -> MonogramArtwork(
            text = entry.title,
            shape = CircleShape,
            large = large,
            modifier = modifier
        )
        else -> {
            val artShape = if (entry.dimension == RotationDimension.Artists) CircleShape else RoundedCornerShape(cornerRadius)
            AsyncArtworkImage(
                artworkUri = entry.artUri,
                shape = artShape,
                placeholderIcon = if (entry.dimension == RotationDimension.Artists) Res.drawable.ic_artist else Res.drawable.ic_outlined_music_note,
                modifier = modifier.clip(artShape)
            )
        }
    }
}

@Composable
private fun MonogramArtwork(
    text: String,
    shape: Shape,
    large: Boolean,
    modifier: Modifier = Modifier
) {
    val initials = remember(text) {
        text.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

private fun bentoShape(index: Int): Shape {
    val outer = 26.dp
    val inner = 8.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 1) outer else inner,
        bottomStart = if (index == 2) outer else inner,
        bottomEnd = if (index == 3) outer else inner
    )
}

@Composable
private fun ListeningHabitsGrid(
    summary: PlaybackStatsSummary,
    modifier: Modifier = Modifier
) {
    val timelineShowsPeakDay = summary.range == StatsTimeRange.DAY || summary.range == StatsTimeRange.WEEK

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Listening habits",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sessionsText = if (summary.totalSessions == 1) "1 session" else "${summary.totalSessions} sessions"
                HabitTile(
                    icon = Res.drawable.ic_outlined_hearing,
                    iconShape = CircleShape,
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    onIconContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    value = formatListeningDurationCompact(summary.averageSessionDurationMs),
                    label = "Avg session",
                    supporting = sessionsText,
                    shape = bentoShape(0),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                if (timelineShowsPeakDay) {
                    val daysText = if (summary.longestStreakDays == 1) "1 day" else "${summary.longestStreakDays} days"
                    HabitTile(
                        icon = Res.drawable.ic_outlined_calendar_month,
                        iconShape = ShapeCache.ExpressiveAvatar,
                        iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                        onIconContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        value = daysText,
                        label = "Longest streak",
                        supporting = null,
                        shape = bentoShape(1),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                } else {
                    val activeDayValue = summary.peakDayLabel?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "—"
                    val activeDaySupporting = if (summary.peakDayDurationMs > 0L) {
                        formatListeningDurationCompact(summary.peakDayDurationMs)
                    } else {
                        "No playback yet"
                    }
                    HabitTile(
                        icon = Res.drawable.ic_outlined_calendar_month,
                        iconShape = ShapeCache.ExpressiveAvatar,
                        iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                        onIconContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                        value = activeDayValue,
                        label = "Most active day",
                        supporting = activeDaySupporting,
                        shape = bentoShape(1),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val longestSessionValue = if (summary.longestSessionDurationMs > 0L) {
                    formatListeningDurationCompact(summary.longestSessionDurationMs)
                } else {
                    "—"
                }
                HabitTile(
                    icon = Res.drawable.ic_outlined_bolt,
                    iconShape = RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f),
                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    onIconContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    value = longestSessionValue,
                    label = "Longest session",
                    supporting = null,
                    shape = bentoShape(2),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                val activeDaysText = if (summary.activeDays == 1) "1 active day" else "${summary.activeDays} active days"
                val avgSessionsPerDay = if (summary.activeDays > 0) summary.totalSessions.toDouble() / summary.activeDays.toDouble() else 0.0
                HabitTile(
                    icon = Res.drawable.ic_outlined_auto_graph,
                    iconShape = RoundedCornerShape(12.dp),
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    onIconContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    value = String.format(Locale.getDefault(), "%.1f", avgSessionsPerDay),
                    label = "Sessions/day",
                    supporting = activeDaysText,
                    shape = bentoShape(3),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun HabitTile(
    icon: DrawableResource,
    iconShape: Shape,
    iconContainer: Color,
    onIconContainer: Color,
    value: String,
    label: String,
    supporting: String?,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainer)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(iconShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = onIconContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(
            Modifier
                .weight(1f)
                .heightIn(min = 18.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (supporting != null) {
            Text(
                text = supporting,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class VarietySlice(
    val label: String,
    val durationMs: Long,
    val share: Float
)

private fun varietySlices(summary: PlaybackStatsSummary): List<VarietySlice> {
    val durations = summary.songs.map { it.totalDurationMs }.sortedDescending()
    val total = durations.sum().coerceAtLeast(1L)
    val topOne = durations.firstOrNull() ?: 0L
    val topTwoThree = durations.drop(1).take(2).sum()
    val others = (total - topOne - topTwoThree).coerceAtLeast(0L)
    return listOf(
        VarietySlice("Top 1", topOne, topOne.toFloat() / total),
        VarietySlice("Top 2-3", topTwoThree, topTwoThree.toFloat() / total),
        VarietySlice("Others", others, others.toFloat() / total)
    ).filter { it.durationMs > 0L }
}

@Composable
private fun ListeningVarietyCard(
    summary: PlaybackStatsSummary,
    modifier: Modifier = Modifier
) {
    val slices = remember(summary) { varietySlices(summary) }
    val sliceColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Listening variety",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "How your listening time is distributed across your top tracks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (slices.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_outlined_auto_graph),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "No concentration data yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Play more tracks to see how focused your listening is.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@Column
        }

        val topThreeShare = slices
            .filter { it.label != "Others" }
            .sumOf { it.share.toDouble() }
            .toFloat()
        val playCount = if (summary.totalPlayCount > 0) summary.totalPlayCount else summary.songs.sumOf { it.playCount }
        val averagePlaysPerTrack = playCount.toFloat() / summary.songs.size.coerceAtLeast(1)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VarietyCapsuleBar(slices = slices, colors = sliceColors)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                slices.forEachIndexed { index, slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(sliceColors[index])
                        )
                        Text(
                            text = slice.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatListeningDurationCompact(slice.durationMs),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VarietyMetricPill(
                    value = "${(topThreeShare * 100f).roundToInt()}%",
                    label = "Top 3 share",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                VarietyMetricPill(
                    value = String.format(Locale.getDefault(), "%.1f", averagePlaysPerTrack),
                    label = "Avg plays/track",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun VarietyCapsuleBar(
    slices: List<VarietySlice>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        slices.forEachIndexed { index, slice ->
            val first = index == 0
            val last = index == slices.lastIndex
            val outer = 11.dp
            val inner = 4.dp
            val weight = slice.share.coerceAtLeast(0.03f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = if (first) outer else inner,
                            bottomStart = if (first) outer else inner,
                            topEnd = if (last) outer else inner,
                            bottomEnd = if (last) outer else inner
                        )
                    )
                    .background(colors[index])
            )
        }
    }
}

@Composable
private fun VarietyMetricPill(
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.8f),
            maxLines = 2
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
    StatsTimeRange.DAY -> "Daily rhythm"
    StatsTimeRange.WEEK -> "Weekly rhythm"
    StatsTimeRange.MONTH -> "Monthly rhythm"
    StatsTimeRange.YEAR -> "Year at a glance"
    StatsTimeRange.ALL -> "All-time progression"
}

private fun getGroupingSubtitle(range: StatsTimeRange): String = when (range) {
    StatsTimeRange.DAY -> "Divided into 4-hour windows"
    StatsTimeRange.WEEK -> "Daily bars by day of week"
    StatsTimeRange.MONTH -> "Grouped by week of month"
    StatsTimeRange.YEAR -> "Grouped by month"
    StatsTimeRange.ALL -> "Grouped by year"
}

private fun getRhythmDescription(range: StatsTimeRange): String = when (range) {
    StatsTimeRange.DAY -> "Split into 4-hour windows to reveal your daily rhythm."
    StatsTimeRange.WEEK -> "Daily bars make week-to-week habits easy to compare."
    StatsTimeRange.MONTH -> "Weekly bars show how the month is trending."
    StatsTimeRange.YEAR -> "Monthly bars show seasonality across the year."
    StatsTimeRange.ALL -> "Yearly bars summarize your full history."
}

private fun getTimelineSupportingCopy(metric: TimelineMetric, range: StatsTimeRange): String {
    return "${metric.description}. ${getRhythmDescription(range)}"
}
