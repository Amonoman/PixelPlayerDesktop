package com.kayoi.pixelplayerdesktop.utils

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcParserTest {

    @Test
    fun parseSyncedLyricsSuccessfully() {
        val lrcContent = """
            [00:12.50]First line
            [01:05.120]Second line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        assertEquals(2, result.synced?.size)
        assertEquals(12500, result.synced?.get(0)?.time)
        assertEquals("First line", result.synced?.get(0)?.line)
        assertEquals(65120, result.synced?.get(1)?.time)
        assertEquals("Second line", result.synced?.get(1)?.line)
    }

    @Test
    fun toLrcStringFormatsSyncedLyrics() {
        val lyrics = Lyrics(
            synced = listOf(
                SyncedLine(time = 12500, line = "First line"),
                SyncedLine(time = 65000, line = "Second line")
            )
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertTrue(lrcString.contains("[00:12.50]First line"))
        assertTrue(lrcString.contains("[01:05.00]Second line"))
    }

    @Test
    fun toLrcStringFormatsPlainLyrics() {
        val lyrics = Lyrics(
            plain = listOf("Line one", "Line two")
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertEquals("Line one\nLine two", lrcString)
    }
}
