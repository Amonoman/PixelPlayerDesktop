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

        val updated = repository.getSettings()
        assertEquals(false, updated.isDynamicColorEnabled)
        assertEquals(0.75f, updated.volume)
        assertTrue(updated.musicDirectories.contains("/test/music"))
        assertEquals(true, updated.isAlbumsListView)
        assertEquals(true, updated.isArtistsListView)
        assertEquals(true, updated.isFoldersPlaylistView)

        // Create new repository instance from the same file to verify persistence
        val reloaded = SettingsRepositoryImpl(settingsFile = tempFile)
        val reloadedSettings = reloaded.getSettings()
        assertEquals(false, reloadedSettings.isDynamicColorEnabled)
        assertEquals(0.75f, reloadedSettings.volume)
        assertTrue(reloadedSettings.musicDirectories.contains("/test/music"))
        assertEquals(true, reloadedSettings.isAlbumsListView)
        assertEquals(true, reloadedSettings.isArtistsListView)
        assertEquals(true, reloadedSettings.isFoldersPlaylistView)
    }
}
