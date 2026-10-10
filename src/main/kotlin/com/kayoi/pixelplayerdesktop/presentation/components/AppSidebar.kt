package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.navigation.NavSection

@Composable
fun AppSidebar(
    currentDestination: DesktopNavDestination,
    onNavigate: (DesktopNavDestination) -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val animatedWidth by animateDpAsState(
        targetValue = if (isCompact) 74.dp else 244.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sidebarWidth"
    )

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight(),
        color = colors.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = colors.surfaceContainerHighest.copy(alpha = 0.60f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PixelPlayerIcons.AppLogo,
                        contentDescription = "PixelPlayer",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    AnimatedVisibility(
                        visible = !isCompact,
                        enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(tween(200)),
                        exit = fadeOut(tween(80)) + shrinkHorizontally(tween(150))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PixelPlayer",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavSection.entries.forEach { section ->
                    if (section != NavSection.SYSTEM) {
                        AnimatedVisibility(
                            visible = !isCompact && section != NavSection.MENU,
                            enter = fadeIn(tween(180, delayMillis = 40)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(60)) + shrinkVertically(tween(140))
                        ) {
                            Text(
                                text = section.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 14.dp, bottom = 4.dp),
                                fontSize = 11.5.sp,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        DesktopNavDestination.entries
                            .filter { it.section == section }
                            .forEach { destination ->
                                SidebarNavItem(
                                    destination = destination,
                                    isSelected = currentDestination == destination,
                                    isCompact = isCompact,
                                    onClick = { onNavigate(destination) }
                                )
                            }
                    }
                }
            }

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            SidebarNavItem(
                destination = DesktopNavDestination.SETTINGS,
                isSelected = currentDestination == DesktopNavDestination.SETTINGS,
                isCompact = isCompact,
                onClick = { onNavigate(DesktopNavDestination.SETTINGS) }
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    destination: DesktopNavDestination,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor = when {
        isSelected -> colors.secondaryContainer
        isHovered -> colors.surfaceContainerHighest.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val iconColor = when {
        isSelected -> colors.onSecondaryContainer
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    val textColor = when {
        isSelected -> colors.onSurface
        isHovered -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            AnimatedVisibility(
                visible = !isCompact,
                enter = fadeIn(tween(180, delayMillis = 60)) + expandHorizontally(tween(200)),
                exit = fadeOut(tween(80)) + shrinkHorizontally(tween(150))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
