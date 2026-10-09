package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun BubblesLine(
    isActive: Boolean,
    color: Color,
    onSeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(250)),
        modifier = modifier
    ) {
        val transition = rememberInfiniteTransition(label = "bubbles_transition")
        val animatedValue by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bubble_animation_progress"
        )

        val density = LocalDensity.current
        val bubbleRadius = remember(density) { with(density) { 4.5f.dp.toPx() } }

        val (morphableCircle, morphableNote) = remember(bubbleRadius) {
            val (noteNodes, curveCount) = createVectorNotePathNodes(targetSize = bubbleRadius * 2.8f)
            val circleNodes = createCirclePathNodes(radius = bubbleRadius, curveCount = curveCount)
            circleNodes to noteNodes
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onSeek)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.size(350.dp, 44.dp)) {
                val bubbleCount = 3
                val bubbleColor = color.copy(alpha = 0.85f)
                val bounceAmplitude = 9.dp.toPx()

                for (i in 0 until bubbleCount) {
                    val progress = (animatedValue + i * (1f / bubbleCount)) % 1f
                    
                    // Sine wave: progress 0..0.5 moves down (positive y), 0.5..1.0 moves up (negative y)
                    val sinVal = sin(progress * 2 * PI).toFloat()
                    val yOffset = sinVal * bounceAmplitude

                    // Morph progress: 1f (Note) when at the bottom, 0f (Circle) when at the top
                    val morphProgress = when {
                        progress in 0f..0.25f -> progress / 0.25f
                        progress in 0.25f..0.5f -> 1.0f - (progress - 0.25f) / 0.25f
                        else -> 0f
                    }.coerceIn(0f, 1f)

                    val morphedPath = lerpPath(
                        start = morphableCircle,
                        stop = morphableNote,
                        fraction = morphProgress
                    ).toPath()

                    val xPos = (size.width / (bubbleCount + 1)) * (i + 1)
                    val yPos = size.height / 2 + yOffset

                    translate(left = xPos, top = yPos) {
                        drawPath(
                            path = morphedPath,
                            color = bubbleColor
                        )
                    }
                }
            }
        }
    }
}

