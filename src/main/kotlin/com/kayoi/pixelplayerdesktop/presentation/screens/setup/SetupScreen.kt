package com.kayoi.pixelplayerdesktop.presentation.screens.setup

import com.kayoi.pixelplayerdesktop.generated.resources.ic_app_logo
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_arrow_back
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_arrow_forward
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_check_circle
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_cloud_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_folder
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_library_music
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_palette
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_security
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_add
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_dark_mode
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_folder
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_light_mode
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.countAudioFilesDeep
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.RoundedSans
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

private enum class SetupStep(val index: Int, val title: String) {
    WELCOME(0, "Welcome"),
    FOLDERS(1, "Music Folders"),
    PREFERENCES(2, "Preferences"),
    ALL_SET(3, "Ready")
}

@Composable
fun SetupScreen(
    viewModel: PlayerViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var currentStep by remember { mutableStateOf(SetupStep.WELCOME) }

    val savedDirectories by viewModel.musicDirectories.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
    val fetchOnlineLyrics by viewModel.fetchOnlineLyrics.collectAsState()

    val configuredDirectories = remember {
        mutableStateListOf<String>().apply {
            if (savedDirectories.isNotEmpty()) {
                addAll(savedDirectories)
            } else {
                DesktopPathProvider.defaultMusicDirectories.forEach { defDir ->
                    val file = File(defDir)
                    if (file.exists() && file.isDirectory) {
                        add(file.absolutePath)
                    }
                }
            }
        }
    }

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            colors.primary.copy(alpha = 0.08f),
            colors.surfaceContainerLowest
        ),
        radius = 1200f
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceContainerLowest)
            .background(backgroundBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceContainerLow,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_app_logo),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PixelPlayer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.outline
                    )
                    Text(
                        text = "Setup Wizard",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardWidth = maxWidth.coerceAtMost(880.dp)
            val cardHeight = maxHeight.coerceAtMost(650.dp)

            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceContainer
                ),
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SetupStepProgressIndicator(
                        currentStep = currentStep,
                        onStepClick = { targetStep ->
                            if (targetStep.index < currentStep.index) {
                                currentStep = targetStep
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                if (targetState.index > initialState.index) {
                                    (slideInHorizontally { width -> width / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                                }
                            },
                            label = "SetupStepTransition"
                        ) { step ->
                            when (step) {
                                SetupStep.WELCOME -> WelcomeStepContent()
                                SetupStep.FOLDERS -> FoldersStepContent(
                                    directories = configuredDirectories,
                                    onAddDirectory = { dir ->
                                        val abs = dir.absolutePath
                                        if (!configuredDirectories.contains(abs)) {
                                            configuredDirectories.add(abs)
                                        }
                                    },
                                    onRemoveDirectory = { dir ->
                                        configuredDirectories.remove(dir)
                                    }
                                )
                                SetupStep.PREFERENCES -> PreferencesStepContent(
                                    isDarkTheme = isDarkTheme,
                                    onToggleDarkTheme = { viewModel.toggleDarkMode() },
                                    fetchDeezerImages = fetchDeezerImages,
                                    onToggleDeezerImages = { viewModel.setFetchDeezerArtistImages(it) },
                                    fetchOnlineLyrics = fetchOnlineLyrics,
                                    onToggleOnlineLyrics = { viewModel.setFetchOnlineLyrics(it) }
                                )
                                SetupStep.ALL_SET -> AllSetStepContent()
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep.index > 0) {
                            OutlinedButton(
                                onClick = {
                                    val previous = SetupStep.entries.getOrNull(currentStep.index - 1)
                                    if (previous != null) currentStep = previous
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(painterResource(Res.drawable.ic_automirrored_rounded_arrow_back), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (currentStep == SetupStep.ALL_SET) {
                                    savedDirectories.filter { !configuredDirectories.contains(it) }.forEach {
                                        viewModel.removeMusicDirectory(it)
                                    }
                                    configuredDirectories.forEach {
                                        if (!savedDirectories.contains(it)) {
                                            viewModel.addMusicDirectory(it)
                                        }
                                    }
                                    onSetupComplete()
                                } else {
                                    val next = SetupStep.entries.getOrNull(currentStep.index + 1)
                                    if (next != null) currentStep = next
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (currentStep == SetupStep.ALL_SET) {
                                Icon(painterResource(Res.drawable.ic_rounded_check), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Playing", fontWeight = FontWeight.Bold)
                            } else {
                                Text(if (currentStep == SetupStep.WELCOME) "Get Started" else "Continue", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(painterResource(Res.drawable.ic_automirrored_rounded_arrow_forward), contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepProgressIndicator(
    currentStep: SetupStep,
    onStepClick: (SetupStep) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SetupStep.entries.forEach { step ->
                val isActive = step == currentStep
                val isCompleted = step.index < currentStep.index

                val containerColor by animateColorAsState(
                    targetValue = when {
                        isActive -> colors.primary
                        isCompleted -> colors.secondaryContainer
                        else -> Color.Transparent
                    },
                    animationSpec = tween(220),
                    label = "stepPillContainer"
                )

                val contentColor by animateColorAsState(
                    targetValue = when {
                        isActive -> colors.onPrimary
                        isCompleted -> colors.onSecondaryContainer
                        else -> colors.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    animationSpec = tween(220),
                    label = "stepPillContent"
                )

                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = isCompleted) { onStepClick(step) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_check),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (isActive) colors.onPrimary.copy(alpha = 0.25f) else colors.surfaceContainerHighest,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${step.index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isActive) colors.onPrimary else colors.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent() {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_app_logo),
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    SetupSineWaveLine(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                        color = colors.primary.copy(alpha = 0.7f),
                        strokeWidth = 3.dp,
                        amplitude = 5.dp,
                        waves = 6f
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome to PixelPlayer",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "An expressive desktop music player built with Material You aesthetics.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureHighlightCard(
                icon = Res.drawable.ic_outlined_library_music,
                title = "Local Audio",
                description = "Local indexing for FLAC, MP3, Opus, Ogg, and metadata tags.",
                modifier = Modifier.weight(1f)
            )
            FeatureHighlightCard(
                icon = Res.drawable.ic_outlined_palette,
                title = "Material You",
                description = "Adaptive dynamic theming matching album art and your desktop palette.",
                modifier = Modifier.weight(1f)
            )
            FeatureHighlightCard(
                icon = Res.drawable.ic_outlined_security,
                title = "Offline First",
                description = "Offline-first design without unsolicited remote data tracking.",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: DrawableResource,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FoldersStepContent(
    directories: List<String>,
    onAddDirectory: (File) -> Unit,
    onRemoveDirectory: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Music Library Location",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.onSurface
            )
            Text(
                text = "Add the directories containing your music files. PixelPlayer will index these into your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONFIGURED DIRECTORIES (${directories.size})",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )

            FilledTonalButton(
                onClick = {
                    DirectoryPicker.chooseDirectory(title = "Select Music Directory") { dir ->
                        onAddDirectory(dir)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(painterResource(Res.drawable.ic_rounded_add), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Folder")
            }
        }

        if (directories.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outlined_folder),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No music folders added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click \"Add Folder\" to pick your music collection folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(directories, key = { it }) { path ->
                    DirectoryItemCard(
                        path = path,
                        onRemove = { onRemoveDirectory(path) }
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceContainerLowest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_outlined_check_circle),
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "You can add more folders or exclude specific subfolders anytime in Settings > Music Folders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DirectoryItemCard(
    path: String,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val file = remember(path) { File(path) }

    val trackCount by produceState(initialValue = null as Int?, path) {
        value = withContext(Dispatchers.IO) {
            countAudioFilesDeep(file)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_folder),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.ifBlank { path },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trackCount != null) {
                    Text(
                        text = "$trackCount audio file(s) found",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_close),
                    contentDescription = "Remove",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PreferencesStepContent(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    fetchDeezerImages: Boolean,
    onToggleDeezerImages: (Boolean) -> Unit,
    fetchOnlineLyrics: Boolean,
    onToggleOnlineLyrics: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Preferences & Privacy",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.onSurface
            )
            Text(
                text = "Customize theme and optional online services (both default to off for maximum privacy).",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDarkTheme() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.secondaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (isDarkTheme) Res.drawable.ic_rounded_dark_mode else Res.drawable.ic_rounded_light_mode),
                            contentDescription = null,
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dark Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = if (isDarkTheme) "Currently using dark theme" else "Currently using light theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme() }
                )
            }
        }

        Text(
            text = "ONLINE SERVICES (OFFLINE FIRST)",
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDeezerImages(!fetchDeezerImages) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.secondaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_outlined_person),
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fetch Artist Portraits from Deezer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Download missing artist avatars online when not present in local tags",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = fetchDeezerImages,
                        onCheckedChange = onToggleDeezerImages
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleOnlineLyrics(!fetchOnlineLyrics) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.secondaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_outlined_cloud_download),
                                contentDescription = null,
                                tint = colors.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fetch Online Lyrics (LRCLIB)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Download synchronized and plain lyrics automatically from LRCLIB",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = fetchOnlineLyrics,
                        onCheckedChange = onToggleOnlineLyrics
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceContainerLowest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_outlined_check_circle),
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "You can customize your theme, audio settings, and online services anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllSetStepContent() {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = colors.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_check),
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Everything is Ready!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Click below to begin indexing your library and enjoy your collection.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
    }
}

@Composable
private fun SetupSineWaveLine(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Dp = 2.dp,
    amplitude: Dp = 8.dp,
    waves: Float = 2f
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "SineWaveAnimation")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val strokePx = with(density) { strokeWidth.toPx() }
        val ampPx = with(density) { amplitude.toPx() }
        val samples = 300

        if (w <= 0f) return@Canvas

        val path = Path().apply {
            val step = w / (samples - 1)
            moveTo(0f, centerY + (ampPx * sin(animatedPhase)))
            for (i in 1 until samples) {
                val x = i * step
                val theta = (x / w) * (2f * PI.toFloat() * waves) + animatedPhase
                val y = centerY + ampPx * sin(theta)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}
