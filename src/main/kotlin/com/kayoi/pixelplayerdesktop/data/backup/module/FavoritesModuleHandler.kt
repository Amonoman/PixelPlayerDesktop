package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSongMetadata
import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class FavoriteBackupEntry(
    val songId: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val song: BackupSongMetadata? = null
)

class FavoritesModuleHandler(
    private val favoritesDao: FavoritesDao,
    private val database: PixelPlayerDatabase,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.FAVORITES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT song_id, date_added FROM favorites ORDER BY date_added DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<Pair<String, Long>>()
                    while (rs.next()) {
                        list.add(rs.getString("song_id") to rs.getLong("date_added"))
                    }
                    list
                }
            }
        }

        val metadataMap = songResolver.metadataFor(entries.map { it.first })
        val backupEntries = entries.map { (songId, dateAdded) ->
            FavoriteBackupEntry(
                songId = songId,
                dateAdded = dateAdded,
                song = metadataMap[songId]
            )
        }

        json.encodeToString(ListSerializer(FavoriteBackupEntry.serializer()), backupEntries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM favorites").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val entries = database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT song_id, date_added FROM favorites"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<FavoriteBackupEntry>()
                    while (rs.next()) {
                        list.add(FavoriteBackupEntry(rs.getString("song_id"), rs.getLong("date_added")))
                    }
                    list
                }
            }
        }
        json.encodeToString(ListSerializer(FavoriteBackupEntry.serializer()), entries)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = runCatching {
            json.decodeFromString(ListSerializer(FavoriteBackupEntry.serializer()), payload)
        }.getOrElse {
            parseLegacyFavorites(payload)
        }
        val library = songResolver.library()

        val resolvedEntries = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            resolvedId to entry.dateAdded
        }.distinctBy { it.first }

        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM favorites")
                stmt.executeUpdate("UPDATE songs SET is_favorite = 0")
            }

            if (resolvedEntries.isNotEmpty()) {
                val insertSql = "INSERT OR REPLACE INTO favorites (song_id, date_added) VALUES (?, ?)"
                conn.prepareStatement(insertSql).use { stmt ->
                    for ((songId, dateAdded) in resolvedEntries) {
                        stmt.setString(1, songId)
                        stmt.setLong(2, dateAdded)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                val updateSongSql = "UPDATE songs SET is_favorite = 1 WHERE id = ?"
                conn.prepareStatement(updateSongSql).use { stmt ->
                    for ((songId, _) in resolvedEntries) {
                        stmt.setString(1, songId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }

        database.notifyTableChanged("favorites")
        database.notifyTableChanged("songs")
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(FavoriteBackupEntry.serializer()), snapshot)
        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM favorites")
                stmt.executeUpdate("UPDATE songs SET is_favorite = 0")
            }

            if (entries.isNotEmpty()) {
                val insertSql = "INSERT OR REPLACE INTO favorites (song_id, date_added) VALUES (?, ?)"
                conn.prepareStatement(insertSql).use { stmt ->
                    for (entry in entries) {
                        stmt.setString(1, entry.songId)
                        stmt.setLong(2, entry.dateAdded)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                val updateSongSql = "UPDATE songs SET is_favorite = 1 WHERE id = ?"
                conn.prepareStatement(updateSongSql).use { stmt ->
                    for (entry in entries) {
                        stmt.setString(1, entry.songId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("favorites")
        database.notifyTableChanged("songs")
    }

    private fun parseLegacyFavorites(payload: String): List<FavoriteBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val dateAdded = (obj["dateAdded"] as? JsonPrimitive)?.longOrNull
                ?: (obj["addedAt"] as? JsonPrimitive)?.longOrNull
                ?: System.currentTimeMillis()
            FavoriteBackupEntry(songId = songId, dateAdded = dateAdded)
        }
    }
}
