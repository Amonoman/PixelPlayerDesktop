package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import com.kayoi.pixelplayerdesktop.generated.resources.ic_lyrics
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_format_align_left
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_format_align_right
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_abc
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_format_align_center
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_restart_alt
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_save
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_tune
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitch

@Composable
fun LyricsOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentAlignment: String,
    onAlignmentChange: (String) -> Unit,
    currentTextSize: String = "medium",
    onTextSizeChange: (String) -> Unit = {},
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
    menuWidth: Dp = 310.dp,
    offset: DpOffset = DpOffset(x = -(menuWidth - 32.dp), y = 8.dp)
) {
    val colors = MaterialTheme.colorScheme

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = offset,
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
                    painter = painterResource(Res.drawable.ic_lyrics),
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
                    icon = Res.drawable.ic_automirrored_rounded_format_align_left,
                    selected = currentAlignment == "left",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("left") }
                )
                AlignmentSegment(
                    label = "Center",
                    icon = Res.drawable.ic_rounded_format_align_center,
                    selected = currentAlignment == "center",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("center") }
                )
                AlignmentSegment(
                    label = "Right",
                    icon = Res.drawable.ic_automirrored_rounded_format_align_right,
                    selected = currentAlignment == "right",
                    modifier = Modifier.weight(1f),
                    onClick = { onAlignmentChange("right") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Text Size",
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
                TextSizeSegment(
                    label = "Small",
                    selected = currentTextSize == "small",
                    modifier = Modifier.weight(1f),
                    onClick = { onTextSizeChange("small") }
                )
                TextSizeSegment(
                    label = "Medium",
                    selected = currentTextSize == "medium",
                    modifier = Modifier.weight(1f),
                    onClick = { onTextSizeChange("medium") }
                )
                TextSizeSegment(
                    label = "Large",
                    selected = currentTextSize == "large",
                    modifier = Modifier.weight(1f),
                    onClick = { onTextSizeChange("large") }
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
                    icon = Res.drawable.ic_rounded_tune,
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
                    icon = Res.drawable.ic_rounded_abc,
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
                    icon = Res.drawable.ic_rounded_save,
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
                icon = Res.drawable.ic_rounded_restart_alt,
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
    icon: DrawableResource,
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
            painter = painterResource(icon),
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
private fun TextSizeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "textSizeSegmentBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 180),
        label = "textSizeSegmentContent"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
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
    icon: DrawableResource,
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
                painter = painterResource(icon),
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
    icon: DrawableResource,
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
                painter = painterResource(icon),
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

