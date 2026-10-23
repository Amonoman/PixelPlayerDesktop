package com.kayoi.pixelplayerdesktop.presentation.components

import kotlin.math.PI
import kotlin.math.sin

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun WavySliderExpressive(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    isMinimized: Boolean = LocalWindowMinimized.current,
    enabled: Boolean = true,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    waveAmplitude: Dp = 3.dp,
    wavelength: Dp = 28.dp,
    strokeWidth: Dp = 4.dp,
    trackEdgePadding: Dp = 6.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val targetAmplitude = if (enabled && isPlaying && !isDragging) 1f else 0f
    var isPhaseRunning by remember { mutableStateOf(targetAmplitude > 0f) }

    if (targetAmplitude > 0f && !isPhaseRunning) {
        isPhaseRunning = true
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavyAmplitude",
        finishedListener = { finalValue ->
            if (finalValue <= 0.001f && targetAmplitude == 0f) {
                isPhaseRunning = false
            }
        }
    )

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "thumbInteraction"
    )

    var phase by remember { mutableFloatStateOf(0f) }
    val shouldAnimatePhase = isPhaseRunning && !isMinimized
    val fullRotation = (2f * PI).toFloat()

    LaunchedEffect(shouldAnimatePhase) {
        if (!shouldAnimatePhase) return@LaunchedEffect
        var lastTime = System.nanoTime()
        val speed = fullRotation / 1.4f
        while (true) {
            delay(33L)
            val now = System.nanoTime()
            val dt = (now - lastTime) / 1_000_000_000f
            phase = (phase + dt * speed) % fullRotation
            lastTime = now
        }
    }

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier
            .graphicsLayer { }
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val edgePadPx = trackEdgePadding.toPx()
                    val trackWidth = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                    val progress = ((offset.x - edgePadPx) / trackWidth).coerceIn(0f, 1f)
                    onValueChange(progress)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val edgePadPx = trackEdgePadding.toPx()
                        val trackWidth = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                        val progress = ((offset.x - edgePadPx) / trackWidth).coerceIn(0f, 1f)
                        dragFraction = progress
                        onValueChange(progress)
                    },
                    onDragEnd = {
                        isDragging = false
                        dragFraction = null
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        dragFraction = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val edgePadPx = trackEdgePadding.toPx()
                        val trackWidth = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                        val progress = ((change.position.x - edgePadPx) / trackWidth).coerceIn(0f, 1f)
                        dragFraction = progress
                        onValueChange(progress)
                    }
                )
            }
    ) {
        val currentPhase = phase
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val strokeWidthPx = strokeWidth.toPx()
        val amplitudePx = waveAmplitude.toPx() * animatedAmplitude
        val wavelengthPx = wavelength.toPx()
        val edgePaddingPx = trackEdgePadding.toPx().coerceAtMost(width / 4f)

        val trackStart = edgePaddingPx
        val trackEnd = width - edgePaddingPx
        val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)

        val displayValue = dragFraction ?: value
        val progressX = (trackStart + displayValue.coerceIn(0f, 1f) * trackWidth).coerceIn(trackStart, trackEnd)

        if (progressX < trackEnd) {
            val inactiveStart = (progressX + 4.dp.toPx() * (1f - thumbInteractionFraction)).coerceAtMost(trackEnd)
            if (inactiveStart < trackEnd) {
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(inactiveStart, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }

        if (progressX > trackStart) {
            wavePath.reset()
            wavePath.moveTo(trackStart, centerY)

            if (amplitudePx > 0.05f) {
                val step = (wavelengthPx / 8f).coerceAtLeast(3.5f)
                val invFadeIn = 1f / (wavelengthPx * 0.6f)
                val invFadeOut = 1f / (wavelengthPx * 0.4f)
                val k = (2f * PI.toFloat()) / wavelengthPx

                var x = trackStart
                while (x <= progressX) {
                    val relX = x - trackStart
                    val fadeIn = (relX * invFadeIn).coerceAtMost(1f)
                    val fadeOut = ((progressX - x) * invFadeOut).coerceIn(0f, 1f)
                    val y = centerY + sin(relX * k - currentPhase) * (amplitudePx * fadeIn * fadeOut)
                    wavePath.lineTo(x, y)
                    x += step
                }
                wavePath.lineTo(progressX, centerY)
            } else {
                wavePath.lineTo(progressX, centerY)
            }

            drawPath(
                path = wavePath,
                color = activeTrackColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        val baseThumbRadius = 5.dp.toPx()
        val draggingThumbRadius = 7.dp.toPx()
        val thumbRadius = baseThumbRadius + (draggingThumbRadius - baseThumbRadius) * thumbInteractionFraction

        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(progressX, centerY)
        )
    }
}
