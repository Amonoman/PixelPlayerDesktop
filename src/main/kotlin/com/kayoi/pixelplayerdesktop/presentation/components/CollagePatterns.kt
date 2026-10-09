package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape

enum class CollagePattern {
    COSMIC_SWIRL,
    HONEYCOMB_GROOVE,
    VINYL_STACK,
    PIXEL_MOSAIC,
    STARDUST_SCATTER;

    companion object {
        val default = COSMIC_SWIRL
    }
}

@Stable
data class CollageConfig(
    val size: Dp,
    val width: Dp,
    val height: Dp,
    val align: Alignment,
    val rot: Float,
    val shape: Shape,
    val offsetX: Dp,
    val offsetY: Dp
)

@Stable
fun buildCollageConfigs(
    pattern: CollagePattern,
    min: Dp,
    boxMaxHeight: Dp
): List<CollageConfig> = when (pattern) {
    CollagePattern.COSMIC_SWIRL -> cosmicSwirlConfigs(min, boxMaxHeight)
    CollagePattern.HONEYCOMB_GROOVE -> honeycombGrooveConfigs(min, boxMaxHeight)
    CollagePattern.VINYL_STACK -> vinylStackConfigs(min, boxMaxHeight)
    CollagePattern.PIXEL_MOSAIC -> pixelMosaicConfigs(min, boxMaxHeight)
    CollagePattern.STARDUST_SCATTER -> stardustScatterConfigs(min, boxMaxHeight)
}

private fun cosmicSwirlConfigs(min: Dp, boxMaxHeight: Dp): List<CollageConfig> = listOf(
    CollageConfig(size = min * 0.80f, width = min * 0.48f, height = min * 0.80f, align = Alignment.Center, rot = 45f, shape = RoundedCornerShape(percent = 50), offsetX = 0.dp, offsetY = 0.dp),
    CollageConfig(size = min * 0.24f, width = min * 0.24f, height = min * 0.24f, align = Alignment.CenterStart, rot = 0f, shape = CircleShape, offsetX = (min * 0.07f), offsetY = -(boxMaxHeight * 0.10f)),
    CollageConfig(size = min * 0.24f, width = min * 0.24f, height = min * 0.24f, align = Alignment.CenterEnd, rot = 0f, shape = CircleShape, offsetX = -(min * 0.07f), offsetY = (boxMaxHeight * 0.06f)),
    CollageConfig(size = min * 0.35f, width = min * 0.35f, height = min * 0.35f, align = Alignment.CenterStart, rot = -20f, shape = RoundedCornerShape(20.dp), offsetX = (min * 0.08f), offsetY = (boxMaxHeight * 0.02f)),
    CollageConfig(size = min * 0.85f, width = min * 0.85f, height = min * 0.85f, align = Alignment.CenterEnd, rot = 0f, shape = RoundedStarShape(sides = 6, curve = 0.09, rotation = 45f), offsetX = -(min * 0.04f), offsetY = (boxMaxHeight * 0.02f))
)

/** Hexagon-themed organic layout with rounded stars and mixed shapes. */
private fun honeycombGrooveConfigs(min: Dp, boxMaxHeight: Dp): List<CollageConfig> = listOf(
    CollageConfig(size = min * 0.7f, width = min * 0.7f, height = min * 0.7f, align = Alignment.Center, rot = 0f, shape = RoundedStarShape(sides = 6, curve = 0.05, rotation = 0f), offsetX = 0.dp, offsetY = 0.dp),
    CollageConfig(size = min * 0.35f, width = min * 0.22f, height = min * 0.22f, align = Alignment.TopEnd, rot = 15f, shape = RoundedCornerShape(16.dp), offsetX = -(300.dp * 0.03f), offsetY = (boxMaxHeight * 0.04f)),
    CollageConfig(size = min * 0.3f, width = min * 0.18f, height = min * 0.18f, align = Alignment.BottomStart, rot = 0f, shape = CircleShape, offsetX = (300.dp * 0.04f), offsetY = -(boxMaxHeight * 0.04f)),
    CollageConfig(size = min * 0.55f, width = min * 0.55f, height = min * 0.55f, align = Alignment.BottomStart, rot = -10f, shape = RoundedStarShape(sides = 6, curve = 0.05, rotation = 30f), offsetX = (300.dp * 0.02f), offsetY = -(boxMaxHeight * 0.02f)),
    CollageConfig(size = min * 0.55f, width = min * 0.42f, height = min * 0.55f, align = Alignment.TopEnd, rot = 30f, shape = RoundedCornerShape(percent = 50), offsetX = -(300.dp * 0.06f), offsetY = (boxMaxHeight * 0.03f))
)

