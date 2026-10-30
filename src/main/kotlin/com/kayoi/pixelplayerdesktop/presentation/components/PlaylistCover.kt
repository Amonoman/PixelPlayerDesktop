package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_headphones
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_mic_external_on
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_piano
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speaker
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            else -> RoundedCornerShape(10.dp)
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
                    painter = painterResource(getPlaylistIconByName(playlist.coverIconName)),
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

private fun getPlaylistIconByName(name: String?): DrawableResource {
    return when (name) {
        "MusicNote" -> Res.drawable.ic_rounded_music_note
        "Headphones" -> Res.drawable.ic_rounded_headphones
        "Album" -> Res.drawable.ic_rounded_album
        "Mic" -> Res.drawable.ic_rounded_mic_external_on
        "Speaker" -> Res.drawable.ic_rounded_speaker
        "Favorite" -> Res.drawable.ic_rounded_favorite
        "Piano" -> Res.drawable.ic_rounded_piano
        "Queue" -> Res.drawable.ic_automirrored_rounded_queue_music
        else -> Res.drawable.ic_rounded_music_note
    }
}
