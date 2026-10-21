package com.kayoi.pixelplayerdesktop.data.backup

import com.kayoi.pixelplayerdesktop.data.backup.format.BackupEncryptedException
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupFormatDetector
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupReader
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupWriter
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import com.kayoi.pixelplayerdesktop.data.backup.model.DeviceInfo
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class BackupFormatTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("pixelplayer_backup_test").toFile()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test unencrypted backup export and read`() = runBlocking {
        val writer = BackupWriter()
        val reader = BackupReader()
        val backupFile = File(tempDir, "test_backup.pxpl")

        val manifest = BackupManifest(
            appVersion = "1.0.0",
            deviceInfo = DeviceInfo(manufacturer = "Desktop", model = "Linux")
        )
        val payloads = mapOf(
            "favorites" to """[{"song_id":"123","date_added":1700000000}]""",
            "lyrics" to """[{"song_id":"123","plain_lyrics":"Hello World"}]"""
        )

        val writeResult = writer.write(backupFile, manifest, payloads)
        assertTrue(writeResult.isSuccess)
        assertTrue(backupFile.exists())

        val format = reader.detectFormat(backupFile).getOrThrow()
        assertEquals(BackupFormatDetector.Format.PXPL_V3_ZIP, format)

        val readManifest = reader.readManifest(backupFile).getOrThrow()
        assertEquals("1.0.0", readManifest.appVersion)
        assertEquals(2, readManifest.modules.size)
        assertTrue(readManifest.modules.containsKey("favorites"))
        assertTrue(readManifest.modules.containsKey("lyrics"))

        val favoritesPayload = reader.readModulePayload(backupFile, "favorites").getOrThrow()
        assertEquals(payloads["favorites"], favoritesPayload)

        val allPayloads = reader.readAllModulePayloads(backupFile).getOrThrow()
        assertEquals(2, allPayloads.size)
        assertEquals(payloads["lyrics"], allPayloads["lyrics"])
    }

    @Test
    fun `test encrypted backup export and decryption`() = runBlocking {
        val writer = BackupWriter()
        val reader = BackupReader()
        val backupFile = File(tempDir, "test_encrypted.pxpl")
        val password = "StrongPassword123!"

        val manifest = BackupManifest(appVersion = "1.0.0")
        val payloads = mapOf("playlists" to """[{"id":"p1","name":"Chill"}]""")

        val writeResult = writer.write(backupFile, manifest, payloads, passphrase = password)
        assertTrue(writeResult.isSuccess)

        val format = reader.detectFormat(backupFile).getOrThrow()
        assertEquals(BackupFormatDetector.Format.PXPL_V4_ENCRYPTED, format)

        assertFailsWith<BackupEncryptedException> {
            reader.readManifest(backupFile, passphrase = null).getOrThrow()
        }

        assertFails {
            reader.readManifest(backupFile, passphrase = "WrongPassword").getOrThrow()
        }

        val decryptedManifest = reader.readManifest(backupFile, passphrase = password).getOrThrow()
        assertEquals("1.0.0", decryptedManifest.appVersion)

        val playlistPayload = reader.readModulePayload(backupFile, "playlists", passphrase = password).getOrThrow()
        assertEquals(payloads["playlists"], playlistPayload)
    }
}
