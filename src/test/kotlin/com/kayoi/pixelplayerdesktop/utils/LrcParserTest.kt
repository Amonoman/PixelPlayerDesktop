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

    @Test
    fun parseWordSyncedLyricsSuccessfully() {
        val lrcContent = """
            [00:01.00]<00:01.00>Hel<00:01.50>lo <00:02.00>world
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)
        assertEquals(1, result.synced?.size)
        val line = result.synced?.firstOrNull()
        assertEquals("Hello world", line?.line)
        assertEquals(3, line?.words?.size)

        val words = line?.words ?: emptyList()
        assertEquals("Hel", words[0].word)
        assertEquals(1000, words[0].time)
        assertTrue(words[0].startsNewWord)

        assertEquals("lo", words[1].word)
        assertEquals(1500, words[1].time)
        assertEquals(false, words[1].startsNewWord)

        assertEquals("world", words[2].word)
        assertEquals(2000, words[2].time)
        assertTrue(words[2].startsNewWord)
    }

    @Test
    fun toLrcStringFormatsWordSyncedLyrics() {
        val lyrics = Lyrics(
            synced = listOf(
                SyncedLine(
                    time = 1000,
                    line = "Hello world",
                    words = listOf(
                        com.kayoi.pixelplayerdesktop.data.model.SyncedWord(1000, "Hello", startsNewWord = true),
                        com.kayoi.pixelplayerdesktop.data.model.SyncedWord(2000, "world", startsNewWord = true)
                    )
                )
            )
        )

        val lrcString = LrcParser.toLrcString(lyrics)
        assertEquals("[00:01.00]<00:01.00>Hello <00:02.00>world", lrcString)
    }
}
