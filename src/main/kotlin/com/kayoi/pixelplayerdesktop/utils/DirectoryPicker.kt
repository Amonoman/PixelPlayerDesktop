package com.kayoi.pixelplayerdesktop.utils

import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

object DirectoryPicker {
    fun chooseDirectory(
        initialDir: File? = null,
        title: String = "Select Music Directory",
        onSelected: (File) -> Unit
    ) {
        SwingUtilities.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (_: Exception) {}

            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isAcceptAllFileFilterUsed = false
                if (initialDir != null && initialDir.exists()) {
                    currentDirectory = initialDir
                }
            }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                onSelected(chooser.selectedFile)
            }
        }
    }

    fun chooseLrcFile(
        title: String = "Select .lrc Lyric File",
        onSelected: (File) -> Unit
    ) {
        SwingUtilities.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (_: Exception) {}

            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter("LRC Lyric Files (*.lrc)", "lrc")
            }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                onSelected(chooser.selectedFile)
            }
        }
    }

    fun chooseImageFile(
        title: String = "Select Album Artwork",
        onSelected: (File) -> Unit
    ) {
        SwingUtilities.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (_: Exception) {}

            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files (*.jpg, *.jpeg, *.png, *.webp)",
                    "jpg", "jpeg", "png", "webp"
                )
            }

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                onSelected(chooser.selectedFile)
            }
        }
    }
}
