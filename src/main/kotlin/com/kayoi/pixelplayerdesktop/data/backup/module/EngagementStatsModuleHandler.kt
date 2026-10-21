package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSongMetadata
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
data class EngagementStatBackupEntry(
    val songId: String,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val song: BackupSongMetadata? = null
)

class EngagementStatsModuleHandler(
    private val database: PixelPlayerDatabase,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.ENGAGEMENT_STATS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val list = mutableListOf<EngagementStatBackupEntry>()
        val songIds = mutableListOf<String>()

        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT id, play_count, last_played_timestamp FROM songs WHERE play_count > 0"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val sId = rs.getString("id")
                        songIds.add(sId)
                        list.add(
                            EngagementStatBackupEntry(
                                songId = sId,
                                playCount = rs.getInt("play_count"),
                                lastPlayedTimestamp = rs.getLong("last_played_timestamp")
                            )
                        )
                    }
                }
            }
        }

        val metadataMap = songResolver.metadataFor(songIds)
        val entries = list.map { it.copy(song = metadataMap[it.songId]) }
        json.encodeToString(ListSerializer(EngagementStatBackupEntry.serializer()), entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM songs WHERE play_count > 0").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val entries = runCatching {
            json.decodeFromString(ListSerializer(EngagementStatBackupEntry.serializer()), trimmed)
        }.getOrElse {
            parseLegacyEngagement(trimmed)
        }

        val library = songResolver.library()
        val resolved = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            entry.copy(songId = resolvedId)
        }

        database.withTransaction { conn ->
            val sql = "UPDATE songs SET play_count = ?, last_played_timestamp = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                for (item in resolved) {
                    stmt.setInt(1, item.playCount)
                    stmt.setLong(2, item.lastPlayedTimestamp)
                    stmt.setString(3, item.songId)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
        database.notifyTableChanged("songs")
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(EngagementStatBackupEntry.serializer()), snapshot)
        database.withTransaction { conn ->
            val sql = "UPDATE songs SET play_count = ?, last_played_timestamp = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                for (item in entries) {
                    stmt.setInt(1, item.playCount)
                    stmt.setLong(2, item.lastPlayedTimestamp)
                    stmt.setString(3, item.songId)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
        database.notifyTableChanged("songs")
    }

    private fun parseLegacyEngagement(payload: String): List<EngagementStatBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val playCount = (obj["playCount"] as? JsonPrimitive)?.intOrNull
                ?: (obj["play_count"] as? JsonPrimitive)?.intOrNull ?: 1
            val lastPlayed = (obj["lastPlayedTimestamp"] as? JsonPrimitive)?.longOrNull
                ?: (obj["last_played_timestamp"] as? JsonPrimitive)?.longOrNull
                ?: System.currentTimeMillis()
            EngagementStatBackupEntry(songId = songId, playCount = playCount, lastPlayedTimestamp = lastPlayed)
        }
    }
}
