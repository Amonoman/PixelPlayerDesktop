package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_unfold_more
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

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
    trackColor: androidx.compose.ui.graphics.Color? = null,
    thumbColor: androidx.compose.ui.graphics.Color? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(-1f) }
    var pendingScrollIndex by remember { mutableIntStateOf(-1) }
    var retainedDragLabel by remember { mutableStateOf<String?>(null) }

    val primaryColor = thumbColor ?: MaterialTheme.colorScheme.primary
    val surfaceVariantColor = trackColor ?: MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
    val innerIcon = Res.drawable.ic_rounded_unfold_more
    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)

    val isInteracting = isPressed || isDragging

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
        val indicatorRightCornerRadiusPx = with(density) { indicatorRightCornerRadius.toPx() }

        val totalItemsCount by remember(listState, gridState) {
            derivedStateOf {
                listState?.layoutInfo?.totalItemsCount ?: gridState?.layoutInfo?.totalItemsCount ?: 0
            }
        }

        val realScrollProgress by remember(listState, gridState) {
            derivedStateOf {
                if (listState != null) {
                    val layoutInfo = listState.layoutInfo
                    val total = layoutInfo.totalItemsCount
                    val visible = layoutInfo.visibleItemsInfo.size
                    if (total <= visible || total == 0) return@derivedStateOf 0f

                    val firstIndex = listState.firstVisibleItemIndex
                    val firstOffset = listState.firstVisibleItemScrollOffset
                    val avgItemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                    val estimatedOffset = (firstIndex.toFloat() + (firstOffset / avgItemSize.coerceAtLeast(1f)))
                    val maxEstimated = (total - visible).coerceAtLeast(1).toFloat()
                    (estimatedOffset / maxEstimated).coerceIn(0f, 1f)
                } else if (gridState != null) {
                    val layoutInfo = gridState.layoutInfo
                    val total = layoutInfo.totalItemsCount
                    val visible = layoutInfo.visibleItemsInfo.size
                    if (total <= visible || total == 0) return@derivedStateOf 0f

                    val firstIndex = gridState.firstVisibleItemIndex
                    val firstOffset = gridState.firstVisibleItemScrollOffset
                    val avgItemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat() ?: 1f
                    val estimatedOffset = (firstIndex.toFloat() + (firstOffset / avgItemSize.coerceAtLeast(1f)))
                    val maxEstimated = (total - visible).coerceAtLeast(1).toFloat()
                    (estimatedOffset / maxEstimated).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

        // Scroll list when dragging
        LaunchedEffect(listState, gridState) {
            snapshotFlow { pendingScrollIndex }
                .distinctUntilChanged()
                .collectLatest { index ->
                    if (index >= 0) {
                        listState?.scrollToItem(index)
                        gridState?.scrollToItem(index)
                    }
                }
        }

        fun updateProgressFromTouch(touchY: Float, grabOffset: Float) {
            val availableHeight = with(density) { constraintsMaxHeight.toPx() }
            val handleHeightPx = with(density) { minHeight.toPx() }
            val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)

            val targetHandleTop = touchY - grabOffset
            val newProgress = (targetHandleTop / scrollableHeight).coerceIn(0f, 1f)

            dragProgress = newProgress
            val targetIdx = (newProgress * (totalItemsCount - 1).coerceAtLeast(0)).toInt()
            pendingScrollIndex = targetIdx.coerceIn(0, (totalItemsCount - 1).coerceAtLeast(0))
        }

        val dragLabelTargetIndex = when {
            pendingScrollIndex >= 0 -> pendingScrollIndex
            listState != null -> listState.firstVisibleItemIndex
            gridState != null -> gridState.firstVisibleItemIndex
            else -> -1
        }
        val activeDragLabel =
            if (isDragging && dragLabelProvider != null && dragLabelTargetIndex >= 0) {
                dragLabelProvider(dragLabelTargetIndex)
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

        val isScrollBarVisible by remember(listState, gridState) {
            derivedStateOf {
                val total = listState?.layoutInfo?.totalItemsCount ?: gridState?.layoutInfo?.totalItemsCount ?: 0
                val visible = listState?.layoutInfo?.visibleItemsInfo?.size ?: gridState?.layoutInfo?.visibleItemsInfo?.size ?: 0
                val canForward = listState?.canScrollForward ?: gridState?.canScrollForward ?: false
                val canBackward = listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false
                (canForward || canBackward || total > visible) && total > 1
            }
        }
        val scrollBarAlpha by animateFloatAsState(
            targetValue = if (isScrollBarVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 150),
            label = "ScrollBarAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrollBarAlpha }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        }
                    )
                }
                .pointerInput(totalItemsCount) {
                    var grabOffset = 0f

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true

                            val availableHeight = constraintsMaxHeight.toPx()
                            val handleHeightPx = minHeight.toPx()
                            val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)

                            val visualProgress = if (dragProgress >= 0f) dragProgress else realScrollProgress
                            val handleY = visualProgress * scrollableHeight

                            val isTouchOnHandle = offset.y >= handleY && offset.y <= (handleY + handleHeightPx)

                            if (isTouchOnHandle) {
                                grabOffset = offset.y - handleY
                                dragProgress = visualProgress
                                pendingScrollIndex =
                                    listState?.firstVisibleItemIndex
                                        ?: gridState?.firstVisibleItemIndex
                                        ?: 0
                            } else {
                                grabOffset = handleHeightPx / 2f
                                updateProgressFromTouch(offset.y, grabOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateProgressFromTouch(change.position.y, grabOffset)
                        }
                    )
                }
        ) {
            val rightAnchorX = with(density) { (constraintsMaxWidth - paddingEnd).toPx() }
            val trackX = rightAnchorX - with(density) { thickness.toPx() / 2 }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val availableHeight = constraintsMaxHeight.toPx()
                val handleHeightPx = minHeight.toPx()
                val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)

                val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else realScrollProgress
                val handleY = (displayProgress * scrollableHeight).coerceIn(0f, scrollableHeight)

                val trackStrokeWidth = thickness.toPx()
                val indicatorWidthPx = animatedWidth.toPx()
                val gapPx = trackGap.toPx()
                val indicatorLeftCornerRadius = indicatorWidthPx / 2f
                val maxAllowedRightCornerRadius = minOf(indicatorWidthPx / 2f, handleHeightPx / 2f)
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

                if (handleY + handleHeightPx + gapPx < size.height) {
                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(trackX, handleY + handleHeightPx + gapPx),
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
                            size = Size(indicatorWidthPx, handleHeightPx)
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
                            val availableHeight = constraintsMaxHeight.toPx()
                            val handleHeightPx = minHeight.toPx()
                            val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)
                            val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else realScrollProgress
                            val handleY = displayProgress * scrollableHeight

                            val iconSizePx = 24.dp.toPx()
                            val paddingEndPx = paddingEnd.toPx()
                            val animatedWidthPx = animatedWidth.toPx()
                            val maxWidthPx = constraintsMaxWidth.toPx()

                            val x = maxWidthPx - paddingEndPx - (animatedWidthPx / 2) - (iconSizePx / 2)
                            val y = handleY + (handleHeightPx / 2) - (iconSizePx / 2)

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
                            val availableHeight = constraintsMaxHeight.toPx()
                            val handleHeightPx = minHeight.toPx()
                            val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)
                            val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else realScrollProgress
                            val handleY = displayProgress * scrollableHeight
                            val dragLabelSizePx = dragLabelSize.toPx()
                            val dragLabelGapPx = dragLabelGap.toPx()
                            val dragLabelSlidePx = dragLabelSlide.toPx()
                            val paddingEndPx = paddingEnd.toPx()
                            val animatedWidthPx = animatedWidth.toPx()
                            val maxWidthPx = constraintsMaxWidth.toPx()

                            val indicatorX = maxWidthPx - paddingEndPx - animatedWidthPx
                            val x = indicatorX - dragLabelSizePx - dragLabelGapPx - dragLabelSlidePx
                            val y = handleY + (handleHeightPx / 2f) - (dragLabelSizePx / 2f)

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
