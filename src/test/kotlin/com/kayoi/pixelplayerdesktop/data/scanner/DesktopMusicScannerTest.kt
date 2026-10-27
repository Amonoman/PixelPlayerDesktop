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

    @Test
    fun testScannerHandlesInaccessibleOrMalformedDirectories() = runTest {
        val tempDb = File.createTempFile("test_scanner_error_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val tempMusicDir = File(System.getProperty("java.io.tmpdir"), "test_music_valid_${System.currentTimeMillis()}")
        tempMusicDir.mkdirs()
        tempMusicDir.deleteOnExit()

        val validSong = File(tempMusicDir, "valid_song.mp3")
        validSong.writeBytes(byteArrayOf(0, 1, 2, 3))
        validSong.deleteOnExit()

        val nonExistentDir = "Z:\\NonExistent\\Path\\DoesNotExist"

        val scanner = DesktopMusicScanner(
            musicDao = musicDao,
            artworkCacheDir = tempMusicDir
        )

        // Scan valid dir alongside non-existent/invalid dir
        val result = scanner.scanDirectories(listOf(tempMusicDir.absolutePath, nonExistentDir))
        assertTrue(result is ScanProgress.Completed)
        assertEquals(1, result.totalScanned)
        assertEquals(1, result.addedOrUpdated)

        val songs = musicDao.getAllSongs()
        assertEquals(1, songs.size)
    }

    @Test
    fun testScanAudioFileWithMismatchedContainerExtension() = runTest {
        val tempDb = File.createTempFile("test_diag_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val tempMusicDir = File(System.getProperty("java.io.tmpdir"), "test_mismatched_${System.currentTimeMillis()}")
        tempMusicDir.mkdirs()
        tempMusicDir.deleteOnExit()

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

        val target = File(tempMusicDir, "mismatched.mp3")
        target.deleteOnExit()
        target.writeBytes(buffer.array())

        val scanner = DesktopMusicScanner(
            musicDao = musicDao,
            artworkCacheDir = tempMusicDir
        )

        val res = scanner.scanDirectories(listOf(tempMusicDir.absolutePath))
        assertTrue(res is ScanProgress.Completed)
        val songs = musicDao.getAllSongs(minDurationMs = 10000L)
        val song = songs.find { it.path == target.absolutePath }
        assertTrue(song != null, "Song should be indexed and pass minDuration filter")
        assertTrue(song.duration >= 12000L, "Song duration should be at least 12000ms, but was ${song.duration}")
    }

    @Test
    fun testPruningDoesNotDeleteSongsFromUnmountedOrUnscannedDirectories() = runTest {
        val tempDb = File.createTempFile("test_prune_scope_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val dirA = File(System.getProperty("java.io.tmpdir"), "test_prune_a_${System.currentTimeMillis()}")
        dirA.mkdirs()
        dirA.deleteOnExit()

        val dirB = File(System.getProperty("java.io.tmpdir"), "test_prune_b_${System.currentTimeMillis()}")
        dirB.mkdirs()
        dirB.deleteOnExit()

        val songA = File(dirA, "songA.mp3")
        songA.writeBytes(byteArrayOf(1, 2, 3, 4))
        songA.deleteOnExit()

        val songB = File(dirB, "songB.mp3")
        songB.writeBytes(byteArrayOf(5, 6, 7, 8))
        songB.deleteOnExit()

        val scanner = DesktopMusicScanner(
            musicDao = musicDao,
            artworkCacheDir = dirA
        )

        val initialScan = scanner.scanDirectories(listOf(dirA.absolutePath, dirB.absolutePath))
        assertTrue(initialScan is ScanProgress.Completed)
        assertEquals(2, musicDao.getAllSongs().size)

        val nonExistentMount = "Z:\\Unmounted\\Drive\\Music"
        val singleFolderScan = scanner.scanDirectories(
            directories = listOf(dirA.absolutePath, nonExistentMount),
            pruneMissing = true
        )
        assertTrue(singleFolderScan is ScanProgress.Completed)

        val songsAfterScan = musicDao.getAllSongs()
        assertEquals(2, songsAfterScan.size)
        assertTrue(songsAfterScan.any { it.path == songA.absolutePath })
        assertTrue(songsAfterScan.any { it.path == songB.absolutePath })
    }

    @Test
    fun testPruningWhenAllDirectoriesAreUnreachableDoesNotWipeLibrary() = runTest {
        val tempDb = File.createTempFile("test_prune_unreachable_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val dirA = File(System.getProperty("java.io.tmpdir"), "test_prune_unreach_${System.currentTimeMillis()}")
        dirA.mkdirs()
        dirA.deleteOnExit()

        val songA = File(dirA, "songA.mp3")
        songA.writeBytes(byteArrayOf(1, 2, 3, 4))
        songA.deleteOnExit()

        val scanner = DesktopMusicScanner(
            musicDao = musicDao,
            artworkCacheDir = dirA
        )

        val initialScan = scanner.scanDirectories(listOf(dirA.absolutePath))
        assertTrue(initialScan is ScanProgress.Completed)
        assertEquals(1, musicDao.getAllSongs().size)

        val unreachableDirs = listOf(
            "Z:\\Disconnected\\ExternalDrive",
            "/media/unmounted/share/music"
        )
        val scanResult = scanner.scanDirectories(
            directories = unreachableDirs,
            pruneMissing = true
        )
        assertTrue(scanResult is ScanProgress.Completed)

        val songsAfterScan = musicDao.getAllSongs()
        assertEquals(1, songsAfterScan.size)
        assertEquals(songA.absolutePath, songsAfterScan[0].path)
    }
}
