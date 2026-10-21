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
import kotlinx.serialization.json.longOrNull

@Serializable
data class PlaybackEventBackupEntry(
    val songId: String,
    val timestamp: Long,
    val durationMs: Long,
    val song: BackupSongMetadata? = null
)

@Serializable
data class SongEngagementBackupEntry(
    val songId: String,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val song: BackupSongMetadata? = null
)

@Serializable
data class PlaybackHistoryBackupPayload(
    val events: List<PlaybackEventBackupEntry> = emptyList(),
    val engagementStats: List<SongEngagementBackupEntry> = emptyList()
)

class PlaybackHistoryModuleHandler(
    private val database: PixelPlayerDatabase,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.PLAYBACK_HISTORY

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val events = mutableListOf<PlaybackEventBackupEntry>()
        val engagementStats = mutableListOf<SongEngagementBackupEntry>()
        val allSongIds = mutableSetOf<String>()

        database.withConnection(readOnly = true) { conn ->
            val eventSql = "SELECT song_id, timestamp, duration_ms FROM playback_events ORDER BY timestamp DESC LIMIT 10000"
            conn.prepareStatement(eventSql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val sId = rs.getString("song_id")
                        allSongIds.add(sId)
                        events.add(
                            PlaybackEventBackupEntry(
                                songId = sId,
                                timestamp = rs.getLong("timestamp"),
                                durationMs = rs.getLong("duration_ms")
                            )
                        )
                    }
                }
            }

            val statsSql = "SELECT id, play_count, last_played_timestamp FROM songs WHERE play_count > 0"
            conn.prepareStatement(statsSql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val sId = rs.getString("id")
                        allSongIds.add(sId)
                        engagementStats.add(
                            SongEngagementBackupEntry(
                                songId = sId,
                                playCount = rs.getInt("play_count"),
                                lastPlayedTimestamp = rs.getLong("last_played_timestamp")
                            )
                        )
                    }
                }
            }
        }

        val metadataMap = songResolver.metadataFor(allSongIds)
        val payload = PlaybackHistoryBackupPayload(
            events = events.map { it.copy(song = metadataMap[it.songId]) },
            engagementStats = engagementStats.map { it.copy(song = metadataMap[it.songId]) }
        )
        json.encodeToString(PlaybackHistoryBackupPayload.serializer(), payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM playback_events").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val events = mutableListOf<PlaybackEventBackupEntry>()
        val engagementStats = mutableListOf<SongEngagementBackupEntry>()

        database.withConnection(readOnly = true) { conn ->
            val eventSql = "SELECT song_id, timestamp, duration_ms FROM playback_events"
            conn.prepareStatement(eventSql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        events.add(
                            PlaybackEventBackupEntry(
                                songId = rs.getString("song_id"),
                                timestamp = rs.getLong("timestamp"),
                                durationMs = rs.getLong("duration_ms")
                            )
                        )
                    }
                }
            }

            val statsSql = "SELECT id, play_count, last_played_timestamp FROM songs WHERE play_count > 0"
            conn.prepareStatement(statsSql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        engagementStats.add(
                            SongEngagementBackupEntry(
                                songId = rs.getString("id"),
                                playCount = rs.getInt("play_count"),
                                lastPlayedTimestamp = rs.getLong("last_played_timestamp")
                            )
                        )
                    }
                }
            }
        }

        val payload = PlaybackHistoryBackupPayload(events, engagementStats)
        json.encodeToString(PlaybackHistoryBackupPayload.serializer(), payload)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val parsed = if (trimmed.startsWith("[")) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(PlaybackEventBackupEntry.serializer()), trimmed)
            }.getOrElse {
                parseLegacyPlaybackEvents(trimmed)
            }
            PlaybackHistoryBackupPayload(events = list)
        } else {
            json.decodeFromString(PlaybackHistoryBackupPayload.serializer(), trimmed)
        }

        val library = songResolver.library()
        val resolvedEvents = parsed.events.mapNotNull { event ->
            val resolvedId = library.resolve(event.songId, event.song) ?: event.songId
            event.copy(songId = resolvedId)
        }

        val resolvedStats = parsed.engagementStats.mapNotNull { stat ->
            val resolvedId = library.resolve(stat.songId, stat.song) ?: stat.songId
            stat.copy(songId = resolvedId)
        }

        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM playback_events")
            }

            if (resolvedEvents.isNotEmpty()) {
                val insertSql = "INSERT INTO playback_events (song_id, timestamp, duration_ms) VALUES (?, ?, ?)"
                conn.prepareStatement(insertSql).use { stmt ->
                    for (e in resolvedEvents) {
                        stmt.setString(1, e.songId)
                        stmt.setLong(2, e.timestamp)
                        stmt.setLong(3, e.durationMs)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            if (resolvedStats.isNotEmpty()) {
                val updateSql = "UPDATE songs SET play_count = ?, last_played_timestamp = ? WHERE id = ?"
                conn.prepareStatement(updateSql).use { stmt ->
                    for (s in resolvedStats) {
                        stmt.setInt(1, s.playCount)
                        stmt.setLong(2, s.lastPlayedTimestamp)
                        stmt.setString(3, s.songId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("playback_events")
        database.notifyTableChanged("songs")
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val parsed = json.decodeFromString(PlaybackHistoryBackupPayload.serializer(), snapshot)
        database.withTransaction { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM playback_events")
            }

            if (parsed.events.isNotEmpty()) {
                val insertSql = "INSERT INTO playback_events (song_id, timestamp, duration_ms) VALUES (?, ?, ?)"
                conn.prepareStatement(insertSql).use { stmt ->
                    for (e in parsed.events) {
                        stmt.setString(1, e.songId)
                        stmt.setLong(2, e.timestamp)
                        stmt.setLong(3, e.durationMs)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            if (parsed.engagementStats.isNotEmpty()) {
                val updateSql = "UPDATE songs SET play_count = ?, last_played_timestamp = ? WHERE id = ?"
                conn.prepareStatement(updateSql).use { stmt ->
                    for (s in parsed.engagementStats) {
                        stmt.setInt(1, s.playCount)
                        stmt.setLong(2, s.lastPlayedTimestamp)
                        stmt.setString(3, s.songId)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }
        }
        database.notifyTableChanged("playback_events")
        database.notifyTableChanged("songs")
    }

    private fun parseLegacyPlaybackEvents(payload: String): List<PlaybackEventBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val timestamp = (obj["timestamp"] as? JsonPrimitive)?.longOrNull ?: 0L
            val durationMs = (obj["durationMs"] as? JsonPrimitive)?.longOrNull ?: 0L
            PlaybackEventBackupEntry(songId = songId, timestamp = timestamp, durationMs = durationMs)
        }
    }
}
