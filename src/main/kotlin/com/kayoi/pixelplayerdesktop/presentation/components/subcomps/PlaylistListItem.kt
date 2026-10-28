package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_file_upload
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_edit
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_more_vert
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_play_arrow
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.onClick
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.PlaylistCover

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistListItem(
    playlist: Playlist,
    playlistSongs: List<Song>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isMenuExpanded by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (isHovered) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        label = "playlistItemContainer"
    )

    val cardShape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(cardShape)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = { isMenuExpanded = true }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCover(
                playlist = playlist,
                playlistSongs = playlistSongs,
                size = 58.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val songCountText = if (playlist.songIds.size == 1) "1 song" else "${playlist.songIds.size} songs"
                Text(
                    text = songCountText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isHovered && playlistSongs.isNotEmpty()) {
                FilledTonalIconButton(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                        contentDescription = "Play playlist",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box {
                FilledTonalIconButton(
                    onClick = { isMenuExpanded = true },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_more_vert),
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp)
                    )
                }

                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                ) {
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        offset = DpOffset(0.dp, 8.dp),
                        modifier = Modifier
                            .width(180.dp)
                            .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp))
                            .border(1.dp, colors.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_edit), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as M3U", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_outlined_file_upload), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onExportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", fontWeight = FontWeight.Medium, color = colors.error) },
                            leadingIcon = {
                                Icon(painterResource(Res.drawable.ic_rounded_delete), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
