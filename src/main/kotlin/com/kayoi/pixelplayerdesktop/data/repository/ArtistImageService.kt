package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Service for fetching and caching high-resolution artist portraits from online providers (Deezer API).
 * Implements a dual-tier cache (LRU memory cache + persistent file storage).
 */
object ArtistImageService {

    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, String?>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?): Boolean {
                return size > 100
            }
        }
    )

    private val cacheDir: File by lazy {
        val dir = File(DesktopPathProvider.configDir, "artist_cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val failedArtists = Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Resolves the artist portrait URI by checking memory cache, disk cache, and remote search endpoints sequentially.
     */
    suspend fun getArtistImageUrl(
        artistName: String,
        fallbackArtworkUri: String? = null,
        fetchEnabled: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext fallbackArtworkUri

        val normalized = artistName.trim().lowercase()

        // 1. Check in-memory LRU cache
        if (memoryCache.containsKey(normalized)) {
            val cached = memoryCache[normalized]
            return@withContext cached ?: fallbackArtworkUri
        }

        // 2. Check local persistent disk cache
        val diskFile = getCacheFile(normalized)
        if (diskFile.exists() && diskFile.length() > 0) {
            val path = diskFile.absolutePath
            memoryCache[normalized] = path
            return@withContext path
        }

        if (!fetchEnabled || failedArtists.contains(normalized)) {
            return@withContext fallbackArtworkUri
        }

        // 3. Query remote API for artist image
        try {
            val encodedName = URLEncoder.encode(artistName.trim(), "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedName&limit=1"
            val uri = URI(urlString)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "PixelPlayerDesktop/1.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(responseText).jsonObject
                val dataArray = root["data"]?.jsonArray
                val firstArtist = dataArray?.firstOrNull()?.jsonObject

                val pictureUrl = firstArtist?.get("picture_big")?.jsonPrimitive?.content
                    ?: firstArtist?.get("picture_medium")?.jsonPrimitive?.content
                    ?: firstArtist?.get("picture")?.jsonPrimitive?.content

                if (!pictureUrl.isNullOrBlank()) {
                    val imgUri = URI(pictureUrl)
                    val imgConnection = imgUri.toURL().openConnection() as HttpURLConnection
                    imgConnection.connectTimeout = 8000
                    imgConnection.readTimeout = 8000
                    imgConnection.setRequestProperty("User-Agent", "PixelPlayerDesktop/1.0")

                    if (imgConnection.responseCode == HttpURLConnection.HTTP_OK) {
                        val bytes = imgConnection.inputStream.use { it.readBytes() }
                        diskFile.writeBytes(bytes)
                        val path = diskFile.absolutePath
                        memoryCache[normalized] = path
                        return@withContext path
                    }
                }
            }
            failedArtists.add(normalized)
            memoryCache[normalized] = null
            fallbackArtworkUri
        } catch (_: Exception) {
            failedArtists.add(normalized)
            fallbackArtworkUri
        }
    }

    private fun getCacheFile(normalizedName: String): File {
        val safeName = normalizedName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return File(cacheDir, "artist_$safeName.jpg")
    }

    fun clearCache() {
        memoryCache.clear()
        failedArtists.clear()
    }
}
