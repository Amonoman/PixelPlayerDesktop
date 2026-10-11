package com.kayoi.pixelplayerdesktop.data.network.deezer

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
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap

object DeezerClient {

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

    suspend fun getArtistImageUrl(
        artistName: String,
        fetchEnabled: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext null

        val normalized = artistName.trim().lowercase()

        if (memoryCache.containsKey(normalized)) {
            return@withContext memoryCache[normalized]
        }

        val diskFile = getCacheFile(normalized)
        if (diskFile.exists() && diskFile.length() > 0) {
            val path = diskFile.absolutePath
            memoryCache[normalized] = path
            return@withContext path
        }

        if (!fetchEnabled || failedArtists.contains(normalized)) {
            return@withContext null
        }

        try {
            val encodedName = URLEncoder.encode(artistName.trim(), "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedName&limit=5"
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

                val hasNonAscii = artistName.any { it.code > 127 }
                val matchedArtist = if (hasNonAscii) {
                    dataArray?.asSequence()
                        ?.mapNotNull { it.jsonObject }
                        ?.firstOrNull { artistObj ->
                            val returnedName = artistObj["name"]?.jsonPrimitive?.content?.trim()
                            returnedName?.equals(artistName.trim(), ignoreCase = true) == true
                        } ?: dataArray?.firstOrNull()?.jsonObject
                } else {
                    dataArray?.asSequence()
                        ?.mapNotNull { it.jsonObject }
                        ?.firstOrNull { artistObj ->
                            val returnedName = artistObj["name"]?.jsonPrimitive?.content?.trim()
                            returnedName?.equals(artistName.trim(), ignoreCase = true) == true
                        }
                }

                val pictureUrl = matchedArtist?.get("picture_big")?.jsonPrimitive?.content
                    ?: matchedArtist?.get("picture_medium")?.jsonPrimitive?.content
                    ?: matchedArtist?.get("picture")?.jsonPrimitive?.content

                val isPlaceholder = pictureUrl?.contains("/artist//") == true ||
                    pictureUrl?.contains("2a96cbd8b46e442fc41c2b86b821562f") == true

                if (!pictureUrl.isNullOrBlank() && !isPlaceholder) {
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
            null
        } catch (_: Exception) {
            failedArtists.add(normalized)
            null
        }
    }

    private fun getCacheFile(normalizedName: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest(normalizedName.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "artist_$hash.jpg")
    }

    fun clearCache() {
        memoryCache.clear()
        failedArtists.clear()
    }
}
