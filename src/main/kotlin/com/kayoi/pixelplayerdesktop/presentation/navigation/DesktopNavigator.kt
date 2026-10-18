package com.kayoi.pixelplayerdesktop.presentation.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

@Stable
class DesktopNavigator(initialTarget: DesktopNavTarget = DesktopNavTarget.Home) {
    private val _backStack = mutableStateListOf<DesktopNavTarget>(initialTarget)
    private val _forwardStack = mutableStateListOf<DesktopNavTarget>()

    val backStack: List<DesktopNavTarget> get() = _backStack
    val forwardStack: List<DesktopNavTarget> get() = _forwardStack

    val currentTarget: DesktopNavTarget get() = _backStack.lastOrNull() ?: DesktopNavTarget.Home
    val currentDestination: DesktopNavDestination get() = currentTarget.rootDestination

    val canGoBack: Boolean get() = _backStack.size > 1
    val canGoForward: Boolean get() = _forwardStack.isNotEmpty()

    fun navigate(target: DesktopNavTarget, clearForward: Boolean = true) {
        if (currentTarget == target) return
        _backStack.add(target)
        if (clearForward) {
            _forwardStack.clear()
        }
    }

    fun navigateToRoot(destination: DesktopNavDestination) {
        val rootTarget = destination.toDefaultTarget()
        if (currentDestination == destination && currentTarget != rootTarget) {
            navigate(rootTarget)
        } else if (currentDestination != destination) {
            navigate(rootTarget)
        }
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        val popped = _backStack.removeAt(_backStack.lastIndex)
        _forwardStack.add(popped)
        return true
    }

    fun goForward(): Boolean {
        if (!canGoForward) return false
        val target = _forwardStack.removeAt(_forwardStack.lastIndex)
        _backStack.add(target)
        return true
    }

    fun resetTo(target: DesktopNavTarget) {
        _backStack.clear()
        _backStack.add(target)
        _forwardStack.clear()
    }
}
