package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun LyricsSyncControls(
    offsetMillis: Int,
    onOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = colors.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SyncStepButton(
                text = "-0.5s",
                tooltip = "Lyrics 0.5s earlier",
                onClick = { onOffsetChange(offsetMillis - 500) },
                modifier = Modifier.weight(1f)
            )

            SyncStepButton(
                text = "-0.1s",
                tooltip = "Lyrics 0.1s earlier",
                onClick = { onOffsetChange(offsetMillis - 100) },
                modifier = Modifier.weight(1f)
            )

            val isOffsetActive = offsetMillis != 0
            val offsetText = when {
                offsetMillis == 0 -> "0.0s"
                offsetMillis > 0 -> String.format(Locale.US, "+%.1fs", offsetMillis / 1000f)
                else -> String.format(Locale.US, "%.1fs", offsetMillis / 1000f)
            }

            val centerBgColor by animateColorAsState(
                targetValue = if (isOffsetActive) colors.primaryContainer else colors.surfaceContainerLowest,
                label = "centerBgColor"
            )
            val centerTextColor by animateColorAsState(
                targetValue = if (isOffsetActive) colors.onPrimaryContainer else colors.onSurfaceVariant,
                label = "centerTextColor"
            )

            AppTooltip(if (isOffsetActive) "Click to reset offset to 0s" else "Timing synced (0.0s)") {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(centerBgColor)
                        .border(
                            width = 1.dp,
                            color = if (isOffsetActive) colors.primary.copy(alpha = 0.45f) else colors.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = isOffsetActive) { onOffsetChange(0) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = offsetText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        fontWeight = if (isOffsetActive) FontWeight.Bold else FontWeight.Medium,
                        color = centerTextColor,
                        maxLines = 1
                    )
                }
            }

            SyncStepButton(
                text = "+0.1s",
                tooltip = "Lyrics 0.1s later",
                onClick = { onOffsetChange(offsetMillis + 100) },
                modifier = Modifier.weight(1f)
            )

            SyncStepButton(
                text = "+0.5s",
                tooltip = "Lyrics 0.5s later",
                onClick = { onOffsetChange(offsetMillis + 500) },
                modifier = Modifier.weight(1f)
            )
        }

        if (onClose != null) {
            AppTooltip("Close sync tuning") {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStepButton(
    text: String,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    AppTooltip(tooltip) {
        Box(
            modifier = modifier
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = colors.outlineVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = (-0.2).sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1
            )
        }
    }
}
