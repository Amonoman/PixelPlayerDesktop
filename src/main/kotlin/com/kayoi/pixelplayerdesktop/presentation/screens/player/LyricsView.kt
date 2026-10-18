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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.BubblesLine
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.utils.MultiLangRomanizer

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    isLoading: Boolean = false,
    currentPositionFlow: StateFlow<Long>? = null,
    onSeekToMs: (Long) -> Unit,
    onSearchLyrics: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    currentPositionMs: Long = 0L,
    alignment: String = "left",
    syncOffsetMs: Int = 0,
    showRomanization: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val positionMsState = currentPositionFlow?.collectAsState()
    val lyricTextAlign = when (alignment.lowercase()) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        else -> TextAlign.Start
    }
    val horizontalAlignment = when (alignment.lowercase()) {
        "center" -> Alignment.CenterHorizontally
        "right" -> Alignment.End
        else -> Alignment.Start
    }
    val lyricTransformOrigin = when (alignment.lowercase()) {
        "center" -> TransformOrigin(0.5f, 0.5f)
        "right" -> TransformOrigin(1f, 0.5f)
        else -> TransformOrigin(0f, 0.5f)
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = colors.primary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading lyrics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

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
        val activeIndex by remember(syncedLines, positionMsState, syncOffsetMs) {
            derivedStateOf {
                val currentPos = (positionMsState?.value ?: currentPositionMs) + syncOffsetMs
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

        val hasKana = remember(syncedLines) {
            syncedLines.any { line -> line.line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 36.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(syncedLines) { index, line ->
                val isActive = activeIndex >= 0 && index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex

                if (line.line.isBlank()) {
                    BubblesLine(
                        isActive = isActive,
                        isPlaying = isPlaying,
                        color = colors.primary,
                        alignment = alignment,
                        onSeek = { onSeekToMs(((line.time - syncOffsetMs).toLong()).coerceAtLeast(0L)) }
                    )
                } else {
                    val scaleAnim = animateFloatAsState(
                        targetValue = if (isActive) 1f else 17f / 23f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "lyricScale"
                    )

                    val alphaAnim = animateFloatAsState(
                        targetValue = when {
                            isActive -> 1f
                            isPast -> 0.85f
                            else -> 0.38f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "lyricAlpha"
                    )

                    val textColor = if (isActive) {
                        colors.primary
                    } else if (isPast) {
                        colors.onSurface
                    } else {
                        colors.onSurfaceVariant
                    }

                    val romanization = remember(line, hasKana) {
                        line.romanization?.takeIf { it.isNotBlank() }
                            ?: if (MultiLangRomanizer.isScriptThatNeedsRomanization(line.line)) {
                                MultiLangRomanizer.romanizeText(line.line, hasKana)
                            } else null
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSeekToMs(((line.time - syncOffsetMs).toLong()).coerceAtLeast(0L)) }
                            .padding(vertical = 4.dp, horizontal = 10.dp)
                            .graphicsLayer {
                                scaleX = scaleAnim.value
                                scaleY = scaleAnim.value
                                alpha = alphaAnim.value
                                transformOrigin = lyricTransformOrigin
                            },
                        horizontalAlignment = horizontalAlignment
                    ) {
                        Text(
                            text = line.line,
                            fontSize = 23.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor,
                            textAlign = lyricTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showRomanization && !romanization.isNullOrBlank()) {
                            Text(
                                text = romanization,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = textColor.copy(alpha = if (isActive) 0.85f else 0.75f),
                                textAlign = lyricTextAlign,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    } else if (!lyrics.plain.isNullOrEmpty()) {
        val hasKana = remember(lyrics.plain) {
            lyrics.plain.any { line -> line.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } }
        }

        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lyrics.plain) { rawLine ->
                val parts = remember(rawLine) { rawLine.split("\n") }
                val primary = parts.firstOrNull().orEmpty()
                val romanization = remember(rawLine, primary, hasKana) {
                    if (parts.size > 1 && MultiLangRomanizer.isScriptThatNeedsRomanization(primary)) {
                        parts[1]
                    } else if (MultiLangRomanizer.isScriptThatNeedsRomanization(primary)) {
                        MultiLangRomanizer.romanizeText(primary, hasKana)
                    } else null
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = horizontalAlignment
                ) {
                    Text(
                        text = primary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface.copy(alpha = 0.85f),
                        textAlign = lyricTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showRomanization && !romanization.isNullOrBlank()) {
                        Text(
                            text = romanization,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = lyricTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
