package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_audio_file
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_file_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_folder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveLyricsDialog(
    song: Song,
    onSaveToSongFolder: () -> Unit,
    onExportFile: () -> Unit,
    onEmbedToAudioFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val fileName = remember(song.path) {
        val f = File(song.path)
        "${f.nameWithoutExtension}.lrc"
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(440.dp)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save Lyrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.title} • ${song.displayArtist}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = colors.onSurfaceVariant
                        )
                    ) {
                        Icon(painterResource(Res.drawable.ic_rounded_close), contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                SaveLyricsOptionCard(
                    icon = Res.drawable.ic_rounded_folder,
                    iconContainerColor = colors.primaryContainer,
                    iconColor = colors.onPrimaryContainer,
                    title = "Save to Song Folder",
                    subtitle = "Saves as $fileName beside the audio file",
                    onClick = onSaveToSongFolder
                )

                Spacer(modifier = Modifier.height(10.dp))

                SaveLyricsOptionCard(
                    icon = Res.drawable.ic_rounded_file_download,
                    iconContainerColor = colors.surfaceContainerHighest,
                    iconColor = colors.primary,
                    title = "Export to File...",
                    subtitle = "Choose custom destination folder on your device",
                    onClick = onExportFile
                )

                Spacer(modifier = Modifier.height(10.dp))

                SaveLyricsOptionCard(
                    icon = Res.drawable.ic_rounded_audio_file,
                    iconContainerColor = colors.secondaryContainer,
                    iconColor = colors.onSecondaryContainer,
                    title = "Embed into Audio File",
                    subtitle = "Writes directly into file tags (ID3 / Vorbis tag)",
                    onClick = onEmbedToAudioFile
                )
            }
        }
    }
}

@Composable
private fun SaveLyricsOptionCard(
    icon: DrawableResource,
    iconContainerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
