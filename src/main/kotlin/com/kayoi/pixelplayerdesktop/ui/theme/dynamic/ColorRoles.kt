package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.ui.theme.DarkColorScheme
import com.kayoi.pixelplayerdesktop.ui.theme.LightColorScheme
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeFruitSalad
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class ColorSchemePair(
    val light: ColorScheme,
    val dark: ColorScheme,
    val seedColor: Color
)

data class ColorScoringConfig(
    val targetChroma: Double = 48.0,
    val weightProportion: Double = 0.7,
    val weightChromaAbove: Double = 0.3,
    val weightChromaBelow: Double = 0.1,
    val cutoffChroma: Double = 5.0,
    val cutoffExcitedProportion: Double = 0.01,
    val maxColorCount: Int = 4,
    val maxHueDifference: Int = 90,
    val minHueDifference: Int = 15
)

data class ColorExtractionConfig(
    val downscaleMaxDimension: Int = 128,
    val quantizerMaxColors: Int = 128,
    val scoring: ColorScoringConfig = ColorScoringConfig(),
    val accuracyLevel: Int = AlbumArtColorAccuracy.DEFAULT
) {
    val normalizedAccuracy: Double
        get() = AlbumArtColorAccuracy.clamp(accuracyLevel).toDouble() / AlbumArtColorAccuracy.MAX.toDouble()
}

private data class ScoredHct(
    val hct: Hct,
    val score: Double
)

private data class RepresentativeArtworkColor(
    val argb: Int,
    val hct: Hct
)

private const val GRAYSCALE_CHROMA_THRESHOLD = 12.0
private const val NEUTRAL_PIXEL_CHROMA_THRESHOLD = 8.0
private const val HIGH_CHROMA_THRESHOLD = 18.0
private const val REQUIRED_NEUTRAL_POPULATION = 0.92
private const val MAX_HIGH_CHROMA_POPULATION = 0.03
private const val MAX_WEIGHTED_CHROMA_FOR_NEUTRAL = 9.0
private const val MAX_GRAYSCALE_CHANNEL_DELTA = 10
private const val REPRESENTATIVE_PIXEL_CHROMA_THRESHOLD = 10.0
private const val MIN_REPRESENTATIVE_PIXEL_RATIO = 0.04
private const val MIN_REFINEMENT_PIXEL_RATIO = 0.08
private const val MIN_VISIBLE_RGB_SUM = 36
private const val MIN_VISIBLE_PIXEL_ALPHA = 28
private const val FIDELITY_HUE_WINDOW = 90.0
private const val FIDELITY_CHROMA_WINDOW = 32.0
private const val FIDELITY_TONE_WINDOW = 28.0
private const val FIDELITY_HUE_WEIGHT = 18.0
private const val FIDELITY_CHROMA_WEIGHT = 7.0
private const val FIDELITY_TONE_WEIGHT = 3.0
private const val ACCURATE_FIDELITY_HUE_WINDOW = 52.0
private const val ACCURATE_FIDELITY_CHROMA_WINDOW = 18.0
private const val ACCURATE_FIDELITY_TONE_WINDOW = 18.0
private const val ACCURATE_FIDELITY_HUE_WEIGHT = 28.0
private const val ACCURATE_FIDELITY_CHROMA_WEIGHT = 14.0
private const val ACCURATE_FIDELITY_TONE_WEIGHT = 6.0
private const val EXCESS_CHROMA_PENALTY_START = 18.0
private const val EXCESS_CHROMA_PENALTY_WEIGHT = 0.18
private const val ACCURATE_EXCESS_CHROMA_PENALTY_START = 8.0
private const val ACCURATE_EXCESS_CHROMA_PENALTY_WEIGHT = 0.38
private const val LOCAL_REFINEMENT_HUE_WINDOW = 32.0
private const val LOCAL_REFINEMENT_BLEND_RATIO = 0.42f
private const val ACCURATE_LOCAL_REFINEMENT_HUE_WINDOW = 18.0
private const val ACCURATE_LOCAL_REFINEMENT_BLEND_RATIO = 0.72f
private const val ACCURATE_REPRESENTATIVE_BLEND_RATIO = 0.42f
private const val ACCURATE_REPRESENTATIVE_PIXEL_CHROMA_THRESHOLD = 6.0
private const val ACCURATE_CHROMA_ABOVE_WEIGHT = 0.14
private const val ACCURATE_CHROMA_BELOW_WEIGHT = 0.04
private const val ACCURATE_CUTOFF_CHROMA = 3.5
private const val ACCURATE_MAX_HUE_DIFFERENCE = 64
private const val ACCURATE_MIN_HUE_DIFFERENCE = 10

