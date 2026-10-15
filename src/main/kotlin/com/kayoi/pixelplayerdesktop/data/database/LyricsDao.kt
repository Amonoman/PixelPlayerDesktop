package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val syncedLineListSerializer = ListSerializer(SyncedLine.serializer())

class LyricsDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getLyrics(songId: String): Lyrics? = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT plain_lyrics, synced_lyrics_json, is_from_remote FROM lyrics_cache WHERE song_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, songId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val plain = rs.getString("plain_lyrics")?.split("\n")?.filter { it.isNotBlank() }
                        val syncedJson = rs.getString("synced_lyrics_json")
                        val isRemote = rs.getInt("is_from_remote") == 1
                        val synced = if (!syncedJson.isNullOrBlank()) {
                            try {
                                json.decodeFromString(syncedLineListSerializer, syncedJson)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                        Lyrics(plain = plain, synced = synced, areFromRemote = isRemote)
                    } else {
                        null
                    }
                }
            }
        }
    }

    suspend fun insertOrUpdateLyrics(songId: String, lyrics: Lyrics) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = """
                INSERT INTO lyrics_cache (song_id, plain_lyrics, synced_lyrics_json, is_from_remote, last_updated)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(song_id) DO UPDATE SET
                    plain_lyrics = excluded.plain_lyrics,
                    synced_lyrics_json = excluded.synced_lyrics_json,
                    is_from_remote = excluded.is_from_remote,
                    last_updated = excluded.last_updated;
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, songId)
                stmt.setString(2, lyrics.plain?.joinToString("\n"))
                stmt.setString(3, lyrics.synced?.let { json.encodeToString(syncedLineListSerializer, it) })
                stmt.setInt(4, if (lyrics.areFromRemote) 1 else 0)
                stmt.setLong(5, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            database.notifyTableChanged("lyrics_cache")
        }
    }

    suspend fun deleteLyrics(songId: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "DELETE FROM lyrics_cache WHERE song_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, songId)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("lyrics_cache")
        }
    }

    suspend fun deleteAllLyrics() = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("DELETE FROM lyrics_cache")
            }
            database.notifyTableChanged("lyrics_cache")
        }
    }
}
