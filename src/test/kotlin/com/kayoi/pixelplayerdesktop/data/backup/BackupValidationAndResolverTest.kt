package com.kayoi.pixelplayerdesktop.data.backup

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupModuleInfo
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSongMetadata
import com.kayoi.pixelplayerdesktop.data.backup.module.BackupSongResolver
import com.kayoi.pixelplayerdesktop.data.backup.validation.ValidationPipeline
import com.kayoi.pixelplayerdesktop.data.model.Song
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class BackupValidationAndResolverTest {

    @Test
    fun `test BackupSongResolver library resolution logic`() {
        val song1 = Song(
            id = "desktop-uuid-1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            duration = 354000L
        )
        val song2 = Song(
            id = "desktop-uuid-2",
            title = "Hotel California",
            artist = "Eagles",
            album = "Hotel California",
            duration = 391000L
        )
        val library = BackupSongResolver.Library(listOf(song1, song2))

        // Direct ID match
        val directMatch = library.resolve("desktop-uuid-1", BackupSongMetadata("Bohemian Rhapsody", "Queen"))
        assertEquals("desktop-uuid-1", directMatch)

        // Android MediaStore ID mapped to desktop UUID by Title + Artist metadata
        val metadataMatch = library.resolve("100234", BackupSongMetadata("Hotel California", "Eagles", "Hotel California", 391000L))
        assertEquals("desktop-uuid-2", metadataMatch)

        // Case-insensitive title + artist matching
        val caseInsensitiveMatch = library.resolve("9999", BackupSongMetadata("bohemian rhapsody", "QUEEN"))
        assertEquals("desktop-uuid-1", caseInsensitiveMatch)

        // Unknown song returns null
        val notFound = library.resolve("8888", BackupSongMetadata("Unknown Track", "Unknown Artist"))
        assertNull(notFound)
    }

    @Test
    fun `test ValidationPipeline checksum and schema validation`() {
        val pipeline = ValidationPipeline()
        val tempDir = Files.createTempDirectory("backup_val_test").toFile()
        try {
            val validPayload = """[{"songId":"1","timestamp":1000}]"""
            val manifest = BackupManifest(
                schemaVersion = 4,
                modules = mapOf(
                    "favorites" to BackupModuleInfo(
                        checksum = "sha256:d8a29b3c588e14e1fb59c1e7a5d3fef1c4722883f3d79048386ae6ee7f174e92", // wrong checksum on purpose
                        entryCount = 1,
                        sizeBytes = validPayload.length.toLong()
                    )
                )
            )

            // 1. Checksum mismatch should produce an Invalid result
            val checksumResult = pipeline.validateModulePayload(
                section = BackupSection.FAVORITES,
                payload = validPayload,
                manifest = manifest
            )
            assertFalse(checksumResult.isValid())

            // 2. Correct checksum
            val actualChecksum = java.security.MessageDigest.getInstance("SHA-256")
                .digest(validPayload.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val validManifest = manifest.copy(
                modules = mapOf(
                    "favorites" to BackupModuleInfo(
                        checksum = "sha256:$actualChecksum",
                        entryCount = 1,
                        sizeBytes = validPayload.length.toLong()
                    )
                )
            )
            val validResult = pipeline.validateModulePayload(
                section = BackupSection.FAVORITES,
                payload = validPayload,
                manifest = validManifest
            )
            assertTrue(validResult.isValid())

            // 3. Malformed JSON payload
            val malformedResult = pipeline.validateModulePayload(
                section = BackupSection.FAVORITES,
                payload = """[{"songId":"""
            )
            assertFalse(malformedResult.isValid())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
