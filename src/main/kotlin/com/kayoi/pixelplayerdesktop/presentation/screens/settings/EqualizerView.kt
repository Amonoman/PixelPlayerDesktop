package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.components.trackInputFocus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitchRow
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@Composable
fun EqualizerContent(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    isDialogMode: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val equalizerPresetName by viewModel.equalizerPreset.collectAsState()
    val bandLevels by viewModel.equalizerBands.collectAsState()
    val preamp by viewModel.equalizerPreamp.collectAsState()
    val customPresets by viewModel.customEqualizerPresets.collectAsState()
    val pinnedPresetNames by viewModel.pinnedEqualizerPresets.collectAsState()
    val viewMode by viewModel.equalizerViewMode.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }

    val allPresets = remember(customPresets) { EqualizerPreset.ALL_PRESETS + customPresets }
    val currentPreset = remember(equalizerPresetName, customPresets, bandLevels, preamp) {
        if (equalizerPresetName == "custom") {
            EqualizerPreset.custom(bandLevels, preamp)
        } else {
            EqualizerPreset.fromName(equalizerPresetName, customPresets)
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            initialName = if (equalizerPresetName != "custom") "$equalizerPresetName copy" else "My Preset",
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCustomEqualizerPreset(name)
                showSaveDialog = false
            }
        )
    }

    if (showManageDialog) {
        ManagePresetsDialog(
            customPresets = customPresets,
            pinnedNames = pinnedPresetNames,
            onRename = { old, new -> viewModel.renameCustomEqualizerPreset(old, new) },
            onDelete = { name -> viewModel.deleteCustomEqualizerPreset(name) },
            onTogglePin = { name ->
                val updated = if (pinnedPresetNames.contains(name)) {
                    pinnedPresetNames - name
                } else {
                    pinnedPresetNames + name
                }
                viewModel.updatePinnedEqualizerPresets(updated)
            },
            onDismiss = { showManageDialog = false }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpressiveSwitchRow(
            title = "Equalizer",
            subtitle = if (equalizerEnabled) "Audio processing active • ${currentPreset.displayName}" else "Equalizer bypassed",
            checked = equalizerEnabled,
            onCheckedChange = { viewModel.setEqualizerEnabled(it) },
            leadingIcon = {
                Icon(Icons.Rounded.GraphicEq, null, tint = colors.secondary, modifier = Modifier.size(24.dp))
            },
            shape = RoundedCornerShape(20.dp)
        )

        AnimatedVisibility(
            visible = equalizerEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PRESETS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ViewModeSelector(
                                    currentMode = viewMode,
                                    onModeSelected = { viewModel.setEqualizerViewMode(it) }
                                )

                                FilledTonalIconButton(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = colors.surfaceContainerHighest
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Save Preset", modifier = Modifier.size(18.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = { showManageDialog = true },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = colors.surfaceContainerHighest
                                    )
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = "Manage Presets", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        val visiblePresets = remember(pinnedPresetNames, allPresets) {
                            val pinned = pinnedPresetNames.mapNotNull { name ->
                                allPresets.find { it.name.equals(name, ignoreCase = true) }
                            }
                            if (equalizerPresetName == "custom" && pinned.none { it.name == "custom" }) {
                                pinned + EqualizerPreset.custom(bandLevels, preamp)
                            } else {
                                pinned
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            visiblePresets.forEach { preset ->
                                val isSelected = currentPreset.name.equals(preset.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setEqualizerPreset(preset) },
                                    label = { Text(preset.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primary,
                                        selectedLabelColor = colors.onPrimary,
                                        containerColor = colors.surfaceContainerHighest,
                                        labelColor = colors.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Preamp Gain",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${if (preamp > 0) "+" else ""}${preamp.roundToInt()} dB",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }

                            if (preamp != 0f) {
                                TextButton(
                                    onClick = { viewModel.setEqualizerPreamp(0f) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset 0 dB", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Slider(
                            value = preamp,
                            onValueChange = { viewModel.setEqualizerPreamp(it) },
                            valueRange = -15f..15f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FREQUENCY BANDS (10-BAND)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )

                            if (bandLevels.any { it != 0f }) {
                                TextButton(
                                    onClick = { viewModel.setEqualizerPreset(EqualizerPreset.FLAT) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flat Reset", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isDialogMode) 260.dp else 300.dp)
                        ) {
                            if (viewMode == "GRAPH" || viewMode == "HYBRID") {
                                EqualizerSplineGraph(
                                    bandLevels = bandLevels,
                                    isEnabled = equalizerEnabled,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (viewMode == "SLIDERS" || viewMode == "HYBRID") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    EqualizerPreset.BAND_FREQUENCIES.forEachIndexed { index, freqLabel ->
                                        val level = bandLevels.getOrElse(index) { 0f }
                                        BandSliderColumn(
                                            frequency = freqLabel,
                                            level = level,
                                            isEnabled = equalizerEnabled,
                                            onLevelChange = { newLevel ->
                                                viewModel.setEqualizerBand(index, newLevel)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val modes = listOf(
        Triple("SLIDERS", Icons.Rounded.GraphicEq, "Sliders"),
        Triple("GRAPH", Icons.AutoMirrored.Rounded.ShowChart, "Graph"),
        Triple("HYBRID", Icons.AutoMirrored.Rounded.ViewQuilt, "Hybrid")
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerHighest)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        modes.forEach { (modeKey, icon, title) ->
            val isSelected = currentMode == modeKey
            val bg by animateColorAsState(if (isSelected) colors.primary else Color.Transparent, label = "viewModeBg")
            val iconColor by animateColorAsState(if (isSelected) colors.onPrimary else colors.onSurfaceVariant, label = "viewModeIcon")

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onModeSelected(modeKey) },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BandSliderColumn(
    frequency: String,
    level: Float,
    isEnabled: Boolean,
    onLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val intLevel = level.roundToInt()

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${if (intLevel > 0) "+" else ""}$intLevel",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (intLevel != 0) FontWeight.Bold else FontWeight.Normal,
            color = if (intLevel != 0) colors.primary else colors.onSurfaceVariant,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            CustomVerticalSlider(
                value = level,
                onValueChange = onLevelChange,
                valueRange = -15f..15f,
                enabled = isEnabled,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceContainerHighest,
                thumbColor = colors.primary,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
            )
        }

        Text(
            text = frequency.replace("Hz", "").replace("kHz", "k"),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    activeTrackColor: Color,
    inactiveTrackColor: Color,
    thumbColor: Color,
    thumbSize: Dp = 16.dp,
    trackThickness: Dp = 4.dp
) {
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val thumbRadiusPx = thumbSizePx / 2
    val trackThicknessPx = with(density) { trackThickness.toPx() }
    val verticalPaddingPx = with(density) { 4.dp.toPx() }

    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isInteracting by remember { mutableStateOf(false) }
    var dragNormalizedValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayNormalizedValue = if (isInteracting) dragNormalizedValue else normalizedValue

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val heightPx = with(density) { maxHeight.toPx() }
        val trackHeight = (heightPx - thumbSizePx - (verticalPaddingPx * 2)).coerceAtLeast(1f)
        val thumbCenterY = heightPx - verticalPaddingPx - thumbRadiusPx - (displayNormalizedValue * trackHeight)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, valueRange.start, valueRange.endInclusive, trackHeight, heightPx) {
                    if (!enabled) return@pointerInput

                    fun dispatchValue(touchY: Float) {
                        val trackTopY = verticalPaddingPx + thumbRadiusPx
                        val relativeY = (touchY - trackTopY).coerceIn(0f, trackHeight)
                        val newNormalized = 1f - (relativeY / trackHeight)
                        dragNormalizedValue = newNormalized
                        val newValue = valueRange.start + newNormalized * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue.roundToInt().toFloat())
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isInteracting = true
                        down.consume()
                        dispatchValue(down.position.y)

                        var activePointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }?.also { activePointerId = it.id }
                                ?: break

                            if (!pointerChange.pressed) {
                                pointerChange.consume()
                                break
                            }

                            if (pointerChange.position.y != pointerChange.previousPosition.y) {
                                pointerChange.consume()
                                dispatchValue(pointerChange.position.y)
                            }
                        }
                        isInteracting = false
                    }
                }
        ) {
            val centerX = size.width / 2

            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(centerX - (trackThicknessPx / 2), verticalPaddingPx + thumbRadiusPx),
                size = Size(trackThicknessPx, trackHeight),
                cornerRadius = CornerRadius(trackThicknessPx / 2)
            )

            val centerY0 = heightPx - verticalPaddingPx - thumbRadiusPx - (0.5f * trackHeight)
            val activeHeight = kotlin.math.abs(thumbCenterY - centerY0)
            val activeTop = minOf(thumbCenterY, centerY0)

            if (activeHeight > 0f) {
                drawRoundRect(
                    color = if (enabled) activeTrackColor else activeTrackColor.copy(alpha = 0.3f),
                    topLeft = Offset(centerX - (trackThicknessPx / 2), activeTop),
                    size = Size(trackThicknessPx, activeHeight),
                    cornerRadius = CornerRadius(trackThicknessPx / 2)
                )
            }

            drawCircle(
                color = if (enabled) thumbColor else Color.Gray,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbCenterY)
            )
        }
    }
}

@Composable
private fun EqualizerSplineGraph(
    bandLevels: List<Float>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.padding(horizontal = 12.dp, vertical = 24.dp)) {
        if (bandLevels.isEmpty()) return@Canvas

        val widthPerBand = size.width / bandLevels.size
        val points = bandLevels.mapIndexed { index, level ->
            val x = (widthPerBand * index) + (widthPerBand / 2)
            val normalized = ((level - (-15f)) / (15f - -15f)).coerceIn(0f, 1f)
            val y = (1f - normalized) * size.height
            Offset(x, y)
        }

        val zeroY = size.height / 2
        drawLine(
            color = primaryColor.copy(alpha = 0.2f),
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 1.dp.toPx()
        )

        if (points.isNotEmpty()) {
            val path = Path()
            path.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[maxOf(0, i - 1)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[minOf(points.size - 1, i + 2)]

                val cp1X = p1.x + (p2.x - p0.x) * 0.2f
                val cp1Y = p1.y + (p2.y - p0.y) * 0.2f
                val cp2X = p2.x - (p3.x - p1.x) * 0.2f
                val cp2Y = p2.y - (p3.y - p1.y) * 0.2f

                path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
            }

            drawPath(
                path = path,
                color = if (isEnabled) primaryColor else primaryColor.copy(alpha = 0.3f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(points.last().x, size.height)
            fillPath.lineTo(points.first().x, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isEnabled) 0.25f else 0.08f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
    }
}

@Composable
fun EqualizerDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 600.dp, max = 800.dp)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                            }
                        }
                        Text(
                            text = "Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    EqualizerContent(viewModel = viewModel, isDialogMode = true)
                }
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    TrackDialogInput()
    var presetName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Custom Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a unique name for your custom equalizer preset:")
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { if (presetName.isNotBlank()) onSave(presetName.trim()) },
                enabled = presetName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManagePresetsDialog(
    customPresets: List<EqualizerPreset>,
    pinnedNames: List<String>,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var renamingTarget by remember { mutableStateOf<EqualizerPreset?>(null) }
    var newName by remember { mutableStateOf("") }

    if (renamingTarget != null) {
        TrackDialogInput()
        AlertDialog(
            onDismissRequest = { renamingTarget = null },
            title = { Text("Rename Preset") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().trackInputFocus()
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val target = renamingTarget ?: return@FilledTonalButton
                        if (newName.isNotBlank()) {
                            onRename(target.name, newName.trim())
                            renamingTarget = null
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingTarget = null }) { Text("Cancel") }
            }
        )
    }

    TrackDialogInput()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Presets") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customPresets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No custom presets created yet.", color = colors.onSurfaceVariant)
                    }
                } else {
                    customPresets.forEach { preset ->
                        val isPinned = pinnedNames.contains(preset.name)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.displayName,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onTogglePin(preset.name) }) {
                                        Icon(
                                            Icons.Rounded.PushPin,
                                            contentDescription = "Pin",
                                            tint = if (isPinned) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(onClick = {
                                        renamingTarget = preset
                                        newName = preset.displayName
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = colors.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onDelete(preset.name) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
