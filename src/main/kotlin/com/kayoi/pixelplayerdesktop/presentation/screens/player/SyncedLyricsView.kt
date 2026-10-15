package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.BubblesLine
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons

@Composable
fun SyncedLyricsView(
    lyrics: Lyrics?,
    currentPositionFlow: StateFlow<Long>? = null,
    onSeekToMs: (Long) -> Unit,
    onSearchLyrics: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    currentPositionMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val positionMsState = currentPositionFlow?.collectAsState()

    if (lyrics == null || (lyrics.synced.isNullOrEmpty() && lyrics.plain.isNullOrEmpty())) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
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
                    text = "No lyrics found for this song",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (onSearchLyrics != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onSearchLyrics,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Lyrics")
                    }
                }
            }
        }
        return
    }

    if (!lyrics.synced.isNullOrEmpty()) {
        val syncedLines = lyrics.synced
        val activeIndex by remember(syncedLines, positionMsState) {
            derivedStateOf {
                val currentPos = positionMsState?.value ?: currentPositionMs
                syncedLines.indexOfLast { it.time <= currentPos }
            }
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
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 36.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(syncedLines) { index, line ->
                val isActive = activeIndex >= 0 && index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex

                if (line.line.isBlank()) {
                    BubblesLine(
                        isActive = isActive,
                        isPlaying = isPlaying,
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
            modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
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
