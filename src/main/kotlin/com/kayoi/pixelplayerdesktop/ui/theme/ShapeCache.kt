package com.kayoi.pixelplayerdesktop.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object ShapeCache {
    val Full = CircleShape
    val ExtraLarge = RoundedCornerShape(32.dp)
    val Large = RoundedCornerShape(24.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Small = RoundedCornerShape(12.dp)
    val ExtraSmall = RoundedCornerShape(8.dp)

    // Expressive Asymmetrical / Morphing shapes
    val ExpressiveCard = RoundedCornerShape(24.dp)
    val ExpressivePill = RoundedCornerShape(50)
    val ExpressiveTopBar = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val ExpressiveBottomBar = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** M3 Expressive 9-sided cookie — artist/people avatars and placeholders */
    val ExpressiveAvatar: androidx.compose.ui.graphics.Shape = CookieShape(lobes = 9, innerRatio = 0.86f)
}

private class CookieShape(
    private val lobes: Int = 9,
    private val innerRatio: Float = 0.86f
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy)
        val minR = maxR * innerRatio
        val path = androidx.compose.ui.graphics.Path()

        val totalSteps = lobes * 4
        val dTheta = (2.0 * Math.PI / totalSteps).toFloat()

        fun radiusAt(angle: Double): Float {
            val wave = Math.cos(lobes * angle).toFloat()
            return minR + (maxR - minR) * (0.5f + 0.5f * wave)
        }

        val r0 = radiusAt(0.0)
        path.moveTo((cx + r0 * Math.cos(0.0)).toFloat(), (cy + r0 * Math.sin(0.0)).toFloat())

        for (i in 1..totalSteps) {
            val a1 = i * dTheta
            val a0 = (i - 1) * dTheta
            val midA = (a0 + a1) / 2.0

            val rMid = radiusAt(midA)
            val r1 = radiusAt(a1.toDouble())

            val cX = (cx + rMid * Math.cos(midA)).toFloat()
            val cY = (cy + rMid * Math.sin(midA)).toFloat()
            val eX = (cx + r1 * Math.cos(a1.toDouble())).toFloat()
            val eY = (cy + r1 * Math.sin(a1.toDouble())).toFloat()

            path.quadraticTo(cX, cY, eX, eY)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