fun extractSeedColorFromImage(
    image: BufferedImage,
    config: ColorExtractionConfig = ColorExtractionConfig()
): Color {
    val scaled = resizeForExtraction(image, config.downscaleMaxDimension)
    val width = scaled.width
    val height = scaled.height
    val pixels = IntArray(width * height)
    scaled.getRGB(0, 0, width, height, pixels, 0, width)

    val seedArgb = runCatching {
        selectSeedColorArgbFromPixels(pixels, config)
    }.getOrElse { DarkColorScheme.primary.toArgb() }

    return Color(seedArgb)
}

fun generateColorSchemeFromSeed(
    seedColor: Color,
    paletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default
): ColorSchemePair {
    return runCatching {
        val seedArgb = seedColor.toArgb()
        val sourceHct = Hct.fromInt(seedArgb)
        val shouldForceNeutral = shouldUseNeutralArtworkScheme(seedArgb, sourceHct)

        val lightScheme = createDynamicScheme(
            sourceHct = sourceHct,
            paletteStyle = paletteStyle,
            isDark = false
        ).toComposeColorScheme(isDark = false)
        val darkScheme = createDynamicScheme(
            sourceHct = sourceHct,
            paletteStyle = paletteStyle,
            isDark = true
        ).toComposeColorScheme(isDark = true)

        if (shouldForceNeutral) {
            ColorSchemePair(
                light = lightScheme.toGrayscaleColorScheme(),
                dark = darkScheme.toGrayscaleColorScheme(),
                seedColor = seedColor
            )
        } else {
            ColorSchemePair(
                light = lightScheme,
                dark = darkScheme,
                seedColor = seedColor
            )
        }
    }.getOrElse {
        ColorSchemePair(
            light = LightColorScheme,
            dark = DarkColorScheme,
            seedColor = seedColor
        )
    }
}

fun generateMonochromeColorSchemeFromSeed(seedColor: Color): ColorSchemePair {
    return runCatching {
        val sourceHct = Hct.fromInt(seedColor.toArgb())
        val lightScheme = SchemeMonochrome(sourceHct, false, 0.0).toComposeColorScheme(isDark = false)
        val darkScheme = SchemeMonochrome(sourceHct, true, 0.0).toComposeColorScheme(isDark = true)
        ColorSchemePair(
            light = lightScheme,
            dark = darkScheme,
            seedColor = seedColor
        )
    }.getOrElse {
        ColorSchemePair(
            light = LightColorScheme,
            dark = DarkColorScheme,
            seedColor = seedColor
        )
    }
}

private fun createDynamicScheme(
    sourceHct: Hct,
    paletteStyle: AlbumArtPaletteStyle,
    isDark: Boolean
): DynamicScheme {
    return when (paletteStyle) {
        AlbumArtPaletteStyle.TONAL_SPOT -> SchemeTonalSpot(sourceHct, isDark, 0.0)
        AlbumArtPaletteStyle.VIBRANT -> SchemeVibrant(sourceHct, isDark, 0.0)
        AlbumArtPaletteStyle.EXPRESSIVE -> SchemeExpressive(sourceHct, isDark, 0.0)
        AlbumArtPaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(sourceHct, isDark, 0.0)
    }
}

