package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsCategoryDetail(
    section: SettingsSection,
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    isDynamicColor: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenFoldersDetail: () -> Unit,
    onOpenArtistsDetail: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.surfaceContainerHighest.copy(alpha = 0.5f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = section.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
        }

        when (section) {
            SettingsSection.LIBRARY -> {
                item {
                    val musicDirectories by viewModel.musicDirectories.collectAsState()
                    val excludedDirectories by viewModel.excludedDirectories.collectAsState()
                    val artistDelimiters by viewModel.artistDelimiters.collectAsState()
                    val wordDelimiters by viewModel.wordDelimiters.collectAsState()

                    val folderSubtitle = when {
                        musicDirectories.isEmpty() -> "No folders configured"
                        excludedDirectories.isEmpty() -> "${musicDirectories.size} configured ${if (musicDirectories.size == 1) "directory" else "directories"}"
                        else -> "${musicDirectories.size} configured • ${excludedDirectories.size} excluded"
                    }

                    val artistSubtitle = "${artistDelimiters.joinToString(" ")} • ${wordDelimiters.size} word delimiters"

                    SettingsSubsection(title = "LIBRARY STRUCTURE") {
                        ExpressiveSettingsRow(
                            title = "Music Folders",
                            subtitle = folderSubtitle,
                            leadingIcon = {
                                Icon(Icons.Outlined.Folder, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 2),
                            onClick = onOpenFoldersDetail
                        )

                        ExpressiveSettingsRow(
                            title = "Artists",
                            subtitle = artistSubtitle,
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            trailingIcon = {
                                Icon(Icons.Rounded.ChevronRight, null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2),
                            onClick = onOpenArtistsDetail
                        )
                    }
                }

                item {
                    SettingsSubsection(title = "SYNC & SCANNING") {
                        Surface(
                            shape = getGroupShape(0, 1),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = null,
                                        tint = colors.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Rescan Media Library",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Scan configured music directories for new songs and metadata",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = onRescanLibrary,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Rescan")
                                }
                            }
                        }
                    }
                }

                item {
                    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()

                    SettingsSubsection(title = "ONLINE SERVICES") {
                        ExpressiveSwitchRow(
                            title = "Fetch Artist Portraits from Deezer",
                            subtitle = "Automatically download and cache missing artist portraits",
                            checked = fetchDeezerImages,
                            onCheckedChange = { viewModel.setFetchDeezerArtistImages(it) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 1)
                        )
                    }
                }
            }

            SettingsSection.APPEARANCE -> {
                item {
                    SettingsSubsection(title = "THEME") {
                        ExpressiveSwitchRow(
                            title = "Dark Theme",
                            subtitle = "Switch between Material You Dark and Light theme",
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                            leadingIcon = {
                                Icon(if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(0, 2)
                        )

                        ExpressiveSwitchRow(
                            title = "Dynamic Artwork Theme",
                            subtitle = "Tint interface colors dynamically based on active album artwork",
                            checked = isDynamicColor,
                            onCheckedChange = { onToggleDynamicColor() },
                            leadingIcon = {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                            },
                            shape = getGroupShape(1, 2)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isDynamicColor,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val currentPaletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
                        val currentColorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()

                        SettingsSubsection(title = "ARTWORK COLOR TUNING") {
                            Surface(
                                shape = getGroupShape(0, 2),
                                color = colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Brush,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Palette Style",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Material 3 color generation scheme for album covers",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }

                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AlbumArtPaletteStyle.entries.forEach { style ->
                                            val isSelected = style == currentPaletteStyle
                                            val animatedContainerColor by animateColorAsState(
                                                targetValue = if (isSelected) colors.primary else colors.surfaceContainerHighest,
                                                label = "btnContainer"
                                            )
                                            val animatedContentColor by animateColorAsState(
                                                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                                label = "btnContent"
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = animatedContainerColor,
                                                modifier = Modifier
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { viewModel.setAlbumArtPaletteStyle(style) }
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = style.label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = animatedContentColor,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = getGroupShape(1, 2),
                                color = colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(colors.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Tune,
                                                    contentDescription = null,
                                                    tint = colors.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Color Extraction Accuracy",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (currentColorAccuracy == 0) "Standard (Fast & Balanced)" else "Fidelity Level $currentColorAccuracy",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.primaryContainer
                                        ) {
                                            Text(
                                                text = "$currentColorAccuracy / ${AlbumArtColorAccuracy.MAX}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Slider(
                                        value = currentColorAccuracy.toFloat(),
                                        onValueChange = { viewModel.setAlbumArtColorAccuracy(it.roundToInt()) },
                                        valueRange = AlbumArtColorAccuracy.MIN.toFloat()..AlbumArtColorAccuracy.MAX.toFloat(),
                                        steps = AlbumArtColorAccuracy.STEPS,
                                        colors = SliderDefaults.colors(
                                            thumbColor = colors.primary,
                                            activeTrackColor = colors.primary,
                                            inactiveTrackColor = colors.surfaceContainerHighest
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection.ABOUT -> {
                item {
                    SettingsSubsection(title = "APPLICATION") {
                        Surface(
                            shape = getGroupShape(0, 1),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.primaryContainer
                                    ) {
                                        Icon(
                                            imageVector = PixelPlayerIcons.AppLogo,
                                            contentDescription = null,
                                            tint = colors.onPrimaryContainer,
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .size(32.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "PixelPlayer Desktop",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Version 1.0.0 • Material 3 Expressive • VLCJ Core",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "Crafted for high fidelity playback with dynamic Material You theming faithfully adapted from PixelPlayer OSS for Android.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
