package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song

@Composable
fun PlaylistCover(
    playlist: Playlist,
    playlistSongs: List<Song>,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val shape = remember(playlist.coverShapeType) {
        when (playlist.coverShapeType) {
            "Circle" -> CircleShape
            else -> RoundedCornerShape(12.dp)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
    ) {
        if (playlist.coverImageUri != null) {
            AsyncArtworkImage(
                artworkUri = playlist.coverImageUri,
                modifier = Modifier.fillMaxSize(),
                contentDescription = playlist.name
            )
        } else if (playlist.coverColorArgb != null) {
            val bg = Color(playlist.coverColorArgb)
            val iconTint = if (isColorDark(bg)) Color.White else Color.Black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPlaylistIconByName(playlist.coverIconName),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(size / 2)
                )
            }
        } else {
            PlaylistArtCollage(
                songs = playlistSongs,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun isColorDark(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}

private fun getPlaylistIconByName(name: String?): ImageVector {
    return when (name) {
        "MusicNote" -> Icons.Rounded.MusicNote
        "Headphones" -> Icons.Rounded.Headphones
        "Album" -> Icons.Rounded.Album
        "Mic" -> Icons.Rounded.MicExternalOn
        "Speaker" -> Icons.Rounded.Speaker
        "Favorite" -> Icons.Rounded.Favorite
        "Piano" -> Icons.Rounded.Piano
        "Queue" -> Icons.AutoMirrored.Rounded.QueueMusic
        else -> Icons.Rounded.MusicNote
    }
}
