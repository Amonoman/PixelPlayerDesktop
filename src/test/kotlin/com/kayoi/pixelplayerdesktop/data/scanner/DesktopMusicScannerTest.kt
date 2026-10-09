package com.kayoi.pixelplayerdesktop.data.scanner

import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMusicScannerTest {

    @Test
    fun testScannerDiscoveryAndIncrementalScan() = runTest {
        val tempDb = File.createTempFile("test_scanner_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val tempMusicDir = File(System.getProperty("java.io.tmpdir"), "test_music_${System.currentTimeMillis()}")
        tempMusicDir.mkdirs()
        tempMusicDir.deleteOnExit()

        val tempArtDir = File(System.getProperty("java.io.tmpdir"), "test_art_${System.currentTimeMillis()}")
        tempArtDir.mkdirs()
        tempArtDir.deleteOnExit()

        val file1 = File(tempMusicDir, "song1.mp3")
        file1.writeBytes(byteArrayOf(0, 1, 2, 3))
        file1.deleteOnExit()

        val subDir = File(tempMusicDir, "SubFolder")
        subDir.mkdirs()
        subDir.deleteOnExit()

        val file2 = File(subDir, "song2.flac")
        file2.writeBytes(byteArrayOf(4, 5, 6, 7))
        file2.deleteOnExit()

        val nonAudioFile = File(tempMusicDir, "notes.txt")
        nonAudioFile.writeText("some notes")
        nonAudioFile.deleteOnExit()

        val scanner = DesktopMusicScanner(
            musicDao = musicDao,
            artworkCacheDir = tempArtDir
        )

        // 1. Initial scan
        val result1 = scanner.scanDirectories(listOf(tempMusicDir.absolutePath))
        assertTrue(result1 is ScanProgress.Completed)
        assertEquals(2, result1.totalScanned)
        assertEquals(2, result1.addedOrUpdated)

        val songsAfterScan1 = musicDao.getAllSongs()
        assertEquals(2, songsAfterScan1.size)

        // 2. Incremental scan (no files modified)
        val result2 = scanner.scanDirectories(listOf(tempMusicDir.absolutePath))
        assertTrue(result2 is ScanProgress.Completed)
        assertEquals(2, result2.totalScanned)
        assertEquals(0, result2.addedOrUpdated) // Skipped unchanged files

        // 3. Delete one file and rescan with pruning
        file2.delete()
        val result3 = scanner.scanDirectories(listOf(tempMusicDir.absolutePath), pruneMissing = true)
        assertTrue(result3 is ScanProgress.Completed)
        assertEquals(1, result3.totalScanned)

        val songsAfterPrune = musicDao.getAllSongs()
        assertEquals(1, songsAfterPrune.size)
        assertEquals("song1", songsAfterPrune[0].title)
    }
}
