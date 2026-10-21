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
import kotlinx.serialization.json.booleanOrNull

@Serializable
data class LyricsBackupEntry(
    val songId: String,
    val plainLyrics: String? = null,
    val syncedLyricsJson: String? = null,
    val isFromRemote: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val song: BackupSongMetadata? = null
)

class LyricsModuleHandler(
    private val database: PixelPlayerDatabase,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.LYRICS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val list = mutableListOf<LyricsBackupEntry>()
        val songIds = mutableListOf<String>()

        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT song_id, plain_lyrics, synced_lyrics_json, is_from_remote, last_updated FROM lyrics_cache"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val sId = rs.getString("song_id")
                        songIds.add(sId)
                        list.add(
                            LyricsBackupEntry(
                                songId = sId,
                                plainLyrics = rs.getString("plain_lyrics"),
                                syncedLyricsJson = rs.getString("synced_lyrics_json"),
                                isFromRemote = rs.getInt("is_from_remote") == 1,
                                lastUpdated = rs.getLong("last_updated")
                            )
                        )
                    }
                }
            }
        }

        val metadataMap = songResolver.metadataFor(songIds)
        val entriesWithMeta = list.map { it.copy(song = metadataMap[it.songId]) }
        json.encodeToString(ListSerializer(LyricsBackupEntry.serializer()), entriesWithMeta)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM lyrics_cache").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val list = mutableListOf<LyricsBackupEntry>()
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT song_id, plain_lyrics, synced_lyrics_json, is_from_remote, last_updated FROM lyrics_cache"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            LyricsBackupEntry(
                                songId = rs.getString("song_id"),
                                plainLyrics = rs.getString("plain_lyrics"),
                                syncedLyricsJson = rs.getString("synced_lyrics_json"),
                                isFromRemote = rs.getInt("is_from_remote") == 1,
                                lastUpdated = rs.getLong("last_updated")
                            )
                        )
                    }
                }
            }
        }
        json.encodeToString(ListSerializer(LyricsBackupEntry.serializer()), list)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = runCatching {
            json.decodeFromString(ListSerializer(LyricsBackupEntry.serializer()), payload)
        }.getOrElse {
            parseLegacyLyrics(payload)
        }
        val library = songResolver.library()

        val resolved = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            entry.copy(songId = resolvedId)
        }.distinctBy { it.songId }

        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM lyrics_cache")
            }

            if (resolved.isNotEmpty()) {
                val sql = """
                    INSERT INTO lyrics_cache (song_id, plain_lyrics, synced_lyrics_json, is_from_remote, last_updated)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    for (item in resolved) {
                        stmt.setString(1, item.songId)
                        stmt.setString(2, item.plainLyrics)
                        stmt.setString(3, item.syncedLyricsJson)
                        stmt.setInt(4, if (item.isFromRemote) 1 else 0)
                        stmt.setLong(5, item.lastUpdated)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("lyrics_cache")
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(LyricsBackupEntry.serializer()), snapshot)
        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM lyrics_cache")
            }

            if (entries.isNotEmpty()) {
                val sql = """
                    INSERT INTO lyrics_cache (song_id, plain_lyrics, synced_lyrics_json, is_from_remote, last_updated)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    for (item in entries) {
                        stmt.setString(1, item.songId)
                        stmt.setString(2, item.plainLyrics)
                        stmt.setString(3, item.syncedLyricsJson)
                        stmt.setInt(4, if (item.isFromRemote) 1 else 0)
                        stmt.setLong(5, item.lastUpdated)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("lyrics_cache")
    }

    private fun parseLegacyLyrics(payload: String): List<LyricsBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val content = (obj["content"] as? JsonPrimitive)?.content
            val isSynced = (obj["isSynced"] as? JsonPrimitive)?.booleanOrNull ?: false
            val plainLyrics = (obj["plainLyrics"] as? JsonPrimitive)?.content
                ?: if (!isSynced) content else null
            val syncedLyricsJson = (obj["syncedLyricsJson"] as? JsonPrimitive)?.content
                ?: if (isSynced) content else null
            val isFromRemote = (obj["isFromRemote"] as? JsonPrimitive)?.booleanOrNull ?: false
            LyricsBackupEntry(
                songId = songId,
                plainLyrics = plainLyrics,
                syncedLyricsJson = syncedLyricsJson,
                isFromRemote = isFromRemote
            )
        }
    }
}
