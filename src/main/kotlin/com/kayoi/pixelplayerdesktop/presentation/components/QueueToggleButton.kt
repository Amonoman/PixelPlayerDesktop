package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Queue toggle pill shared by the main top bar and the expanded player.
 *
 * Shows the queue icon, an optional "Queue" label and a badge with the current
 * queue count. The label is hidden on narrow widths by the caller, and the
 * badge count animates whenever [queueCount] changes.
 *
 * [visible] drives the enter/exit animation so both screens behave identically.
 */
@Composable
fun QueueToggleButton(
    visible: Boolean,
    isQueueOpen: Boolean,
    queueCount: Int,
    onClick: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) +
            scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) +
            expandHorizontally(
                expandFrom = Alignment.End,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        exit = fadeOut(animationSpec = tween(150)) +
            scaleOut(targetScale = 0.8f, animationSpec = tween(150)) +
            shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(180)),
        modifier = modifier
    ) {
        val containerColor by animateColorAsState(
            targetValue = if (isQueueOpen) colors.primaryContainer else colors.surfaceContainerHighest.copy(alpha = 0.6f),
            animationSpec = tween(180),
            label = "queueToggleContainerColor"
        )
        val contentColor by animateColorAsState(
            targetValue = if (isQueueOpen) colors.onPrimaryContainer else colors.onSurfaceVariant,
            animationSpec = tween(180),
            label = "queueToggleContentColor"
        )
        val badgeBackgroundColor by animateColorAsState(
            targetValue = if (isQueueOpen) colors.primary else colors.surfaceContainerHighest,
            animationSpec = tween(180),
            label = "queueToggleBadgeContainerColor"
        )
        val badgeTextColor by animateColorAsState(
            targetValue = if (isQueueOpen) colors.onPrimary else colors.onSurfaceVariant,
            animationSpec = tween(180),
            label = "queueToggleBadgeTextColor"
        )

        Surface(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (showLabel) 12.dp else 8.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (showLabel) 6.dp else 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Toggle Queue Panel",
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                if (showLabel) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isQueueOpen) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBackgroundColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    AnimatedContent(
                        targetState = queueCount,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        }
                    ) { count ->
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
