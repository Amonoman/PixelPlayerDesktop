package com.kayoi.pixelplayerdesktop.platform.mediasession.linux

import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaMetadata
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaPlaybackStatus
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinuxMprisMediaSessionControllerTest {

    private class RecordingCommands : MediaSessionCommands {
        val playCount = AtomicInteger(0)
        val pauseCount = AtomicInteger(0)
        val playPauseCount = AtomicInteger(0)
        val nextCount = AtomicInteger(0)
        val prevCount = AtomicInteger(0)
        val stopCount = AtomicInteger(0)
        val lastSeekOffsetMs = AtomicLong(0L)
        val lastSetPositionMs = AtomicLong(-1L)

        override fun play() { playCount.incrementAndGet() }
        override fun pause() { pauseCount.incrementAndGet() }
        override fun playPause() { playPauseCount.incrementAndGet() }
        override fun next() { nextCount.incrementAndGet() }
        override fun previous() { prevCount.incrementAndGet() }
        override fun stop() { stopCount.incrementAndGet() }
        override fun seek(offsetMs: Long) { lastSeekOffsetMs.set(offsetMs) }
        override fun setPosition(positionMs: Long) { lastSetPositionMs.set(positionMs) }
    }

    private lateinit var scope: CoroutineScope
    private lateinit var commands: RecordingCommands
    private lateinit var testBusName: String
    private lateinit var playerctlName: String
    private var controller: LinuxMprisMediaSessionController? = null

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        commands = RecordingCommands()
        val uniqueSuffix = "test_${System.currentTimeMillis()}"
        testBusName = "org.mpris.MediaPlayer2.pixelplayer_$uniqueSuffix"
        playerctlName = "pixelplayer_$uniqueSuffix"
    }

    @AfterTest
    fun tearDown() {
        controller?.release()
        scope.cancel()
    }

    private fun runProcess(vararg args: String): Pair<Int, String> {
        val pb = ProcessBuilder(*args)
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val text = proc.inputStream.bufferedReader().use { it.readText().trim() }
        val exit = proc.waitFor()
        return exit to text
    }

    @Test
    fun testMprisPropertiesAndPlayerctlCommands() {
        val isLinux = System.getProperty("os.name", "").lowercase().contains("linux")
        val hasPlayerctl = try {
            ProcessBuilder("which", "playerctl").start().waitFor() == 0
        } catch (_: Exception) { false }
        val hasDbus = try {
            ProcessBuilder("which", "dbus-send").start().waitFor() == 0
        } catch (_: Exception) { false }

        assumeTrue(
            isLinux && hasPlayerctl && hasDbus,
            "Linux with playerctl and dbus-send is required for MPRIS integration test"
        )

        val ctrl = LinuxMprisMediaSessionController(
            commands = commands,
            coroutineScope = scope,
            preferredBusName = testBusName
        )
        controller = ctrl

        val dummyArtFile = File(System.getProperty("java.io.tmpdir"), "pixelplayer_sunflower_test.jpg")
        ctrl.updateMetadata(
            MediaMetadata(
                trackId = "track_123",
                title = "Sunflower",
                artist = "Post Malone, Swae Lee",
                album = "Spider-Man: Into the Spider-Verse",
                albumArtist = "Various Artists",
                artUri = dummyArtFile.absolutePath,
                durationMs = 158000L
            )
        )
        ctrl.updatePlaybackStatus(MediaPlaybackStatus.PLAYING)
        ctrl.updatePosition(30000L)
        ctrl.updateCapabilities(
            canGoNext = true,
            canGoPrevious = true,
            canPlay = true,
            canPause = true,
            canSeek = true
        )

        val (statusExit, statusOut) = runProcess("playerctl", "-p", playerctlName, "status")
        assertEquals(0, statusExit, "playerctl status should exit 0, got output: $statusOut")
        assertEquals("Playing", statusOut)

        val (titleExit, titleOut) = runProcess("playerctl", "-p", playerctlName, "metadata", "title")
        assertEquals(0, titleExit)
        assertEquals("Sunflower", titleOut)

        val (artistExit, artistOut) = runProcess("playerctl", "-p", playerctlName, "metadata", "artist")
        assertEquals(0, artistExit)
        assertEquals("Post Malone, Swae Lee", artistOut)

        val (albumExit, albumOut) = runProcess("playerctl", "-p", playerctlName, "metadata", "album")
        assertEquals(0, albumExit)
        assertEquals("Spider-Man: Into the Spider-Verse", albumOut)

        // Test commands
        runProcess("playerctl", "-p", playerctlName, "play-pause")
        Thread.sleep(150)
        assertEquals(1, commands.playPauseCount.get())

        runProcess("playerctl", "-p", playerctlName, "next")
        Thread.sleep(150)
        assertEquals(1, commands.nextCount.get())

        runProcess("playerctl", "-p", playerctlName, "previous")
        Thread.sleep(150)
        assertEquals(1, commands.prevCount.get())

        runProcess("playerctl", "-p", playerctlName, "stop")
        Thread.sleep(150)
        assertEquals(1, commands.stopCount.get())

        // Test position
        runProcess("playerctl", "-p", playerctlName, "position", "45")
        Thread.sleep(150)
        assertEquals(45000L, commands.lastSetPositionMs.get())

        // Test relative seek
        runProcess("playerctl", "-p", playerctlName, "position", "10+")
        Thread.sleep(150)
        assertEquals(10000L, commands.lastSeekOffsetMs.get())

        // Test dbus-send GetAll
        val (dbusExit, dbusOut) = runProcess(
            "dbus-send", "--session", "--print-reply",
            "--dest=$testBusName",
            "/org/mpris/MediaPlayer2",
            "org.freedesktop.DBus.Properties.GetAll",
            "string:org.mpris.MediaPlayer2.Player"
        )
        assertEquals(0, dbusExit)
        assertTrue(dbusOut.contains("PlaybackStatus"), "D-Bus GetAll should contain PlaybackStatus: $dbusOut")
        assertTrue(dbusOut.contains("CanControl"), "D-Bus GetAll should contain CanControl")

        // Test release
        ctrl.release()
        val (afterReleaseExit, _) = runProcess("playerctl", "-p", playerctlName, "status")
        assertTrue(afterReleaseExit != 0, "playerctl should not find player after release")
    }
}