internal fun selectSeedColorArgbFromPixels(
    pixels: IntArray,
    config: ColorExtractionConfig = ColorExtractionConfig()
): Int {
    val fallbackArgb = averageColorArgb(pixels)
    val quantized = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)
    val mostlyNeutralArtwork = isMostlyNeutralArtwork(quantized)

    if (mostlyNeutralArtwork && isArgbNearGrayscale(fallbackArgb)) {
        return fallbackArgb
    }

    val representativeColor = calculateRepresentativeArtworkColor(
        pixels = pixels,
        accuracy = config.normalizedAccuracy
    )
    val rankedSeeds = scoreQuantizedColors(
        colorsToPopulation = quantized,
        scoring = config.scoring,
        fallbackColorArgb = fallbackArgb,
        representativeColor = representativeColor,
        accuracy = config.normalizedAccuracy
    )
    val selectedSeed = rankedSeeds.firstOrNull() ?: fallbackArgb

    return refineSeedColorArgb(
        candidateArgb = selectedSeed,
        pixels = pixels,
        representativeColor = representativeColor,
        cutoffChroma = config.scoring.cutoffChroma,
        accuracy = config.normalizedAccuracy
    )
}

private fun resizeForExtraction(image: BufferedImage, maxDimension: Int): BufferedImage {
    if (maxDimension <= 0) return image
    if (image.width <= maxDimension && image.height <= maxDimension) return image
    val scale = maxDimension.toDouble() / max(image.width, image.height).toDouble()
    val targetWidth = max(1, (image.width * scale).roundToInt())
    val targetHeight = max(1, (image.height * scale).roundToInt())

    val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    g.drawImage(image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH), 0, 0, null)
    g.dispose()
    return scaled
}

private fun scoreQuantizedColors(
    colorsToPopulation: Map<Int, Int>,
    scoring: ColorScoringConfig,
    fallbackColorArgb: Int,
    representativeColor: RepresentativeArtworkColor?,
    accuracy: Double
): List<Int> {
    if (colorsToPopulation.isEmpty()) return listOf(fallbackColorArgb)

    val colorsHct = ArrayList<Hct>(colorsToPopulation.size)
    val huePopulation = IntArray(360)
    var populationSum = 0.0

    for ((argb, population) in colorsToPopulation) {
        if (population <= 0) continue
        val hct = Hct.fromInt(argb)
        colorsHct.add(hct)
        val hue = sanitizeDegreesInt(floor(hct.hue).toInt())
        huePopulation[hue] += population
        populationSum += population.toDouble()
    }

    if (populationSum <= 0.0) return listOf(fallbackColorArgb)

    val effectiveCutoffChroma = lerpDouble(scoring.cutoffChroma, ACCURATE_CUTOFF_CHROMA, accuracy)
    val effectiveTargetChroma = representativeColor?.let { representative ->
        lerpDouble(scoring.targetChroma, representative.hct.chroma.coerceIn(12.0, 72.0), accuracy * 0.92)
    } ?: scoring.targetChroma
    val chromaAboveWeight = lerpDouble(scoring.weightChromaAbove, ACCURATE_CHROMA_ABOVE_WEIGHT, accuracy)
    val chromaBelowWeight = lerpDouble(scoring.weightChromaBelow, ACCURATE_CHROMA_BELOW_WEIGHT, accuracy)

    val hueExcitedProportions = DoubleArray(360)
    for (hue in 0 until 360) {
        val proportion = huePopulation[hue] / populationSum
        for (neighbor in hue - 14..hue + 15) {
            val wrappedHue = sanitizeDegreesInt(neighbor)
            hueExcitedProportions[wrappedHue] += proportion
        }
    }

    val scoredColors = ArrayList<ScoredHct>(colorsHct.size)
    for (hct in colorsHct) {
        val hue = sanitizeDegreesInt(hct.hue.roundToInt())
        val excitedProportion = hueExcitedProportions[hue]
        if (hct.chroma < effectiveCutoffChroma || excitedProportion <= scoring.cutoffExcitedProportion) {
            continue
        }

        val proportionScore = excitedProportion * 100.0 * scoring.weightProportion
        val chromaWeight =
            if (hct.chroma < effectiveTargetChroma) chromaBelowWeight else chromaAboveWeight
        val chromaScore = (hct.chroma - effectiveTargetChroma) * chromaWeight
        val fidelityScore = representativeColor?.let { representative ->
            calculateRepresentativeFidelityScore(hct, representative.hct, accuracy)
        } ?: 0.0
        val excessChromaPenalty = representativeColor?.let { representative ->
            calculateExcessChromaPenalty(hct, representative.hct, accuracy)
        } ?: 0.0
        scoredColors.add(
            ScoredHct(
                hct = hct,
                score = proportionScore + chromaScore + fidelityScore - excessChromaPenalty
            )
        )
    }

    if (scoredColors.isEmpty()) return listOf(fallbackColorArgb)
    scoredColors.sortByDescending { it.score }

    val minHueDifference = lerpDouble(
        scoring.minHueDifference.toDouble(),
        ACCURATE_MIN_HUE_DIFFERENCE.toDouble(),
        accuracy
    ).roundToInt().coerceAtLeast(1)
    val maxHueDifference = lerpDouble(
        scoring.maxHueDifference.toDouble(),
        ACCURATE_MAX_HUE_DIFFERENCE.toDouble(),
        accuracy
    ).roundToInt().coerceAtLeast(minHueDifference)
    val desiredColorCount = scoring.maxColorCount.coerceAtLeast(1)
    val chosen = mutableListOf<Hct>()

    for (differenceDegrees in maxHueDifference downTo minHueDifference) {
        chosen.clear()
        for (candidate in scoredColors) {
            val isDuplicateHue = chosen.any {
                differenceDegrees(candidate.hct.hue, it.hue) < differenceDegrees.toDouble()
            }
            if (!isDuplicateHue) {
                chosen.add(candidate.hct)
            }
            if (chosen.size >= desiredColorCount) break
        }
        if (chosen.size >= desiredColorCount) break
    }

    if (chosen.isEmpty()) return listOf(fallbackColorArgb)
    return chosen.map { it.toInt() }
}

