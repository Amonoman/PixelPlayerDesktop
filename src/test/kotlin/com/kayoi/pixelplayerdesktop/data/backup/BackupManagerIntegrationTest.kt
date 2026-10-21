package com.kayoi.pixelplayerdesktop.data.backup

import com.kayoi.pixelplayerdesktop.data.backup.format.BackupFormatDetector
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupReader
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupWriter
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.RestoreResult
import com.kayoi.pixelplayerdesktop.data.backup.module.*
import com.kayoi.pixelplayerdesktop.data.backup.restore.RestoreExecutor
import com.kayoi.pixelplayerdesktop.data.backup.restore.RestorePlanner
import com.kayoi.pixelplayerdesktop.data.backup.validation.BackupFileValidator
import com.kayoi.pixelplayerdesktop.data.backup.validation.ManifestValidator
import com.kayoi.pixelplayerdesktop.data.backup.validation.ModuleSchemaValidator
import com.kayoi.pixelplayerdesktop.data.backup.validation.ValidationPipeline
import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
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

class BackupManagerIntegrationTest {

    private lateinit var tempDir: File
    private lateinit var dbFile: File
    private lateinit var database: PixelPlayerDatabase
    private lateinit var favoritesDao: FavoritesDao
    private lateinit var localPlaylistDao: LocalPlaylistDao
    private lateinit var musicDao: MusicDao
    private lateinit var backupManager: BackupManager
    private lateinit var mockSettingsRepo: MockSettingsRepository

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("backup_mgr_test").toFile()
        dbFile = File(tempDir, "test.db")
        database = PixelPlayerDatabase(dbFile)
        favoritesDao = FavoritesDao(database)
        localPlaylistDao = LocalPlaylistDao(database)
        musicDao = MusicDao(database)
        mockSettingsRepo = MockSettingsRepository()

        val formatDetector = BackupFormatDetector()
        val writer = BackupWriter()
        val reader = BackupReader(formatDetector)
        val songResolver = BackupSongResolver(musicDao)
        val fileValidator = BackupFileValidator(formatDetector)
        val manifestValidator = ManifestValidator()
        val moduleSchemaValidator = ModuleSchemaValidator()
        val pipeline = ValidationPipeline(fileValidator, manifestValidator, moduleSchemaValidator)
        val planner = RestorePlanner(reader)

        val handlers: Map<BackupSection, BackupModuleHandler> = mapOf(
            BackupSection.FAVORITES to FavoritesModuleHandler(favoritesDao, database, songResolver),
            BackupSection.PLAYLISTS to PlaylistsModuleHandler(localPlaylistDao, database, songResolver),
            BackupSection.GLOBAL_SETTINGS to GlobalSettingsModuleHandler(mockSettingsRepo),
            BackupSection.EQUALIZER to EqualizerModuleHandler(mockSettingsRepo)
        )

        val executor = RestoreExecutor(reader, pipeline, handlers)

        backupManager = BackupManager(
            backupWriter = writer,
            backupReader = reader,
            restorePlanner = planner,
            restoreExecutor = executor,
            validationPipeline = pipeline,
            handlers = handlers
        )
    }

    @AfterTest
    fun cleanup() {
        database.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun `test full backup export inspection and restore flow`() = runBlocking {
        // 1. Seed initial data
        val song = Song(id = "s-1", title = "Stairway to Heaven", artist = "Led Zeppelin")
        musicDao.upsertSongs(listOf(song))
        favoritesDao.addFavorite("s-1")
        localPlaylistDao.insertPlaylist(Playlist(id = "pl-rock", name = "Classic Rock", songIds = listOf("s-1")))
        mockSettingsRepo.updateSettings { it.copy(volume = 0.85f, isDarkMode = false) }

        // 2. Export backup
        val archiveFile = File(tempDir, "my_backup.pxpl")
        val exportResult = backupManager.export(
            targetFile = archiveFile,
            sections = setOf(BackupSection.FAVORITES, BackupSection.PLAYLISTS, BackupSection.GLOBAL_SETTINGS)
        )
        assertTrue(exportResult.isSuccess)
        assertTrue(archiveFile.exists())

        // 3. Mutate current data
        favoritesDao.removeFavorite("s-1")
        localPlaylistDao.deletePlaylist("pl-rock")
        mockSettingsRepo.updateSettings { it.copy(volume = 0.2f, isDarkMode = true) }

        assertFalse(favoritesDao.isFavorite("s-1"))
        assertNull(localPlaylistDao.getPlaylistById("pl-rock"))
        assertEquals(0.2f, mockSettingsRepo.getSettings().volume)

        // 4. Inspect backup
        val inspectResult = backupManager.inspectBackup(archiveFile)
        assertTrue(inspectResult.isSuccess)
        val plan = inspectResult.getOrThrow()
        assertEquals(3, plan.availableModules.size)
        assertTrue(plan.availableModules.contains(BackupSection.FAVORITES))
        assertTrue(plan.availableModules.contains(BackupSection.PLAYLISTS))
        assertTrue(plan.availableModules.contains(BackupSection.GLOBAL_SETTINGS))

        // 5. Restore backup
        val restoreResult = backupManager.restore(archiveFile, plan)
        assertTrue(restoreResult is RestoreResult.Success)

        // 6. Verify restored state
        assertTrue(favoritesDao.isFavorite("s-1"))
        val restoredPl = localPlaylistDao.getPlaylistById("pl-rock")
        assertNotNull(restoredPl)
        assertEquals("Classic Rock", restoredPl.name)
        assertEquals(0.85f, mockSettingsRepo.getSettings().volume)
        assertFalse(mockSettingsRepo.getSettings().isDarkMode)
    }

    private class MockSettingsRepository(
        private val delegate: SettingsRepository = SettingsRepositoryImpl(
            settingsFile = File.createTempFile("mock_settings", ".json").apply { deleteOnExit() }
        )
    ) : SettingsRepository by delegate
}
