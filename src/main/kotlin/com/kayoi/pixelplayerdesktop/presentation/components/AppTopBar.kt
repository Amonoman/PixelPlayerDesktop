package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AppTooltip
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
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    onSearchFocusChange: (Boolean) -> Unit = {},
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    onGoBack: () -> Unit = {},
    onGoForward: () -> Unit = {},
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
        val isCompactQueue = availableWidth < 800.dp
        val horizontalPadding = if (isNarrow) 12.dp else 20.dp

        val searchBarWidth = when {
            availableWidth >= 1150.dp -> 460.dp
            availableWidth >= 900.dp -> 380.dp
            availableWidth >= 720.dp -> 300.dp
            availableWidth >= 560.dp -> 240.dp
            else -> (availableWidth - 220.dp).coerceIn(140.dp, 200.dp)
        }

        val targetName = when (currentDestination) {
            DesktopNavDestination.HOME -> if (searchBarWidth >= 420.dp) "songs, albums, artists" else "songs, artists"
            DesktopNavDestination.SONGS -> "songs"
            DesktopNavDestination.ALBUMS -> "albums"
            DesktopNavDestination.ARTISTS -> "artists"
            DesktopNavDestination.PLAYLISTS -> "playlists"
            DesktopNavDestination.FOLDERS -> "folders"
            DesktopNavDestination.LIKED -> "liked songs"
            DesktopNavDestination.SETTINGS -> "settings"
        }

        val placeholder = when {
            searchBarWidth >= 260.dp -> "Search $targetName... (Ctrl+K)"
            searchBarWidth >= 180.dp -> "Search $targetName..."
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

        val sideSlotMaxWidth = (((availableWidth - searchBarWidth) / 2) - horizontalPadding - 8.dp).coerceAtLeast(70.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(max = sideSlotMaxWidth),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTooltip(
                    text = "Back (Alt+Left)",
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(0.dp, 6.dp)
                ) {
                    IconButton(
                        onClick = onGoBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back (Alt+Left)",
                            tint = if (canGoBack) colors.onSurface else colors.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AppTooltip(
                    text = "Forward (Alt+Right)",
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(0.dp, 6.dp)
                ) {
                    IconButton(
                        onClick = onGoForward,
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Forward (Alt+Right)",
                            tint = if (canGoForward) colors.onSurface else colors.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = currentDestination.title,
                    style = if (isNarrow) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
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

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 4.dp else 8.dp)
            ) {
                QueueToggleButton(
                    visible = showQueueToggle && queueCount > 0,
                    isQueueOpen = isQueueOpen,
                    queueCount = queueCount,
                    onClick = onToggleQueue,
                    showLabel = !isCompactQueue
                )
            }
        }
    }
}
