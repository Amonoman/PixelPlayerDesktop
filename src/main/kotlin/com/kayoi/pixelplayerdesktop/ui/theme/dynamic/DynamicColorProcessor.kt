package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kayoi.pixelplayerdesktop.data.network.RemoteArtworkResolver
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object DynamicColorProcessor {

    @Volatile
    var defaultPaletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default

    @Volatile
    var defaultColorAccuracyLevel: Int = AlbumArtColorAccuracy.DEFAULT

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val diskCacheFile: File by lazy {
        File(DesktopPathProvider.cacheDir, "palette_seed_cache.json")
    }

    private val memoryCache = ConcurrentHashMap<String, ColorSchemePair>()
    private val seedDiskCache = ConcurrentHashMap<String, Int>()

    init {
        loadDiskCache()
    }

    private fun loadDiskCache() {
        runCatching {
            if (diskCacheFile.exists() && diskCacheFile.canRead()) {
                val content = diskCacheFile.readText()
                if (content.isNotBlank()) {
                    val map = json.decodeFromString<Map<String, Int>>(content)
                    seedDiskCache.putAll(map)
                }
            }
        }
    }

    private fun scheduleSaveToDisk() {
        scope.launch {
            runCatching {
                val snapshot = seedDiskCache.toMap()
                val tempFile = File(diskCacheFile.parentFile, "${diskCacheFile.name}.tmp")
                tempFile.writeText(json.encodeToString(snapshot))
                if (tempFile.exists()) {
                    if (diskCacheFile.exists()) diskCacheFile.delete()
                    tempFile.renameTo(diskCacheFile)
                }
            }
        }
    }

    private fun buildSeedKey(file: File, colorAccuracyLevel: Int): String {
        return "${file.absolutePath}@${file.lastModified()}|acc_$colorAccuracyLevel"
    }

    private fun buildFullKey(file: File, paletteStyle: AlbumArtPaletteStyle, colorAccuracyLevel: Int): String {
        return "${file.absolutePath}@${file.lastModified()}|${paletteStyle.storageKey}|acc_$colorAccuracyLevel"
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun resolveFile(pathOrUri: String?): File? {
        if (pathOrUri.isNullOrBlank()) return null
        val cleanPath = if (pathOrUri.startsWith("file://")) pathOrUri.removePrefix("file://") else pathOrUri
        val directFile = File(cleanPath)
        if (directFile.exists() && directFile.canRead() && directFile.length() > 0) return directFile

        val cacheDir = DesktopPathProvider.artworkCacheDir
        val cacheFile = when {
            pathOrUri.startsWith("navidrome_cover://") -> {
                val coverId = pathOrUri.removePrefix("navidrome_cover://")
                File(cacheDir, "nd_${md5(coverId)}.jpg")
            }
            pathOrUri.startsWith("jellyfin_cover://") -> {
                val itemId = pathOrUri.removePrefix("jellyfin_cover://")
                File(cacheDir, "jf_${md5(itemId)}.jpg")
            }
            pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://") -> {
                File(cacheDir, "url_${md5(pathOrUri)}.jpg")
            }
            else -> null
        }
        if (cacheFile != null && cacheFile.exists() && cacheFile.canRead() && cacheFile.length() > 0) {
            return cacheFile
        }
        return null
    }

    fun getCachedColorScheme(
        pathOrUri: String?,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        val file = resolveFile(pathOrUri) ?: return null
        val fullKey = buildFullKey(file, paletteStyle, colorAccuracyLevel)
        memoryCache[fullKey]?.let { return it }

        val seedKey = buildSeedKey(file, colorAccuracyLevel)
        val cachedArgb = seedDiskCache[seedKey] ?: return null
        val scheme = generateColorSchemeFromSeed(Color(cachedArgb), paletteStyle)
        memoryCache[fullKey] = scheme
        return scheme
    }

    fun extractColorScheme(
        pathOrUri: String?,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        if (pathOrUri.isNullOrBlank()) return null
        val file = resolveFile(pathOrUri)
        if (file != null) {
            return extractColorScheme(file, paletteStyle, colorAccuracyLevel)
        }

        if (pathOrUri.startsWith("navidrome_cover://") ||
            pathOrUri.startsWith("jellyfin_cover://") ||
            pathOrUri.startsWith("http://") ||
            pathOrUri.startsWith("https://")
        ) {
            return try {
                val resolver = GlobalContext.getOrNull()
                    ?.getOrNull<RemoteArtworkResolver>()
                val bytes = if (resolver != null) {
                    runBlocking {
                        resolver.resolveArtworkBytes(pathOrUri)
                    }
                } else null

                if (bytes != null && bytes.isNotEmpty()) {
                    val resolvedFile = resolveFile(pathOrUri)
                    if (resolvedFile != null) {
                        extractColorScheme(resolvedFile, paletteStyle, colorAccuracyLevel)
                    } else {
                        extractColorScheme(bytes, paletteStyle, colorAccuracyLevel)
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    fun extractColorScheme(
        file: File,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        if (!file.exists() || !file.canRead()) return null

        getCachedColorScheme(file.absolutePath, paletteStyle, colorAccuracyLevel)?.let { return it }

        return try {
            val image = ImageIO.read(file) ?: return null
            val config = ColorExtractionConfig(accuracyLevel = colorAccuracyLevel)
            val seedColor = extractSeedColorFromImage(image, config)
            val seedArgb = seedColor.toArgb()

            val seedKey = buildSeedKey(file, colorAccuracyLevel)
            seedDiskCache[seedKey] = seedArgb
            scheduleSaveToDisk()

            val result = generateColorSchemeFromSeed(seedColor, paletteStyle)
            val fullKey = buildFullKey(file, paletteStyle, colorAccuracyLevel)
            memoryCache[fullKey] = result
            result
        } catch (e: Exception) {
            null
        }
    }

    fun extractColorScheme(
        bytes: ByteArray,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        if (bytes.isEmpty()) return null
        return try {
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
            extractFromBufferedImage(image, paletteStyle, colorAccuracyLevel)
        } catch (e: Exception) {
            null
        }
    }

    fun extractFromBufferedImage(
        image: BufferedImage,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair {
        val config = ColorExtractionConfig(accuracyLevel = colorAccuracyLevel)
        val seedColor = extractSeedColorFromImage(image, config)
        return generateColorSchemeFromSeed(seedColor, paletteStyle)
    }

    fun generateColorSchemePair(
        seed: Color,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle
    ): ColorSchemePair {
        return generateColorSchemeFromSeed(seed, paletteStyle)
    }

    fun clearCache() {
        memoryCache.clear()
        seedDiskCache.clear()
        runCatching {
            if (diskCacheFile.exists()) {
                diskCacheFile.delete()
            }
        }
    }
}
