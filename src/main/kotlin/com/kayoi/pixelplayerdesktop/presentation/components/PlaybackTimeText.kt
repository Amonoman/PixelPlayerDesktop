package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun PlaybackCurrentTimeText(
    positionMsState: State<Long>?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified
) {
    val formattedTime by remember(positionMsState) {
        derivedStateOf {
            formatTime(positionMsState?.value ?: 0L)
        }
    }
    Text(
        text = formattedTime,
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

@Composable
fun PlaybackCurrentTimeText(
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified
) {
    val formattedTime by remember(positionProvider) {
        derivedStateOf {
            formatTime(positionProvider())
        }
    }
    Text(
        text = formattedTime,
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}
