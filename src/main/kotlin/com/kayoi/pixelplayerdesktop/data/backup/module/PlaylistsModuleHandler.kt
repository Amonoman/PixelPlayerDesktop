package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSongMetadata
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.sql.Types
import java.util.Base64

@Serializable
data class PlaylistsBackupPayload(
    val playlists: List<PlaylistBackupEntry> = emptyList(),
    val songMetadata: Map<String, BackupSongMetadata>? = null,
    val coverImages: Map<String, String>? = null
)

@Serializable
data class PlaylistBackupEntry(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isQueueGenerated: Boolean = false,
    val coverImageUri: String? = null,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null,
    val coverShapeType: String? = null,
    val coverShapeDetail1: Float? = null,
    val coverShapeDetail2: Float? = null,
    val coverShapeDetail3: Float? = null,
    val coverShapeDetail4: Float? = null,
    val source: String = "LOCAL"
)

class PlaylistsModuleHandler(
    private val localPlaylistDao: LocalPlaylistDao,
    private val database: PixelPlayerDatabase,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.PLAYLISTS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val playlists = localPlaylistDao.getAllPlaylists(SortOption.PlaylistNameAZ)
            .filter { it.source == "LOCAL" }

        val allSongIds = playlists.flatMap { it.songIds }.distinct()
        val metadata = songResolver.metadataFor(allSongIds)

        val coverImages = mutableMapOf<String, String>()
        for (pl in playlists) {
            val uri = pl.coverImageUri ?: continue
            val base64 = readFileAsBase64(uri)
            if (base64 != null) {
                coverImages[pl.id] = base64
            }
        }

        val entries = playlists.map { pl ->
            PlaylistBackupEntry(
                id = pl.id,
                name = pl.name,
                songIds = pl.songIds,
                createdAt = pl.createdAt,
                lastModified = pl.lastModified,
                isQueueGenerated = pl.isQueueGenerated,
                coverImageUri = pl.coverImageUri,
                coverColorArgb = pl.coverColorArgb,
                coverIconName = pl.coverIconName,
                coverShapeType = pl.coverShapeType,
                coverShapeDetail1 = pl.coverShapeDetail1,
                coverShapeDetail2 = pl.coverShapeDetail2,
                coverShapeDetail3 = pl.coverShapeDetail3,
                coverShapeDetail4 = pl.coverShapeDetail4,
                source = pl.source
            )
        }

        val payload = PlaylistsBackupPayload(
            playlists = entries,
            songMetadata = metadata.ifEmpty { null },
            coverImages = coverImages.ifEmpty { null }
        )
        json.encodeToString(PlaylistsBackupPayload.serializer(), payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        localPlaylistDao.getAllPlaylists().count { it.source == "LOCAL" }
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val playlists = localPlaylistDao.getAllPlaylists(SortOption.PlaylistNameAZ)
        val entries = playlists.map { pl ->
            PlaylistBackupEntry(
                id = pl.id,
                name = pl.name,
                songIds = pl.songIds,
                createdAt = pl.createdAt,
                lastModified = pl.lastModified,
                isQueueGenerated = pl.isQueueGenerated,
                coverImageUri = pl.coverImageUri,
                coverColorArgb = pl.coverColorArgb,
                coverIconName = pl.coverIconName,
                coverShapeType = pl.coverShapeType,
                coverShapeDetail1 = pl.coverShapeDetail1,
                coverShapeDetail2 = pl.coverShapeDetail2,
                coverShapeDetail3 = pl.coverShapeDetail3,
                coverShapeDetail4 = pl.coverShapeDetail4,
                source = pl.source
            )
        }
        json.encodeToString(ListSerializer(PlaylistBackupEntry.serializer()), entries)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val (backupPlaylists, songMetadata, coverImages) = if (trimmed.startsWith("[")) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(PlaylistBackupEntry.serializer()), trimmed)
            }.getOrElse {
                parseLegacyPlaylists(trimmed)
            }
            Triple(list, null, null)
        } else {
            val parsed = json.decodeFromString(PlaylistsBackupPayload.serializer(), trimmed)
            Triple(parsed.playlists, parsed.songMetadata, parsed.coverImages)
        }

        val library = songResolver.library()
        val resolvedPlaylists = backupPlaylists.map { pl ->
            val resolvedSongIds = pl.songIds.mapNotNull { origId ->
                val meta = songMetadata?.get(origId)
                library.resolve(origId, meta) ?: origId.takeIf { library.resolve(it, null) != null }
            }

            var restoredCoverUri = pl.coverImageUri
            if (coverImages != null && coverImages.containsKey(pl.id)) {
                val base64 = coverImages[pl.id]
                if (!base64.isNullOrBlank()) {
                    val file = File(DesktopPathProvider.artworkCacheDir, "playlist_cover_${pl.id}.jpg")
                    try {
                        file.writeBytes(Base64.getDecoder().decode(base64))
                        restoredCoverUri = file.absolutePath
                    } catch (_: Exception) {}
                }
            }

            pl.copy(songIds = resolvedSongIds, coverImageUri = restoredCoverUri)
        }

        insertPlaylistsTransaction(resolvedPlaylists)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val list = json.decodeFromString(ListSerializer(PlaylistBackupEntry.serializer()), snapshot)
        insertPlaylistsTransaction(list)
    }

    private fun insertPlaylistsTransaction(playlists: List<PlaylistBackupEntry>) {
        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM playlists WHERE source = 'LOCAL'")
            }

            if (playlists.isNotEmpty()) {
                val sql = """
                    INSERT INTO playlists (
                        id, name, song_ids_json, created_at, last_modified, is_queue_generated,
                        cover_image_uri, cover_color_argb, cover_icon_name, cover_shape_type,
                        cover_shape_detail1, cover_shape_detail2, cover_shape_detail3, cover_shape_detail4, source
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    for (pl in playlists) {
                        stmt.setString(1, pl.id)
                        stmt.setString(2, pl.name)
                        stmt.setString(3, json.encodeToString(ListSerializer(String.serializer()), pl.songIds))
                        stmt.setLong(4, pl.createdAt)
                        stmt.setLong(5, pl.lastModified)
                        stmt.setInt(6, if (pl.isQueueGenerated) 1 else 0)
                        stmt.setString(7, pl.coverImageUri)
                        if (pl.coverColorArgb != null) stmt.setInt(8, pl.coverColorArgb) else stmt.setNull(8, Types.INTEGER)
                        stmt.setString(9, pl.coverIconName)
                        stmt.setString(10, pl.coverShapeType)
                        if (pl.coverShapeDetail1 != null) stmt.setFloat(11, pl.coverShapeDetail1) else stmt.setNull(11, Types.REAL)
                        if (pl.coverShapeDetail2 != null) stmt.setFloat(12, pl.coverShapeDetail2) else stmt.setNull(12, Types.REAL)
                        if (pl.coverShapeDetail3 != null) stmt.setFloat(13, pl.coverShapeDetail3) else stmt.setNull(13, Types.REAL)
                        if (pl.coverShapeDetail4 != null) stmt.setFloat(14, pl.coverShapeDetail4) else stmt.setNull(14, Types.REAL)
                        stmt.setString(15, pl.source)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("playlists")
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

    private fun parseLegacyPlaylists(jsonStr: String): List<PlaylistBackupEntry> {
        val element = runCatching { json.parseToJsonElement(jsonStr) }.getOrNull() as? JsonArray ?: return emptyList()
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: "Playlist"
            val songIdsArray = obj["songIds"] as? JsonArray ?: obj["song_ids"] as? JsonArray
            val songIds = songIdsArray?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
            PlaylistBackupEntry(
                id = id,
                name = name,
                songIds = songIds,
                createdAt = (obj["createdAt"] as? JsonPrimitive)?.longOrNull ?: System.currentTimeMillis(),
                lastModified = (obj["lastModified"] as? JsonPrimitive)?.longOrNull ?: System.currentTimeMillis(),
                isQueueGenerated = (obj["isQueueGenerated"] as? JsonPrimitive)?.booleanOrNull ?: false
            )
        }
    }
}
