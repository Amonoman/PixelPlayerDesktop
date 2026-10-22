package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun PlaylistArtCollage(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
                contentDescription = "Playlist",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    } else {
        Surface(
            modifier = modifier.aspectRatio(1f),
            shape = RoundedCornerShape(0.dp),
            color = Color.Transparent
        ) {
            when (songs.size) {
                1 -> {
                    AsyncArtworkImage(
                        artworkUri = songs[0].albumArtUriString,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentDescription = songs[0].title
                    )
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncArtworkImage(
                            artworkUri = songs[0].albumArtUriString,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape),
                            contentDescription = songs[0].title
                        )
                        AsyncArtworkImage(
                            artworkUri = songs[1].albumArtUriString,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape),
                            contentDescription = songs[1].title
                        )
                    }
                }
                3 -> {
                    Layout(
                        content = {
                            songs.take(3).forEach { song ->
                                AsyncArtworkImage(
                                    artworkUri = song.albumArtUriString,
                                    modifier = Modifier.clip(CircleShape),
                                    contentDescription = song.title
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { measurables, constraints ->
                        val separation = 2.dp.toPx()
                        val itemSize = floor((constraints.maxWidth * 2f / (2f + sqrt(3f))) - separation).toInt()

                        val placeables = measurables.map {
                            it.measure(
                                constraints.copy(
                                    minWidth = itemSize, maxWidth = itemSize,
                                    minHeight = itemSize, maxHeight = itemSize
                                )
                            )
                        }

                        val length = itemSize + separation
                        val height = length * sqrt(3f) / 2f

                        val collageHeight = height + itemSize
                        val collageWidth = length + itemSize

                        val offsetX = ((constraints.maxWidth - collageWidth) / 2f).toInt()
                        val offsetY = ((constraints.maxHeight - collageHeight) / 2f).toInt()

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeables[0].placeRelative(
                                x = (offsetX + (collageWidth - itemSize) / 2f).toInt(),
                                y = offsetY
                            )
                            placeables[1].placeRelative(
                                x = offsetX,
                                y = (offsetY + height).toInt()
                            )
                            placeables[2].placeRelative(
                                x = (offsetX + length).toInt(),
                                y = (offsetY + height).toInt()
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AsyncArtworkImage(
                                artworkUri = songs[0].albumArtUriString,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape),
                                contentDescription = songs[0].title
                            )
                            AsyncArtworkImage(
                                artworkUri = songs[1].albumArtUriString,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape),
                                contentDescription = songs[1].title
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AsyncArtworkImage(
                                artworkUri = songs[2].albumArtUriString,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape),
                                contentDescription = songs[2].title
                            )
                            AsyncArtworkImage(
                                artworkUri = songs[3].albumArtUriString,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape),
                                contentDescription = songs[3].title
                            )
                        }
                    }
                }
            }
        }
    }
}
