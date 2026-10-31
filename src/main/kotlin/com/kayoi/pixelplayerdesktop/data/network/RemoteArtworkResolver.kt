package com.kayoi.pixelplayerdesktop.data.network

import com.kayoi.pixelplayerdesktop.domain.repository.JellyfinRepository
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

class RemoteArtworkResolver(
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val okHttpClient: OkHttpClient,
    private val cacheDir: File = DesktopPathProvider.artworkCacheDir
) {

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    suspend fun resolveArtworkBytes(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        if (uri.isBlank()) return@withContext null

        val cleanUri = if (uri.startsWith("file://")) uri.removePrefix("file://") else uri
        val filePath = cleanUri.substringBefore("?")
        val localFile = File(filePath)
        if (localFile.exists() && localFile.isFile && localFile.length() > 0) {
            return@withContext runCatching { localFile.readBytes() }.getOrNull()
        }

        val cacheFile = when {
            uri.startsWith("navidrome_cover://") -> {
                val coverId = uri.removePrefix("navidrome_cover://")
                File(cacheDir, "nd_${md5(coverId)}.jpg")
            }
            uri.startsWith("jellyfin_cover://") -> {
                val itemId = uri.removePrefix("jellyfin_cover://")
                File(cacheDir, "jf_${md5(itemId)}.jpg")
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                File(cacheDir, "url_${md5(uri)}.jpg")
            }
            else -> null
        }

        if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext runCatching { cacheFile.readBytes() }.getOrNull()
        }

        val targetUrl = when {
            uri.startsWith("navidrome_cover://") -> {
                val coverId = uri.removePrefix("navidrome_cover://")
                navidromeRepository.getCoverArtUrl(coverId)
            }
            uri.startsWith("jellyfin_cover://") -> {
                val itemId = uri.removePrefix("jellyfin_cover://")
                jellyfinRepository.getImageUrl(itemId)
            }
            uri.startsWith("http://") || uri.startsWith("https://") -> uri
            else -> null
        } ?: return@withContext null

        downloadAndCache(targetUrl, cacheFile)
    }

    private fun downloadAndCache(url: String, cacheFile: File?): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (cacheFile != null) {
                    runCatching {
                        val temp = File.createTempFile("art_", ".tmp", cacheDir)
                        temp.writeBytes(bytes)
                        temp.renameTo(cacheFile)
                    }
                }
                bytes
            }
        } catch (_: Exception) {
            null
        }
    }
}
