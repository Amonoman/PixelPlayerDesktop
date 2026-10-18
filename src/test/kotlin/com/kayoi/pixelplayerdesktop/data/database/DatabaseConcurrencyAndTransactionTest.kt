package com.kayoi.pixelplayerdesktop.data.database

import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseConcurrencyAndTransactionTest {

    @Test
    fun testParallelReadsWithConcurrentWrite() = runTest {
        val tempDb = File.createTempFile("test_concurrency", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb, readPoolSize = 4)
        val musicDao = MusicDao(db)

        val initialSong = Song(
            id = "concurrent_1",
            title = "Concurrent Song 1",
            artist = "Artist 1",
            artistId = 1L,
            album = "Album 1",
            albumId = 1L,
            path = "/music/song1.mp3",
            contentUriString = "file:///music/song1.mp3",
            duration = 180000L,
            genre = "Rock",
            year = 2024,
            dateAdded = 1000L
        )
        musicDao.upsertSongs(listOf(initialSong))

        val writeStartedLatch = CountDownLatch(1)
        val readsFinishedLatch = CountDownLatch(4)
        val writeCanProceedLatch = CountDownLatch(1)
        val readSuccessCount = AtomicInteger(0)
        val writerFinished = AtomicBoolean(false)

        val writerThread = thread(name = "WriterThread") {
            db.withConnection(readOnly = false) { conn ->
                conn.autoCommit = false
                try {
                    conn.prepareStatement("INSERT INTO songs (id, title, artist, artist_id, album, album_id, path, content_uri_string, duration, year, date_added) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                        stmt.setString(1, "concurrent_2")
                        stmt.setString(2, "Concurrent Song 2")
                        stmt.setString(3, "Artist 2")
                        stmt.setLong(4, 2L)
                        stmt.setString(5, "Album 2")
                        stmt.setLong(6, 2L)
                        stmt.setString(7, "/music/song2.mp3")
                        stmt.setString(8, "file:///music/song2.mp3")
                        stmt.setLong(9, 200000L)
                        stmt.setInt(10, 2024)
                        stmt.setLong(11, 2000L)
                        stmt.executeUpdate()
                    }
                    writeStartedLatch.countDown()
                    writeCanProceedLatch.await(5, TimeUnit.SECONDS)
                    conn.commit()
                } finally {
                    writerFinished.set(true)
                }
            }
        }

        assertTrue(writeStartedLatch.await(5, TimeUnit.SECONDS), "Write transaction should have started")
        assertFalse(writerFinished.get(), "Writer must still be holding the transaction lock")

        val readers = (1..4).map { readerId ->
            async(Dispatchers.IO) {
                val song = musicDao.getSongById("concurrent_1")
                assertNotNull(song)
                assertEquals("Concurrent Song 1", song.title)
                readSuccessCount.incrementAndGet()
                readsFinishedLatch.countDown()
            }
        }

        val completedDuringWrite = readsFinishedLatch.await(3, TimeUnit.SECONDS)
        assertTrue(completedDuringWrite, "All 4 readers must complete concurrently while writer transaction is open")
        assertFalse(writerFinished.get(), "Writer should still be in progress when readers completed")

        writeCanProceedLatch.countDown()
        writerThread.join(5000)
        assertTrue(writerFinished.get(), "Writer must have finished")

        readers.awaitAll()
        assertEquals(4, readSuccessCount.get())

        val writtenSong = musicDao.getSongById("concurrent_2")
        assertNotNull(writtenSong)
        assertEquals("Concurrent Song 2", writtenSong.title)

        db.close()
    }

    @Test
    fun testTransactionRollbackOnException() = runTest {
        val tempDb = File.createTempFile("test_rollback_exc", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        val song = Song(
            id = "rollback_1",
            title = "Rollback Song",
            artist = "Rollback Artist",
            artistId = 10L,
            album = "Rollback Album",
            albumId = 10L,
            path = "/music/rollback.mp3",
            contentUriString = "file:///music/rollback.mp3",
            duration = 120000L,
            genre = "Jazz",
            year = 2021,
            dateAdded = 500L
        )

        assertFailsWith<IllegalStateException> {
            db.withConnection { conn ->
                conn.autoCommit = false
                conn.prepareStatement("INSERT INTO songs (id, title, artist, artist_id, album, album_id, path, content_uri_string, duration, year, date_added) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                    stmt.setString(1, song.id)
                    stmt.setString(2, song.title)
                    stmt.setString(3, song.artist)
                    stmt.setLong(4, song.artistId)
                    stmt.setString(5, song.album)
                    stmt.setLong(6, song.albumId)
                    stmt.setString(7, song.path)
                    stmt.setString(8, song.contentUriString)
                    stmt.setLong(9, song.duration)
                    stmt.setInt(10, song.year)
                    stmt.setLong(11, song.dateAdded)
                    stmt.executeUpdate()
                }
                throw IllegalStateException("Simulated crash before commit")
            }
        }

        val queryResult = musicDao.getSongById("rollback_1")
        assertNull(queryResult, "Uncommitted insert must be rolled back on exception")

        // Verify connection autoCommit is restored and can execute subsequent transactions
        db.withConnection { conn ->
            assertTrue(conn.autoCommit, "autoCommit must be restored to true after rollback")
        }

        // Verify successful write after rollback
        musicDao.upsertSongs(listOf(song))
        val afterSuccess = musicDao.getSongById("rollback_1")
        assertNotNull(afterSuccess)
        assertEquals("Rollback Song", afterSuccess.title)

        db.close()
    }

    @Test
    fun testExplicitTransactionRollback() = runTest {
        val tempDb = File.createTempFile("test_explicit_rollback", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)

        db.withConnection { conn ->
            conn.autoCommit = false
            conn.prepareStatement("INSERT INTO songs (id, title, artist, artist_id, album, album_id, path, content_uri_string, duration, year, date_added) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                stmt.setString(1, "explicit_rb")
                stmt.setString(2, "Explicit Title")
                stmt.setString(3, "Artist")
                stmt.setLong(4, 1L)
                stmt.setString(5, "Album")
                stmt.setLong(6, 1L)
                stmt.setString(7, "/music/explicit.mp3")
                stmt.setString(8, "file:///music/explicit.mp3")
                stmt.setLong(9, 1000L)
                stmt.setInt(10, 2022)
                stmt.setLong(11, 100L)
                stmt.executeUpdate()
            }
            conn.rollback()
        }

        val result = musicDao.getSongById("explicit_rb")
        assertNull(result, "Explicit rollback must discard inserted row")

        db.withConnection { conn ->
            assertTrue(conn.autoCommit, "autoCommit must be restored to true after explicit rollback")
        }

        db.close()
    }

    @Test
    fun testNestedWithConnection() = runTest {
        val tempDb = File.createTempFile("test_nested", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)

        // Nested write in write
        db.withConnection(readOnly = false) { outerConn ->
            outerConn.autoCommit = false
            outerConn.prepareStatement("INSERT INTO favorites (song_id, date_added) VALUES (?, ?)").use { stmt ->
                stmt.setString(1, "fav_nested_1")
                stmt.setLong(2, 100L)
                stmt.executeUpdate()
            }

            // Inner write should reuse outer write connection
            db.withConnection(readOnly = false) { innerConn ->
                innerConn.prepareStatement("INSERT INTO favorites (song_id, date_added) VALUES (?, ?)").use { stmt ->
                    stmt.setString(1, "fav_nested_2")
                    stmt.setLong(2, 200L)
                    stmt.executeUpdate()
                }
            }

            // Inner read should see uncommitted changes from outer write
            db.withConnection(readOnly = true) { readConn ->
                readConn.prepareStatement("SELECT COUNT(*) FROM favorites").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                        assertEquals(2, rs.getInt(1), "Inner read must see uncommitted changes from outer transaction")
                    }
                }
            }

            outerConn.commit()
        }

        // Verify committed after outer completed
        db.withConnection(readOnly = true) { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM favorites").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                    assertEquals(2, rs.getInt(1))
                }
            }
        }

        db.close()
    }

    @Test
    fun testDatabaseCloseRejectsQueriesAndReleasesConnections() {
        val tempDb = File.createTempFile("test_close", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)
        db.withConnection(readOnly = true) { conn ->
            assertFalse(conn.isClosed)
        }

        db.close()

        assertFailsWith<IllegalStateException> {
            db.withConnection(readOnly = true) { }
        }

        assertFailsWith<IllegalStateException> {
            db.withConnection(readOnly = false) { }
        }
    }
}
