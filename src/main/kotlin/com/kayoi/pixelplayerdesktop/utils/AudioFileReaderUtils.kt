package com.kayoi.pixelplayerdesktop.utils

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.FlacFileReader
import org.jaudiotagger.audio.generic.AudioFileReader
import org.jaudiotagger.audio.mp3.MP3FileReader
import org.jaudiotagger.audio.mp4.Mp4FileReader
import org.jaudiotagger.audio.ogg.OggFileReader
import org.jaudiotagger.audio.wav.WavFileReader
import java.io.File
import java.io.FileInputStream

object AudioFileReaderUtils {

    fun readAudioFile(file: File): AudioFile {
        return try {
            AudioFileIO.read(file)
        } catch (originalException: Exception) {
            readWithContainerFallback(file, originalException)
        }
    }

    private fun readWithContainerFallback(file: File, originalException: Exception): AudioFile {
        val detectedReader = detectReaderByMagicBytes(file)
        if (detectedReader != null) {
            try {
                return detectedReader.read(file)
            } catch (_: Exception) { }
        }

        val fallbackReaders = listOf(
            Mp4FileReader(),
            MP3FileReader(),
            FlacFileReader(),
            OggFileReader(),
            WavFileReader()
        )

        for (reader in fallbackReaders) {
            if (detectedReader != null && reader.javaClass == detectedReader.javaClass) continue
            try {
                return reader.read(file)
            } catch (_: Exception) { }
        }

        throw originalException
    }

    private fun detectReaderByMagicBytes(file: File): AudioFileReader? {
        return try {
            val header = ByteArray(12)
            val bytesRead = FileInputStream(file).use { it.read(header) }
            if (bytesRead < 4) return null

            if (bytesRead >= 8 &&
                header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
            ) {
                return Mp4FileReader()
            }

            if (header[0] == 'f'.code.toByte() && header[1] == 'L'.code.toByte() &&
                header[2] == 'a'.code.toByte() && header[3] == 'C'.code.toByte()
            ) {
                return FlacFileReader()
            }

            if (header[0] == 'O'.code.toByte() && header[1] == 'g'.code.toByte() &&
                header[2] == 'g'.code.toByte() && header[3] == 'S'.code.toByte()
            ) {
                return OggFileReader()
            }

            if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                return MP3FileReader()
            }

            if (bytesRead >= 12 &&
                header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'A'.code.toByte() &&
                header[10] == 'V'.code.toByte() && header[11] == 'E'.code.toByte()
            ) {
                return WavFileReader()
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}
