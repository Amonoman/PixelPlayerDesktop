package com.kayoi.pixelplayerdesktop.presentation.components

import java.io.File
import java.util.UUID
import javax.imageio.ImageIO
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image as SkiaImage

import com.kayoi.pixelplayerdesktop.data.model.PlaylistShapeType
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_queue_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_image
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider

private val PRESET_PLAYLIST_COLORS = listOf(
    0xFF6750A4.toInt(),
    0xFF3E5F8A.toInt(),
    0xFF006A6A.toInt(),
    0xFF2E6C38.toInt(),
    0xFFA04000.toInt(),
    0xFFB3261E.toInt(),
    0xFF9C27B0.toInt(),
    0xFF455A64.toInt()
)

private val PLAYLIST_ICON_NAMES = listOf(
    "MusicNote", "Headphones", "Album", "Mic", "Speaker", "Favorite", "Piano", "Queue"
)

@Composable
fun CreatePlaylistDialog(
    title: String = "New Playlist",
    initialName: String = "",
    confirmButtonText: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    CreatePlaylistDialog(
        title = title,
        initialName = initialName,
        confirmButtonText = confirmButtonText,
        showCoverCustomization = false,
        onDismiss = onDismiss,
        onConfirm = { name, _, _, _, _ -> onConfirm(name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePlaylistDialog(
    title: String = "New Playlist",
    initialName: String = "",
    confirmButtonText: String = "Create",
    showCoverCustomization: Boolean = true,
    initialCoverImageUri: String? = null,
    initialCoverColor: Int? = null,
    initialCoverIconName: String? = null,
    initialCoverShapeType: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        coverImageUri: String?,
        coverColorArgb: Int?,
        coverIconName: String?,
        coverShapeType: String?
    ) -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var playlistName by remember { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }

    var selectedTab by remember {
        mutableIntStateOf(
            when {
                initialCoverImageUri != null -> 1
                initialCoverColor != null || initialCoverIconName != null -> 2
                else -> 0
            }
        )
    }

    var selectedImagePath by remember { mutableStateOf(initialCoverImageUri) }
    var previewBitmap by remember(selectedImagePath) { mutableStateOf<ImageBitmap?>(null) }
    var selectedColor by remember { mutableIntStateOf(initialCoverColor ?: PRESET_PLAYLIST_COLORS.first()) }
    var selectedIconName by remember { mutableStateOf(initialCoverIconName ?: "MusicNote") }
    var selectedShapeType by remember {
        mutableStateOf(
            PlaylistShapeType.entries.firstOrNull { it.name.equals(initialCoverShapeType, ignoreCase = true) }
                ?: PlaylistShapeType.Circle
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedImagePath) {
        val path = selectedImagePath
        if (path.isNullOrBlank()) {
            previewBitmap = null
        } else {
            withContext(Dispatchers.IO) {
                previewBitmap = try {
                    val cleanPath = path.removePrefix("file://").substringBefore("?")
                    val f = File(cleanPath)
                    if (f.exists() && f.isFile) {
                        val bytes = f.readBytes()
                        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    } else null
                } catch (_: Throwable) {
                    try {
                        val cleanPath = path.removePrefix("file://").substringBefore("?")
                        ImageIO.read(File(cleanPath))?.toComposeImageBitmap()
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        }
    }

    val openImagePicker = {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.Image,
                dialogSettings = FileKitDialogSettings(title = "Select Playlist Cover Image")
            )
            if (file != null) {
                selectedImagePath = file.file.absolutePath
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 440.dp, max = 500.dp)
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showCoverCustomization) {
                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "hero_preview"
                    ) { targetTab ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (targetTab) {
                                0 -> {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(colors.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
                                                contentDescription = null,
                                                modifier = Modifier.size(52.dp),
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Auto Collage",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    val bitmap = previewBitmap
                                    if (bitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(colors.surfaceContainerHighest)
                                        ) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = "Cover preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(colors.surfaceContainerHighest)
                                                .clickable { openImagePicker() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_image),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = colors.primary
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    "Choose Image",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.primary
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    val currentShape = remember(selectedShapeType) {
                                        getPlaylistShape(selectedShapeType.name, 130.dp)
                                    }
                                    val isPill = selectedShapeType == PlaylistShapeType.RotatedPill
                                    val shapeMod = if (isPill) Modifier.graphicsLayer(rotationZ = 45f) else Modifier
                                    val iconMod = if (isPill) Modifier.graphicsLayer(rotationZ = -45f) else Modifier

                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .then(shapeMod)
                                            .clip(currentShape)
                                            .background(Color(selectedColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconTint = if (isColorDark(Color(selectedColor))) Color.White else Color.Black
                                        Icon(
                                            painter = painterResource(getPlaylistIconByName(selectedIconName)),
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(62.dp).then(iconMod)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    placeholder = { Text("Enter a name for your playlist") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .trackInputFocus(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outlineVariant
                    )
                )

                if (showCoverCustomization) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            listOf("Auto Collage", "Custom Image", "Custom Icon").forEachIndexed { index, label ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isSelected) colors.primaryContainer else Color.Transparent)
                                        .clickable { selectedTab = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Cover will be assembled dynamically from songs in this playlist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { openImagePicker() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (previewBitmap == null) "Choose Image" else "Change Image")
                                }

                                if (previewBitmap != null) {
                                    Button(
                                        onClick = {
                                            selectedImagePath = null
                                            previewBitmap = null
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.errorContainer,
                                            contentColor = colors.onErrorContainer
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Remove Image")
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Shape",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PlaylistShapeType.entries.forEach { shapeType ->
                                        val isSelected = selectedShapeType == shapeType
                                        val previewShape = remember(shapeType) { getPlaylistShape(shapeType.name, 28.dp) }
                                        val isPill = shapeType == PlaylistShapeType.RotatedPill
                                        val rotMod = if (isPill) Modifier.graphicsLayer(rotationZ = 45f) else Modifier

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) colors.primaryContainer else colors.surfaceContainer)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) colors.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedShapeType = shapeType },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .then(rotMod)
                                                    .clip(previewShape)
                                                    .background(if (isSelected) colors.primary else colors.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Color",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    PRESET_PLAYLIST_COLORS.forEach { colorArgb ->
                                        val isSelected = selectedColor == colorArgb
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorArgb))
                                                .clickable { selectedColor = colorArgb },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                val checkTint = if (isColorDark(Color(colorArgb))) Color.White else Color.Black
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_rounded_check),
                                                    contentDescription = null,
                                                    tint = checkTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Icon",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    PLAYLIST_ICON_NAMES.forEach { iconName ->
                                        val isSelected = selectedIconName == iconName
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) colors.primaryContainer else colors.surfaceContainer)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) colors.primary else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedIconName = iconName },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(getPlaylistIconByName(iconName)),
                                                contentDescription = null,
                                                tint = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                scope.launch {
                                    val finalCoverImageUri = if (selectedTab == 1 && !selectedImagePath.isNullOrBlank()) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val srcFile = File(selectedImagePath!!.removePrefix("file://").substringBefore("?"))
                                                val image = ImageIO.read(srcFile)
                                                if (image != null) {
                                                    val dest = File(DesktopPathProvider.playlistCoversDir, "playlist_cover_${UUID.randomUUID()}.jpg")
                                                    ImageIO.write(image, "jpg", dest)
                                                    dest.absolutePath
                                                } else selectedImagePath
                                            } catch (_: Exception) {
                                                selectedImagePath
                                            }
                                        }
                                    } else null

                                    val finalColor = if (selectedTab == 2) selectedColor else null
                                    val finalIcon = if (selectedTab == 2) selectedIconName else null
                                    val finalShape = if (selectedTab == 2) selectedShapeType.name else null

                                    onConfirm(
                                        playlistName.trim(),
                                        finalCoverImageUri,
                                        finalColor,
                                        finalIcon,
                                        finalShape
                                    )
                                }
                            }
                        },
                        enabled = playlistName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text(confirmButtonText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
