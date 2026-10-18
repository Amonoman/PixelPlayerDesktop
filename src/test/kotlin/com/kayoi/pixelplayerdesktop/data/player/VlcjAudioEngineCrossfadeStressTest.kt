package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class VlcjAudioEngineCrossfadeStressTest {

    companion object {
        private val tempDir = File(System.getProperty("java.io.tmpdir"))
        private val audioFile1 = File(tempDir, "pixelplayer_stress_tone1.wav")
        private val audioFile2 = File(tempDir, "pixelplayer_stress_tone2.wav")

        init {
            ensureWav(audioFile1, durationSec = 10, freqHz = 440.0)
            ensureWav(audioFile2, durationSec = 10, freqHz = 880.0)
        }

        private fun ensureWav(file: File, durationSec: Int, freqHz: Double) {
            if (file.exists() && file.length() > 5000) return
            val sampleRate = 44100f
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val numSamples = (durationSec * sampleRate).toInt()
            val buffer = ByteArray(numSamples * 2)
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i * freqHz / sampleRate
                val sample = (Math.sin(angle) * 16000).toInt().toShort()
                buffer[2 * i] = (sample.toInt() and 0xFF).toByte()
                buffer[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            val bais = ByteArrayInputStream(buffer)
            val ais = AudioInputStream(bais, format, numSamples.toLong())
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file)
        }
    }

    private var engine: VlcjAudioEngine? = null

    @BeforeTest
    fun setUp() {
        val eng = VlcjAudioEngine()
        assumeTrue(eng.getActivePlayerForTest() != null, "LibVLC is required for crossfade stress tests")
        engine = eng
    }

    @AfterTest
    fun tearDown() {
        engine?.release()
        engine = null
    }

    private fun waitForPlaying(eng: VlcjAudioEngine, timeoutMs: Long = 3000L): Boolean {
        return runBlocking {
            withTimeoutOrNull(timeoutMs) {
                eng.playbackState.first { it == PlaybackState.PLAYING }
                true
            } ?: false
        }
    }

    @Test
    fun testRapidSkipDuringCrossfadeGuaranteesConsistentSnapshotAndNoAudioLeaks() {
        val eng = engine ?: return
        eng.setVolume(0.5f)
        eng.play(audioFile1.absolutePath)
        assertTrue(waitForPlaying(eng))

        repeat(15) { iteration ->
            val nextFile = if (iteration % 2 == 0) audioFile2 else audioFile1
            eng.crossfadeTo(nextFile.absolutePath, 1500L)
            Thread.sleep(Random.nextLong(20L, 60L))

            val active = eng.getActivePlayerForTest()
            val standby = eng.getStandbyPlayerForTest()
            assertNotSame(active, standby, "Active and standby players must never be the same instance")
        }

        eng.stop()
        assertFalse(eng.isCrossfading.value, "isCrossfading must be false after stop()")

        val active = eng.getActivePlayerForTest()
        val standby = eng.getStandbyPlayerForTest()
        val activePlaying = try { active?.status()?.isPlaying ?: false } catch (_: Exception) { false }
        val standbyPlaying = try { standby?.status()?.isPlaying ?: false } catch (_: Exception) { false }

        assertFalse(activePlaying, "Active player must not be playing after stop()")
        assertFalse(standbyPlaying, "Standby player must not be playing after stop()")
        assertEquals(PlaybackState.IDLE, eng.playbackState.value)
    }

    @Test
    fun testPauseDuringCrossfadeCancelsCrossfadeAndLeavesSinglePlayerPaused() {
        val eng = engine ?: return
        eng.setVolume(0.8f)
        eng.play(audioFile1.absolutePath)
        assertTrue(waitForPlaying(eng))

        eng.crossfadeTo(audioFile2.absolutePath, 2000L)
        runBlocking {
            withTimeoutOrNull(2000L) {
                eng.isCrossfading.first { it }
            }
        }
        Thread.sleep(100L)

        eng.pause()

        assertFalse(eng.isCrossfading.value, "Crossfade must be cancelled upon pause()")
        assertEquals(PlaybackState.PAUSED, eng.playbackState.value)

        val active = eng.getActivePlayerForTest()
        val standby = eng.getStandbyPlayerForTest()
        val standbyPlaying = try { standby?.status()?.isPlaying ?: false } catch (_: Exception) { false }
        assertFalse(standbyPlaying, "Standby player must be stopped on pause")

        var activeVol = try { active?.audio()?.volume() ?: 0 } catch (_: Exception) { 0 }
        var attempts = 0
        while (activeVol != 80 && attempts < 10) {
            Thread.sleep(50L)
            activeVol = try { active?.audio()?.volume() ?: 0 } catch (_: Exception) { 0 }
            attempts++
        }
        assertEquals(80, activeVol, "Incoming player must be at target volume (80) after crossfade is cancelled by pause")

        eng.play()
        assertTrue(waitForPlaying(eng))
        assertEquals(PlaybackState.PLAYING, eng.playbackState.value)

        val standbyPlayingAfterResume = try { standby?.status()?.isPlaying ?: false } catch (_: Exception) { false }
        assertFalse(standbyPlayingAfterResume, "Standby player must remain stopped after resuming from pause")
    }

    @Test
    fun testSeekDuringCrossfadeCancelsCrossfadeAndRestoresVolume() {
        val eng = engine ?: return
        eng.setVolume(0.6f)
        eng.play(audioFile1.absolutePath)
        assertTrue(waitForPlaying(eng))

        eng.crossfadeTo(audioFile2.absolutePath, 2000L)
        runBlocking {
            withTimeoutOrNull(2000L) {
                eng.isCrossfading.first { it }
            }
        }
        Thread.sleep(100L)

        eng.seekTo(1500L)

        assertFalse(eng.isCrossfading.value, "Crossfade must be cancelled upon seekTo()")

        val active = eng.getActivePlayerForTest()
        val standby = eng.getStandbyPlayerForTest()
        val standbyPlaying = try { standby?.status()?.isPlaying ?: false } catch (_: Exception) { false }
        assertFalse(standbyPlaying, "Standby player must be stopped after seek during crossfade")

        var activeVol = try { active?.audio()?.volume() ?: 0 } catch (_: Exception) { 0 }
        var attempts = 0
        while (activeVol != 60 && attempts < 10) {
            Thread.sleep(50L)
            activeVol = try { active?.audio()?.volume() ?: 0 } catch (_: Exception) { 0 }
            attempts++
        }
        assertEquals(60, activeVol, "Active player volume must be restored to target volume (60)")
    }

    @Test
    fun testConcurrentCommandSpamStressDoesNotDeadlockOrCorruptSnapshot() {
        val eng = engine ?: return
        eng.setVolume(0.5f)
        eng.play(audioFile1.absolutePath)
        assertTrue(waitForPlaying(eng))

        val threadCount = 6
        val operationsPerThread = 20
        val latch = CountDownLatch(threadCount)
        val errors = AtomicInteger(0)

        repeat(threadCount) { threadIndex ->
            thread {
                try {
                    for (i in 0 until operationsPerThread) {
                        when ((threadIndex + i) % 5) {
                            0 -> eng.crossfadeTo(if (i % 2 == 0) audioFile2.absolutePath else audioFile1.absolutePath, 1000L)
                            1 -> eng.pause()
                            2 -> eng.play()
                            3 -> eng.seekTo(Random.nextLong(100L, 2000L))
                            4 -> eng.setVolume(Random.nextFloat().coerceIn(0.1f, 0.9f))
                        }
                        val active = eng.getActivePlayerForTest()
                        val standby = eng.getStandbyPlayerForTest()
                        if (active === standby && active != null) {
                            errors.incrementAndGet()
                        }
                        Thread.sleep(Random.nextLong(5L, 25L))
                    }
                } catch (_: Throwable) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent stress test must complete within 10 seconds without deadlocking")
        assertEquals(0, errors.get(), "No snapshot corruption or exceptions during concurrent spam")

        eng.stop()
        assertFalse(eng.isCrossfading.value)
        val active = eng.getActivePlayerForTest()
        val standby = eng.getStandbyPlayerForTest()
        assertNotSame(active, standby)
    }
}
