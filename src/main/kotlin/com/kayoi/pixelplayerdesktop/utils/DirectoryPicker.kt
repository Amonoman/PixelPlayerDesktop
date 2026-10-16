package com.kayoi.pixelplayerdesktop.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object DirectoryPicker {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun chooseDirectory(
        initialDir: File? = null,
        title: String = "Select Music Directory",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val directory = FileKit.openDirectoryPicker(
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (directory != null) {
                onSelected(directory.file)
            }
        }
    }

    fun chooseLrcFile(
        title: String = "Select .lrc Lyric File",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File("lrc"),
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }

    fun saveLrcFile(
        defaultFileName: String = "lyrics.lrc",
        initialDir: File? = null,
        title: String = "Save .lrc Lyric File",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val baseName = if (defaultFileName.endsWith(".lrc", ignoreCase = true)) {
                defaultFileName.substringBeforeLast(".lrc")
            } else {
                defaultFileName
            }

            val file = FileKit.openFileSaver(
                suggestedName = baseName,
                defaultExtension = "lrc",
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                val finalFile = if (file.file.name.endsWith(".lrc", ignoreCase = true)) {
                    file.file
                } else {
                    File(file.file.parentFile, "${file.file.name}.lrc")
                }
                onSelected(finalFile)
            }
        }
    }

    fun chooseImageFile(
        title: String = "Select Album Artwork",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.Image,
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }
}
