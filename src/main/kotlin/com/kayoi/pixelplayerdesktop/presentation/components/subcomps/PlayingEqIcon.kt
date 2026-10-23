package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import kotlin.math.PI
import kotlin.math.sin

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

import com.kayoi.pixelplayerdesktop.presentation.components.LocalWindowMinimized

@Composable
fun PlayingEqIcon(
    modifier: Modifier = Modifier,
    color: Color,
    isPlaying: Boolean = true,
    isMinimized: Boolean = LocalWindowMinimized.current,
    bars: Int = 3,
    minHeightFraction: Float = 0.28f,
    maxHeightFraction: Float = 1.0f,
    phaseDurationMillis: Int = 3600,
    wanderDurationMillis: Int = 12000,
    gapFraction: Float = 0.30f
) {
    val shouldAnimate = isPlaying && !isMinimized
    val fullRotation = (2f * PI).toFloat()
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(shouldAnimate, phaseDurationMillis) {
        if (!shouldAnimate) return@LaunchedEffect
        var last = System.nanoTime()
        val speed = fullRotation / (phaseDurationMillis / 1000f)
        while (true) {
            delay(40L)
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000_000f
            phase = (phase + dt * speed) % fullRotation
            last = now
        }
    }

    val activity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "activity"
    )

    val wanderRatio = remember(phaseDurationMillis, wanderDurationMillis) {
        phaseDurationMillis.toFloat() / wanderDurationMillis.toFloat()
    }
    val speeds = remember(bars) { List(bars) { (it + 1).toFloat() } }
    val shifts = remember(bars) { List(bars) { i -> i * 0.9f } }

    Canvas(
        modifier = modifier.graphicsLayer { }
    ) {
        val currentPhase = phase
        val wander = currentPhase * wanderRatio
        val w = size.width
        val h = size.height

        val tentativeBarW = w / (bars + (bars - 1) * (1f + gapFraction))
        val gap = tentativeBarW * gapFraction
        val barW = tentativeBarW
        val corner = CornerRadius(barW / 2f, barW / 2f)

        repeat(bars) { i ->
            val slowShift = 0.6f * sin(wander + i * 0.4f)
            val slowAmp   = 0.85f + 0.15f * sin(wander * 0.5f + 1.1f + i * 0.3f)

            val v = (sin(currentPhase * speeds[i] + shifts[i] + slowShift) * slowAmp + 1f) * 0.5f
            val eased = v * v * (3 - 2 * v)

            val fracBars = minHeightFraction + (maxHeightFraction - minHeightFraction) * eased
            val barH = h * fracBars
            val dotH = barW
            val blendedH = dotH + (barH - dotH) * activity

            val top = (h - blendedH) / 2f
            val left = i * (barW + gap)

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barW, blendedH),
                cornerRadius = corner
            )
        }
    }
}
