package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_more_vert
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.onClick
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp as lerpDp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage

@Immutable
private data class EnhancedSongAnimationTarget(
    val isHighlighted: Boolean = false,
    val isSelected: Boolean = false
)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedSongListItem(
    modifier: Modifier = Modifier,
    song: Song,
    isPlaying: Boolean = false,
    isCurrentSong: Boolean = false,
    showAlbumArt: Boolean = true,
    albumArtSize: Dp = 48.dp,
    showDuration: Boolean = true,
    customDurationText: String? = null,
    customDurationColor: Color? = null,
    customShape: androidx.compose.ui.graphics.Shape? = null,
    containerColorOverride: Color? = null,
    isDragHandleVisible: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    showMoreOptionsButton: Boolean = true,
    onMoreOptionsClick: ((Song) -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    customDropdownItems: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
    moreOptionsMenu: (@Composable () -> Unit)? = null,
    isRemoveButtonVisible: Boolean = false,
    onRemoveClick: (() -> Unit)? = null,
    isDragging: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    var showInternalDropdown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val hasDropdownActions = onPlayNext != null || onAddToQueue != null || onAddToPlaylist != null ||
        onToggleFavorite != null || onEditMetadata != null || onRemoveFromPlaylist != null || onRemoveFromQueue != null ||
        onDelete != null || customDropdownItems != null

    val highlightProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isCurrentSong) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "highlightProgress"
    )

    val isHighlighted = isCurrentSong || highlightProgress > 0.01f

    val surfaceShape = remember(highlightProgress, customShape, isHighlighted) {
        if (!isHighlighted && customShape != null) {
            customShape
        } else if (!isHighlighted) {
            RoundedCornerShape(18.dp)
        } else {
            val radius = lerpDp(18.dp, 32.dp, highlightProgress)
            RoundedCornerShape(radius)
        }
    }

    val albumShape = remember(highlightProgress, isHighlighted) {
        if (!isHighlighted) {
            RoundedCornerShape(12.dp)
        } else {
            val radius = lerpDp(12.dp, 24.dp, highlightProgress)
            RoundedCornerShape(radius)
        }
    }

    val colors = MaterialTheme.colorScheme
    val baseContainerColor = containerColorOverride ?: colors.surfaceContainerLow
    val containerColor = lerpColor(baseContainerColor, colors.primaryContainer, highlightProgress)

    val baseContentColor = colors.onSurface
    val contentColor = lerpColor(baseContentColor, colors.onPrimaryContainer, highlightProgress)

    val mvContainerColor = lerpColor(colors.surfaceContainerHighest.copy(alpha = 0.6f), colors.onPrimaryContainer.copy(alpha = 0.15f), highlightProgress)
    val mvContentColor = lerpColor(colors.onSurfaceVariant, colors.onPrimaryContainer, highlightProgress)

    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "elevationAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val hoverColor = colors.onSurface.copy(alpha = 0.06f)
    val effectiveContainerColor = when {
        isDragging -> colors.surfaceContainerHighest
        isHovered && !isCurrentSong -> lerpColor(containerColor, hoverColor, 0.45f)
        else -> containerColor
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(surfaceShape)
            .hoverable(interactionSource),
        shape = surfaceShape,
        color = effectiveContainerColor,
        shadowElevation = elevation,
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isDragHandleVisible && dragHandle != null,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    dragHandle?.invoke()
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(10.dp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isDragging,
                        onClick = onClick
                    )
                    .onClick(
                        enabled = !isDragging && (hasDropdownActions || onMoreOptionsClick != null),
                        matcher = PointerMatcher.mouse(PointerButton.Secondary),
                        onClick = {
                            if (onMoreOptionsClick != null) {
                                onMoreOptionsClick(song)
                            } else if (hasDropdownActions) {
                                showInternalDropdown = true
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAlbumArt) {
                    AsyncArtworkImage(
                        artworkUri = song.albumArtUriString,
                        modifier = Modifier.size(albumArtSize),
                        shape = albumShape,
                        placeholderIcon = Res.drawable.ic_rounded_music_note,
                        placeholderIconSize = 22.dp,
                        containerColor = if (isHighlighted) colors.onPrimaryContainer.copy(alpha = 0.12f) else colors.surfaceContainerHighest,
                        contentColor = if (isHighlighted) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        contentDescription = "${song.title} artwork"
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        color = contentColor,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.displayArtist,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.5.sp,
                        color = contentColor.copy(alpha = if (isHighlighted) 0.85f else 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (customDurationText != null) {
                    Text(
                        text = customDurationText,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        color = customDurationColor ?: (if (isHighlighted) colors.onPrimaryContainer else colors.primary),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else if (showDuration && song.duration > 0) {
                    Text(
                        text = formatDuration(song.duration),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = contentColor.copy(alpha = if (isHighlighted) 0.8f else 0.55f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                if (isCurrentSong) {
                    PlayingEqIcon(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(width = 20.dp, height = 18.dp),
                        color = if (isHighlighted) colors.onPrimaryContainer else colors.primary,
                        isPlaying = isPlaying
                    )
                }
            }

            Box {
                val shouldShowButton = showMoreOptionsButton && (moreOptionsMenu != null || hasDropdownActions || onMoreOptionsClick != null)
                if (shouldShowButton) {
                    FilledIconButton(
                        onClick = {
                            if (onMoreOptionsClick != null) {
                                onMoreOptionsClick(song)
                            } else if (hasDropdownActions) {
                                showInternalDropdown = !showInternalDropdown
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = mvContainerColor,
                            contentColor = mvContentColor
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_more_vert),
                            contentDescription = "Options for ${song.title}",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (moreOptionsMenu != null) {
                    moreOptionsMenu()
                } else if (hasDropdownActions) {
                    SongOptionsDropdownMenu(
                        expanded = showInternalDropdown,
                        onDismissRequest = { showInternalDropdown = false },
                        song = song,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onEditMetadata = onEditMetadata,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onDelete = onDelete,
                        customItems = customDropdownItems
                    )
                }
            }

            AnimatedVisibility(
                visible = isRemoveButtonVisible && onRemoveClick != null,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledIconButton(
                        onClick = { onRemoveClick?.invoke() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.surfaceContainerHighest.copy(alpha = 0.7f),
                            contentColor = colors.onSurface
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_close),
                            contentDescription = "Remove from playlist",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            trailingContent?.invoke()
        }
    }
}
