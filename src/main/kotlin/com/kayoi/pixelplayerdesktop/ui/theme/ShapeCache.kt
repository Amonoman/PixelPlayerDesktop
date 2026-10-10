package com.kayoi.pixelplayerdesktop.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    val ExpressiveAvatar: Shape = CookieShape(lobes = 9, innerRatio = 0.86f)
}

private class CookieShape(
    private val lobes: Int = 9,
    private val innerRatio: Float = 0.86f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy)
        val minR = maxR * innerRatio
        val path = Path()

        val totalSteps = lobes * 4
        val dTheta = (2.0 * PI / totalSteps).toFloat()

        fun radiusAt(angle: Double): Float {
            val wave = cos(lobes * angle).toFloat()
            return minR + (maxR - minR) * (0.5f + 0.5f * wave)
        }

        val r0 = radiusAt(0.0)
        path.moveTo((cx + r0 * cos(0.0)).toFloat(), (cy + r0 * sin(0.0)).toFloat())

        for (i in 1..totalSteps) {
            val a1 = i * dTheta
            val a0 = (i - 1) * dTheta
            val midA = (a0 + a1) / 2.0

            val rMid = radiusAt(midA)
            val r1 = radiusAt(a1.toDouble())

            val cX = (cx + rMid * cos(midA)).toFloat()
            val cY = (cy + rMid * sin(midA)).toFloat()
            val eX = (cx + r1 * cos(a1.toDouble())).toFloat()
            val eY = (cy + r1 * sin(a1.toDouble())).toFloat()

            path.quadraticTo(cX, cY, eX, eY)
        }
        path.close()
        return Outline.Generic(path)
    }
}