private fun lerpPath(start: List<PathNode>, stop: List<PathNode>, fraction: Float): List<PathNode> {
    val minSize = minOf(start.size, stop.size)
    return (0 until minSize).map { index ->
        val startNode = start[index]
        val stopNode = stop[index]
        when {
            startNode is PathNode.MoveTo && stopNode is PathNode.MoveTo -> {
                PathNode.MoveTo(
                    lerpFloat(startNode.x, stopNode.x, fraction),
                    lerpFloat(startNode.y, stopNode.y, fraction)
                )
            }
            startNode is PathNode.CurveTo && stopNode is PathNode.CurveTo -> {
                PathNode.CurveTo(
                    lerpFloat(startNode.x1, stopNode.x1, fraction),
                    lerpFloat(startNode.y1, stopNode.y1, fraction),
                    lerpFloat(startNode.x2, stopNode.x2, fraction),
                    lerpFloat(startNode.y2, stopNode.y2, fraction),
                    lerpFloat(startNode.x3, stopNode.x3, fraction),
                    lerpFloat(startNode.y3, stopNode.y3, fraction)
                )
            }
            else -> stopNode
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun List<PathNode>.toPath(): Path = Path().apply {
    this@toPath.forEach { node ->
        when (node) {
            is PathNode.MoveTo -> moveTo(node.x, node.y)
            is PathNode.LineTo -> lineTo(node.x, node.y)
            is PathNode.CurveTo -> cubicTo(node.x1, node.y1, node.x2, node.y2, node.x3, node.y3)
            is PathNode.Close -> close()
            else -> {}
        }
    }
}

private fun createVectorNotePathNodes(targetSize: Float): Pair<MutableList<PathNode>, Int> {
    val pathData = "M239.5,1.9c-4.6,1.1 -8.7,3.6 -12.2,7.3 -6.7,6.9 -6.3,-2.5 -6.3,151.9 0,76.9 -0.3,140 -0.7,140.2 -0.5,0.3 -4.2,-0.9 -8.3,-2.5 -48.1,-19.3 -102.8,-8.3 -138.6,27.7 -35.8,36.1 -41.4,85.7 -13.6,120.7 18.6,23.4 52.8,37.4 86.2,35.3 34.8,-2.1 65.8,-16 89.5,-39.9 14.5,-14.6 24.9,-31.9 30.7,-50.6l2.3,-7.5 0.2,-133c0.2,-73.2 0.5,-133.6 0.8,-134.2 0.8,-2.4 62,28.5 84.3,42.4 22.4,14.1 34.1,30.4 37.2,51.9 2.4,16.5 -2.2,34.5 -13,50.9 -6,9.1 -7,12.1 -4.8,14.3 2.2,2.2 5.3,1.2 13.8,-4.5 26.4,-17.9 45.6,-48 50,-78.2 1.9,-12.9 0.8,-34.3 -2.4,-46.1 -8.7,-31.7 -30.4,-58 -64.1,-77.8 -64.3,-37.9 -116,-67.3 -119.6,-68.1 -5,-1.2 -7.1,-1.2 -11.4,-0.2z"
    val parser = PathParser().parsePathString(pathData)
    val rawNodes = parser.toNodes().toAbsolute().toCurvesOnly()
    val rawPath = rawNodes.toPath()
    val bounds = rawPath.getBounds()
    
    val centerX = bounds.left + bounds.width / 2f
    val centerY = bounds.top + bounds.height / 2f
    val maxDim = max(bounds.width, bounds.height)
    val scale = if (maxDim > 0f) targetSize / maxDim else 1f

    val centeredNodes = rawNodes.map { node ->
        when (node) {
            is PathNode.MoveTo -> PathNode.MoveTo((node.x - centerX) * scale, (node.y - centerY) * scale)
            is PathNode.LineTo -> PathNode.LineTo((node.x - centerX) * scale, (node.y - centerY) * scale)
            is PathNode.CurveTo -> PathNode.CurveTo(
                (node.x1 - centerX) * scale, (node.y1 - centerY) * scale,
                (node.x2 - centerX) * scale, (node.y2 - centerY) * scale,
                (node.x3 - centerX) * scale, (node.y3 - centerY) * scale
            )
            else -> node
        }
    }.toMutableList()

    val curveCount = centeredNodes.count { it is PathNode.CurveTo }
    return centeredNodes to curveCount
}

private fun createCirclePathNodes(radius: Float, curveCount: Int): MutableList<PathNode> {
    val count = curveCount.coerceAtLeast(4)
    val result = mutableListOf<PathNode>()
    val startAngle = -PI / 2.0
    val startX = (radius * cos(startAngle)).toFloat()
    val startY = (radius * sin(startAngle)).toFloat()
    result.add(PathNode.MoveTo(startX, startY))

    val dTheta = (2.0 * PI) / count
    val alpha = ((4.0 / 3.0) * tan(dTheta / 4.0)).toFloat()

    for (i in 0 until count) {
        val t0 = startAngle + i * dTheta
        val t1 = startAngle + (i + 1) * dTheta
        val p0x = (radius * cos(t0)).toFloat()
        val p0y = (radius * sin(t0)).toFloat()
        val p3x = (radius * cos(t1)).toFloat()
        val p3y = (radius * sin(t1)).toFloat()

        val p1x = p0x - alpha * p0y
        val p1y = p0y + alpha * p0x
        val p2x = p3x + alpha * p3y
        val p2y = p3y - alpha * p3x

        result.add(PathNode.CurveTo(p1x, p1y, p2x, p2y, p3x, p3y))
    }
    result.add(PathNode.Close)
    return result
}

private fun List<PathNode>.toAbsolute(): MutableList<PathNode> {
    val absoluteNodes = mutableListOf<PathNode>()
    var currentX = 0f
    var currentY = 0f
    this.forEach { node ->
        when (node) {
            is PathNode.MoveTo -> { currentX = node.x; currentY = node.y; absoluteNodes.add(node) }
            is PathNode.RelativeMoveTo -> { currentX += node.dx; currentY += node.dy; absoluteNodes.add(PathNode.MoveTo(currentX, currentY)) }
            is PathNode.LineTo -> { currentX = node.x; currentY = node.y; absoluteNodes.add(node) }
            is PathNode.RelativeLineTo -> { currentX += node.dx; currentY += node.dy; absoluteNodes.add(PathNode.LineTo(currentX, currentY)) }
            is PathNode.CurveTo -> { currentX = node.x3; currentY = node.y3; absoluteNodes.add(node) }
            is PathNode.RelativeCurveTo -> {
                absoluteNodes.add(PathNode.CurveTo(currentX + node.dx1, currentY + node.dy1, currentX + node.dx2, currentY + node.dy2, currentX + node.dx3, currentY + node.dy3))
                currentX += node.dx3; currentY += node.dy3
            }
            is PathNode.Close -> absoluteNodes.add(node)
            else -> {}
        }
    }
    return absoluteNodes
}

private fun MutableList<PathNode>.toCurvesOnly(): MutableList<PathNode> {
    val curveNodes = mutableListOf<PathNode>()
    var lastX = 0f
    var lastY = 0f

    this.forEach { node ->
        when(node) {
            is PathNode.MoveTo -> { curveNodes.add(node); lastX = node.x; lastY = node.y }
            is PathNode.LineTo -> { curveNodes.add(PathNode.CurveTo(lastX, lastY, node.x, node.y, node.x, node.y)); lastX = node.x; lastY = node.y }
            is PathNode.CurveTo -> { curveNodes.add(node); lastX = node.x3; lastY = node.y3 }
            is PathNode.Close -> curveNodes.add(node)
            else -> {}
        }
    }
    return curveNodes
}
