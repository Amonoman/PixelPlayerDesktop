package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

@Composable
fun ArtistOptionsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    artist: Artist,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    customItems: (@Composable ColumnScope.() -> Unit)? = null,
    menuWidth: Dp = 250.dp,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = offset,
            modifier = modifier
                .width(menuWidth)
                .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp))
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ShapeCache.ExpressiveAvatar)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUri = artist.effectiveImageUrl
                    if (!imageUri.isNullOrBlank()) {
                        AsyncArtworkImage(
                            artworkUri = imageUri,
                            shape = ShapeCache.ExpressiveAvatar,
                            placeholderIcon = PixelPlayerIcons.Artist,
                            placeholderIconSize = 20.dp,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            contentDescription = "${artist.name} portrait",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = PixelPlayerIcons.Artist,
                            contentDescription = artist.name,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (artist.songCount > 0) {
                        "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}"
                    } else {
                        "Artist"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (onPlay != null) {
                DropdownMenuItem(
                    text = { Text("Play", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = {
                        onDismissRequest()
                        onPlay()
                    }
                )
            }

            if (onPlayNext != null) {
                DropdownMenuItem(
                    text = { Text("Play Next", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = {
                        onDismissRequest()
                        onPlayNext()
                    }
                )
            }

            if (onAddToQueue != null) {
                DropdownMenuItem(
                    text = { Text("Add to Queue", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = {
                        onDismissRequest()
                        onAddToQueue()
                    }
                )
            }

            if (onAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    onClick = {
                        onDismissRequest()
                        onAddToPlaylist()
                    }
                )
            }

            if (customItems != null) {
                customItems()
            }
        }
    }
}
