package com.kayoi.pixelplayerdesktop.data.database

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PixelPlayerDatabase(
    private val dbFile: File,
    private val readPoolSize: Int = 4
) : AutoCloseable {
    private val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

    private val _tableInvalidationFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val tableInvalidationFlow: SharedFlow<String> = _tableInvalidationFlow.asSharedFlow()

    private val isClosed = AtomicBoolean(false)
    private val writeLock = ReentrantLock()
    private val writeConnection: Connection
    private val readPool = ArrayBlockingQueue<Connection>(readPoolSize)
    private val allReadConnections = mutableListOf<Connection>()

    private class ConnectionHolder(
        val connection: Connection,
        val isWrite: Boolean,
        var refCount: Int = 1,
        val parent: ConnectionHolder? = null,
        var explicitlySetAutoCommitFalse: Boolean = false,
        var isCommittedOrRolledBack: Boolean = false
    )

    private val currentHolder = ThreadLocal<ConnectionHolder?>()

    init {
        try {
            val parent = dbFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            writeConnection = createConnection(isWrite = true)
            initDatabase()
            for (i in 0 until readPoolSize) {
                val readConn = createConnection(isWrite = false)
                allReadConnections.add(readConn)
                readPool.offer(readConn)
            }
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    private fun createConnection(isWrite: Boolean): Connection {
        val conn = DriverManager.getConnection(dbUrl)
        conn.createStatement().use { stmt ->
            if (isWrite) {
                stmt.execute("PRAGMA journal_mode=WAL;")
            }
            stmt.execute("PRAGMA synchronous=NORMAL;")
            stmt.execute("PRAGMA busy_timeout=5000;")
            stmt.execute("PRAGMA foreign_keys=ON;")
        }
        return conn
    }

    fun getConnection(readOnly: Boolean = false): Connection {
        checkClosed()
        val existing = currentHolder.get()
        if (existing != null) {
            if (existing.isWrite) {
                existing.refCount++
                return LeasedConnection(existing.connection, existing) {
                    releaseHolder(existing)
                }
            } else {
                if (readOnly) {
                    existing.refCount++
                    return LeasedConnection(existing.connection, existing) {
                        releaseHolder(existing)
                    }
                } else {
                    writeLock.lock()
                    try {
                        checkClosed()
                        val newHolder = ConnectionHolder(
                            connection = writeConnection,
                            isWrite = true,
                            refCount = 1,
                            parent = existing
                        )
                        currentHolder.set(newHolder)
                        return LeasedConnection(writeConnection, newHolder) {
                            releaseHolder(newHolder)
                        }
                    } catch (t: Throwable) {
                        writeLock.unlock()
                        throw t
                    }
                }
            }
        } else {
            if (!readOnly) {
                writeLock.lock()
                try {
                    checkClosed()
                    val newHolder = ConnectionHolder(
                        connection = writeConnection,
                        isWrite = true,
                        refCount = 1,
                        parent = null
                    )
                    currentHolder.set(newHolder)
                    return LeasedConnection(writeConnection, newHolder) {
                        releaseHolder(newHolder)
                    }
                } catch (t: Throwable) {
                    writeLock.unlock()
                    throw t
                }
            } else {
                checkClosed()
                val readConn = borrowReadConnection()
                try {
                    val newHolder = ConnectionHolder(
                        connection = readConn,
                        isWrite = false,
                        refCount = 1,
                        parent = null
                    )
                    currentHolder.set(newHolder)
                    return LeasedConnection(readConn, newHolder) {
                        releaseHolder(newHolder)
                    }
                } catch (t: Throwable) {
                    returnReadConnection(readConn)
                    throw t
                }
            }
        }
    }

    inline fun <T> withConnection(readOnly: Boolean = false, block: (Connection) -> T): T {
        return getConnection(readOnly).use(block)
    }

    inline fun <T> withTransaction(block: (Connection) -> T): T {
        return withConnection(readOnly = false) { conn ->
            val wasAutoCommit = conn.autoCommit
            if (wasAutoCommit) {
                conn.autoCommit = false
            }
            try {
                val result = block(conn)
                if (wasAutoCommit) {
                    conn.commit()
                }
                result
            } catch (e: Throwable) {
                if (wasAutoCommit) {
                    runCatching { conn.rollback() }
                }
                throw e
            } finally {
                if (wasAutoCommit) {
                    runCatching { conn.autoCommit = true }
                }
            }
        }
    }

    private fun borrowReadConnection(): Connection {
        checkClosed()
        return readPool.poll(5000, TimeUnit.MILLISECONDS)
            ?: throw SQLException("Timed out waiting for available read connection")
    }

    private fun returnReadConnection(conn: Connection) {
        if (!isClosed.get()) {
            readPool.offer(conn)
        } else {
            runCatching { conn.close() }
        }
    }

    private fun releaseHolder(holder: ConnectionHolder) {
        holder.refCount--
        if (holder.refCount > 0) {
            return
        }

        try {
            if (!holder.connection.isClosed) {
                if (holder.explicitlySetAutoCommitFalse && !holder.isCommittedOrRolledBack) {
                    runCatching { holder.connection.rollback() }
                }
                if (!holder.connection.autoCommit) {
                    runCatching { holder.connection.autoCommit = true }
                }
            }
        } finally {
            if (holder.parent != null) {
                currentHolder.set(holder.parent)
            } else {
                currentHolder.remove()
            }

            if (holder.isWrite) {
                writeLock.unlock()
            } else {
                returnReadConnection(holder.connection)
            }
        }
    }

    private fun checkClosed() {
        if (isClosed.get()) {
            throw IllegalStateException("PixelPlayerDatabase is closed")
        }
    }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        writeLock.withLock {
            runCatching {
                if (!writeConnection.isClosed) {
                    writeConnection.close()
                }
            }
            for (readConn in allReadConnections) {
                runCatching {
                    if (!readConn.isClosed) {
                        readConn.close()
                    }
                }
            }
            readPool.clear()
            allReadConnections.clear()
        }
    }

    fun notifyTableChanged(tableName: String) {
        _tableInvalidationFlow.tryEmit(tableName)
    }

    private class LeasedConnection(
        private val delegate: Connection,
        private val holder: ConnectionHolder,
        private val onRelease: () -> Unit
    ) : Connection by delegate {
        private val isClosedFlag = AtomicBoolean(false)

        override fun setAutoCommit(autoCommit: Boolean) {
            checkNotClosed()
            if (!autoCommit) {
                holder.explicitlySetAutoCommitFalse = true
                holder.isCommittedOrRolledBack = false
            } else {
                holder.explicitlySetAutoCommitFalse = false
                holder.isCommittedOrRolledBack = false
            }
            delegate.autoCommit = autoCommit
        }

        override fun commit() {
            checkNotClosed()
            if (holder.refCount == 1) {
                delegate.commit()
                holder.isCommittedOrRolledBack = true
            }
        }

        override fun rollback() {
            checkNotClosed()
            delegate.rollback()
            holder.isCommittedOrRolledBack = true
        }

        override fun close() {
            if (isClosedFlag.compareAndSet(false, true)) {
                onRelease()
            }
        }

        override fun isClosed(): Boolean = isClosedFlag.get() || delegate.isClosed

        private fun checkNotClosed() {
            if (isClosedFlag.get()) {
                throw SQLException("Connection is closed")
            }
        }
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

                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS listenbrainz_pending_listens (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            listened_at_ms INTEGER NOT NULL,
                            track_name TEXT NOT NULL,
                            artist_name TEXT NOT NULL,
                            release_name TEXT,
                            duration_ms INTEGER,
                            recording_mbid TEXT,
                            source TEXT NOT NULL DEFAULT 'LOCAL',
                            created_at_ms INTEGER NOT NULL
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
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_lb_pending_created ON listenbrainz_pending_listens(created_at_ms);")
                }
                conn.commit()
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            }
        }
    }
}
