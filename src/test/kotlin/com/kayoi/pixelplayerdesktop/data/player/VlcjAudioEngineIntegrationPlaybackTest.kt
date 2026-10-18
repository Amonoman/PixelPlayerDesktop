package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertTrue

class VlcjAudioEngineIntegrationPlaybackTest {

    private fun ensureTestWav(file: File) {
        if (file.exists() && file.length() > 1000) return
        val sampleRate = 44100f
        val format = AudioFormat(sampleRate, 16, 1, true, false)
        val numSamples = (2 * sampleRate).toInt()
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i * 440.0 / sampleRate
            val sample = (Math.sin(angle) * 16000).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        val bais = ByteArrayInputStream(buffer)
        val ais = AudioInputStream(bais, format, numSamples.toLong())
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file)
    }

    @Test
    fun testPlayTrackThenReleaseCleansUpAudioStreamWithinBound() {
        val testAudio = File(System.getProperty("java.io.tmpdir"), "pixelplayer_integration_test_tone.wav")
        ensureTestWav(testAudio)

        val engine = VlcjAudioEngine()
        assumeTrue(engine.getActivePlayerForTest() != null, "LibVLC is required for audio integration test")

        engine.setVolume(0.1f)
        engine.play(testAudio.absolutePath)

        // Wait up to 3s for playback to transition into PLAYING
        runBlocking {
            withTimeoutOrNull(3000L) {
                engine.playbackState.first { it == PlaybackState.PLAYING }
            }
        }

        // Measure teardown time
        val startRelease = System.currentTimeMillis()
        engine.release()
        val elapsedRelease = System.currentTimeMillis() - startRelease

        assertTrue(engine.isClosed(), "Engine must report isClosed() == true after release()")
        assertTrue(elapsedRelease < 3000L, "Engine release must complete within 3 seconds, took ${elapsedRelease}ms")
    }
}
