package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_repeat_one
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_timer
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    TrackDialogInput()

    val colors = MaterialTheme.colorScheme

    val isEndOfTrackActive by viewModel.isEndOfTrackTimerActive.collectAsState()
    val activeDurationMinutes by viewModel.activeTimerDurationMinutes.collectAsState()
    val activeDisplay by viewModel.activeTimerValueDisplay.collectAsState()
    val remainingSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()
    val playCount by viewModel.sleepTimerPlayCount.collectAsState()

    val isAnyTimerActive = isEndOfTrackActive || activeDurationMinutes != null || playCount > 1f

    var sliderMinutes by remember(activeDurationMinutes) {
        mutableFloatStateOf((activeDurationMinutes ?: 30).toFloat())
    }
    var localPlayCount by remember(playCount) {
        mutableIntStateOf(playCount.roundToInt().coerceIn(1, 10))
    }

    val presets = listOf(5, 10, 15, 30, 45, 60, 90)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isAnyTimerActive) colors.primary else colors.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_timer),
                                contentDescription = null,
                                tint = if (isAnyTimerActive) colors.onPrimary else colors.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sleep Timer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            val statusText = when {
                                remainingSeconds != null && remainingSeconds!! > 0 -> {
                                    val mins = remainingSeconds!! / 60
                                    val secs = remainingSeconds!! % 60
                                    "Active: %02d:%02d remaining".format(mins, secs)
                                }
                                isEndOfTrackActive -> "Active: Stop at end of track"
                                playCount > 1f -> "Active: Playing ${playCount.roundToInt()} times"
                                else -> "Off"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAnyTimerActive) colors.primary else colors.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setEndOfTrackTimer(!isEndOfTrackActive)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End of track",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Pause after the current song ends",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        ExpressiveSwitch(
                            checked = isEndOfTrackActive,
                            onCheckedChange = { viewModel.setEndOfTrackTimer(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Timer Duration",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { minutes ->
                        val isSelected = activeDurationMinutes == minutes
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                sliderMinutes = minutes.toFloat()
                                viewModel.setSleepTimer(minutes)
                            },
                            label = { Text("${minutes}m") },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_check),
                                        contentDescription = null,
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                                selectedLeadingIconColor = colors.onPrimary,
                                containerColor = colors.surfaceContainer,
                                labelColor = colors.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Custom: ${sliderMinutes.roundToInt()} min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.setSleepTimer(sliderMinutes.roundToInt()) }
                    ) {
                        Text("Apply")
                    }
                }

                Slider(
                    value = sliderMinutes,
                    onValueChange = { sliderMinutes = it },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_repeat_one),
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Play current song $localPlayCount times",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                            }
                            TextButton(
                                onClick = { viewModel.playCounted(localPlayCount) }
                            ) {
                                Text("Start")
                            }
                        }

                        Slider(
                            value = localPlayCount.toFloat(),
                            onValueChange = { localPlayCount = it.roundToInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceContainerHighest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isAnyTimerActive) {
                        TextButton(
                            onClick = {
                                viewModel.cancelAllSleepTimers()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                        ) {
                            Text("Turn Off")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