private fun calculateRepresentativeArtworkColor(
    pixels: IntArray,
    accuracy: Double
): RepresentativeArtworkColor? {
    if (pixels.isEmpty()) return null

    var totalRed = 0.0
    var totalGreen = 0.0
    var totalBlue = 0.0
    var totalWeight = 0.0
    var representativePixelCount = 0
    val chromaThreshold = lerpDouble(
        REPRESENTATIVE_PIXEL_CHROMA_THRESHOLD,
        ACCURATE_REPRESENTATIVE_PIXEL_CHROMA_THRESHOLD,
        accuracy
    )
    val chromaWeightMultiplier = lerpDouble(1.0, 0.42, accuracy)

    for (argb in pixels) {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < MIN_VISIBLE_PIXEL_ALPHA) continue

        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        if (red + green + blue <= MIN_VISIBLE_RGB_SUM) continue

        val hct = Hct.fromInt(argb)
        if (hct.chroma < chromaThreshold) continue

        val weight = 1.0 +
            (((hct.chroma - chromaThreshold) / 24.0).coerceAtLeast(0.0) * chromaWeightMultiplier) +
            (hct.tone / 100.0)

        totalRed += red * weight
        totalGreen += green * weight
        totalBlue += blue * weight
        totalWeight += weight
        representativePixelCount++
    }

    if (totalWeight <= 0.0) return null
    if (representativePixelCount.toDouble() / pixels.size.toDouble() < MIN_REPRESENTATIVE_PIXEL_RATIO) {
        return null
    }

    val argb = (
        (0xFF shl 24) or
            ((totalRed / totalWeight).roundToInt().coerceIn(0, 255) shl 16) or
            ((totalGreen / totalWeight).roundToInt().coerceIn(0, 255) shl 8) or
            (totalBlue / totalWeight).roundToInt().coerceIn(0, 255)
        )
    val hct = Hct.fromInt(argb)

    return RepresentativeArtworkColor(argb = argb, hct = hct)
}

