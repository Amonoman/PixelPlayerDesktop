package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.ui.graphics.Color
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

object DynamicColorProcessor {

    @Volatile
    var defaultPaletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default

    @Volatile
    var defaultColorAccuracyLevel: Int = AlbumArtColorAccuracy.DEFAULT

    private val cache = mutableMapOf<String, ColorSchemePair>()

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
        val cacheKey = "${file.absolutePath}@${file.lastModified()}|${paletteStyle.storageKey}|acc_$colorAccuracyLevel"
        cache[cacheKey]?.let { return it }

        return try {
            val image = ImageIO.read(file) ?: return null
            val result = extractFromBufferedImage(image, paletteStyle, colorAccuracyLevel)
            cache[cacheKey] = result
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
        cache.clear()
    }
}
