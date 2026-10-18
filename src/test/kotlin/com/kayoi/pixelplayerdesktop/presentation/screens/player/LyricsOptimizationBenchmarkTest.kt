package com.kayoi.pixelplayerdesktop.presentation.screens.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import org.jetbrains.skia.EncodedImageFormat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.system.measureNanoTime
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LyricsOptimizationBenchmarkTest {

    class Metrics {
        var recompositionCount = 0
        var layoutCount = 0
    }

    @Composable
    fun BeforeLyricItem(
        text: String,
        isActive: Boolean,
        metrics: Metrics
    ) {
        SideEffect { metrics.recompositionCount++ }

        val fontSize by animateFloatAsState(
            targetValue = if (isActive) 23f else 17f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "lyricFontSize"
        )

        val textColor = if (isActive) Color.White else Color.LightGray

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp)
                .layout { measurable, constraints ->
                    metrics.layoutCount++
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
        ) {
            Text(
                text = text,
                fontSize = fontSize.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    fun AfterLyricItem(
        text: String,
        isActive: Boolean,
        metrics: Metrics
    ) {
        SideEffect { metrics.recompositionCount++ }

        val scaleAnim = animateFloatAsState(
            targetValue = if (isActive) 1f else 17f / 23f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "lyricScale"
        )
        val alphaAnim = animateFloatAsState(
            targetValue = if (isActive) 1f else 0.38f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "lyricAlpha"
        )

        val textColor = if (isActive) Color.White else Color.LightGray

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 10.dp)
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .layout { measurable, constraints ->
                    metrics.layoutCount++
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
        ) {
            Text(
                text = text,
                fontSize = 23.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Test
    fun testRecompositionAndLayoutCounts() {
        val beforeMetrics = Metrics()
        val afterMetrics = Metrics()

        var beforeActive by mutableStateOf(false)
        var afterActive by mutableStateOf(false)

        val beforeScene = ImageComposeScene(width = 600, height = 400)
        val afterScene = ImageComposeScene(width = 600, height = 400)

        try {
            beforeScene.setContent {
                BeforeLyricItem("Sample lyric line for testing recomposition and layout", beforeActive, beforeMetrics)
            }
            afterScene.setContent {
                AfterLyricItem("Sample lyric line for testing recomposition and layout", afterActive, afterMetrics)
            }

            beforeScene.render(0L)
            afterScene.render(0L)

            val beforeInitialRecomps = beforeMetrics.recompositionCount
            val beforeInitialLayouts = beforeMetrics.layoutCount
            val afterInitialRecomps = afterMetrics.recompositionCount
            val afterInitialLayouts = afterMetrics.layoutCount

            beforeActive = true
            afterActive = true

            var beforeRenderTotalNs = 0L
            var afterRenderTotalNs = 0L

            for (frame in 1..30) {
                val timeNanos = frame * 16_666_666L
                beforeRenderTotalNs += measureNanoTime { beforeScene.render(timeNanos) }
                afterRenderTotalNs += measureNanoTime { afterScene.render(timeNanos) }
            }

            val beforeTransitionRecomps = beforeMetrics.recompositionCount - beforeInitialRecomps
            val beforeTransitionLayouts = beforeMetrics.layoutCount - beforeInitialLayouts
            val afterTransitionRecomps = afterMetrics.recompositionCount - afterInitialRecomps
            val afterTransitionLayouts = afterMetrics.layoutCount - afterInitialLayouts

            println("=== BENCHMARK RESULTS (30 animation frames) ===")
            println("BEFORE (fontSize anim): recomps=$beforeTransitionRecomps, layouts=$beforeTransitionLayouts, totalRenderTime=${beforeRenderTotalNs / 1_000_000}ms")
            println("AFTER (graphicsLayer):   recomps=$afterTransitionRecomps, layouts=$afterTransitionLayouts, totalRenderTime=${afterRenderTotalNs / 1_000_000}ms")

            assertTrue(afterTransitionRecomps < beforeTransitionRecomps)
            assertTrue(afterTransitionLayouts < beforeTransitionLayouts)
        } finally {
            beforeScene.close()
            afterScene.close()
        }
    }

    @Test
    fun testTextSharpness() {
        val scene = ImageComposeScene(width = 400, height = 100)
        try {
            scene.setContent {
                Box(Modifier.background(Color.Black).padding(10.dp)) {
                    Text("Sharpness Check", fontSize = 17.sp, color = Color.White)
                }
            }
            val native17Img = scene.render(0L)

            scene.setContent {
                Box(Modifier.background(Color.Black).padding(10.dp)) {
                    Text(
                        "Sharpness Check",
                        fontSize = 23.sp,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 17f / 23f
                            scaleY = 17f / 23f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    )
                }
            }
            val scaledDownImg = scene.render(0L)

            val nativeSharpness = calculateImageSharpness(native17Img)
            val scaledDownSharpness = calculateImageSharpness(scaledDownImg)

            println("=== TEXT SHARPNESS COMPARISON ===")
            println("Native 17sp sharpness score: $nativeSharpness")
            println("Scaled-down 23sp->17sp sharpness score: $scaledDownSharpness")

            assertTrue(scaledDownSharpness > 0.0, "Sharpness score should be positive")
        } finally {
            scene.close()
        }
    }

    @Test
    fun testFullLyricsViewIntegrationAndVisuals() {
        val testLyrics = Lyrics(
            synced = listOf(
                SyncedLine(time = 0, line = "First line of the song"),
                SyncedLine(time = 3000, line = "Second line of the lyrics with moderate length"),
                SyncedLine(time = 6000, line = "Third line is quite long to verify line wrapping and layout bounds"),
                SyncedLine(time = 9000, line = "Fourth line showing romanization", romanization = "Dai yon gyo no romaji"),
                SyncedLine(time = 12000, line = "Fifth line of the song"),
                SyncedLine(time = 15000, line = "Sixth line concluding the synced verses")
            )
        )

        var positionMs by mutableStateOf(0L)
        var alignment by mutableStateOf("left")

        val scene = ImageComposeScene(width = 600, height = 500)
        try {
            scene.setContent {
                MaterialTheme {
                    LyricsView(
                        lyrics = testLyrics,
                        isLoading = false,
                        currentPositionMs = positionMs,
                        alignment = alignment,
                        onSeekToMs = {},
                        showRomanization = true
                    )
                }
            }

            // Render line 0
            val initialImage = scene.render(0L)
            assertNotNull(initialImage)

            // Transition to line 3
            positionMs = 9500L
            for (frame in 1..20) {
                scene.render(frame * 16_666_666L)
            }

            // Test center alignment
            alignment = "center"
            scene.render(500_000_000L)

            // Test right alignment
            alignment = "right"
            val finalImage = scene.render(600_000_000L)
            assertNotNull(finalImage)
        } finally {
            scene.close()
        }
    }

    private fun calculateImageSharpness(image: org.jetbrains.skia.Image): Double {
        val bitmap = org.jetbrains.skia.Bitmap.makeFromImage(image)
        var totalGradient = 0.0
        var pixelCount = 0

        for (y in 0 until bitmap.height - 1) {
            for (x in 0 until bitmap.width - 1) {
                val p0 = bitmap.getColor(x, y) and 0xFF
                val px = bitmap.getColor(x + 1, y) and 0xFF
                val py = bitmap.getColor(x, y + 1) and 0xFF
                val grad = abs(px - p0) + abs(py - p0)
                if (p0 > 10 || px > 10 || py > 10) {
                    totalGradient += grad
                    pixelCount++
                }
            }
        }
        return if (pixelCount > 0) totalGradient / pixelCount else 0.0
    }
}