private fun calculateRepresentativeFidelityScore(
    candidate: Hct,
    representative: Hct,
    accuracy: Double
): Double {
    val hueDistance = differenceDegrees(candidate.hue, representative.hue)
    val chromaDistance = abs(candidate.chroma - representative.chroma)
    val toneDistance = abs(candidate.tone - representative.tone)
    val hueWindow = lerpDouble(FIDELITY_HUE_WINDOW, ACCURATE_FIDELITY_HUE_WINDOW, accuracy)
    val chromaWindow = lerpDouble(FIDELITY_CHROMA_WINDOW, ACCURATE_FIDELITY_CHROMA_WINDOW, accuracy)
    val toneWindow = lerpDouble(FIDELITY_TONE_WINDOW, ACCURATE_FIDELITY_TONE_WINDOW, accuracy)
    val hueWeight = lerpDouble(FIDELITY_HUE_WEIGHT, ACCURATE_FIDELITY_HUE_WEIGHT, accuracy)
    val chromaWeight = lerpDouble(FIDELITY_CHROMA_WEIGHT, ACCURATE_FIDELITY_CHROMA_WEIGHT, accuracy)
    val toneWeight = lerpDouble(FIDELITY_TONE_WEIGHT, ACCURATE_FIDELITY_TONE_WEIGHT, accuracy)

    val hueScore =
        ((hueWindow - hueDistance).coerceAtLeast(0.0) / hueWindow) * hueWeight
    val chromaScore =
        ((chromaWindow - chromaDistance).coerceAtLeast(0.0) / chromaWindow) * chromaWeight
    val toneScore =
        ((toneWindow - toneDistance).coerceAtLeast(0.0) / toneWindow) * toneWeight

    return hueScore + chromaScore + toneScore
}

private fun calculateExcessChromaPenalty(
    candidate: Hct,
    representative: Hct,
    accuracy: Double
): Double {
    val penaltyStart = lerpDouble(EXCESS_CHROMA_PENALTY_START, ACCURATE_EXCESS_CHROMA_PENALTY_START, accuracy)
    val penaltyWeight = lerpDouble(EXCESS_CHROMA_PENALTY_WEIGHT, ACCURATE_EXCESS_CHROMA_PENALTY_WEIGHT, accuracy)
    val excessChroma = candidate.chroma - representative.chroma - penaltyStart
    if (excessChroma <= 0.0) return 0.0
    return excessChroma * penaltyWeight
}

private fun refineSeedColorArgb(
    candidateArgb: Int,
    pixels: IntArray,
    representativeColor: RepresentativeArtworkColor?,
    cutoffChroma: Double,
    accuracy: Double
): Int {
    if (pixels.isEmpty()) return candidateArgb

    val candidateHct = Hct.fromInt(candidateArgb)
    val effectiveCutoffChroma = lerpDouble(cutoffChroma, ACCURATE_CUTOFF_CHROMA, accuracy)
    val localHueWindow = lerpDouble(LOCAL_REFINEMENT_HUE_WINDOW, ACCURATE_LOCAL_REFINEMENT_HUE_WINDOW, accuracy)
    val localBlendRatio = lerpFloat(
        LOCAL_REFINEMENT_BLEND_RATIO,
        ACCURATE_LOCAL_REFINEMENT_BLEND_RATIO,
        accuracy.toFloat()
    )
    val representativeBlendRatio = lerpFloat(
        LOCAL_REFINEMENT_BLEND_RATIO / 2f,
        ACCURATE_REPRESENTATIVE_BLEND_RATIO,
        accuracy.toFloat()
    )
    var totalRed = 0.0
    var totalGreen = 0.0
    var totalBlue = 0.0
    var totalWeight = 0.0
    var matchingPixelCount = 0

    for (argb in pixels) {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha < MIN_VISIBLE_PIXEL_ALPHA) continue

        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        if (red + green + blue <= MIN_VISIBLE_RGB_SUM) continue

        val hct = Hct.fromInt(argb)
        if (hct.chroma < effectiveCutoffChroma) continue

        val hueDistance = differenceDegrees(candidateHct.hue, hct.hue)
        if (hueDistance > localHueWindow) continue

        val weight = 1.0 +
            ((localHueWindow - hueDistance) / localHueWindow) +
            ((hct.chroma - effectiveCutoffChroma) / 32.0).coerceAtLeast(0.0)

        totalRed += red * weight
        totalGreen += green * weight
        totalBlue += blue * weight
        totalWeight += weight
        matchingPixelCount++
    }

    if (totalWeight <= 0.0) return candidateArgb
    if (matchingPixelCount.toDouble() / pixels.size.toDouble() < MIN_REFINEMENT_PIXEL_RATIO) {
        return candidateArgb
    }

    val localAverageArgb = (
        (0xFF shl 24) or
            ((totalRed / totalWeight).roundToInt().coerceIn(0, 255) shl 16) or
            ((totalGreen / totalWeight).roundToInt().coerceIn(0, 255) shl 8) or
            (totalBlue / totalWeight).roundToInt().coerceIn(0, 255)
        )
    val localAverageHct = Hct.fromInt(localAverageArgb)
    if (differenceDegrees(candidateHct.hue, localAverageHct.hue) > localHueWindow) {
        return candidateArgb
    }

    val refinedArgb = blendArgb(candidateArgb, localAverageArgb, localBlendRatio)
    if (representativeColor == null) return refinedArgb

    val fidelityHueWindow = lerpDouble(FIDELITY_HUE_WINDOW, ACCURATE_FIDELITY_HUE_WINDOW, accuracy)
    return if (differenceDegrees(localAverageHct.hue, representativeColor.hct.hue) <= fidelityHueWindow) {
        blendArgb(refinedArgb, representativeColor.argb, representativeBlendRatio)
    } else {
        refinedArgb
    }
}

