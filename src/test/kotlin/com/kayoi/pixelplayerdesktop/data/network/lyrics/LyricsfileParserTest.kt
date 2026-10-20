package com.kayoi.pixelplayerdesktop.data.network.lyrics

import com.kayoi.pixelplayerdesktop.utils.LrcParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LyricsfileParserTest {

    @Test
    fun wordSyncedLyricsfileBecomesEnhancedLrcWithoutLosingText() {
        val lyricsfile = """
            version: '1.0'
            metadata:
              title: 'Test song'
            lines:
              - text: 'Hello: world #1'
                start_ms: 1000
                end_ms: 2400
                words:
                  - text: 'Hel'
                    start_ms: 1000
                    end_ms: 1200
                  - text: 'lo '
                    start_ms: 1200
                    end_ms: 1500
                  - text: 'world #1'
                    start_ms: 1500
                    end_ms: 2400
        """.trimIndent()

        val enhancedLrc = LyricsfileParser.toEnhancedLrc(lyricsfile)
        assertNotNull(enhancedLrc)

        val parsed = LrcParser.parse(enhancedLrc)
        assertEquals(1, parsed.synced?.size)
        val line = parsed.synced?.single()
        assertEquals("Hello: world #1", line?.line)
        assertEquals(listOf(1000, 1200, 1500), line?.words?.map { it.time })
    }

    @Test
    fun lineSyncedLyricsfileRemainsUsableWhenWordsAreAbsent() {
        val lyricsfile = """
            version: '1.0'
            lines:
              - text: "It's only a line"
                start_ms: 2500
                end_ms: 4000
        """.trimIndent()

        val enhancedLrc = LyricsfileParser.toEnhancedLrc(lyricsfile)
        assertNotNull(enhancedLrc)

        val parsed = LrcParser.parse(enhancedLrc)
        val single = parsed.synced?.single()
        assertEquals(2500, single?.time)
        assertEquals("It's only a line", single?.line)
        assertNull(single?.words)
    }

    @Test
    fun malformedOrEmptyLyricsfileIsIgnored() {
        assertNull(LyricsfileParser.toEnhancedLrc(null))
        assertNull(LyricsfileParser.toEnhancedLrc(""))
        assertNull(LyricsfileParser.toEnhancedLrc("not: yaml: content: ["))
        assertNull(LyricsfileParser.toEnhancedLrc("version: '2.0'\nlines: []"))
    }
}
