package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

/**
 * Detects native system accent colors across Linux (GNOME/KDE) and Windows platforms
 * to seamlessly align dynamic themes with user OS preferences.
 */
object SystemAccentColorProvider {

    private var cachedColor: Color? = null

    /**
     * Resolves the current operating system accent color with fallback to Material 3 baseline purple.
     */
    fun getSystemAccentColor(): Color {
        cachedColor?.let { return it }

        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        val detected = when {
            os.contains("win") -> getWindowsAccentColor()
            os.contains("linux") || os.contains("nix") -> getLinuxAccentColor()
            else -> null
        }

        val finalColor = detected ?: Color(0xFF6750A4)
        cachedColor = finalColor
        return finalColor
    }

    /**
     * Queries Windows Desktop Window Manager (DWM) registry for ColorizationColor.
     */
    private fun getWindowsAccentColor(): Color? {
        return try {
            val process = ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "ColorizationColor").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var hexString: String? = null
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.contains("ColorizationColor")) {
                    val parts = currentLine.trim().split(Regex("\\s+"))
                    if (parts.size >= 3) {
                        hexString = parts[2]
                        break
                    }
                }
            }
            process.waitFor()

            if (hexString != null) {
                val cleanHex = hexString.removePrefix("0x")
                val value = cleanHex.toLongOrNull(16)
                if (value != null) {
                    val r = ((value shr 16) and 0xFF).toInt()
                    val g = ((value shr 8) and 0xFF).toInt()
                    val b = (value and 0xFF).toInt()
                    Color(red = r, green = g, blue = b, alpha = 255)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attempts accent extraction for Linux environments (GNOME gsettings or KDE Plasma kdeglobals).
     */
    private fun getLinuxAccentColor(): Color? {
        // Query GNOME 47+ gsettings accent-color key
        try {
            val process = ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "accent-color").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()?.trim()?.removeSurrounding("'")?.lowercase(Locale.ROOT)
            process.waitFor()

            if (!line.isNullOrBlank()) {
                val color = when (line) {
                    "blue" -> Color(0xFF3584E4)
                    "teal" -> Color(0xFF2190A4)
                    "green" -> Color(0xFF3A944C)
                    "yellow" -> Color(0xFFE5A50A)
                    "orange" -> Color(0xFFE66100)
                    "red" -> Color(0xFFE01B24)
                    "pink" -> Color(0xFFD56199)
                    "purple" -> Color(0xFF9141AC)
                    "slate" -> Color(0xFF63717B)
                    else -> null
                }
                if (color != null) return color
            }
        } catch (_: Exception) {}

        // Query KDE Plasma AccentColor from user configuration
        try {
            val kdeglobals = File(System.getProperty("user.home"), ".config/kdeglobals")
            if (kdeglobals.exists() && kdeglobals.canRead()) {
                var inGeneralSection = false
                kdeglobals.bufferedReader().use { br ->
                    var rawLine: String?
                    while (br.readLine().also { rawLine = it } != null) {
                        val currentLine = rawLine?.trim() ?: continue
                        if (currentLine.startsWith("[") && currentLine.endsWith("]")) {
                            inGeneralSection = currentLine.equals("[General]", ignoreCase = true)
                        } else if (inGeneralSection && currentLine.startsWith("AccentColor=", ignoreCase = true)) {
                            val value = currentLine.substringAfter("=").trim()
                            val parts = value.split(",").mapNotNull { it.trim().toIntOrNull() }
                            if (parts.size >= 3) {
                                return Color(parts[0], parts[1], parts[2])
                            }
                        }
                    }
                }
            }

            for (cmd in listOf(
                arrayOf("kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "AccentColor"),
                arrayOf("kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "AccentColor")
            )) {
                val process = ProcessBuilder(*cmd).start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine()?.trim()
                process.waitFor()
                if (!line.isNullOrBlank() && line.contains(",")) {
                    val parts = line.split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (parts.size >= 3) {
                        return Color(parts[0], parts[1], parts[2])
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }
}
