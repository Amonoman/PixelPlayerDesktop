package com.kayoi.pixelplayerdesktop.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DirectoryFilterUtilsTest {

    @Test
    fun `returns false when excluded list is empty`() {
        val path = File("/home/user/music/song.mp3").absolutePath
        assertFalse(isDirectoryOrParentExcluded(path, emptyList()))
    }

    @Test
    fun `returns true when path is exact excluded directory`() {
        val excludedDir = File("/home/user/music/blocked").absolutePath
        assertTrue(isDirectoryOrParentExcluded(excludedDir, listOf(excludedDir)))
    }

    @Test
    fun `returns true when file is inside excluded directory`() {
        val excludedDir = File("/home/user/music/blocked").absolutePath
        val songFile = File(excludedDir, "track.mp3").absolutePath
        assertTrue(isDirectoryOrParentExcluded(songFile, listOf(excludedDir)))
    }

    @Test
    fun `returns true when file is inside nested subdirectory of excluded directory`() {
        val excludedDir = File("/home/user/music/blocked").absolutePath
        val nestedSong = File(File(excludedDir, "sub"), "track.mp3").absolutePath
        assertTrue(isDirectoryOrParentExcluded(nestedSong, listOf(excludedDir)))
    }

    @Test
    fun `returns false when file is in sibling directory with similar prefix`() {
        val excludedDir = File("/home/user/music/blocked").absolutePath
        val siblingDirSong = File("/home/user/music/blocked-allowed/song.mp3").absolutePath
        assertFalse(isDirectoryOrParentExcluded(siblingDirSong, listOf(excludedDir)))
    }

    @Test
    fun `returns false when file is outside excluded directory`() {
        val excludedDir = File("/home/user/music/blocked").absolutePath
        val allowedSong = File("/home/user/music/allowed/song.mp3").absolutePath
        assertFalse(isDirectoryOrParentExcluded(allowedSong, listOf(excludedDir)))
    }

    @Test
    fun `isPathExcluded works with pre-normalized prefixes`() {
        val prefixes = DirectoryFilterUtils.normalizePrefixes(listOf(File("/home/user/music/blocked").absolutePath))
        val excludedSong = File("/home/user/music/blocked/nested/song.mp3").absolutePath
        val allowedSong = File("/home/user/music/allowed/song.mp3").absolutePath

        assertTrue(DirectoryFilterUtils.isPathExcluded(excludedSong, prefixes))
        assertFalse(DirectoryFilterUtils.isPathExcluded(allowedSong, prefixes))
    }
}
