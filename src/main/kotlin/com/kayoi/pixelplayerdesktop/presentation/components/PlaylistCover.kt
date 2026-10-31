
package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.PlaylistShapeType
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_headphones
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_mic_external_on
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_piano
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_speaker
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape

@Composable
fun PlaylistCover(
    playlist: Playlist,
    playlistSongs: List<Song>,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val shape: Shape = remember(playlist.coverShapeType, playlist.coverShapeDetail1, playlist.coverShapeDetail2, playlist.coverShapeDetail3, size) {
        when (playlist.coverShapeType) {
            PlaylistShapeType.Circle.name, "Circle" -> CircleShape
            PlaylistShapeType.SmoothRect.name, "SmoothRect" -> RoundedCornerShape(14.dp)
            PlaylistShapeType.RotatedPill.name, "RotatedPill" -> {
                GenericShape { shapeSize, _ ->
                    val w = shapeSize.width
                    val h = shapeSize.height
                    val pillW = w * 0.75f
                    val offset = (w - pillW) / 2
                    addRoundRect(RoundRect(offset, 0f, offset + pillW, h, CornerRadius(pillW / 2, pillW / 2)))
                }
            }
            PlaylistShapeType.Star.name, "Star" -> RoundedStarShape(
                sides = (playlist.coverShapeDetail4 ?: 5f).toInt(),
                curve = (playlist.coverShapeDetail1 ?: 0.15f).toDouble(),
                rotation = playlist.coverShapeDetail2 ?: 0f
            )
            else -> RoundedCornerShape(10.dp)
        }
    }

    val isRotatedPill = playlist.coverShapeType == PlaylistShapeType.RotatedPill.name || playlist.coverShapeType == "RotatedPill"
    val isStar = playlist.coverShapeType == PlaylistShapeType.Star.name || playlist.coverShapeType == "Star"
    val shapeMod = if (isRotatedPill) Modifier.graphicsLayer(rotationZ = 45f) else Modifier
    val iconMod = if (isRotatedPill) Modifier.graphicsLayer(rotationZ = -45f) else Modifier
    val scaleMod = if (isStar) {
        val s = playlist.coverShapeDetail3 ?: 1f
        Modifier.graphicsLayer(scaleX = s, scaleY = s)
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(scaleMod)
            .then(shapeMod)
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
                    modifier = Modifier.size(size / 2).then(iconMod)
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

internal fun getPlaylistShape(shapeType: String?, size: Dp = 56.dp): Shape {
    return when (shapeType) {
        PlaylistShapeType.Circle.name, "Circle" -> CircleShape
        PlaylistShapeType.SmoothRect.name, "SmoothRect" -> {
            val corner = (size * 0.22f).coerceIn(4.dp, 28.dp)
            RoundedCornerShape(corner)
        }
        PlaylistShapeType.RotatedPill.name, "RotatedPill" -> {
            GenericShape { shapeSize, _ ->
                val w = shapeSize.width
                val h = shapeSize.height
                val pillW = w * 0.72f
                val offset = (w - pillW) / 2
                addRoundRect(RoundRect(offset, 0f, offset + pillW, h, CornerRadius(pillW / 2, pillW / 2)))
            }
        }
        PlaylistShapeType.Star.name, "Star" -> RoundedStarShape(sides = 5, curve = 0.15, rotation = 0f)
        else -> RoundedCornerShape((size * 0.18f).coerceIn(4.dp, 16.dp))
    }
}


internal fun isColorDark(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}

internal fun getPlaylistIconByName(name: String?): DrawableResource {
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
