package com.kayoi.pixelplayerdesktop.presentation.components

import kotlin.math.roundToInt

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_unfold_more

@Composable
fun ExpressiveScrollBar(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    minHeight: Dp = 48.dp,
    thickness: Dp = 8.dp,
    indicatorExpandedWidth: Dp = 24.dp,
    indicatorExpandedWidthBoost: Dp = 4.dp,
    indicatorRightCornerRadius: Dp = 6.dp,
    paddingEnd: Dp = 4.dp,
    trackGap: Dp = 8.dp,
    dragLabelProvider: ((Int) -> String?)? = null,
    dragLabelSize: Dp = 42.dp,
    dragLabelGap: Dp = 12.dp,
    trackColor: Color? = null,
    thumbColor: Color? = null
) {
    val adapter = when {
        listState != null -> rememberScrollbarAdapter(listState)
        gridState != null -> rememberScrollbarAdapter(gridState)
        else -> null
    }

    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var isDragging by remember { mutableStateOf(false) }
    var dragThumbY by remember { mutableStateOf<Float?>(null) }
    var grabOffset by remember { mutableFloatStateOf(0f) }
    var retainedDragLabel by remember { mutableStateOf<String?>(null) }

    val primaryColor = thumbColor ?: MaterialTheme.colorScheme.primary
    val surfaceVariantColor = trackColor ?: MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    val innerIcon = Res.drawable.ic_rounded_unfold_more
    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)

    val isInteracting = isHovered || isDragging

    val animatedWidth by animateDpAsState(
        targetValue = if (isInteracting) expandedIndicatorWidth else thickness,
        animationSpec = tween(durationMillis = 200),
        label = "WidthAnimation"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "IconAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(expandedIndicatorWidth + paddingEnd)
    ) {
        val density = LocalDensity.current
        val constraintsMaxWidth = maxWidth
        val constraintsMaxHeight = maxHeight
        val availableHeightPx = with(density) { constraintsMaxHeight.toPx() }
        val minHeightPx = with(density) { minHeight.toPx() }
        val indicatorRightCornerRadiusPx = with(density) { indicatorRightCornerRadius.toPx() }

        val thumbHeightPx by remember(adapter, availableHeightPx, minHeightPx) {
            derivedStateOf {
                val content = adapter?.contentSize ?: 0.0
                val viewport = adapter?.viewportSize ?: 0.0
                if (content <= 0.0 || viewport <= 0.0 || availableHeightPx <= 0f) {
                    minHeightPx
                } else {
                    val ratio = (viewport / content).toFloat()
                    (ratio * availableHeightPx).coerceIn(minHeightPx, availableHeightPx)
                }
            }
        }

        val scrollableTrackPx by remember(availableHeightPx, thumbHeightPx) {
            derivedStateOf {
                (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)
            }
        }

        val maxScrollOffset by remember(adapter) {
            derivedStateOf {
                val content = adapter?.contentSize ?: 0.0
                val viewport = adapter?.viewportSize ?: 0.0
                (content - viewport).coerceAtLeast(0.0)
            }
        }

        val scrollScale by remember(scrollableTrackPx, maxScrollOffset) {
            derivedStateOf {
                if (maxScrollOffset > 0.0) scrollableTrackPx / maxScrollOffset else 1.0
            }
        }

        val currentThumbY by remember(adapter, scrollScale, scrollableTrackPx) {
            derivedStateOf {
                if (maxScrollOffset > 0.0) {
                    val offset = adapter?.scrollOffset ?: 0.0
                    (offset * scrollScale).toFloat().coerceIn(0f, scrollableTrackPx)
                } else {
                    0f
                }
            }
        }

        val totalItemsCount = listState?.layoutInfo?.totalItemsCount ?: gridState?.layoutInfo?.totalItemsCount ?: 0

        val isScrollBarVisible by remember(adapter, maxScrollOffset, totalItemsCount) {
            derivedStateOf {
                maxScrollOffset > 1.0 && totalItemsCount > 1
            }
        }

        val scrollBarAlpha by animateFloatAsState(
            targetValue = if (isScrollBarVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 150),
            label = "ScrollBarAlpha"
        )

        val activeDragLabelTargetIndex by remember(isDragging, dragThumbY, currentThumbY, scrollableTrackPx, totalItemsCount) {
            derivedStateOf {
                if (!isDragging || totalItemsCount <= 0) return@derivedStateOf -1
                val y = dragThumbY ?: currentThumbY
                val fraction = if (scrollableTrackPx > 0f) (y / scrollableTrackPx).coerceIn(0f, 1f) else 0f
                (fraction * (totalItemsCount - 1)).roundToInt().coerceIn(0, totalItemsCount - 1)
            }
        }

        val activeDragLabel = if (isDragging && dragLabelProvider != null && activeDragLabelTargetIndex >= 0) {
            dragLabelProvider(activeDragLabelTargetIndex)
        } else {
            null
        }

        val showDragLabel = isDragging && !activeDragLabel.isNullOrBlank()

        LaunchedEffect(activeDragLabel) {
            if (!activeDragLabel.isNullOrBlank()) {
                retainedDragLabel = activeDragLabel
            }
        }

        val dragLabelAlpha by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelAlpha"
        )
        val dragLabelScale by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0.82f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelScale"
        )
        val dragLabelSlide by animateDpAsState(
            targetValue = if (showDragLabel) 0.dp else 8.dp,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "DragLabelSlide"
        )

        val indicatorPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrollBarAlpha }
                .hoverable(interactionSource)
                .pointerInput(adapter, scrollableTrackPx, scrollScale, thumbHeightPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val thumbY = dragThumbY ?: currentThumbY
                            val isTouchOnThumb = offset.y >= thumbY && offset.y <= (thumbY + thumbHeightPx)
                            grabOffset = if (isTouchOnThumb) {
                                offset.y - thumbY
                            } else {
                                thumbHeightPx / 2f
                            }
                            val targetY = (offset.y - grabOffset).coerceIn(0f, scrollableTrackPx)
                            dragThumbY = targetY
                            val targetScrollOffset = targetY / scrollScale
                            coroutineScope.launch {
                                adapter?.scrollTo(targetScrollOffset)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val targetY = (change.position.y - grabOffset).coerceIn(0f, scrollableTrackPx)
                            dragThumbY = targetY
                            val targetScrollOffset = targetY / scrollScale
                            coroutineScope.launch {
                                adapter?.scrollTo(targetScrollOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragThumbY = null
                        },
                        onDragCancel = {
                            isDragging = false
                            dragThumbY = null
                        }
                    )
                }
        ) {
            val rightAnchorX = with(density) { (constraintsMaxWidth - paddingEnd).toPx() }
            val trackX = rightAnchorX - with(density) { thickness.toPx() / 2 }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val handleY = (dragThumbY ?: currentThumbY).coerceIn(0f, scrollableTrackPx)
                val trackStrokeWidth = thickness.toPx()
                val indicatorWidthPx = animatedWidth.toPx()
                val gapPx = trackGap.toPx()
                val indicatorLeftCornerRadius = indicatorWidthPx / 2f
                val maxAllowedRightCornerRadius = minOf(indicatorWidthPx / 2f, thumbHeightPx / 2f)
                val resolvedRightCornerRadius = indicatorRightCornerRadiusPx.coerceIn(0f, maxAllowedRightCornerRadius)

                val currentIndicatorX = rightAnchorX - indicatorWidthPx

                if (handleY > gapPx) {
                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(trackX, 0f),
                        end = Offset(trackX, handleY - gapPx),
                        strokeWidth = trackStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                if (handleY + thumbHeightPx + gapPx < size.height) {
                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(trackX, handleY + thumbHeightPx + gapPx),
                        end = Offset(trackX, size.height),
                        strokeWidth = trackStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                indicatorPath.reset()
                indicatorPath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(currentIndicatorX, handleY),
                            size = Size(indicatorWidthPx, thumbHeightPx)
                        ),
                        topLeft = CornerRadius(indicatorLeftCornerRadius, indicatorLeftCornerRadius),
                        topRight = CornerRadius(resolvedRightCornerRadius, resolvedRightCornerRadius),
                        bottomRight = CornerRadius(resolvedRightCornerRadius, resolvedRightCornerRadius),
                        bottomLeft = CornerRadius(indicatorLeftCornerRadius, indicatorLeftCornerRadius)
                    )
                )
                drawPath(
                    path = indicatorPath,
                    color = primaryColor
                )
            }

            if (iconAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .offset {
                            val handleY = (dragThumbY ?: currentThumbY).coerceIn(0f, scrollableTrackPx)
                            val iconSizePx = 24.dp.toPx()
                            val paddingEndPx = paddingEnd.toPx()
                            val animatedWidthPx = animatedWidth.toPx()
                            val maxWidthPx = constraintsMaxWidth.toPx()

                            val x = maxWidthPx - paddingEndPx - (animatedWidthPx / 2) - (iconSizePx / 2)
                            val y = handleY + (thumbHeightPx / 2) - (iconSizePx / 2)

                            IntOffset(x.toInt(), y.toInt())
                        }
                        .size(24.dp)
                        .graphicsLayer {
                            alpha = iconAlpha
                            scaleX = iconAlpha
                            scaleY = iconAlpha
                        }
                ) {
                    Icon(
                        painter = painterResource(innerIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            val displayedDragLabel = activeDragLabel ?: retainedDragLabel
            if (dragLabelAlpha > 0f && !displayedDragLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .offset {
                            val handleY = (dragThumbY ?: currentThumbY).coerceIn(0f, scrollableTrackPx)
                            val dragLabelSizePx = dragLabelSize.toPx()
                            val dragLabelGapPx = dragLabelGap.toPx()
                            val dragLabelSlidePx = dragLabelSlide.toPx()
                            val paddingEndPx = paddingEnd.toPx()
                            val animatedWidthPx = animatedWidth.toPx()
                            val maxWidthPx = constraintsMaxWidth.toPx()

                            val indicatorX = maxWidthPx - paddingEndPx - animatedWidthPx
                            val x = indicatorX - dragLabelSizePx - dragLabelGapPx - dragLabelSlidePx
                            val y = handleY + (thumbHeightPx / 2f) - (dragLabelSizePx / 2f)

                            IntOffset(x.toInt(), y.toInt())
                        }
                        .size(dragLabelSize)
                        .graphicsLayer {
                            alpha = dragLabelAlpha
                            scaleX = dragLabelScale
                            scaleY = dragLabelScale
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayedDragLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
