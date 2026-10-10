package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ToggleSegmentButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    activeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    activeCornerRadius: Dp = 20.dp,
    inactiveCornerRadius: Dp = 10.dp,
    text: String,
    imageVector: ImageVector? = null,
    onClick: () -> Unit
) {
    val targetBgColor = if (active) activeColor else inactiveColor
    val bgColor by animateColorAsState(
        targetValue = if (enabled) targetBgColor else targetBgColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "segmentBg"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (active) activeCornerRadius else inactiveCornerRadius,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "segmentCorner"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = if (active) activeContentColor else inactiveContentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (active) activeContentColor else inactiveContentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
