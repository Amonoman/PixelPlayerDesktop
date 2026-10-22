package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Song

@Composable
fun AlbumArtCollage(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    height: Dp = 380.dp,
    padding: Dp = 0.dp,
    pattern: CollagePattern = CollagePattern.default,
    onSongClick: (Song) -> Unit
) {
    val songsToShow = remember(songs) {
        songs.take(6) + List((6 - songs.size).coerceAtLeast(0)) { null }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(padding)
    ) {
        val boxMaxHeight = maxHeight
        val minDimension = minOf(300.dp, height)
        val shapeConfigs = remember(pattern, minDimension, boxMaxHeight) {
            buildCollageConfigs(pattern, minDimension, boxMaxHeight)
        }

        if (shapeConfigs.isNotEmpty()) {
            val topConfigs = shapeConfigs.take(3)
            val bottomConfigs = shapeConfigs.drop(3)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boxMaxHeight * 0.6f)
                ) {
                    topConfigs.forEachIndexed { idx, cfg ->
                        val song = songsToShow.getOrNull(idx)
                        if (song != null) {
                            Box(
                                modifier = Modifier
                                    .size(cfg.width, cfg.height)
                                    .align(cfg.align)
                                    .offset(cfg.offsetX, cfg.offsetY)
                                    .graphicsLayer { rotationZ = cfg.rot }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onSongClick(song) }
                                    .background(
                                        shape = cfg.shape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clip(cfg.shape)
                            ) {
                                AsyncArtworkImage(
                                    artworkUri = song.albumArtUriString,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Res.drawable.ic_rounded_music_note,
                                    placeholderIconSize = 48.dp,
                                    contentDescription = song.title
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boxMaxHeight * 0.4f)
                ) {
                    bottomConfigs.forEachIndexed { j, cfg ->
                        val song = songsToShow.getOrNull(j + 3)
                        if (song != null) {
                            Box(
                                modifier = Modifier
                                    .size(cfg.width, cfg.height)
                                    .align(cfg.align)
                                    .offset(cfg.offsetX, cfg.offsetY)
                                    .graphicsLayer { rotationZ = cfg.rot }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onSongClick(song) }
                                    .background(
                                        shape = cfg.shape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clip(cfg.shape)
                            ) {
                                AsyncArtworkImage(
                                    artworkUri = song.albumArtUriString,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholderIcon = Res.drawable.ic_rounded_music_note,
                                    placeholderIconSize = 36.dp,
                                    contentDescription = song.title
                                )
                            }
                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_music_note),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }
    }
}
