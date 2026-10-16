package com.kayoi.pixelplayerdesktop.data.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayGainManagerTest {

    private val manager = ReplayGainManager()

    @Test
    fun testParseGainString() {
        assertEquals(-12.0f, manager.parseGainString("-12.00 dB"))
        assertEquals(-6.5f, manager.parseGainString("-6.5 dB"))
        assertEquals(2.4f, manager.parseGainString("+2.4 dB"))
        assertEquals(-3.0f, manager.parseGainString("-3.0dB"))
        assertEquals(0.0f, manager.parseGainString("0.0"))
        assertNull(manager.parseGainString("not_a_number"))
        assertNull(manager.parseGainString(""))
    }

    @Test
    fun testTrackVsAlbumModeSelection() {
        val values = ReplayGainValues(trackGainDb = -12.0f, albumGainDb = -6.0f)

        // Track mode: should pick trackGainDb (-12 dB -> ~0.251)
        val trackMultiplier = manager.getVolumeMultiplier(values, useAlbumGain = false)
        assertTrue(trackMultiplier in 0.24f..0.26f, "Track multiplier was $trackMultiplier")

        // Album mode: should pick albumGainDb (-6 dB -> ~0.501)
        val albumMultiplier = manager.getVolumeMultiplier(values, useAlbumGain = true)
        assertTrue(albumMultiplier in 0.49f..0.51f, "Album multiplier was $albumMultiplier")
    }

    @Test
    fun testModeFallbacks() {
        // Track only: album mode falls back to track
        val trackOnly = ReplayGainValues(trackGainDb = -6.0f, albumGainDb = null)
        val fallbackFromAlbum = manager.getVolumeMultiplier(trackOnly, useAlbumGain = true)
        assertTrue(fallbackFromAlbum in 0.49f..0.51f)

        // Album only: track mode falls back to album
        val albumOnly = ReplayGainValues(trackGainDb = null, albumGainDb = -6.0f)
        val fallbackFromTrack = manager.getVolumeMultiplier(albumOnly, useAlbumGain = false)
        assertTrue(fallbackFromTrack in 0.49f..0.51f)

        // None: returns 1.0f
        assertEquals(1.0f, manager.getVolumeMultiplier(null, useAlbumGain = false))
        assertEquals(1.0f, manager.getVolumeMultiplier(ReplayGainValues(null, null), useAlbumGain = true))
    }

    @Test
    fun testNonExistentFileReturnsNull() {
        assertNull(manager.readReplayGain(""))
        assertNull(manager.readReplayGain("/non/existent/path/song.mp3"))
    }
}
