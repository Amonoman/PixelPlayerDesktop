package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext

import com.kayoi.pixelplayerdesktop.data.network.RemoteArtworkResolver
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider

object DynamicColorProcessor {

    private const val MAX_MEMORY_CACHE_ENTRIES = 256

    @Volatile
    var defaultPaletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default

    @Volatile
    var defaultColorAccuracyLevel: Int = AlbumArtColorAccuracy.DEFAULT

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val diskCacheFile: File by lazy {
        File(DesktopPathProvider.cacheDir, "palette_seed_cache.json")
    }

    private val memoryCacheLock = Any()
    private val memoryCache = object : LinkedHashMap<String, ColorSchemePair>(MAX_MEMORY_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, ColorSchemePair>?): Boolean {
            return size > MAX_MEMORY_CACHE_ENTRIES
        }
    }
    private val seedDiskCache = ConcurrentHashMap<String, Int>()
    private val extractionSemaphore = Semaphore(4)
    private val inFlightJobs = ConcurrentHashMap<String, Deferred<ColorSchemePair?>>()

    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        loadDiskCache()
    }

    internal fun memoryCacheSize(): Int = synchronized(memoryCacheLock) { memoryCache.size }

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
            saveMutex.withLock {
                saveJob?.cancel()
                saveJob = scope.launch {
                    delay(1000)
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
        synchronized(memoryCacheLock) {
            memoryCache[fullKey]?.let { return it }
        }

        val seedKey = buildSeedKey(file, colorAccuracyLevel)
        val cachedArgb = seedDiskCache[seedKey] ?: return null
        val scheme = generateColorSchemeFromSeed(Color(cachedArgb), paletteStyle)
        synchronized(memoryCacheLock) {
            memoryCache[fullKey] = scheme
        }
        return scheme
    }

    suspend fun extractColorScheme(
        pathOrUri: String?,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? = extractColorSchemeAsync(pathOrUri, paletteStyle, colorAccuracyLevel)

    suspend fun extractColorSchemeAsync(
        pathOrUri: String?,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? = withContext(Dispatchers.IO) {
        if (pathOrUri.isNullOrBlank()) return@withContext null
        getCachedColorScheme(pathOrUri, paletteStyle, colorAccuracyLevel)?.let { return@withContext it }

        val cacheKey = "$pathOrUri|${paletteStyle.storageKey}|$colorAccuracyLevel"
        val deferred = inFlightJobs.computeIfAbsent(cacheKey) {
            scope.async(Dispatchers.IO) {
                extractionSemaphore.withPermit {
                    try {
                        extractColorSchemeInternal(pathOrUri, paletteStyle, colorAccuracyLevel)
                    } finally {
                        inFlightJobs.remove(cacheKey)
                    }
                }
            }
        }
        deferred.await()
    }

    private suspend fun extractColorSchemeInternal(
        pathOrUri: String,
        paletteStyle: AlbumArtPaletteStyle,
        colorAccuracyLevel: Int
    ): ColorSchemePair? {
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
                val bytes = resolver?.resolveArtworkBytes(pathOrUri)

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

    private fun readSampledImage(file: File, maxDimension: Int = 128): BufferedImage? {
        return try {
            val iis = ImageIO.createImageInputStream(file) ?: return null
            val readers = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) {
                iis.close()
                return null
            }
            val reader = readers.next()
            try {
                reader.input = iis
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val maxDim = maxOf(width, height)
                val sampleSize = maxOf(1, maxDim / maxDimension)
                val param = reader.defaultReadParam
                param.setSourceSubsampling(sampleSize, sampleSize, 0, 0)
                reader.read(0, param)
            } finally {
                reader.dispose()
                iis.close()
            }
        } catch (_: Exception) {
            runCatching { ImageIO.read(file) }.getOrNull()
        }
    }

    private fun readSampledImage(bytes: ByteArray, maxDimension: Int = 128): BufferedImage? {
        return try {
            val iis = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return null
            val readers = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) {
                iis.close()
                return null
            }
            val reader = readers.next()
            try {
                reader.input = iis
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val maxDim = maxOf(width, height)
                val sampleSize = maxOf(1, maxDim / maxDimension)
                val param = reader.defaultReadParam
                param.setSourceSubsampling(sampleSize, sampleSize, 0, 0)
                reader.read(0, param)
            } finally {
                reader.dispose()
                iis.close()
            }
        } catch (_: Exception) {
            runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
        }
    }

    fun extractColorScheme(
        file: File,
        paletteStyle: AlbumArtPaletteStyle = defaultPaletteStyle,
        colorAccuracyLevel: Int = defaultColorAccuracyLevel
    ): ColorSchemePair? {
        if (!file.exists() || !file.canRead()) return null

        getCachedColorScheme(file.absolutePath, paletteStyle, colorAccuracyLevel)?.let { return it }

        return try {
            val image = readSampledImage(file, 128) ?: return null
            val config = ColorExtractionConfig(accuracyLevel = colorAccuracyLevel)
            val seedColor = extractSeedColorFromImage(image, config)
            val seedArgb = seedColor.toArgb()

            val seedKey = buildSeedKey(file, colorAccuracyLevel)
            seedDiskCache[seedKey] = seedArgb
            scheduleSaveToDisk()

            val result = generateColorSchemeFromSeed(seedColor, paletteStyle)
            val fullKey = buildFullKey(file, paletteStyle, colorAccuracyLevel)
            synchronized(memoryCacheLock) {
                memoryCache[fullKey] = result
            }
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
            val image = readSampledImage(bytes, 128) ?: return null
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
        synchronized(memoryCacheLock) {
            memoryCache.clear()
        }
        seedDiskCache.clear()
        runCatching {
            if (diskCacheFile.exists()) {
                diskCacheFile.delete()
            }
        }
    }
}
