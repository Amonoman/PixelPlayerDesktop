package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
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

    fun getCachedColorScheme(
        pathOrUri: String?,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        if (pathOrUri.isNullOrBlank()) return null
        val cleanPath = if (pathOrUri.startsWith("file://")) pathOrUri.removePrefix("file://") else pathOrUri
        val file = File(cleanPath)
        if (!file.exists() || !file.canRead()) return null

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
        val cleanPath = if (pathOrUri.startsWith("file://")) pathOrUri.removePrefix("file://") else pathOrUri
        return extractColorScheme(File(cleanPath), paletteStyle, colorAccuracyLevel)
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
