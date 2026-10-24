package com.kayoi.pixelplayerdesktop.utils

import java.awt.Desktop
import java.net.URI

object BrowserUtils {
    fun openBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: Exception) {}

        val os = System.getProperty("os.name", "").lowercase()
        runCatching {
            val command = when {
                os.contains("mac") -> arrayOf("open", url)
                os.contains("win") -> arrayOf("cmd", "/c", "start", "", url)
                else -> arrayOf("xdg-open", url)
            }
            Runtime.getRuntime().exec(command)
        }
    }
}
