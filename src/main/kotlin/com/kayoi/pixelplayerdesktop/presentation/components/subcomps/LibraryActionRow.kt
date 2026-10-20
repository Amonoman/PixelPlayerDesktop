package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter

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
    showStorageFilterButton: Boolean = false,
    currentStorageFilter: StorageFilter = StorageFilter.ALL,
    onStorageFilterClick: () -> Unit = {},
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

    val hasFilterLeftNeighbors = showLocateButton || showViewModeToggle
    val hasSortLeftNeighbors = hasFilterLeftNeighbors || showStorageFilterButton

    val sortStartCorner by animateDpAsState(
        targetValue = if (hasSortLeftNeighbors) innerCorner else outerCorner,
        label = "SortStartCorner"
    )

    val filterStartCorner by animateDpAsState(
        targetValue = if (hasFilterLeftNeighbors) innerCorner else outerCorner,
        label = "FilterStartCorner"
    )

    val viewModeStartCorner by animateDpAsState(
        targetValue = if (showLocateButton) innerCorner else outerCorner,
        label = "ViewModeStartCorner"
    )

    val gapBetweenLocateAndNext by animateDpAsState(
        targetValue = if (showLocateButton) 4.dp else 0.dp,
        label = "GapBetweenLocateAndNext"
    )

    val gapBetweenViewModeAndNext by animateDpAsState(
        targetValue = if (showViewModeToggle) 4.dp else 0.dp,
        label = "GapBetweenViewModeAndNext"
    )

    val gapBetweenFilterAndSort by animateDpAsState(
        targetValue = if (showStorageFilterButton) 4.dp else 0.dp,
        label = "GapBetweenFilterAndSort"
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
                    AppTooltip("Locate playing song") {
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
                                contentDescription = "Locate currently playing song",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(gapBetweenLocateAndNext))

                if (showViewModeToggle) {
                    val viewModeTooltip = if (isListView) "Switch to Grid view" else "Switch to List view"
                    AppTooltip(viewModeTooltip) {
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
                    }

                    Spacer(modifier = Modifier.width(gapBetweenViewModeAndNext))
                }

                AnimatedVisibility(
                    visible = showStorageFilterButton,
                    enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
                ) {
                    val finalIcon = when (currentStorageFilter) {
                        StorageFilter.ALL -> Icons.Rounded.Dataset
                        StorageFilter.ONLINE -> Icons.Rounded.Cloud
                        StorageFilter.OFFLINE -> Icons.Rounded.Computer
                    }
                    val tooltipText = when (currentStorageFilter) {
                        StorageFilter.ALL -> "Filter: All music"
                        StorageFilter.ONLINE -> "Filter: Cloud only"
                        StorageFilter.OFFLINE -> "Filter: Local only"
                    }

                    val isFiltering = currentStorageFilter != StorageFilter.ALL
                    val filterContainer by animateColorAsState(
                        targetValue = if (isFiltering) colors.tertiaryContainer else colors.secondaryContainer,
                        label = "StorageFilterContainer"
                    )
                    val filterContent by animateColorAsState(
                        targetValue = if (isFiltering) colors.onTertiaryContainer else colors.onSecondaryContainer,
                        label = "StorageFilterContent"
                    )

                    AppTooltip(tooltipText) {
                        FilledTonalIconButton(
                            onClick = onStorageFilterClick,
                            shape = RoundedCornerShape(
                                topStart = filterStartCorner,
                                bottomStart = filterStartCorner,
                                topEnd = innerCorner,
                                bottomEnd = innerCorner
                            ),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = filterContainer,
                                contentColor = filterContent
                            ),
                            modifier = Modifier.size(genHeight)
                        ) {
                            AnimatedContent(
                                targetState = finalIcon,
                                label = "StorageFilterIcon"
                            ) { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tooltipText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(gapBetweenFilterAndSort))

                Box {
                    if (isCompact) {
                        AppTooltip("Sort: ${currentSortOption.displayName}") {
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

                    val scope = rememberCoroutineScope()
                    var pendingSortJob by remember { mutableStateOf<Job?>(null) }
                    var localSortOption by remember(currentSortOption) { mutableStateOf(currentSortOption) }

                    val isCurrentAscending = localSortOption.isAscending()
                    val canToggleOrder = localSortOption != localSortOption.toggleOrder()
                    val categories = remember(availableSortOptions) {
                        availableSortOptions.map { it.category() }.distinct()
                    }
                    val iconRotation by animateFloatAsState(
                        targetValue = if (isCurrentAscending) 0f else -180f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "SortArrowRotation"
                    )

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                    ) {
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = {
                                pendingSortJob?.cancel()
                                if (localSortOption != currentSortOption) {
                                    onSortOptionSelect(localSortOption)
                                }
                                isSortMenuExpanded = false
                            },
                            offset = DpOffset(0.dp, 8.dp),
                            modifier = Modifier
                                .width(285.dp)
                                .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp))
                                .border(1.dp, colors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sort by",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                if (canToggleOrder) {
                                    Surface(
                                        onClick = {
                                            val toggled = localSortOption.toggleOrder()
                                            localSortOption = toggled
                                            pendingSortJob?.cancel()
                                            pendingSortJob = scope.launch {
                                                delay(240L)
                                                onSortOptionSelect(toggled)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.secondaryContainer,
                                        contentColor = colors.onSecondaryContainer,
                                        modifier = Modifier
                                            .width(114.dp)
                                            .height(28.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowUpward,
                                                    contentDescription = "Toggle sort order",
                                                    tint = colors.onSecondaryContainer,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .graphicsLayer { rotationZ = iconRotation }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                AnimatedContent(
                                                    targetState = isCurrentAscending,
                                                    transitionSpec = {
                                                        val slideOffset = if (targetState) 1 else -1
                                                        (slideInVertically(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            ),
                                                            initialOffsetY = { height -> slideOffset * height }
                                                        ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                                            .togetherWith(
                                                                slideOutVertically(
                                                                    animationSpec = spring(
                                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                                        stiffness = Spring.StiffnessMediumLow
                                                                    ),
                                                                    targetOffsetY = { height -> -slideOffset * height }
                                                                ) + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                                                            )
                                                    },
                                                    label = "SortOrderText"
                                                ) { ascending ->
                                                    Text(
                                                        text = if (ascending) "Ascending" else "Descending",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = colors.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )

                            categories.forEach { category ->
                                val isCategoryActive = localSortOption.category() == category
                                val itemBackground = if (isCategoryActive) colors.secondaryContainer else Color.Transparent
                                val itemTextColor = if (isCategoryActive) colors.onSecondaryContainer else colors.onSurface
                                val itemIconColor = if (isCategoryActive) colors.onSecondaryContainer else colors.onSurfaceVariant

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = category.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCategoryActive) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = if (isCategoryActive) {
                                        {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (canToggleOrder) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ArrowUpward,
                                                        contentDescription = null,
                                                        tint = itemIconColor,
                                                        modifier = Modifier
                                                            .size(15.dp)
                                                            .graphicsLayer { rotationZ = iconRotation }
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else null,
                                    colors = MenuDefaults.itemColors(
                                        textColor = itemTextColor,
                                        leadingIconColor = itemIconColor,
                                        trailingIconColor = itemTextColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(itemBackground),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    onClick = {
                                        pendingSortJob?.cancel()
                                        val optionsForCategory = availableSortOptions.filter { it.category() == category }
                                        if (optionsForCategory.isNotEmpty()) {
                                            val target = if (isCategoryActive && canToggleOrder) {
                                                localSortOption.toggleOrder()
                                            } else {
                                                optionsForCategory.firstOrNull { it.isAscending() == isCurrentAscending }
                                                    ?: optionsForCategory.first()
                                            }
                                            localSortOption = target
                                            onSortOptionSelect(target)
                                        }
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
}

private enum class SortCategory(val displayName: String, val icon: ImageVector) {
    TITLE("Title", Icons.Rounded.SortByAlpha),
    NAME("Name", Icons.Rounded.SortByAlpha),
    ARTIST("Artist", Icons.Rounded.Person),
    ALBUM("Album", Icons.Rounded.Album),
    DATE_ADDED("Date Added", Icons.Rounded.CalendarToday),
    DATE_CREATED("Date Created", Icons.Rounded.CalendarToday),
    DURATION("Duration", Icons.Rounded.Schedule),
    RELEASE_YEAR("Release Year", Icons.Rounded.DateRange),
    SONG_COUNT("Song Count", Icons.Rounded.Numbers),
    SUBFOLDER_COUNT("Subfolder Count", Icons.Rounded.Folder)
}

private fun SortOption.category(): SortCategory = when (this) {
    SortOption.SongDefaultOrder,
    SortOption.SongTitleAZ,
    SortOption.SongTitleZA -> SortCategory.TITLE
    SortOption.AlbumTitleAZ, SortOption.AlbumTitleZA -> SortCategory.TITLE
    SortOption.ArtistNameAZ, SortOption.ArtistNameZA -> SortCategory.NAME
    SortOption.PlaylistNameAZ, SortOption.PlaylistNameZA -> SortCategory.NAME
    SortOption.FolderNameAZ, SortOption.FolderNameZA -> SortCategory.NAME
    SortOption.SongArtist, SortOption.SongArtistDesc -> SortCategory.ARTIST
    SortOption.AlbumArtist, SortOption.AlbumArtistDesc -> SortCategory.ARTIST
    SortOption.SongAlbum, SortOption.SongAlbumDesc -> SortCategory.ALBUM
    SortOption.SongDateAdded, SortOption.SongDateAddedAsc -> SortCategory.DATE_ADDED
    SortOption.AlbumDateAdded -> SortCategory.DATE_ADDED
    SortOption.PlaylistDateCreated, SortOption.PlaylistDateCreatedAsc -> SortCategory.DATE_CREATED
    SortOption.SongDuration, SortOption.SongDurationAsc -> SortCategory.DURATION
    SortOption.AlbumReleaseYear, SortOption.AlbumReleaseYearAsc -> SortCategory.RELEASE_YEAR
    SortOption.AlbumSizeAsc, SortOption.AlbumSizeDesc,
    SortOption.ArtistNumSongsAsc, SortOption.ArtistNumSongsDesc,
    SortOption.FolderSongCountAsc, SortOption.FolderSongCountDesc -> SortCategory.SONG_COUNT
    SortOption.FolderSubdirCountAsc, SortOption.FolderSubdirCountDesc -> SortCategory.SUBFOLDER_COUNT
}

private fun SortOption.isAscending(): Boolean = when (this) {
    SortOption.SongDefaultOrder,
    SortOption.SongTitleAZ,
    SortOption.AlbumTitleAZ,
    SortOption.ArtistNameAZ,
    SortOption.PlaylistNameAZ,
    SortOption.FolderNameAZ,
    SortOption.SongArtist,
    SortOption.AlbumArtist,
    SortOption.SongAlbum,
    SortOption.SongDateAddedAsc,
    SortOption.PlaylistDateCreatedAsc,
    SortOption.SongDurationAsc,
    SortOption.AlbumReleaseYearAsc,
    SortOption.AlbumSizeAsc,
    SortOption.ArtistNumSongsAsc,
    SortOption.FolderSongCountAsc,
    SortOption.FolderSubdirCountAsc -> true

    SortOption.SongTitleZA,
    SortOption.AlbumTitleZA,
    SortOption.ArtistNameZA,
    SortOption.PlaylistNameZA,
    SortOption.FolderNameZA,
    SortOption.SongArtistDesc,
    SortOption.AlbumArtistDesc,
    SortOption.SongAlbumDesc,
    SortOption.SongDateAdded,
    SortOption.AlbumDateAdded,
    SortOption.PlaylistDateCreated,
    SortOption.SongDuration,
    SortOption.AlbumReleaseYear,
    SortOption.AlbumSizeDesc,
    SortOption.ArtistNumSongsDesc,
    SortOption.FolderSongCountDesc,
    SortOption.FolderSubdirCountDesc -> false
}

private fun SortOption.toggleOrder(): SortOption = when (this) {
    SortOption.SongDefaultOrder -> SortOption.SongTitleZA
    SortOption.SongTitleAZ -> SortOption.SongTitleZA
    SortOption.SongTitleZA -> SortOption.SongTitleAZ
    SortOption.AlbumTitleAZ -> SortOption.AlbumTitleZA
    SortOption.AlbumTitleZA -> SortOption.AlbumTitleAZ
    SortOption.ArtistNameAZ -> SortOption.ArtistNameZA
    SortOption.ArtistNameZA -> SortOption.ArtistNameAZ
    SortOption.PlaylistNameAZ -> SortOption.PlaylistNameZA
    SortOption.PlaylistNameZA -> SortOption.PlaylistNameAZ
    SortOption.FolderNameAZ -> SortOption.FolderNameZA
    SortOption.FolderNameZA -> SortOption.FolderNameAZ

    SortOption.SongArtist -> SortOption.SongArtistDesc
    SortOption.SongArtistDesc -> SortOption.SongArtist
    SortOption.AlbumArtist -> SortOption.AlbumArtistDesc
    SortOption.AlbumArtistDesc -> SortOption.AlbumArtist

    SortOption.SongAlbum -> SortOption.SongAlbumDesc
    SortOption.SongAlbumDesc -> SortOption.SongAlbum

    SortOption.SongDateAdded -> SortOption.SongDateAddedAsc
    SortOption.SongDateAddedAsc -> SortOption.SongDateAdded
    SortOption.AlbumDateAdded -> SortOption.AlbumDateAdded

    SortOption.PlaylistDateCreated -> SortOption.PlaylistDateCreatedAsc
    SortOption.PlaylistDateCreatedAsc -> SortOption.PlaylistDateCreated

    SortOption.SongDuration -> SortOption.SongDurationAsc
    SortOption.SongDurationAsc -> SortOption.SongDuration

    SortOption.AlbumReleaseYear -> SortOption.AlbumReleaseYearAsc
    SortOption.AlbumReleaseYearAsc -> SortOption.AlbumReleaseYear

    SortOption.AlbumSizeAsc -> SortOption.AlbumSizeDesc
    SortOption.AlbumSizeDesc -> SortOption.AlbumSizeAsc
    SortOption.ArtistNumSongsAsc -> SortOption.ArtistNumSongsDesc
    SortOption.ArtistNumSongsDesc -> SortOption.ArtistNumSongsAsc
    SortOption.FolderSongCountAsc -> SortOption.FolderSongCountDesc
    SortOption.FolderSongCountDesc -> SortOption.FolderSongCountAsc

    SortOption.FolderSubdirCountAsc -> SortOption.FolderSubdirCountDesc
    SortOption.FolderSubdirCountDesc -> SortOption.FolderSubdirCountAsc
}

