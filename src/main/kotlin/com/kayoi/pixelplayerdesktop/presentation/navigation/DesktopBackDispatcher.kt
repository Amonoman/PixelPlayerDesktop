package com.kayoi.pixelplayerdesktop.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

class DesktopBackDispatcher {
    private val backHandlers = mutableListOf<() -> Boolean>()
    private val forwardHandlers = mutableListOf<() -> Boolean>()

    fun registerBackHandler(handler: () -> Boolean): AutoCloseable {
        backHandlers.add(0, handler)
        return AutoCloseable { backHandlers.remove(handler) }
    }

    fun registerForwardHandler(handler: () -> Boolean): AutoCloseable {
        forwardHandlers.add(0, handler)
        return AutoCloseable { forwardHandlers.remove(handler) }
    }

    fun dispatchBack(): Boolean {
        for (handler in backHandlers.toList()) {
            if (handler()) return true
        }
        return false
    }

    fun dispatchForward(): Boolean {
        for (handler in forwardHandlers.toList()) {
            if (handler()) return true
        }
        return false
    }
}

val LocalBackDispatcher = staticCompositionLocalOf {
    DesktopBackDispatcher()
}

@Composable
fun DesktopBackHandler(
    enabled: Boolean = true,
    dispatcher: DesktopBackDispatcher = LocalBackDispatcher.current,
    onBack: () -> Boolean
) {
    DisposableEffect(enabled, dispatcher, onBack) {
        if (!enabled) return@DisposableEffect onDispose {}
        val registration = dispatcher.registerBackHandler(onBack)
        onDispose {
            registration.close()
        }
    }
}
