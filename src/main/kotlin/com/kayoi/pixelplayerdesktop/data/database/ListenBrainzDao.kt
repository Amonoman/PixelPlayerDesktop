package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.sql.Types

class ListenBrainzDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun insert(listen: ListenBrainzPendingListen): Boolean = withContext(ioDispatcher) {
        database.withConnection { conn ->
            val sql = """
                INSERT INTO listenbrainz_pending_listens (
                    listened_at_ms, track_name, artist_name, release_name, duration_ms, recording_mbid, source, created_at_ms
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, listen.listenedAtMs)
                stmt.setString(2, listen.trackName)
                stmt.setString(3, listen.artistName)
                stmt.setString(4, listen.releaseName)
                if (listen.durationMs != null) stmt.setLong(5, listen.durationMs) else stmt.setNull(5, Types.BIGINT)
                stmt.setString(6, listen.recordingMbid)
                stmt.setString(7, listen.source)
                stmt.setLong(8, listen.createdAtMs)
                val rows = stmt.executeUpdate()
                if (rows > 0) {
                    database.notifyTableChanged("listenbrainz_pending_listens")
                    true
                } else {
                    false
                }
            }
        }
    }

    suspend fun getPendingListens(limit: Int = 100): List<ListenBrainzPendingListen> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT * FROM listenbrainz_pending_listens ORDER BY created_at_ms ASC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<ListenBrainzPendingListen>()
                    while (rs.next()) {
                        val duration = rs.getLong("duration_ms")
                        result.add(
                            ListenBrainzPendingListen(
                                id = rs.getLong("id"),
                                listenedAtMs = rs.getLong("listened_at_ms"),
                                trackName = rs.getString("track_name"),
                                artistName = rs.getString("artist_name"),
                                releaseName = rs.getString("release_name"),
                                durationMs = if (rs.wasNull()) null else duration,
                                recordingMbid = rs.getString("recording_mbid"),
                                source = rs.getString("source") ?: "LOCAL",
                                createdAtMs = rs.getLong("created_at_ms")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    suspend fun deleteListens(ids: List<Long>): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        database.withConnection { conn ->
            val placeholders = ids.joinToString(",") { "?" }
            val sql = "DELETE FROM listenbrainz_pending_listens WHERE id IN ($placeholders)"
            conn.prepareStatement(sql).use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.setLong(index + 1, id)
                }
                val deleted = stmt.executeUpdate()
                if (deleted > 0) {
                    database.notifyTableChanged("listenbrainz_pending_listens")
                }
                deleted
            }
        }
    }

    suspend fun count(): Int = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM listenbrainz_pending_listens").use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    fun countFlow(): Flow<Int> {
        return database.tableInvalidationFlow
            .filter { it == "listenbrainz_pending_listens" }
            .onStart { emit("listenbrainz_pending_listens") }
            .map { count() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    suspend fun trimQueue(maxSize: Int = 3000): Int = withContext(ioDispatcher) {
        val total = count()
        if (total <= maxSize) return@withContext 0
        val toRemove = total - maxSize
        database.withConnection { conn ->
            val sql = """
                DELETE FROM listenbrainz_pending_listens WHERE id IN (
                    SELECT id FROM listenbrainz_pending_listens ORDER BY created_at_ms ASC LIMIT ?
                )
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, toRemove)
                val deleted = stmt.executeUpdate()
                if (deleted > 0) {
                    database.notifyTableChanged("listenbrainz_pending_listens")
                }
                deleted
            }
        }
    }
}
