package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentDestination: DesktopNavDestination,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showQueueToggle: Boolean = true,
    isQueueOpen: Boolean = false,
    queueCount: Int = 0,
    onToggleQueue: () -> Unit = {},
    isMaximized: Boolean = false,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {},
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    onSearchFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var isSearchFocused by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val availableWidth = maxWidth
        val isNarrow = availableWidth < 780.dp
        val isVeryNarrow = availableWidth < 580.dp
        val isCompactQueue = availableWidth < 960.dp
        val horizontalPadding = if (isNarrow) 12.dp else 20.dp

        val searchBarWidth = when {
            availableWidth >= 1150.dp -> 460.dp
            availableWidth >= 900.dp -> 380.dp
            availableWidth >= 720.dp -> 300.dp
            availableWidth >= 560.dp -> 240.dp
            else -> (availableWidth - 220.dp).coerceIn(140.dp, 200.dp)
        }

        val placeholder = when {
            searchBarWidth >= 420.dp -> "Search tracks, albums, artists... (Ctrl+K)"
            searchBarWidth >= 320.dp -> "Search tracks, artists... (Ctrl+K)"
            searchBarWidth >= 220.dp -> "Search... (Ctrl+K)"
            else -> "Search..."
        }

        val searchBorderColor by animateColorAsState(
            targetValue = if (isSearchFocused) colors.primary else colors.outlineVariant.copy(alpha = 0.25f),
            animationSpec = tween(150)
        )
        val searchBgColor by animateColorAsState(
            targetValue = if (isSearchFocused) colors.surfaceContainerHighest else colors.surfaceContainerHighest.copy(alpha = 0.6f),
            animationSpec = tween(150)
        )

        val leftSlotMaxWidth = if (isNarrow) 120.dp else 220.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.widthIn(min = 40.dp, max = leftSlotMaxWidth),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentDestination.title,
                    style = if (isNarrow) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .width(searchBarWidth)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = searchBgColor,
                border = BorderStroke(
                    width = if (isSearchFocused) 1.5.dp else 1.dp,
                    color = searchBorderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { state ->
                                    isSearchFocused = state.isFocused
                                    onSearchFocusChange(state.isFocused)
                                },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.onSurface,
                                fontSize = 13.5.sp
                            ),
                            cursorBrush = SolidColor(colors.primary)
                        )
                    }

                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onSearchQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear Search",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 4.dp else 8.dp)
            ) {
                AnimatedVisibility(
                    visible = showQueueToggle && queueCount > 0,
                enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) +
                        scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) +
                        expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(animationSpec = tween(150)) +
                       scaleOut(targetScale = 0.8f, animationSpec = tween(150)) +
                       shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(180))
            ) {
                val queueContainerColor by animateColorAsState(
                    targetValue = if (isQueueOpen) colors.primaryContainer else colors.surfaceContainerHighest.copy(alpha = 0.6f),
                    animationSpec = tween(180)
                )
                val queueContentColor by animateColorAsState(
                    targetValue = if (isQueueOpen) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    animationSpec = tween(180)
                )
                val badgeBgColor by animateColorAsState(
                    targetValue = if (isQueueOpen) colors.primary else colors.surfaceContainerHighest,
                    animationSpec = tween(180)
                )
                val badgeTextColor by animateColorAsState(
                    targetValue = if (isQueueOpen) colors.onPrimary else colors.onSurfaceVariant,
                    animationSpec = tween(180)
                )

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onToggleQueue),
                    shape = RoundedCornerShape(18.dp),
                    color = queueContainerColor
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (isCompactQueue) 8.dp else 12.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompactQueue) 4.dp else 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Toggle Queue Panel",
                            tint = queueContentColor,
                            modifier = Modifier.size(17.dp)
                        )
                        if (!isCompactQueue) {
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isQueueOpen) FontWeight.Bold else FontWeight.Medium,
                                color = queueContentColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(badgeBgColor)
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

            Spacer(modifier = Modifier.width(if (isNarrow) 2.dp else 6.dp))

            val windowButtonSize = if (isNarrow) 34.dp else 38.dp
            val windowIconSize = if (isNarrow) 18.dp else 22.dp

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 2.dp else 4.dp)
            ) {
                WindowControlButton(
                    onClick = onMinimize,
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Minimize",
                    buttonSize = windowButtonSize,
                    iconSize = windowIconSize
                )

                WindowControlButton(
                    onClick = onMaximizeRestore,
                    icon = if (isMaximized) Icons.Rounded.FullscreenExit else Icons.Rounded.CropSquare,
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    buttonSize = windowButtonSize,
                    iconSize = if (isNarrow) 16.dp else 20.dp
                )

                WindowControlButton(
                    onClick = onClose,
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close",
                    buttonSize = windowButtonSize,
                    iconSize = windowIconSize,
                    isClose = true
                )
            }
        }
    }
}
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp = 38.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    isClose: Boolean = false
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
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(120)
    )

    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = buttonSize / 2),
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
