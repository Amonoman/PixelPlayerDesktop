package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_playlist_add
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage

@Composable
fun AlbumOptionsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    album: Album,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onNavigateToArtist: (() -> Unit)? = null,
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
                AsyncArtworkImage(
                    artworkUri = album.albumArtUriString,
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    placeholderIcon = Res.drawable.ic_rounded_album,
                    placeholderIconSize = 20.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (album.songCount > 0) {
                        "${album.artist} • ${album.songCount} ${if (album.songCount == 1) "song" else "songs"}"
                    } else {
                        album.artist
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
                            painter = painterResource(Res.drawable.ic_rounded_play_arrow),
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
                            painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
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
                            painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
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
                            painter = painterResource(Res.drawable.ic_automirrored_rounded_playlist_add),
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

            if (onNavigateToArtist != null) {
                DropdownMenuItem(
                    text = { Text("Go to Artist", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_person),
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
                        onNavigateToArtist()
                    }
                )
            }

            if (customItems != null) {
                customItems()
            }
        }
    }
}