/** Cascading discs / record-inspired circles. */
private fun vinylStackConfigs(min: Dp, boxMaxHeight: Dp): List<CollageConfig> = listOf(
    CollageConfig(size = min * 0.55f, width = min * 0.55f, height = min * 0.55f, align = Alignment.CenterStart, rot = 0f, shape = CircleShape, offsetX = (300.dp * 0.02f), offsetY = 0.dp),
    CollageConfig(size = min * 0.38f, width = min * 0.38f, height = min * 0.38f, align = Alignment.CenterEnd, rot = 0f, shape = CircleShape, offsetX = -(300.dp * 0.04f), offsetY = -(boxMaxHeight * 0.08f)),
    CollageConfig(size = min * 0.0f, width = min * 0.15f, height = min * 0.15f, align = Alignment.TopEnd, rot = -45f, shape = RoundedCornerShape(percent = 50), offsetX = -(300.dp * 0.02f), offsetY = (boxMaxHeight * 0.43f)),
    CollageConfig(size = min * 0.5f, width = min * 0.5f, height = min * 0.5f, align = Alignment.Center, rot = 0f, shape = CircleShape, offsetX = 70.dp, offsetY = -(boxMaxHeight * 0.02f)),
    CollageConfig(size = min * 0.35f, width = min * 0.35f, height = min * 0.35f, align = Alignment.BottomStart, rot = 0f, shape = RoundedStarShape(sides = 8, curve = 0.06, rotation = 22f), offsetX = (300.dp * 0.05f), offsetY = -(boxMaxHeight * 0.03f))
)

/** Grid-like arrangement of tilted rounded rectangles. */
private fun pixelMosaicConfigs(min: Dp, boxMaxHeight: Dp): List<CollageConfig> = listOf(
    CollageConfig(size = min * 0.65f, width = min * 0.42f, height = min * 0.65f, align = Alignment.TopStart, rot = 0f, shape = RoundedCornerShape(24.dp), offsetX = (300.dp * 0.03f), offsetY = (boxMaxHeight * 0.02f)),
    CollageConfig(size = min * 0.45f, width = min * 0.52f, height = min * 0.42f, align = Alignment.TopEnd, rot = 8f, shape = RoundedCornerShape(20.dp), offsetX = -(300.dp * 0.04f), offsetY = (boxMaxHeight * 0.06f)),
    CollageConfig(size = min * 0.1f, width = min * 0.52f, height = min * 0.12f, align = Alignment.BottomEnd, rot = -5f, shape = RoundedCornerShape(12.dp), offsetX = -(300.dp * 0.06f), offsetY = -(boxMaxHeight * 0.05f)),
    CollageConfig(size = min * 0.55f, width = min * 0.42f, height = min * 0.52f, align = Alignment.BottomEnd, rot = -12f, shape = RoundedCornerShape(28.dp), offsetX = -(300.dp * 0.02f), offsetY = -(boxMaxHeight * 0.02f)),
    CollageConfig(size = min * 0.4f, width = min * 0.50f, height = min * 0.48f, align = Alignment.TopStart, rot = 5f, shape = RoundedCornerShape(16.dp), offsetX = (300.dp * 0.04f), offsetY = (boxMaxHeight * 0.04f))
)

/** Star-heavy whimsical arrangement with mixed star variants. */
private fun stardustScatterConfigs(min: Dp, boxMaxHeight: Dp): List<CollageConfig> = listOf(
    CollageConfig(size = min * 0.65f, width = min * 0.65f, height = min * 0.65f, align = Alignment.Center, rot = 10f, shape = RoundedStarShape(sides = 5, curve = 0.12, rotation = 0f), offsetX = 0.dp, offsetY = 0.dp),
    CollageConfig(size = min * 0.37f, width = min * 0.22f, height = min * 0.22f, align = Alignment.TopStart, rot = 0f, shape = CircleShape, offsetX = (300.dp * 0.04f), offsetY = (boxMaxHeight * 0.03f)),
    CollageConfig(size = min * 0.36f, width = min * 0.26f, height = min * 0.26f, align = Alignment.BottomEnd, rot = 0f, shape = RoundedStarShape(sides = 4, curve = 0.18, rotation = 45f), offsetX = -(300.dp * 0.06f), offsetY = (boxMaxHeight * 0.00f)),
    CollageConfig(size = min * 0.5f, width = min * 0.5f, height = min * 0.5f, align = Alignment.CenterEnd, rot = -15f, shape = RoundedStarShape(sides = 8, curve = 0.04, rotation = 0f), offsetX = -(300.dp * 0.04f), offsetY = 0.dp),
    CollageConfig(size = min * 0.5f, width = min * 0.5f, height = min * 0.42f, align = Alignment.BottomStart, rot = 25f, shape = RoundedCornerShape(percent = 50), offsetX = (300.dp * 0.06f), offsetY = -(boxMaxHeight * 0.03f))
)
