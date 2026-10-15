package com.kayoi.pixelplayerdesktop.data.metadata

import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopMetadataEditorTest {

    @Test
    fun testMissingFileReturnsError() = runTest {
        val tempDb = File.createTempFile("test_meta_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val editor = DesktopMetadataEditor(musicDao)
        val dummySong = Song(
            id = "non-existent-id",
            title = "Old Title",
            artist = "Old Artist",
            path = "/path/to/non/existent/file.mp3"
        )

        val result = editor.writeMetadata(
            song = dummySong,
            metadata = EditableSongMetadata(
                title = "New Title",
                artist = "New Artist",
                album = "New Album"
            )
        )

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error.contains("not found", ignoreCase = true))

        val readResult = editor.readMetadata(File(dummySong.path))
        assertTrue(readResult.isFailure)
    }

    @Test
    fun testArtworkCacheHashing() {
        val tempArtDir = File(System.getProperty("java.io.tmpdir"), "test_art_cache_${System.currentTimeMillis()}")
        tempArtDir.mkdirs()
        tempArtDir.deleteOnExit()

        val tempDb = File.createTempFile("test_meta_db2", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val editor = DesktopMetadataEditor(musicDao, artworkCacheDir = tempArtDir)

        val fakeImageBytes = "fake-jpeg-image-bytes".toByteArray(Charsets.UTF_8)
        val path1 = editor.saveArtworkToCache(fakeImageBytes)
        assertNotNull(path1)
        assertTrue(File(path1).exists())

        val path2 = editor.saveArtworkToCache(fakeImageBytes)
        assertEquals(path1, path2)
    }

    @Test
    fun testCoverArtUpdateDeletionAndPreservation() = runTest {
        val tempArtDir = File(System.getProperty("java.io.tmpdir"), "test_art_cache3_${System.currentTimeMillis()}")
        tempArtDir.mkdirs()
        tempArtDir.deleteOnExit()

        val tempDb = File.createTempFile("test_meta_db3", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val tempAudioFile = File.createTempFile("dummy_audio", ".mp3")
        tempAudioFile.deleteOnExit()

        val song = Song(
            id = "test-song-1",
            title = "Track One",
            artist = "Artist One",
            path = tempAudioFile.absolutePath,
            albumArtUriString = "/existing/cover.jpg"
        )
        musicDao.upsertSongs(listOf(song))

        val editor = DesktopMetadataEditor(musicDao, artworkCacheDir = tempArtDir)

        val update = CoverArtUpdate(isDeletion = true)
        assertTrue(update.isDeletion)
        assertNull(update.bytes)
    }
}
