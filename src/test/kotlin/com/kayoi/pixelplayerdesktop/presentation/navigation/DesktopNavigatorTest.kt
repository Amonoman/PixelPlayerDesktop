package com.kayoi.pixelplayerdesktop.presentation.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DesktopNavigatorTest {

    @Test
    fun `initial state has correct defaults`() {
        val navigator = DesktopNavigator(DesktopNavTarget.Home)
        assertEquals(DesktopNavTarget.Home, navigator.currentTarget)
        assertEquals(DesktopNavDestination.HOME, navigator.currentDestination)
        assertFalse(navigator.canGoBack)
        assertFalse(navigator.canGoForward)
        assertEquals(1, navigator.backStack.size)
        assertTrue(navigator.forwardStack.isEmpty())
    }

    @Test
    fun `navigating pushes to stack and clears forward stack`() {
        val navigator = DesktopNavigator(DesktopNavTarget.Home)
        navigator.navigate(DesktopNavTarget.Songs)

        assertEquals(DesktopNavTarget.Songs, navigator.currentTarget)
        assertTrue(navigator.canGoBack)
        assertFalse(navigator.canGoForward)
        assertEquals(2, navigator.backStack.size)

        navigator.goBack()
        assertTrue(navigator.canGoForward)

        navigator.navigate(DesktopNavTarget.Albums)
        assertEquals(DesktopNavTarget.Albums, navigator.currentTarget)
        assertFalse(navigator.canGoForward)
    }

    @Test
    fun `goBack and goForward cycle correctly`() {
        val navigator = DesktopNavigator(DesktopNavTarget.Home)
        navigator.navigate(DesktopNavTarget.Songs)
        navigator.navigate(DesktopNavTarget.Albums)

        assertEquals(DesktopNavTarget.Albums, navigator.currentTarget)
        assertTrue(navigator.goBack())
        assertEquals(DesktopNavTarget.Songs, navigator.currentTarget)
        assertTrue(navigator.canGoForward)

        assertTrue(navigator.goForward())
        assertEquals(DesktopNavTarget.Albums, navigator.currentTarget)
        assertFalse(navigator.canGoForward)
    }

    @Test
    fun `navigateToRoot resets to target tab root`() {
        val navigator = DesktopNavigator(DesktopNavTarget.Home)
        navigator.navigateToRoot(DesktopNavDestination.ALBUMS)
        assertEquals(DesktopNavTarget.Albums, navigator.currentTarget)

        navigator.navigateToRoot(DesktopNavDestination.SETTINGS)
        assertEquals(DesktopNavTarget.Settings(), navigator.currentTarget)
    }

    @Test
    fun `back dispatcher prioritizes LIFO handlers`() {
        val dispatcher = DesktopBackDispatcher()
        val order = mutableListOf<String>()

        val reg1 = dispatcher.registerBackHandler {
            order.add("handler1")
            true
        }
        val reg2 = dispatcher.registerBackHandler {
            order.add("handler2")
            true
        }

        assertTrue(dispatcher.dispatchBack())
        assertEquals(listOf("handler2"), order)

        reg2.close()
        order.clear()

        assertTrue(dispatcher.dispatchBack())
        assertEquals(listOf("handler1"), order)

        reg1.close()
        assertFalse(dispatcher.dispatchBack())
    }

    @Test
    fun `navigating to RecentlyAdded updates currentTarget and rootDestination`() {
        val navigator = DesktopNavigator(DesktopNavTarget.Home)
        val target = DesktopNavTarget.RecentlyAdded
        navigator.navigate(target)

        assertEquals(target, navigator.currentTarget)
        assertEquals(DesktopNavDestination.HOME, navigator.currentDestination)
        assertTrue(navigator.canGoBack)
    }
}
