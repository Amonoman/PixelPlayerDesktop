package com.kayoi.pixelplayerdesktop.utils

import java.awt.Window
import java.nio.charset.StandardCharsets
import javax.swing.RootPaneContainer

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.IntByReference

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
    if (!Platform.isWindows()) return@lazy null
    try {
        Native.load("dwmapi", DwmApi::class.java)
    } catch (_: Throwable) {
        null
    }
}

fun applyNativeWindowAppearance(window: Window, darkTheme: Boolean) {
    when {
        Platform.isMac() -> applyMacOsAppearance(window, darkTheme)
        Platform.isWindows() -> applyWindowsAppearance(window, darkTheme)
        Platform.isLinux() -> applyLinuxAppearance(window, darkTheme)
    }
}

private fun applyMacOsAppearance(window: Window, darkTheme: Boolean) {
    if (window is RootPaneContainer) {
        val appearance = if (darkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
        window.rootPane.putClientProperty("apple.awt.windowAppearance", appearance)
    }
}

private fun applyWindowsAppearance(window: Window, darkTheme: Boolean) {
    val dwm = dwmApi ?: return
    val hwnd = try {
        Native.getComponentPointer(window)
    } catch (_: Throwable) {
        return
    }
    val enabled = if (darkTheme) 1 else 0
    if (setAttribute(dwm, hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, enabled)) return
    setAttribute(dwm, hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY, enabled)
}

private fun applyLinuxAppearance(window: Window, darkTheme: Boolean) {
    runCatching {
        val x11 = X11.INSTANCE
        val display = x11.XOpenDisplay(null) ?: return
        try {
            val windowPointer = Native.getComponentPointer(window) ?: return
            val xid = X11.Window(Pointer.nativeValue(windowPointer))
            val propAtom = x11.XInternAtom(display, "_GTK_THEME_VARIANT", false)
            var typeAtom = x11.XInternAtom(display, "UTF8_STRING", false)
            if (typeAtom.toInt() == 0) {
                typeAtom = x11.XInternAtom(display, "STRING", false)
            }
            val variant = if (darkTheme) "dark" else "light"
            val bytes = variant.toByteArray(StandardCharsets.UTF_8)
            val mem = Memory(bytes.size.toLong())
            mem.write(0, bytes, 0, bytes.size)
            x11.XChangeProperty(
                display,
                xid,
                propAtom,
                typeAtom,
                8,
                X11.PropModeReplace,
                mem,
                variant.length
            )
            x11.XFlush(display)
        } finally {
            x11.XCloseDisplay(display)
        }
    }
}

private fun setAttribute(dwm: DwmApi, hwnd: Pointer, attribute: Int, value: Int): Boolean = try {
    val reference = IntByReference(value)
    dwm.DwmSetWindowAttribute(hwnd, attribute, reference, Int.SIZE_BYTES) == 0
} catch (_: Throwable) {
    false
}
