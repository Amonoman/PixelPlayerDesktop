package com.kayoi.pixelplayerdesktop.presentation.screens.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopLandscapeHeroPlayerTest {

    private fun createSong(
        path: String = "test.flac",
        mimeType: String? = null,
        bitrate: Int? = null,
        sampleRate: Int? = null
    ): Song {
        return Song(
            id = "1",
            title = "Title",
            artist = "Artist",
            path = path,
            mimeType = mimeType,
            bitrate = bitrate,
            sampleRate = sampleRate
        )
    }

    @Test
    fun formatAudioMetaLabel_flacFullInfo() {
        val song = createSong(
            path = "test.flac",
            mimeType = "audio/flac",
            bitrate = 1171,
            sampleRate = 44100
        )
        val result = formatAudioMetaLabel(song)
        assertEquals("44.1 kHz • 1171 kbps • FLAC", result)
    }

    @Test
    fun formatAudioMetaLabel_bitrateInBps() {
        val song = createSong(
            path = "test.flac",
            mimeType = "audio/flac",
            bitrate = 1171000,
            sampleRate = 44100
        )
        val result = formatAudioMetaLabel(song)
        assertEquals("44.1 kHz • 1171 kbps • FLAC", result)
    }

    @Test
    fun formatAudioMetaLabel_sampleRateAndFormatOnly() {
        val song = createSong(
            path = "test.opus",
            mimeType = "audio/opus",
            bitrate = null,
            sampleRate = 48000
        )
        val result = formatAudioMetaLabel(song)
        assertEquals("48.0 kHz • OPUS", result)
    }

    @Test
    fun formatAudioMetaLabel_bitrateAndFormatOnly() {
        val song = createSong(
            path = "test.mp3",
            mimeType = null,
            bitrate = 320,
            sampleRate = null
        )
        val result = formatAudioMetaLabel(song)
        assertEquals("320 kbps • MP3", result)
    }

    @Test
    fun formatAudioMetaLabel_fallbackToExtension() {
        val song = createSong(
            path = "test.wav",
            mimeType = null,
            bitrate = null,
            sampleRate = null
        )
        val result = formatAudioMetaLabel(song)
        assertEquals("WAV", result)
    }

    @Test
    fun audioMetaUtils_formatBitrateHandlesKbpsAndBps() {
        assertEquals("1171 kbps", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatBitrate(1171))
        assertEquals("320 kbps", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatBitrate(320))
        assertEquals("320 kbps", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatBitrate(320000))
        assertEquals(null, com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatBitrate(null))
        assertEquals(null, com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatBitrate(0))
    }

    @Test
    fun audioMetaUtils_formatSampleRate() {
        assertEquals("44.1 kHz", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatSampleRate(44100))
        assertEquals("48.0 kHz", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatSampleRate(48000))
        assertEquals("96.0 kHz", com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatSampleRate(96000))
        assertEquals(null, com.kayoi.pixelplayerdesktop.utils.AudioMetaUtils.formatSampleRate(null))
    }
}
