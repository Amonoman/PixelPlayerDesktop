package com.kayoi.pixelplayerdesktop.utils.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural continuous-curvature star shape with smoothly rounded tips.
 *
 * Uses the parametric polar curve equation `r(t) = R * (1 + curve * cos(sides * t))`
 * to produce harmonic floral and organic star contours without sharp vertices.
 *
 * @param sides Number of rounded star petals/points.
 * @param curve Curvature depth factor (0.0 for pure circle, higher for deeper valleys).
 * @param rotation Angular offset in degrees.
 * @param iterations Number of angular sampling steps along the perimeter.
 */
class RoundedStarShape(
    private val sides: Int,
    private val curve: Double = 0.09,
    private val rotation: Float = 0f,
    iterations: Int = 360,
) : Shape {

    private companion object {
        const val TWO_PI = 2 * PI
    }

    private val steps = (TWO_PI) / min(iterations, 360)
    private val rotationDegree = (PI / 180) * rotation

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(Path().apply {
        // Adjust radius to account for outward curvature expansion and maintain bounded bounds
        val r = min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, curve)

        val xCenter = size.width * 0.5f
        val yCenter = size.height * 0.5f

        fun pointAt(t: Double): Pair<Float, Float> {
            val radiusModulation = 1 + curve * cos(sides * t)
            val x = r * (cos(t - rotationDegree) * radiusModulation)
            val y = r * (sin(t - rotationDegree) * radiusModulation)
            return (x + xCenter).toFloat() to (y + yCenter).toFloat()
        }

        val (startX, startY) = pointAt(0.0)
        moveTo(startX, startY)

        var t = steps
        while (t < TWO_PI) {
            val (x, y) = pointAt(t)
            lineTo(x, y)
            t += steps
        }

        close()
    })

    private fun mapRange(a: Double, b: Double, c: Double, d: Double, x: Double): Double {
        return (x - a) / (b - a) * (d - c) + c
    }
}
