package com.kayoi.pixelplayerdesktop.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object AlbumArtCacheManager {

    private const val CLEANUP_PERCENTAGE = 0.25
    private val cleanupMutex = Mutex()

    @Volatile
    private var lastCleanupTime = 0L
    private const val MIN_CLEANUP_INTERVAL_MS = 60 * 1000L

    suspend fun cleanCacheIfNeeded(
        maxCacheSizeMb: Long = 200L,
        cacheDir: File = DesktopPathProvider.artworkCacheDir,
        force: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - lastCleanupTime < MIN_CLEANUP_INTERVAL_MS) {
            return@withContext 0
        }

        cleanupMutex.withLock {
            if (!force && now - lastCleanupTime < MIN_CLEANUP_INTERVAL_MS) {
                return@withLock 0
            }

            if (!cacheDir.exists() || !cacheDir.isDirectory) {
                return@withLock 0
            }

            val files = cacheDir.listFiles()?.filter { it.isFile } ?: return@withLock 0
            if (files.isEmpty()) {
                return@withLock 0
            }

            val maxCacheSizeBytes = maxCacheSizeMb * 1024 * 1024
            val currentSize = files.sumOf { it.length() }

            if (currentSize <= maxCacheSizeBytes) {
                return@withLock 0
            }

            val sortedByLru = files.sortedBy { it.lastModified() }
            val countToDelete = (sortedByLru.size * CLEANUP_PERCENTAGE).toInt().coerceAtLeast(1)
            val filesToDelete = sortedByLru.take(countToDelete)

            var deletedCount = 0
            for (file in filesToDelete) {
                if (file.delete()) {
                    deletedCount++
                }
            }

            lastCleanupTime = System.currentTimeMillis()
            deletedCount
        }
    }

    suspend fun getCacheSizeBytes(cacheDir: File = DesktopPathProvider.artworkCacheDir): Long = withContext(Dispatchers.IO) {
        if (!cacheDir.exists() || !cacheDir.isDirectory) return@withContext 0L
        cacheDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }
}
