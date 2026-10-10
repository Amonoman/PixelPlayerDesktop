package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WindowControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    iconSize: Dp = 22.dp,
    isClose: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme

    val bgColor by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> colors.errorContainer
            isHovered -> colors.surfaceContainerHighest.copy(alpha = 0.85f)
            else -> Color.Transparent
        },
        animationSpec = tween(120)
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> colors.onErrorContainer
            isHovered -> colors.onSurface
            else -> colors.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = tween(120)
    )

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
