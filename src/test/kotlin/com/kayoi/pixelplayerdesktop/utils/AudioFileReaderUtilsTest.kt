package com.kayoi.pixelplayerdesktop.utils

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AudioFileReaderUtilsTest {

    @Test
    fun testAudioFileReaderUtilsThrowsOnNonAudioFile() {
        val tempFile = File.createTempFile("test_corrupt", ".mp3")
        tempFile.deleteOnExit()
        tempFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7))

        assertFailsWith<Exception> {
            AudioFileReaderUtils.readAudioFile(tempFile)
        }
    }

    @Test
    fun testAudioFileReaderUtilsReadsMismatchedWavWithMp3Extension() {
        val sampleRate = 8000
        val durationSeconds = 12
        val dataSize = sampleRate * durationSeconds
        val chunkSize = 36 + dataSize
        val buffer = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(chunkSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate)
        buffer.putShort(1)
        buffer.putShort(8)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        buffer.put(ByteArray(dataSize))

        val tempFile = File.createTempFile("test_synthetic_wav", ".mp3")
        tempFile.deleteOnExit()
        tempFile.writeBytes(buffer.array())

        val audioFile = AudioFileReaderUtils.readAudioFile(tempFile)
        assertEquals(durationSeconds, audioFile.audioHeader.trackLength)
    }
}
