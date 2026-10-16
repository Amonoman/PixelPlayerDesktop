package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.PlaybackStatsSummary
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.data.model.TimelineEntry
import com.kayoi.pixelplayerdesktop.utils.formatListeningDurationCompact
import com.kayoi.pixelplayerdesktop.utils.formatListeningDurationLong

@Composable
fun StatsOverviewCard(
    summary: PlaybackStatsSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(28.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 20.dp)
                    ) {
                        Text(
                            text = "Listening Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = (summary?.range ?: StatsTimeRange.WEEK).displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "View Listening Stats",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Crossfade(
                    targetState = summary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 22.dp)
                ) { currentSummary ->
                    if (currentSummary == null) {
                        PlaceholderOverviewContent()
                    } else {
                        OverviewContent(currentSummary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewContent(summary: PlaybackStatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = formatListeningDurationLong(summary.totalDurationMs),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Plays",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.totalPlayCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Avg per Day",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatListeningDurationCompact(summary.averageDailyDurationMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val topTrack = summary.topSongs.firstOrNull()
        if (topTrack != null) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Top Track",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = topTrack.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${topTrack.artist} • ${topTrack.playCount} plays",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        MiniListeningTimeline(summary)
    }
}

@Composable
private fun MiniListeningTimeline(summary: PlaybackStatsSummary?) {
    val timeline = remember(summary) { summary?.timeline ?: emptyList() }
    if (summary?.range == StatsTimeRange.MONTH && timeline.isNotEmpty()) {
        MonthlyHorizontalListeningTimeline(timeline)
        return
    }

    val maxDuration = timeline.maxOfOrNull { it.totalDurationMs }?.takeIf { it > 0 } ?: 1L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val entries = if (timeline.isEmpty()) {
            List(7) { null }
        } else {
            timeline.takeLast(minOf(7, timeline.size))
        }

        entries.forEach { entry ->
            val heightFraction = entry?.let { it.totalDurationMs.toFloat() / maxDuration.toFloat() }?.coerceIn(0f, 1f) ?: 0.1f
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((70.dp * heightFraction).coerceAtLeast(8.dp))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyHorizontalListeningTimeline(timeline: List<TimelineEntry>) {
    val maxDuration = timeline.maxOfOrNull { it.totalDurationMs }?.takeIf { it > 0 } ?: 1L
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timeline.forEach { entry ->
            val widthFraction = (entry.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0.06f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderOverviewContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaceholderLine(width = 140.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PlaceholderLine(width = 60.dp)
            PlaceholderLine(width = 60.dp)
        }
        PlaceholderLine(width = 120.dp)
        MiniListeningTimeline(null)
    }
}

@Composable
private fun PlaceholderLine(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(18.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    )
}
