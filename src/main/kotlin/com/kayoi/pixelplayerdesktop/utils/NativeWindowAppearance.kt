package com.kayoi.pixelplayerdesktop.utils

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY = 19

private interface DwmApi : Library {
    fun DwmSetWindowAttribute(
        hwnd: Pointer,
        dwAttribute: Int,
        pvAttribute: IntByReference,
        cbAttribute: Int
    ): Int
}

private val dwmApi: DwmApi? by lazy {
    if (!isWindowsOs()) return@lazy null
    try {
        Native.load("dwmapi", DwmApi::class.java)
    } catch (_: Throwable) {
        null
    }
}

private fun isWindowsOs(): Boolean =
    System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

/**
 * Applies [darkTheme] to the native title bar. Windows-only; no-op elsewhere.
 * Call after the window peer exists.
 */
fun applyNativeWindowAppearance(window: Window, darkTheme: Boolean) {
    val dwm = dwmApi ?: return
    val hwnd = try {
        Native.getComponentPointer(window)
    } catch (_: Throwable) {
        return
    }
    val enabled = if (darkTheme) 1 else 0
    if (setAttribute(dwm, hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, enabled)) return
    // Windows 10 builds before 20H1 exposed the same attribute under a different id.
    setAttribute(dwm, hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY, enabled)
}

private fun setAttribute(dwm: DwmApi, hwnd: Pointer, attribute: Int, value: Int): Boolean = try {
    val reference = IntByReference(value)
    dwm.DwmSetWindowAttribute(hwnd, attribute, reference, Int.SIZE_BYTES) == 0
} catch (_: Throwable) {
    false
}
