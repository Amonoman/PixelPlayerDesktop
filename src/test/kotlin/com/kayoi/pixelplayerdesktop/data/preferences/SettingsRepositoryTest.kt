package com.kayoi.pixelplayerdesktop.data.preferences

import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsRepositoryTest {

    @Test
    fun testSettingsLoadAndSave() = runTest {
        val tempFile = File.createTempFile("test_settings", ".json")
        tempFile.deleteOnExit()

        val repository = SettingsRepositoryImpl(settingsFile = tempFile)

        // Initial check
        val initial = repository.getSettings()
        assertTrue(initial.isDynamicColorEnabled)

        // Update settings
        repository.setDynamicColoringEnabled(false)
        repository.setVolume(0.75f)
        repository.addMusicDirectory("/test/music")
        repository.setAlbumsListView(true)
        repository.setArtistsListView(true)
        repository.setFoldersPlaylistView(true)
        repository.setLyricsAlignment("center")
        repository.setLyricsSyncOffset("song_123", 500)
        repository.setLyricsSyncOffset("song_456", -300)

        val updated = repository.getSettings()
        assertEquals(false, updated.isDynamicColorEnabled)
        assertEquals(0.75f, updated.volume)
        assertTrue(updated.musicDirectories.contains("/test/music"))
        assertEquals(true, updated.isAlbumsListView)
        assertEquals(true, updated.isArtistsListView)
        assertEquals(true, updated.isFoldersPlaylistView)
        assertEquals("center", updated.lyricsAlignment)
        assertEquals(500, updated.lyricsSyncOffsets["song_123"])
        assertEquals(-300, updated.lyricsSyncOffsets["song_456"])

        // Reset offset to 0 should remove the entry
        repository.setLyricsSyncOffset("song_123", 0)
        val resetOffsets = repository.getSettings().lyricsSyncOffsets
        assertEquals(null, resetOffsets["song_123"])
        assertEquals(-300, resetOffsets["song_456"])

        // Create new repository instance from the same file to verify persistence
        val reloaded = SettingsRepositoryImpl(settingsFile = tempFile)
        val reloadedSettings = reloaded.getSettings()
        assertEquals(false, reloadedSettings.isDynamicColorEnabled)
        assertEquals(0.75f, reloadedSettings.volume)
        assertTrue(reloadedSettings.musicDirectories.contains("/test/music"))
        assertEquals(true, reloadedSettings.isAlbumsListView)
        assertEquals(true, reloadedSettings.isArtistsListView)
        assertEquals(true, reloadedSettings.isFoldersPlaylistView)
        assertEquals("center", reloadedSettings.lyricsAlignment)
        assertEquals(-300, reloadedSettings.lyricsSyncOffsets["song_456"])
    }
}
