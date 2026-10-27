package com.kayoi.pixelplayerdesktop.data.playlist

import java.io.File
import java.io.IOException
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song

class M3uManagerTest {

    private val songs = listOf(
        Song(
            id = "song_1",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            path = "/home/user/Music/M83/Midnight City.mp3",
            duration = 243000L
        ),
        Song(
            id = "song_2",
            title = "Starboy",
            artist = "The Weeknd",
            album = "Starboy",
            path = "/home/user/Music/The Weeknd/Starboy.flac",
            duration = 230000L
        )
    )

    private val testPlaylist = Playlist(
        id = "pl_100",
        name = "Synthwave & Pop",
        songIds = listOf("song_1", "song_2")
    )

    @Test
    fun testGenerateAndParseExactContent() {
        val targetFile = File("/home/user/Music/Playlists/favorite.m3u")
        val content = M3uManager.generateContent(testPlaylist, songs, targetFile)

        assertTrue(content.startsWith("#EXTM3U\n#PIXELPLAYER-PLAYLIST-ID:pl_100\n"))
        assertTrue(content.contains("#EXTINF:243,M83 - Midnight City"))
        assertTrue(content.contains("../M83/Midnight City.mp3"))

        val parsed = M3uManager.parseContent(content, songs, targetFile.parentFile)
        assertEquals("pl_100", parsed.playlistId)
        assertEquals(listOf("song_1", "song_2"), parsed.songIds)
        assertTrue(parsed.unresolvedEntries.isEmpty())
        assertTrue(parsed.ambiguousEntries.isEmpty())
    }

    @Test
    fun testParseCrossPlatformBasenameMatch() {
        val externalM3uContent = """
            #EXTM3U
            #EXTINF:243,M83 - Midnight City
            C:\Users\John\Music\M83\Midnight City.mp3
            #EXTINF:230,The Weeknd - Starboy
            /storage/emulated/0/Music/Starboy.flac
        """.trimIndent()

        val parsed = M3uManager.parseContent(externalM3uContent, songs)
        assertEquals(listOf("song_1", "song_2"), parsed.songIds)
        assertTrue(parsed.unresolvedEntries.isEmpty())
    }

    @Test
    fun testParseWithUtf8BomAndEmptyLines() {
        val content = "\uFEFF#EXTM3U\n\n#EXTINF:243,M83 - Midnight City\n/home/user/Music/M83/Midnight City.mp3\n\n"
        val parsed = M3uManager.parseContent(content, songs)
        assertEquals(listOf("song_1"), parsed.songIds)
    }

    @Test
    fun testDisambiguationByMetadata() {
        val duplicateBasenameSongs = listOf(
            Song(
                id = "track_a",
                title = "Intro",
                artist = "Artist A",
                path = "/music/a/Intro.mp3",
                duration = 60000L
            ),
            Song(
                id = "track_b",
                title = "Intro",
                artist = "Artist B",
                path = "/music/b/Intro.mp3",
                duration = 120000L
            )
        )

        val content = """
            #EXTM3U
            #EXTINF:120,Artist B - Intro
            /other_path/Intro.mp3
        """.trimIndent()

        val parsed = M3uManager.parseContent(content, duplicateBasenameSongs)
        assertEquals(listOf("track_b"), parsed.songIds)
    }

    @Test
    fun testParseWithMalformedOrUrlEntriesDoesNotCrash() {
        val content = """
            #EXTM3U
            #EXTINF:243,M83 - Midnight City
            http://stream.example.com/live.m3u8
            #EXTINF:230,The Weeknd - Starboy
            /home/user/Music/The Weeknd/Starboy.flac
        """.trimIndent()

        val parsed = M3uManager.parseContent(content, songs, baseDirectory = File("/home/user/Music/Playlists"))
        assertEquals(listOf("song_1", "song_2"), parsed.songIds)
    }

    @Test
    fun testFallbackFindByMetadataWhenPathDoesNotMatch() {
        val content = """
            #EXTM3U
            #EXTINF:243,M83 - Midnight City
            /completely/unknown/device/path/audio.mp3
        """.trimIndent()

        val parsed = M3uManager.parseContent(content, songs)
        assertEquals(listOf("song_1"), parsed.songIds)
        assertTrue(parsed.unresolvedEntries.isEmpty())
    }

    @Test
    fun testParseWithFileUriAndPercentEncoding() {
        val content = """
            #EXTM3U
            file:///home/user/Music/M83/Midnight%20City.mp3
            file://localhost/home/user/Music/The%20Weeknd/Starboy.flac
        """.trimIndent()

        val parsed = M3uManager.parseContent(content, songs)
        assertEquals(listOf("song_1", "song_2"), parsed.songIds)
        assertTrue(parsed.unresolvedEntries.isEmpty())
    }

    @Test
    fun testReadM3uTextBoundedLimit() {
        val normalText = "Valid playlist line"
        val read = readM3uTextBounded(StringReader(normalText), maxCharacters = 100)
        assertEquals(normalText, read)

        assertFailsWith<IOException> {
            readM3uTextBounded(StringReader("1234567890"), maxCharacters = 5)
        }
    }
}
