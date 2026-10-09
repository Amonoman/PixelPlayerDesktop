package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * An expressive audio playback slider featuring an animated sine wave track when active
 * and a smooth flat transition when paused or scrubbed.
 */
@Composable
fun WavySliderExpressive(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
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

    // Interpolate wave amplitude to 0 when playback is paused or the user is actively seeking
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (enabled && isPlaying && !isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavyAmplitude"
    )

    // Expand thumb size smoothly during user drag interaction
    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "thumbInteraction"
    )

    // Continuous looping phase offset for smooth horizontal wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "wavyProgressPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val edgePadPx = trackEdgePadding.toPx()
                    val trackWidth = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                    val progress = ((offset.x - edgePadPx) / trackWidth).coerceIn(0f, 1f)
                    onValueChange(progress)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val edgePadPx = trackEdgePadding.toPx()
                        val trackWidth = (size.width - 2f * edgePadPx).coerceAtLeast(1f)
                        val progress = ((change.position.x - edgePadPx) / trackWidth).coerceIn(0f, 1f)
                        onValueChange(progress)
                    }
                )
            }
    ) {
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

        val progressX = (trackStart + value.coerceIn(0f, 1f) * trackWidth).coerceIn(trackStart, trackEnd)

        // Draw inactive background track from current progress thumb to end boundary
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

        // Draw active track with sinusoidal modulation
        if (progressX > trackStart) {
            val path = Path()
            path.moveTo(trackStart, centerY)

            if (amplitudePx > 0.05f) {
                val step = 1.5f
                var x = trackStart
                while (x <= progressX) {
                    val relX = x - trackStart
                    // Taper amplitude near track start and the thumb to prevent harsh discontinuities
                    val fadeIn = (relX / (wavelengthPx * 0.6f)).coerceIn(0f, 1f)
                    val fadeOut = ((progressX - x) / (wavelengthPx * 0.4f)).coerceIn(0f, 1f)
                    val envelope = fadeIn * fadeOut

                    val angle = (relX / wavelengthPx) * 2f * PI.toFloat() - phase
                    val y = centerY + sin(angle) * amplitudePx * envelope
                    path.lineTo(x, y)
                    x += step
                }
                path.lineTo(progressX, centerY)
            } else {
                path.lineTo(progressX, centerY)
            }

            drawPath(
                path = path,
                color = activeTrackColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Draw draggable interactive thumb
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
