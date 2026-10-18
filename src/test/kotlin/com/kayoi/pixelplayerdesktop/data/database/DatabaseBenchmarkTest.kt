package com.kayoi.pixelplayerdesktop.data.database

import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class DatabaseBenchmarkTest {

    @Test
    fun benchmarkSimpleQueries() = runTest {
        val tempDb = File.createTempFile("benchmark_test", ".db")
        tempDb.deleteOnExit()

        val db = PixelPlayerDatabase(tempDb)

        // 1. Benchmark BEFORE: opening a new connection and running 4 PRAGMAs each query
        val dbUrl = "jdbc:sqlite:${tempDb.absolutePath}"
        fun getOldConnection(): java.sql.Connection {
            val conn = DriverManager.getConnection(dbUrl)
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL;")
                stmt.execute("PRAGMA synchronous=NORMAL;")
                stmt.execute("PRAGMA busy_timeout=5000;")
                stmt.execute("PRAGMA foreign_keys=ON;")
            }
            return conn
        }

        // Warm up old
        repeat(10) {
            getOldConnection().use { conn ->
                conn.createStatement().use { it.executeQuery("SELECT 1;").use { rs -> rs.next() } }
            }
        }

        val elapsedBeforeMs = measureTimeMillis {
            repeat(1000) {
                getOldConnection().use { conn ->
                    conn.createStatement().use { it.executeQuery("SELECT 1;").use { rs -> rs.next() } }
                }
            }
        }

        // Warm up new
        repeat(10) {
            db.withConnection(readOnly = true) { conn ->
                conn.createStatement().use { it.executeQuery("SELECT 1;").use { rs -> rs.next() } }
            }
        }

        // 2. Benchmark AFTER: using pooled read connection with PRAGMAs executed once on init
        val elapsedAfterMs = measureTimeMillis {
            repeat(1000) {
                db.withConnection(readOnly = true) { conn ->
                    conn.createStatement().use { it.executeQuery("SELECT 1;").use { rs -> rs.next() } }
                }
            }
        }

        println("BENCHMARK_BEFORE: 1000 queries took $elapsedBeforeMs ms")
        println("BENCHMARK_AFTER: 1000 queries took $elapsedAfterMs ms")

        assertTrue(elapsedAfterMs < elapsedBeforeMs, "New implementation should be significantly faster")
    }
}
