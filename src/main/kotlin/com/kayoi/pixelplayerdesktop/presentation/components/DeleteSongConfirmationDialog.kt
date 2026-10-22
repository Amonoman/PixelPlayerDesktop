package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_delete
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.kayoi.pixelplayerdesktop.data.model.Song

@Composable
fun DeleteSongConfirmationDialog(
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_delete),
                contentDescription = null,
                tint = colors.error
            )
        },
        title = {
            Text(
                text = "Delete Song",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Are you sure you want to permanently delete \"${song.title}\"? This will delete the file from your computer.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
