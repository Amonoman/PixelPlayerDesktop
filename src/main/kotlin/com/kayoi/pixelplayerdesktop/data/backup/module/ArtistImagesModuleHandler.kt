package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64

@Serializable
data class ArtistImageBackupEntry(
    val artistName: String,
    val imageUrl: String = "",
    val customImageBase64: String? = null
)

class ArtistImagesModuleHandler(
    private val database: PixelPlayerDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.ARTIST_IMAGES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArtistImageBackupEntry>()
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT name, image_url, custom_image_uri FROM artists WHERE (image_url IS NOT NULL AND image_url != '') OR (custom_image_uri IS NOT NULL AND custom_image_uri != '')"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("name")
                        val imgUrl = rs.getString("image_url") ?: ""
                        val customUri = rs.getString("custom_image_uri")
                        val base64 = customUri?.let { readFileAsBase64(it) }

                        if (imgUrl.isNotBlank() || base64 != null) {
                            entries.add(
                                ArtistImageBackupEntry(
                                    artistName = name,
                                    imageUrl = imgUrl,
                                    customImageBase64 = base64
                                )
                            )
                        }
                    }
                }
            }
        }
        json.encodeToString(ListSerializer(ArtistImageBackupEntry.serializer()), entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                val sql = "SELECT COUNT(*) FROM artists WHERE (image_url IS NOT NULL AND image_url != '') OR (custom_image_uri IS NOT NULL AND custom_image_uri != '')"
                stmt.executeQuery(sql).use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(ArtistImageBackupEntry.serializer()), payload)
        database.withTransaction { conn ->
            val findArtistStmt = conn.prepareStatement("SELECT id FROM artists WHERE name COLLATE NOCASE = ?")
            val updateStmt = conn.prepareStatement("UPDATE artists SET image_url = ?, custom_image_uri = ? WHERE id = ?")

            for (entry in entries) {
                findArtistStmt.setString(1, entry.artistName)
                val artistId = findArtistStmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong("id") else null
                } ?: continue

                var customPath: String? = null
                if (!entry.customImageBase64.isNullOrBlank()) {
                    try {
                        val file = File(DesktopPathProvider.artworkCacheDir, "artist_art_${artistId}.jpg")
                        file.writeBytes(Base64.getDecoder().decode(entry.customImageBase64))
                        customPath = file.absolutePath
                    } catch (_: Exception) {}
                }

                updateStmt.setString(1, entry.imageUrl)
                updateStmt.setString(2, customPath)
                updateStmt.setLong(3, artistId)
                updateStmt.executeUpdate()
            }
        }
        database.notifyTableChanged("artists")
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        restore(snapshot)
    }

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) null
            else Base64.getEncoder().encodeToString(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }
}
