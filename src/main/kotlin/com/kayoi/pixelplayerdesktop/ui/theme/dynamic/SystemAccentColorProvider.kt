package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

object SystemAccentColorProvider {

    val defaultAccentColor = Color(0xFF6750A4)
    private const val PROCESS_TIMEOUT_MS = 500L

    @Volatile
    private var cachedColor: Color? = null

    fun getCachedAccentColor(): Color? = cachedColor

    fun getSystemAccentColor(): Color {
        cachedColor?.let { return it }

        val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
        val detected = when {
            os.contains("win") -> getWindowsAccentColor()
            os.contains("linux") || os.contains("nix") -> getLinuxAccentColor()
            os.contains("mac") || os.contains("darwin") -> getMacAccentColor()
            else -> null
        }

        val finalColor = detected ?: defaultAccentColor
        cachedColor = finalColor
        return finalColor
    }

    private fun getWindowsAccentColor(): Color? {
        try {
            val value = Advapi32Util.registryGetIntValue(
                WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Windows\\DWM",
                "ColorizationColor"
            )
            val r = (value shr 16) and 0xFF
            val g = (value shr 8) and 0xFF
            val b = value and 0xFF
            return Color(red = r, green = g, blue = b, alpha = 255)
        } catch (_: Throwable) {
            return getWindowsAccentColorViaRegProcess()
        }
    }

    private fun getWindowsAccentColorViaRegProcess(): Color? {
        val output = runProcessWithTimeout(
            listOf("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\DWM", "/v", "ColorizationColor")
        ) ?: return null

        val hexString = output.lineSequence().firstOrNull { it.contains("ColorizationColor") }
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.getOrNull(2)
            ?: return null

        val cleanHex = hexString.removePrefix("0x")
        val value = cleanHex.toLongOrNull(16) ?: return null
        val r = ((value shr 16) and 0xFF).toInt()
        val g = ((value shr 8) and 0xFF).toInt()
        val b = (value and 0xFF).toInt()
        return Color(red = r, green = g, blue = b, alpha = 255)
    }

    private fun getLinuxAccentColor(): Color? {
        getGnomeAccentColor()?.let { return it }
        return getKdeAccentColor()
    }

    private fun getGnomeAccentColor(): Color? {
        val output = runProcessWithTimeout(listOf("gsettings", "get", "org.gnome.desktop.interface", "accent-color"))
            ?: return null
        val name = output.removeSurrounding("'").lowercase(Locale.ROOT)
        return when (name) {
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
    }

    private fun getKdeAccentColor(): Color? {
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
                listOf("kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "AccentColor"),
                listOf("kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "AccentColor")
            )) {
                val output = runProcessWithTimeout(cmd) ?: continue
                val parts = output.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 3) {
                    return Color(parts[0], parts[1], parts[2])
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun getMacAccentColor(): Color? {
        val accentCode = runProcessWithTimeout(listOf("defaults", "read", "-g", "AppleAccentColor"))
            ?.toIntOrNull()
        if (accentCode != null) {
            return when (accentCode) {
                -1 -> Color(0xFF8E8E93)
                0 -> Color(0xFFFF3B30)
                1 -> Color(0xFFFF9500)
                2 -> Color(0xFFFFCC00)
                3 -> Color(0xFF34C759)
                4 -> Color(0xFFAF52DE)
                5 -> Color(0xFFFF2D55)
                else -> Color(0xFF007AFF)
            }
        }

        val highlightOutput = runProcessWithTimeout(listOf("defaults", "read", "-g", "AppleHighlightColor"))
        if (highlightOutput != null) {
            val parts = highlightOutput.split(Regex("\\s+"))
            if (parts.size >= 3) {
                val r = parts[0].toFloatOrNull()
                val g = parts[1].toFloatOrNull()
                val b = parts[2].toFloatOrNull()
                if (r != null && g != null && b != null) {
                    return Color(r, g, b)
                }
            }
        }

        return Color(0xFF007AFF)
    }

    internal fun runProcessWithTimeout(
        command: List<String>,
        timeoutMs: Long = PROCESS_TIMEOUT_MS
    ): String? {
        return try {
            val process = ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() != 0) {
                return null
            }
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Throwable) {
            null
        }
    }

    internal fun resetCache() {
        cachedColor = null
    }
}
