package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemAccentColorProviderTest {

    @Test
    fun testJnaClassesOnClasspath() {
        assertNotNull(Advapi32Util::class.java)
        assertNotNull(WinReg::class.java)
    }

    @Test
    fun testMissingBinaryHandledWithoutThrowing() {
        val result = SystemAccentColorProvider.runProcessWithTimeout(
            listOf("non-existent-binary-xyz-987654321")
        )
        assertNull(result)
    }

    @Test
    fun testProcessTimeoutEnforcedAndDestroyedForcibly() {
        val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
        val cmd = if (isWindows) listOf("ping", "127.0.0.1", "-n", "6") else listOf("sleep", "5")
        val duration = measureTimeMillis {
            val result = SystemAccentColorProvider.runProcessWithTimeout(
                cmd,
                timeoutMs = 100L
            )
            assertNull(result)
        }
        assertTrue(duration < 2000L, "Process should be killed on timeout, took: ${duration}ms")
    }

    @Test
    fun testNonZeroExitCodeReturnsNullWithoutThrowing() {
        val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
        val cmd = if (isWindows) listOf("cmd", "/c", "exit 2") else listOf("sh", "-c", "echo 'failed' >&2; exit 2")
        val result = SystemAccentColorProvider.runProcessWithTimeout(cmd)
        assertNull(result)
    }

    @Test
    fun testSystemAccentColorResolvesAndCaches() {
        SystemAccentColorProvider.resetCache()
        assertNull(SystemAccentColorProvider.getCachedAccentColor())

        val first = SystemAccentColorProvider.getSystemAccentColor()
        assertNotNull(first)
        assertEquals(first, SystemAccentColorProvider.getCachedAccentColor())

        val second = SystemAccentColorProvider.getSystemAccentColor()
        assertEquals(first, second)
    }

    @Test
    fun testSensibleDefaultWhenNoAccentFound() {
        assertEquals(Color(0xFF6750A4), SystemAccentColorProvider.defaultAccentColor)
    }
}