private fun blendArgb(firstArgb: Int, secondArgb: Int, ratio: Float): Int {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    val inverseRatio = 1f - clampedRatio

    val alpha = (
        ((firstArgb ushr 24) and 0xFF) * inverseRatio +
            ((secondArgb ushr 24) and 0xFF) * clampedRatio
        ).roundToInt().coerceIn(0, 255)
    val red = (
        ((firstArgb ushr 16) and 0xFF) * inverseRatio +
            ((secondArgb ushr 16) and 0xFF) * clampedRatio
        ).roundToInt().coerceIn(0, 255)
    val green = (
        ((firstArgb ushr 8) and 0xFF) * inverseRatio +
            ((secondArgb ushr 8) and 0xFF) * clampedRatio
        ).roundToInt().coerceIn(0, 255)
    val blue = (
        (firstArgb and 0xFF) * inverseRatio +
            (secondArgb and 0xFF) * clampedRatio
        ).roundToInt().coerceIn(0, 255)

    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun lerpDouble(start: Double, stop: Double, fraction: Double): Double {
    val clampedFraction = fraction.coerceIn(0.0, 1.0)
    return start + ((stop - start) * clampedFraction)
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return start + ((stop - start) * clampedFraction)
}

private fun sanitizeDegreesInt(degrees: Int): Int {
    var deg = degrees % 360
    if (deg < 0) deg += 360
    return deg
}

private fun differenceDegrees(a: Double, b: Double): Double {
    return 180.0 - abs(abs(a - b) - 180.0)
}

private fun averageColorArgb(pixels: IntArray): Int {
    if (pixels.isEmpty()) return DarkColorScheme.primary.toArgb()

    var totalRed = 0L
    var totalGreen = 0L
    var totalBlue = 0L

    for (argb in pixels) {
        totalRed += (argb ushr 16) and 0xFF
        totalGreen += (argb ushr 8) and 0xFF
        totalBlue += argb and 0xFF
    }

    val size = pixels.size.toLong()
    val r = (totalRed / size).toInt().coerceIn(0, 255)
    val g = (totalGreen / size).toInt().coerceIn(0, 255)
    val b = (totalBlue / size).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun isMostlyNeutralArtwork(colorsToPopulation: Map<Int, Int>): Boolean {
    if (colorsToPopulation.isEmpty()) return false

    var totalPopulation = 0.0
    var neutralPopulation = 0.0
    var highChromaPopulation = 0.0
    var weightedChroma = 0.0

    for ((argb, populationInt) in colorsToPopulation) {
        if (populationInt <= 0) continue
        val population = populationInt.toDouble()
        val chroma = Hct.fromInt(argb).chroma

        totalPopulation += population
        weightedChroma += chroma * population

        if (chroma <= NEUTRAL_PIXEL_CHROMA_THRESHOLD) {
            neutralPopulation += population
        }
        if (chroma >= HIGH_CHROMA_THRESHOLD) {
            highChromaPopulation += population
        }
    }

    if (totalPopulation <= 0.0) return false

    val neutralRatio = neutralPopulation / totalPopulation
    val highChromaRatio = highChromaPopulation / totalPopulation
    val meanChroma = weightedChroma / totalPopulation

    return neutralRatio >= REQUIRED_NEUTRAL_POPULATION &&
        highChromaRatio <= MAX_HIGH_CHROMA_POPULATION &&
        meanChroma <= MAX_WEIGHTED_CHROMA_FOR_NEUTRAL
}

private fun shouldUseNeutralArtworkScheme(argb: Int, sourceHct: Hct): Boolean {
    return sourceHct.chroma <= GRAYSCALE_CHROMA_THRESHOLD &&
        isArgbNearGrayscale(argb)
}

private fun isArgbNearGrayscale(argb: Int): Boolean {
    val red = (argb ushr 16) and 0xFF
    val green = (argb ushr 8) and 0xFF
    val blue = argb and 0xFF
    return maxOf(
        abs(red - green),
        abs(green - blue),
        abs(red - blue)
    ) <= MAX_GRAYSCALE_CHANNEL_DELTA
}

private fun colorToHSL(colorArgb: Int, hsl: FloatArray) {
    val r = ((colorArgb ushr 16) and 0xFF) / 255f
    val g = ((colorArgb ushr 8) and 0xFF) / 255f
    val b = (colorArgb and 0xFF) / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var h = 0f
    var s = 0f
    val l = (max + min) / 2f

    if (delta > 0f) {
        s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
        h = when (max) {
            r -> ((g - b) / delta) + (if (g < b) 6f else 0f)
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        h *= 60f
    }

    hsl[0] = h
    hsl[1] = s
    hsl[2] = l
}

private fun hslToColor(hsl: FloatArray): Int {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]

    val c = (1f - abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f % 2f) - 1f))

    val hueSegment = (h / 60f).toInt() % 6
    var r = 0f
    var g = 0f
    var b = 0f

    when (hueSegment) {
        0 -> { r = c; g = x; b = 0f }
        1 -> { r = x; g = c; b = 0f }
        2 -> { r = 0f; g = c; b = x }
        3 -> { r = 0f; g = x; b = c }
        4 -> { r = x; g = 0f; b = c }
        5 -> { r = c; g = 0f; b = x }
    }

    val red = ((r + m) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((g + m) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((b + m) * 255f).roundToInt().coerceIn(0, 255)

    return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun ColorScheme.toGrayscaleColorScheme(): ColorScheme {
    fun convert(color: Color): Color {
        val hsl = FloatArray(3)
        colorToHSL(color.toArgb(), hsl)
        hsl[0] = 0f
        hsl[1] = 0f
        return Color(hslToColor(hsl))
    }

    return copy(
        primary = convert(primary),
        onPrimary = convert(onPrimary),
        primaryContainer = convert(primaryContainer),
        onPrimaryContainer = convert(onPrimaryContainer),
        inversePrimary = convert(inversePrimary),
        secondary = convert(secondary),
        onSecondary = convert(onSecondary),
        secondaryContainer = convert(secondaryContainer),
        onSecondaryContainer = convert(onSecondaryContainer),
        tertiary = convert(tertiary),
        onTertiary = convert(onTertiary),
        tertiaryContainer = convert(tertiaryContainer),
        onTertiaryContainer = convert(onTertiaryContainer),
        background = convert(background),
        onBackground = convert(onBackground),
        surface = convert(surface),
        onSurface = convert(onSurface),
        surfaceVariant = convert(surfaceVariant),
        onSurfaceVariant = convert(onSurfaceVariant),
        surfaceTint = convert(surfaceTint),
        inverseSurface = convert(inverseSurface),
        inverseOnSurface = convert(inverseOnSurface),
        error = convert(error),
        onError = convert(onError),
        errorContainer = convert(errorContainer),
        onErrorContainer = convert(onErrorContainer),
        outline = convert(outline),
        outlineVariant = convert(outlineVariant),
        scrim = convert(scrim),
        surfaceBright = convert(surfaceBright),
        surfaceDim = convert(surfaceDim),
        surfaceContainer = convert(surfaceContainer),
        surfaceContainerHigh = convert(surfaceContainerHigh),
        surfaceContainerHighest = convert(surfaceContainerHighest),
        surfaceContainerLow = convert(surfaceContainerLow),
        surfaceContainerLowest = convert(surfaceContainerLowest)
    )
}

private fun DynamicScheme.toComposeColorScheme(isDark: Boolean): ColorScheme {
    fun TonalPalette.color(tone: Int): Color = Color(tone(tone))

    return if (isDark) {
        ColorScheme(
            primary = primaryPalette.color(80),
            onPrimary = primaryPalette.color(20),
            primaryContainer = primaryPalette.color(30),
            onPrimaryContainer = primaryPalette.color(90),
            inversePrimary = primaryPalette.color(40),
            secondary = secondaryPalette.color(80),
            onSecondary = secondaryPalette.color(20),
            secondaryContainer = secondaryPalette.color(30),
            onSecondaryContainer = secondaryPalette.color(90),
            tertiary = tertiaryPalette.color(80),
            onTertiary = tertiaryPalette.color(20),
            tertiaryContainer = tertiaryPalette.color(30),
            onTertiaryContainer = tertiaryPalette.color(90),
            background = neutralPalette.color(6),
            onBackground = neutralPalette.color(90),
            surface = neutralPalette.color(6),
            onSurface = neutralPalette.color(90),
            surfaceVariant = neutralVariantPalette.color(30),
            onSurfaceVariant = neutralVariantPalette.color(80),
            surfaceTint = primaryPalette.color(80),
            inverseSurface = neutralPalette.color(90),
            inverseOnSurface = neutralPalette.color(20),
            error = errorPalette.color(80),
            onError = errorPalette.color(20),
            errorContainer = errorPalette.color(30),
            onErrorContainer = errorPalette.color(90),
            outline = neutralVariantPalette.color(60),
            outlineVariant = neutralVariantPalette.color(30),
            scrim = neutralPalette.color(0),
            surfaceBright = neutralPalette.color(24),
            surfaceDim = neutralPalette.color(6),
            surfaceContainer = neutralPalette.color(12),
            surfaceContainerHigh = neutralPalette.color(17),
            surfaceContainerHighest = neutralPalette.color(22),
            surfaceContainerLow = neutralPalette.color(10),
            surfaceContainerLowest = neutralPalette.color(4)
        )
    } else {
        ColorScheme(
            primary = primaryPalette.color(40),
            onPrimary = primaryPalette.color(100),
            primaryContainer = primaryPalette.color(90),
            onPrimaryContainer = primaryPalette.color(10),
            inversePrimary = primaryPalette.color(80),
            secondary = secondaryPalette.color(40),
            onSecondary = secondaryPalette.color(100),
            secondaryContainer = secondaryPalette.color(90),
            onSecondaryContainer = secondaryPalette.color(10),
            tertiary = tertiaryPalette.color(40),
            onTertiary = tertiaryPalette.color(100),
            tertiaryContainer = tertiaryPalette.color(90),
            onTertiaryContainer = tertiaryPalette.color(10),
            background = neutralPalette.color(98),
            onBackground = neutralPalette.color(10),
            surface = neutralPalette.color(98),
            onSurface = neutralPalette.color(10),
            surfaceVariant = neutralVariantPalette.color(90),
            onSurfaceVariant = neutralVariantPalette.color(30),
            surfaceTint = primaryPalette.color(40),
            inverseSurface = neutralPalette.color(20),
            inverseOnSurface = neutralPalette.color(95),
            error = errorPalette.color(40),
            onError = errorPalette.color(100),
            errorContainer = errorPalette.color(90),
            onErrorContainer = errorPalette.color(10),
            outline = neutralVariantPalette.color(50),
            outlineVariant = neutralVariantPalette.color(80),
            scrim = neutralPalette.color(0),
            surfaceBright = neutralPalette.color(98),
            surfaceDim = neutralPalette.color(87),
            surfaceContainer = neutralPalette.color(94),
            surfaceContainerHigh = neutralPalette.color(92),
            surfaceContainerHighest = neutralPalette.color(90),
            surfaceContainerLow = neutralPalette.color(96),
            surfaceContainerLowest = neutralPalette.color(100)
        )
    }
}
