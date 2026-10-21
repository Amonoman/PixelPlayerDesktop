package com.kayoi.pixelplayerdesktop.data.backup

import com.kayoi.pixelplayerdesktop.data.backup.module.*
import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class ModuleHandlersTest {

    private lateinit var tempDir: File
    private lateinit var dbFile: File
    private lateinit var database: PixelPlayerDatabase
    private lateinit var favoritesDao: FavoritesDao
    private lateinit var localPlaylistDao: LocalPlaylistDao
    private lateinit var musicDao: MusicDao
    private lateinit var songResolver: BackupSongResolver

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("backup_handlers_test").toFile()
        dbFile = File(tempDir, "test.db")
        database = PixelPlayerDatabase(dbFile)
        favoritesDao = FavoritesDao(database)
        localPlaylistDao = LocalPlaylistDao(database)
        musicDao = MusicDao(database)
        songResolver = BackupSongResolver(musicDao)
    }

    @AfterTest
    fun cleanup() {
        database.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun `test FavoritesModuleHandler export restore and rollback`() = runBlocking {
        // Seed songs
        val song1 = Song(id = "song-1", title = "Track One", artist = "Artist A")
        val song2 = Song(id = "song-2", title = "Track Two", artist = "Artist B")
        musicDao.upsertSongs(listOf(song1, song2))

        favoritesDao.addFavorite("song-1")
        val handler = FavoritesModuleHandler(favoritesDao, database, songResolver)

        assertEquals(1, handler.countEntries())
        val exportedJson = handler.export()
        assertTrue(exportedJson.contains("song-1"))

        // Add second favorite
        favoritesDao.addFavorite("song-2")
        val snapshot = handler.snapshot()
        assertEquals(2, handler.countEntries())

        // Restore back to single favorite
        handler.restore(exportedJson)
        assertEquals(1, handler.countEntries())
        assertTrue(favoritesDao.isFavorite("song-1"))
        assertFalse(favoritesDao.isFavorite("song-2"))

        // Rollback back to snapshot
        handler.rollback(snapshot)
        assertEquals(2, handler.countEntries())
        assertTrue(favoritesDao.isFavorite("song-1"))
        assertTrue(favoritesDao.isFavorite("song-2"))
    }

    @Test
    fun `test PlaylistsModuleHandler export and restore with song resolution`() = runBlocking {
        val song1 = Song(id = "desktop-uuid-1", title = "My Song", artist = "My Artist")
        musicDao.upsertSongs(listOf(song1))

        val playlist = Playlist(
            id = "pl-1",
            name = "Test Playlist",
            songIds = listOf("desktop-uuid-1")
        )
        localPlaylistDao.insertPlaylist(playlist)

        val handler = PlaylistsModuleHandler(localPlaylistDao, database, songResolver)
        val exported = handler.export()
        assertTrue(exported.contains("Test Playlist"))
        assertTrue(exported.contains("My Song"))

        // Delete playlist
        localPlaylistDao.deletePlaylist("pl-1")
        assertEquals(0, handler.countEntries())

        // Restore playlist
        handler.restore(exported)
        val restored = localPlaylistDao.getPlaylistById("pl-1")
        assertNotNull(restored)
        assertEquals("Test Playlist", restored.name)
        assertEquals(listOf("desktop-uuid-1"), restored.songIds)
    }

    @Test
    fun `test EqualizerModuleHandler export and restore`() = runBlocking {
        val mockSettingsRepo = MockSettingsRepository()
        val handler = EqualizerModuleHandler(mockSettingsRepo)

        val customPreset = EqualizerPreset(
            name = "Bass Boost Custom",
            displayName = "Bass Boost Custom",
            bandLevels = listOf(4.0f, 3.0f, 2.0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            preamp = 2.5f,
            isCustom = true
        )
        mockSettingsRepo.updateSettings {
            it.copy(
                customEqualizerPresets = listOf(customPreset),
                equalizerEnabled = true
            )
        }

        val exported = handler.export()
        assertTrue(exported.contains("Bass Boost Custom"))

        // Reset settings
        mockSettingsRepo.updateSettings { UserSettings() }
        assertFalse(mockSettingsRepo.getSettings().equalizerEnabled)
        assertEquals(0, mockSettingsRepo.getSettings().customEqualizerPresets.size)

        // Restore
        handler.restore(exported)
        assertTrue(mockSettingsRepo.getSettings().equalizerEnabled)
        assertEquals(1, mockSettingsRepo.getSettings().customEqualizerPresets.size)
        assertEquals("Bass Boost Custom", mockSettingsRepo.getSettings().customEqualizerPresets[0].name)
    }

    private class MockSettingsRepository(
        private val delegate: SettingsRepository = SettingsRepositoryImpl(
            settingsFile = File.createTempFile("mock_settings", ".json").apply { deleteOnExit() }
        )
    ) : SettingsRepository by delegate
}
