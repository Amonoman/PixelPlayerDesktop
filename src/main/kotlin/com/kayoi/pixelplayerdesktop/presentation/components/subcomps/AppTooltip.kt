package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltip(
    text: String,
    modifier: Modifier = Modifier,
    delayMillis: Int = 450,
    offset: DpOffset = DpOffset(0.dp, (-6).dp),
    alignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit
) {
    if (text.isBlank()) {
        content()
        return
    }

    val colors = MaterialTheme.colorScheme

    TooltipArea(
        tooltip = {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                visible = true
            }

            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                label = "tooltipAlpha"
            )
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.90f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tooltipScale"
            )

            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(6.dp, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceContainerHighest,
                contentColor = colors.onSurface
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        },
        modifier = modifier,
        delayMillis = delayMillis,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = alignment,
            alignment = alignment,
            offset = offset
        ),
        content = content
    )
}
