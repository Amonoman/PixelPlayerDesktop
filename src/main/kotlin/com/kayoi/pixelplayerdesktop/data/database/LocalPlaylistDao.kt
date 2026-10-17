package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.sql.ResultSet
import java.sql.Types

private val stringListSerializer = ListSerializer(String.serializer())

class LocalPlaylistDao(
    private val database: PixelPlayerDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getAllPlaylists(sortOption: SortOption = SortOption.PlaylistNameAZ): List<Playlist> = withContext(ioDispatcher) {
        val orderByClause = when (sortOption) {
            SortOption.PlaylistNameAZ -> "name COLLATE NOCASE ASC"
            SortOption.PlaylistNameZA -> "name COLLATE NOCASE DESC"
            SortOption.PlaylistDateCreated -> "created_at DESC"
            SortOption.PlaylistDateCreatedAsc -> "created_at ASC"
            else -> "name COLLATE NOCASE ASC"
        }

        database.getConnection().use { conn ->
            val sql = "SELECT * FROM playlists ORDER BY $orderByClause"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val playlists = mutableListOf<Playlist>()
                    while (rs.next()) {
                        playlists.add(rs.toPlaylist(json))
                    }
                    playlists
                }
            }
        }
    }

    suspend fun getPlaylistById(id: String): Playlist? = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "SELECT * FROM playlists WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toPlaylist(json) else null
                }
            }
        }
    }

    suspend fun insertPlaylist(playlist: Playlist) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = """
                INSERT INTO playlists (
                    id, name, song_ids_json, created_at, last_modified, is_queue_generated,
                    cover_image_uri, cover_color_argb, cover_icon_name, cover_shape_type,
                    cover_shape_detail1, cover_shape_detail2, cover_shape_detail3, cover_shape_detail4, source
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    name = excluded.name,
                    song_ids_json = excluded.song_ids_json,
                    last_modified = excluded.last_modified,
                    is_queue_generated = excluded.is_queue_generated,
                    cover_image_uri = excluded.cover_image_uri,
                    cover_color_argb = excluded.cover_color_argb,
                    cover_icon_name = excluded.cover_icon_name,
                    cover_shape_type = excluded.cover_shape_type,
                    cover_shape_detail1 = excluded.cover_shape_detail1,
                    cover_shape_detail2 = excluded.cover_shape_detail2,
                    cover_shape_detail3 = excluded.cover_shape_detail3,
                    cover_shape_detail4 = excluded.cover_shape_detail4,
                    source = excluded.source;
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playlist.id)
                stmt.setString(2, playlist.name)
                stmt.setString(3, json.encodeToString(stringListSerializer, playlist.songIds))
                stmt.setLong(4, playlist.createdAt)
                stmt.setLong(5, playlist.lastModified)
                stmt.setInt(6, if (playlist.isQueueGenerated) 1 else 0)
                stmt.setString(7, playlist.coverImageUri)
                if (playlist.coverColorArgb != null) stmt.setInt(8, playlist.coverColorArgb) else stmt.setNull(8, Types.INTEGER)
                stmt.setString(9, playlist.coverIconName)
                stmt.setString(10, playlist.coverShapeType)
                if (playlist.coverShapeDetail1 != null) stmt.setFloat(11, playlist.coverShapeDetail1) else stmt.setNull(11, Types.REAL)
                if (playlist.coverShapeDetail2 != null) stmt.setFloat(12, playlist.coverShapeDetail2) else stmt.setNull(12, Types.REAL)
                if (playlist.coverShapeDetail3 != null) stmt.setFloat(13, playlist.coverShapeDetail3) else stmt.setNull(13, Types.REAL)
                if (playlist.coverShapeDetail4 != null) stmt.setFloat(14, playlist.coverShapeDetail4) else stmt.setNull(14, Types.REAL)
                stmt.setString(15, playlist.source)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("playlists")
        }
    }

    suspend fun deletePlaylist(id: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "DELETE FROM playlists WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("playlists")
        }
    }

    suspend fun clearPlaylistsBySource(source: String) = withContext(ioDispatcher) {
        database.getConnection().use { conn ->
            val sql = "DELETE FROM playlists WHERE source = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, source)
                stmt.executeUpdate()
            }
            database.notifyTableChanged("playlists")
        }
    }
}

private fun ResultSet.toPlaylist(json: Json): Playlist {
    val songIdsRaw = getString("song_ids_json") ?: "[]"
    val songIds = try {
        json.decodeFromString(stringListSerializer, songIdsRaw)
    } catch (e: Exception) {
        emptyList()
    }
    val coverColorArgb = getInt("cover_color_argb")
    val wasNullColor = wasNull()
    val d1 = getFloat("cover_shape_detail1")
    val wasNullD1 = wasNull()
    val d2 = getFloat("cover_shape_detail2")
    val wasNullD2 = wasNull()
    val d3 = getFloat("cover_shape_detail3")
    val wasNullD3 = wasNull()
    val d4 = getFloat("cover_shape_detail4")
    val wasNullD4 = wasNull()

    return Playlist(
        id = getString("id"),
        name = getString("name"),
        songIds = songIds,
        createdAt = getLong("created_at"),
        lastModified = getLong("last_modified"),
        isQueueGenerated = getInt("is_queue_generated") == 1,
        coverImageUri = getString("cover_image_uri"),
        coverColorArgb = if (wasNullColor) null else coverColorArgb,
        coverIconName = getString("cover_icon_name"),
        coverShapeType = getString("cover_shape_type"),
        coverShapeDetail1 = if (wasNullD1) null else d1,
        coverShapeDetail2 = if (wasNullD2) null else d2,
        coverShapeDetail3 = if (wasNullD3) null else d3,
        coverShapeDetail4 = if (wasNullD4) null else d4,
        source = getString("source") ?: "LOCAL"
    )
}
