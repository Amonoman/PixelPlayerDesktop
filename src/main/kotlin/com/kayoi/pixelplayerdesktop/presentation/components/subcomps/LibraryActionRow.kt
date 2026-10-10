package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.SortOption

private val genHeight = 42.dp
private val outerCorner = 26.dp
private val innerCorner = 8.dp

@Composable
fun LibraryActionRow(
    modifier: Modifier = Modifier,
    itemCount: Int,
    itemCountLabel: String = "songs",
    currentSortOption: SortOption,
    availableSortOptions: List<SortOption> = SortOption.SONGS,
    showLocateButton: Boolean = false,
    onLocateClick: () -> Unit = {},
    showViewModeToggle: Boolean = false,
    isListView: Boolean = false,
    onToggleViewMode: () -> Unit = {},
    showShuffleButton: Boolean = true,
    showCreateButton: Boolean = false,
    createButtonLabel: String = "New",
    onCreateClick: () -> Unit = {},
    customLeadingContent: (@Composable () -> Unit)? = null,
    onSortOptionSelect: (SortOption) -> Unit,
    onShuffleClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val hasLeftNeighbors = showLocateButton || showViewModeToggle

    val sortStartCorner by animateDpAsState(
        targetValue = if (hasLeftNeighbors) innerCorner else outerCorner,
        label = "SortStartCorner"
    )

    val viewModeStartCorner by animateDpAsState(
        targetValue = if (showLocateButton) innerCorner else outerCorner,
        label = "ViewModeStartCorner"
    )

    val gapBetweenLocateAndNext by animateDpAsState(
        targetValue = if (showLocateButton) 4.dp else 0.dp,
        label = "GapBetweenLocateAndNext"
    )

    val gapBetweenViewModeAndSort by animateDpAsState(
        targetValue = if (showViewModeToggle) 4.dp else 0.dp,
        label = "GapBetweenViewModeAndSort"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        val isCompact = maxWidth < 480.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (customLeadingContent != null) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    customLeadingContent()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showCreateButton) {
                        FilledTonalButton(
                            onClick = onCreateClick,
                            shape = RoundedCornerShape(outerCorner),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(genHeight)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = createButtonLabel,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = createButtonLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (showShuffleButton) {
                        FilledTonalButton(
                            onClick = onShuffleClick,
                            shape = RoundedCornerShape(outerCorner),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.tertiaryContainer,
                                contentColor = colors.onTertiaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.height(genHeight)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle all",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!isCompact) {
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceContainerLow,
                            contentColor = colors.onSurfaceVariant
                        ) {
                            Text(
                                text = "$itemCount $itemCountLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                AnimatedVisibility(
                    visible = showLocateButton,
                    enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
                ) {
                    FilledTonalIconButton(
                        onClick = onLocateClick,
                        shape = RoundedCornerShape(
                            topStart = outerCorner,
                            bottomStart = outerCorner,
                            topEnd = innerCorner,
                            bottomEnd = innerCorner
                        ),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ),
                        modifier = Modifier.size(genHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Locate currently playing track",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(gapBetweenLocateAndNext))

                if (showViewModeToggle) {
                    FilledTonalIconButton(
                        onClick = onToggleViewMode,
                        shape = RoundedCornerShape(
                            topStart = viewModeStartCorner,
                            bottomStart = viewModeStartCorner,
                            topEnd = innerCorner,
                            bottomEnd = innerCorner
                        ),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        ),
                        modifier = Modifier.size(genHeight)
                    ) {
                        Icon(
                            imageVector = if (isListView) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = if (isListView) "Switch to Grid view" else "Switch to List view",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(gapBetweenViewModeAndSort))
                }

                Box {
                    if (isCompact) {
                        FilledTonalIconButton(
                            onClick = { isSortMenuExpanded = true },
                            shape = RoundedCornerShape(
                                topStart = sortStartCorner,
                                bottomStart = sortStartCorner,
                                topEnd = outerCorner,
                                bottomEnd = outerCorner
                            ),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.secondaryContainer,
                                contentColor = colors.onSecondaryContainer
                            ),
                            modifier = Modifier.size(genHeight)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort options (${currentSortOption.displayName})",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { isSortMenuExpanded = true },
                            shape = RoundedCornerShape(
                                topStart = sortStartCorner,
                                bottomStart = sortStartCorner,
                                topEnd = outerCorner,
                                bottomEnd = outerCorner
                            ),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.secondaryContainer,
                                contentColor = colors.onSecondaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(genHeight)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort options",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentSortOption.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        availableSortOptions.forEach { option ->
                            val isSelected = option.storageKey == currentSortOption.storageKey
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.primary else colors.onSurface
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onSortOptionSelect(option)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
