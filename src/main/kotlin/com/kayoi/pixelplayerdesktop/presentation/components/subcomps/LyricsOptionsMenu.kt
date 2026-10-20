package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitch
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons

@Composable
fun LyricsOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentAlignment: String,
    onAlignmentChange: (String) -> Unit,
    hasLyrics: Boolean,
    hasSyncedLyrics: Boolean = false,
    hasRomanizedLyrics: Boolean = false,
    showRomanization: Boolean = true,
    onToggleRomanization: () -> Unit = {},
    showSyncControls: Boolean = false,
    onToggleSyncControls: () -> Unit = {},
    onSaveLyrics: () -> Unit,
    onResetLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    menuWidth: Dp = 310.dp
) {
    val colors = MaterialTheme.colorScheme

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .width(menuWidth)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = colors.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PixelPlayerIcons.Lyrics,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lyrics Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Text Alignment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceContainerLowest)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AlignmentSegment(
                    label = "Left",
                    icon = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                    selected = currentAlignment == "left",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("left") }
                )
                AlignmentSegment(
                    label = "Center",
                    icon = Icons.Rounded.FormatAlignCenter,
                    selected = currentAlignment == "center",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("center") }
                )
                AlignmentSegment(
                    label = "Right",
                    icon = Icons.AutoMirrored.Rounded.FormatAlignRight,
                    selected = currentAlignment == "right",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("right") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (hasSyncedLyrics) {
                ExpressiveMenuItem(
                    icon = Icons.Rounded.Tune,
                    iconContainerColor = if (showSyncControls) colors.primaryContainer else colors.surfaceContainerHighest,
                    iconColor = if (showSyncControls) colors.onPrimaryContainer else colors.primary,
                    title = if (showSyncControls) "Hide Sync Tuning" else "Adjust Lyrics Sync",
                    subtitle = if (showSyncControls) "Close timing adjustment bar" else "Calibrate sync offset",
                    onClick = {
                        onDismissRequest()
                        onToggleSyncControls()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (hasRomanizedLyrics) {
                ExpressiveToggleMenuItem(
                    icon = Icons.Rounded.Abc,
                    iconContainerColor = if (showRomanization) colors.primaryContainer else colors.surfaceContainerHighest,
                    iconColor = if (showRomanization) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    title = "Show Romanization",
                    subtitle = "Display Romaji / Pinyin",
                    checked = showRomanization,
                    onCheckedChange = { onToggleRomanization() }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            if (hasLyrics) {
                ExpressiveMenuItem(
                    icon = Icons.Rounded.Save,
                    iconContainerColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer,
                    title = "Save Lyrics...",
                    subtitle = "Save to folder, export, or embed tag",
                    onClick = {
                        onDismissRequest()
                        onSaveLyrics()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            ExpressiveMenuItem(
                icon = Icons.Rounded.RestartAlt,
                iconContainerColor = colors.errorContainer.copy(alpha = 0.7f),
                iconColor = colors.onErrorContainer,
                title = "Reset Lyrics",
                titleColor = colors.error,
                subtitle = "Revert to embedded / local file",
                subtitleColor = colors.error.copy(alpha = 0.75f),
                onClick = {
                    onDismissRequest()
                    onResetLyrics()
                }
            )
        }
    }
}

@Composable
private fun AlignmentSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "alignSegmentBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 180),
        label = "alignSegmentContent"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun ExpressiveMenuItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun ExpressiveToggleMenuItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

