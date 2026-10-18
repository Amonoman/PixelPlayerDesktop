package com.kayoi.pixelplayerdesktop.data.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun isFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
            val sql = "SELECT 1 FROM favorites WHERE song_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, songId)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    suspend fun getFavoriteSongIds(): List<String> = withContext(ioDispatcher) {
        database.withConnection(readOnly = true) { conn ->
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
        database.withTransaction { conn ->
            conn.prepareStatement("INSERT OR REPLACE INTO favorites (song_id, date_added) VALUES (?, ?)").use { stmt ->
                stmt.setString(1, songId)
                stmt.setLong(2, System.currentTimeMillis())
                stmt.executeUpdate()
            }
            conn.prepareStatement("UPDATE songs SET is_favorite = 1 WHERE id = ?").use { stmt ->
                stmt.setString(1, songId)
                stmt.executeUpdate()
            }
        }
        database.notifyTableChanged("favorites")
        database.notifyTableChanged("songs")
    }

    suspend fun removeFavorite(songId: String) = withContext(ioDispatcher) {
        database.withTransaction { conn ->
            conn.prepareStatement("DELETE FROM favorites WHERE song_id = ?").use { stmt ->
                stmt.setString(1, songId)
                stmt.executeUpdate()
            }
            conn.prepareStatement("UPDATE songs SET is_favorite = 0 WHERE id = ?").use { stmt ->
                stmt.setString(1, songId)
                stmt.executeUpdate()
            }
        }
        database.notifyTableChanged("favorites")
        database.notifyTableChanged("songs")
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
