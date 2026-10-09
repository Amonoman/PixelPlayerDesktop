package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SearchHistoryItem
import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class FavoritesDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun isFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT 1 FROM favorites WHERE song_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, songId)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    suspend fun getFavoriteSongIds(): List<String> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT song_id FROM favorites ORDER BY date_added DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val ids = mutableListOf<String>()
                    while (rs.next()) {
                        ids.add(rs.getString("song_id"))
                    }
                    ids
                }
            }
        }
    }

    suspend fun addFavorite(songId: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("INSERT OR REPLACE INTO favorites (song_id, date_added) VALUES (?, ?)").use { stmt ->
                    stmt.setString(1, songId)
                    stmt.setLong(2, System.currentTimeMillis())
                    stmt.executeUpdate()
                }
                conn.prepareStatement("UPDATE songs SET is_favorite = 1 WHERE id = ?").use { stmt ->
                    stmt.setString(1, songId)
                    stmt.executeUpdate()
                }
                conn.commit()
                database.notifyTableChanged("favorites")
                database.notifyTableChanged("songs")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun removeFavorite(songId: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("DELETE FROM favorites WHERE song_id = ?").use { stmt ->
                    stmt.setString(1, songId)
                    stmt.executeUpdate()
                }
                conn.prepareStatement("UPDATE songs SET is_favorite = 0 WHERE id = ?").use { stmt ->
                    stmt.setString(1, songId)
                    stmt.executeUpdate()
                }
                conn.commit()
                database.notifyTableChanged("favorites")
                database.notifyTableChanged("songs")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    suspend fun toggleFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        val currentlyFav = isFavorite(songId)
        if (currentlyFav) {
            removeFavorite(songId)
            false
        } else {
            addFavorite(songId)
            true
        }
    }
}

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
                                json.decodeFromString<List<com.kayoi.pixelplayerdesktop.data.model.SyncedLine>>(syncedJson)
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
                stmt.setString(3, lyrics.synced?.let { json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.kayoi.pixelplayerdesktop.data.model.SyncedLine.serializer()), it) })
                stmt.setInt(4, if (lyrics.areFromRemote) 1 else 0)
                stmt.setLong(5, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            database.notifyTableChanged("lyrics_cache")
        }
    }
}

class SearchHistoryDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getRecentSearches(limit: Int = 20): List<SearchHistoryItem> = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT id, query, timestamp FROM search_history ORDER BY timestamp DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val history = mutableListOf<SearchHistoryItem>()
                    while (rs.next()) {
                        history.add(
                            SearchHistoryItem(
                                id = rs.getLong("id"),
                                query = rs.getString("query"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    history
                }
            }
        }
    }

    suspend fun insertSearch(query: String) = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext
        database.getConnection().use { conn ->
            val sql = """
                INSERT INTO search_history (query, timestamp) VALUES (?, ?)
                ON CONFLICT(query) DO UPDATE SET timestamp = excluded.timestamp
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, query.trim())
                stmt.setLong(2, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            database.notifyTableChanged("search_history")
        }
    }

    suspend fun clearHistory() = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            conn.createStatement().use { it.executeUpdate("DELETE FROM search_history") }
            database.notifyTableChanged("search_history")
        }
    }

    suspend fun deleteSearch(id: Long) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM search_history WHERE id = ?").use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("search_history")
        }
    }
}
