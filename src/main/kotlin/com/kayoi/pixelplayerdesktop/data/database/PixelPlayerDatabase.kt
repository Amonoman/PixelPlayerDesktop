package com.kayoi.pixelplayerdesktop.data.database

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class PixelPlayerDatabase(
    private val dbFile: File
) {
    private val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

    private val _tableInvalidationFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val tableInvalidationFlow: SharedFlow<String> = _tableInvalidationFlow.asSharedFlow()

    init {
        val parent = dbFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        initDatabase()
    }

    fun getConnection(): Connection {
        val conn = DriverManager.getConnection(dbUrl)
        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL;")
            stmt.execute("PRAGMA synchronous=NORMAL;")
            stmt.execute("PRAGMA busy_timeout=5000;")
            stmt.execute("PRAGMA foreign_keys=ON;")
        }
        return conn
    }

    fun notifyTableChanged(tableName: String) {
        _tableInvalidationFlow.tryEmit(tableName)
    }

    private fun initDatabase() {
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS songs (
                            id TEXT PRIMARY KEY,
                            title TEXT NOT NULL,
                            artist TEXT NOT NULL,
                            artist_id INTEGER NOT NULL DEFAULT 0,
                            artists_json TEXT,
                            album TEXT NOT NULL,
                            album_id INTEGER NOT NULL DEFAULT 0,
                            album_artist TEXT,
                            path TEXT NOT NULL UNIQUE,
                            content_uri_string TEXT NOT NULL,
                            album_art_uri_string TEXT,
                            duration INTEGER NOT NULL DEFAULT 0,
                            genre TEXT,
                            lyrics TEXT,
                            is_favorite INTEGER NOT NULL DEFAULT 0,
                            track_number INTEGER NOT NULL DEFAULT 0,
                            disc_number INTEGER,
                            year INTEGER NOT NULL DEFAULT 0,
                            date_added INTEGER NOT NULL DEFAULT 0,
                            date_modified INTEGER NOT NULL DEFAULT 0,
                            mime_type TEXT,
                            bitrate INTEGER,
                            sample_rate INTEGER,
                            navidrome_id TEXT,
                            jellyfin_id TEXT,
                            play_count INTEGER NOT NULL DEFAULT 0,
                            last_played_timestamp INTEGER NOT NULL DEFAULT 0
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS artists (
                            id INTEGER PRIMARY KEY,
                            name TEXT NOT NULL UNIQUE,
                            track_count INTEGER NOT NULL DEFAULT 0,
                            image_url TEXT,
                            custom_image_uri TEXT
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS song_artists (
                            song_id TEXT NOT NULL,
                            artist_id INTEGER NOT NULL,
                            is_primary INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY (song_id, artist_id)
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS playlists (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            song_ids_json TEXT NOT NULL DEFAULT '[]',
                            created_at INTEGER NOT NULL,
                            last_modified INTEGER NOT NULL,
                            is_queue_generated INTEGER NOT NULL DEFAULT 0,
                            cover_image_uri TEXT,
                            cover_color_argb INTEGER,
                            cover_icon_name TEXT,
                            cover_shape_type TEXT,
                            cover_shape_detail1 REAL,
                            cover_shape_detail2 REAL,
                            cover_shape_detail3 REAL,
                            cover_shape_detail4 REAL,
                            source TEXT NOT NULL DEFAULT 'LOCAL'
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS favorites (
                            song_id TEXT PRIMARY KEY,
                            date_added INTEGER NOT NULL
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS lyrics_cache (
                            song_id TEXT PRIMARY KEY,
                            plain_lyrics TEXT,
                            synced_lyrics_json TEXT,
                            is_from_remote INTEGER NOT NULL DEFAULT 0,
                            last_updated INTEGER NOT NULL
                        );
                        """.trimIndent()
                    )

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS playback_events (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            song_id TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            duration_ms INTEGER NOT NULL,
                            FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
                        );
                        """.trimIndent()
                    )

                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title COLLATE NOCASE);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs(artist COLLATE NOCASE);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_album ON songs(album COLLATE NOCASE);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_genre ON songs(genre COLLATE NOCASE);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_date_added ON songs(date_added);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_songs_last_played ON songs(last_played_timestamp);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_song_artists_artist ON song_artists(artist_id);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_song_artists_song ON song_artists(song_id);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_artists_name ON artists(name COLLATE NOCASE);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_playback_events_timestamp ON playback_events(timestamp);")
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_playback_events_song_id ON playback_events(song_id);")
                }
                conn.commit()
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            }
        }
    }
}
