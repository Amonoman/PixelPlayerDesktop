package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.domain.DailyMixManager
import com.kayoi.pixelplayerdesktop.presentation.components.AlbumArtCollage
import com.kayoi.pixelplayerdesktop.presentation.components.CollagePattern
import com.kayoi.pixelplayerdesktop.presentation.components.DailyMixSection
import com.kayoi.pixelplayerdesktop.presentation.components.RecentlyPlayedSection
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape

@Composable
fun HomeScreen(
    songs: List<Song>,
    favoriteSongs: List<Song>,
    recentlyPlayedSongs: List<Song> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelect: (Song) -> Unit,
    onPlaySongs: ((List<Song>, Int) -> Unit)? = null,
    onShuffleAll: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onRescanLibrary: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val dailyMixManager = remember { DailyMixManager() }

    val playSongInContext: (List<Song>, Song) -> Unit = { list, song ->
        if (onPlaySongs != null) {
            val idx = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            onPlaySongs(list, idx)
        } else {
            onSongSelect(song)
        }
    }

    val favoriteIds = remember(favoriteSongs) {
        favoriteSongs.map { it.id }.toSet()
    }

    val yourMixSongs = remember(songs, favoriteIds) {
        val mix = dailyMixManager.generateYourMix(songs, favoriteIds, limit = 30)
        if (mix.isNotEmpty()) mix else songs
    }

    val dailyMixSongs = remember(songs) {
        dailyMixManager.generateDailyMix(songs, limit = 30)
    }

    if (songs.isEmpty()) {
        HomeEmptyState(
            onNavigateToFolders = onNavigateToFolders,
            onRescanLibrary = onRescanLibrary,
            modifier = modifier
        )
        return
    }

    val standardTitleStyle = rememberYourMixTitleStyle(isCompact = false)
    val compactTitleStyle = rememberYourMixTitleStyle(isCompact = true)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {

        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 760.dp

                    if (isNarrow) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.Start
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy((-14).dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.wrapContentWidth()) {
                                    Text(
                                        text = "Your\nMix",
                                        style = compactTitleStyle,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = "Today's Mix for you",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 2.dp)
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = {
                                            if (onPlaySongs != null) {
                                                onPlaySongs(yourMixSongs.shuffled(), 0)
                                            } else {
                                                onShuffleAll()
                                            }
                                        }),
                                    shape = CircleShape,
                                    color = colors.tertiaryContainer,
                                    contentColor = colors.onTertiaryContainer,
                                    tonalElevation = 6.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle Play",
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtCollage(
                                    songs = yourMixSongs,
                                    modifier = Modifier.width(300.dp).height(210.dp),
                                    height = 210.dp,
                                    pattern = CollagePattern.COSMIC_SWIRL,
                                    onSongClick = { song -> playSongInContext(yourMixSongs, song) }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 24.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy((-24).dp)
                            ) {
                                Column(modifier = Modifier.wrapContentWidth()) {
                                    Text(
                                        text = "Your\nMix",
                                        style = standardTitleStyle,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Today's Mix for you",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .size(116.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = {
                                            if (onPlaySongs != null) {
                                                onPlaySongs(yourMixSongs.shuffled(), 0)
                                            } else {
                                                onShuffleAll()
                                            }
                                        }),
                                    shape = CircleShape,
                                    color = colors.tertiaryContainer,
                                    contentColor = colors.onTertiaryContainer,
                                    tonalElevation = 6.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle Play",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(380.dp)
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtCollage(
                                    songs = yourMixSongs,
                                    modifier = Modifier.fillMaxSize(),
                                    height = 300.dp,
                                    pattern = CollagePattern.COSMIC_SWIRL,
                                    onSongClick = { song -> playSongInContext(yourMixSongs, song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dailyMixSongs.isNotEmpty()) {
            item {
                DailyMixSection(
                    songs = dailyMixSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = { song -> playSongInContext(dailyMixSongs, song) },
                    onPlayMix = {
                        if (dailyMixSongs.isNotEmpty()) {
                            if (onPlaySongs != null) {
                                onPlaySongs(dailyMixSongs, 0)
                            } else {
                                onSongSelect(dailyMixSongs.first())
                            }
                        }
                    },
                    onSeeAllClick = onNavigateToLibrary,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete
                )
            }
        }

        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                RecentlyPlayedSection(
                    songs = recentlyPlayedSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = { song -> playSongInContext(recentlyPlayedSongs, song) },
                    onSeeAllClick = onNavigateToLibrary
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberYourMixTitleStyle(isCompact: Boolean = false): TextStyle {
    return remember(isCompact) {
        val fontSize = if (isCompact) 54.sp else 78.sp
        val lineHeight = if (isCompact) 52.sp else 74.sp
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    "font/gflex_variable.ttf",
                    weight = FontWeight(760),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(636),
                        FontVariation.width(152f),
                        FontVariation.Setting("ROND", 50f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(760),
            fontSize = fontSize,
            lineHeight = lineHeight
        )
    }
}

@Composable
private fun HomeEmptyState(
    onNavigateToFolders: () -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 28.dp)
                ) {
                    val isNarrow = maxWidth < 760.dp

                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.primaryContainer.copy(alpha = 0.85f),
                                contentColor = colors.onPrimaryContainer
                            ) {
                                Text(
                                    text = "GETTING STARTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Welcome to PixelPlayer",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Your music library is currently empty. Add your music folders or trigger a scan to start listening with personalized mixes, dynamic themes, and lyrics.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 22.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onNavigateToFolders,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Music Folders", fontWeight = FontWeight.SemiBold)
                                }

                                FilledTonalButton(
                                    onClick = onRescanLibrary,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.primaryContainer.copy(alpha = 0.85f),
                                    contentColor = colors.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "GETTING STARTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        letterSpacing = 1.2.sp
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Welcome to PixelPlayer",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "Your music library is currently empty. Add your music folders or trigger a scan to start listening with personalized mixes, dynamic colors, and synced lyrics.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                                        lineHeight = 24.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onNavigateToFolders,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add Music Folders", fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = onRescanLibrary,
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scan Library", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedStarShape(sides = 8, rotation = 12f))
                                    .background(colors.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PixelPlayerIcons.AppLogo,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 760.dp

                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureHighlightCard(
                            icon = Icons.Rounded.Folder,
                            iconTint = colors.primary,
                            title = "Local Music Library",
                            description = "Plays MP3, FLAC, AAC, WAV, OGG, and OPUS with fast indexing."
                        )
                        FeatureHighlightCard(
                            icon = Icons.Rounded.Palette,
                            iconTint = colors.secondary,
                            title = "Dynamic Theming",
                            description = "Adapts interface colors dynamically to match your current song artwork."
                        )
                        FeatureHighlightCard(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = colors.tertiary,
                            title = "Personalized Mixes",
                            description = "Generates Daily Mixes and smart queues as you listen."
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureHighlightCard(
                            icon = Icons.Rounded.Folder,
                            iconTint = colors.primary,
                            title = "Local Music Library",
                            description = "Plays MP3, FLAC, AAC, WAV, OGG, and OPUS with fast indexing.",
                            modifier = Modifier.weight(1f)
                        )
                        FeatureHighlightCard(
                            icon = Icons.Rounded.Palette,
                            iconTint = colors.secondary,
                            title = "Dynamic Theming",
                            description = "Adapts interface colors dynamically to match your current song artwork.",
                            modifier = Modifier.weight(1f)
                        )
                        FeatureHighlightCard(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = colors.tertiary,
                            title = "Personalized Mixes",
                            description = "Generates Daily Mixes and smart queues as you listen.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}
