package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.songFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.ColorSchemePair
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun formatTotalDuration(songs: List<Song>): String {
    val totalMs = songs.sumOf { it.duration }
    val totalSeconds = (totalMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours} hr ${remainingMinutes} min"
    } else {
        "${minutes} min"
    }
}

@Composable
fun AlbumDetailScreen(
    album: Album,
    songs: List<Song>,
    viewModel: PlayerViewModel,
    onBack: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    onBatchEditMetadata: ((List<Song>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val baseColorScheme = MaterialTheme.colorScheme

    var colorSchemePair by remember(
        album.albumArtUriString,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        mutableStateOf<ColorSchemePair?>(DynamicColorProcessor.getCachedColorScheme(album.albumArtUriString))
    }

    LaunchedEffect(
        album.albumArtUriString,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        if (colorSchemePair == null && !album.albumArtUriString.isNullOrBlank()) {
            val extracted = withContext(Dispatchers.IO) {
                DynamicColorProcessor.extractColorScheme(album.albumArtUriString)
            }
            if (extracted != null) {
                colorSchemePair = extracted
            }
        }
    }

    val albumColorScheme = remember(colorSchemePair, isDarkTheme, baseColorScheme) {
        val pair = colorSchemePair
        if (pair != null) {
            if (isDarkTheme) pair.dark else pair.light
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = albumColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        val colors = MaterialTheme.colorScheme
        val currentSong by viewModel.currentSong.collectAsState()
        val playerInfo by viewModel.playerInfo.collectAsState()
        val favoriteSongs by viewModel.favoriteSongs.collectAsState()

        val albumSongs = remember(album, songs) {
            songs.filter {
                (it.albumId != 0L && it.albumId == album.id) ||
                it.album.equals(album.title, ignoreCase = true)
            }.sortedWith(
                compareBy<Song> { it.discNumber ?: 1 }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        }

        val songsByDisc = remember(albumSongs) {
            albumSongs.groupBy { it.discNumber ?: 1 }
        }

        val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }
        val listState = rememberLazyListState()

        val songFastScrollLabelProvider = remember(albumSongs) {
            { index: Int ->
                songFastScrollLabel(
                    song = albumSongs.getOrNull(index),
                    sortOption = SortOption.SongTitleAZ
                )
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val isWide = maxWidth >= 760.dp
                val headerHeight = if (isWide) 180.dp else 140.dp
                val coverSize = if (isWide) 140.dp else 100.dp
                val coverCorner = if (isWide) 20.dp else 14.dp
                val titleStyle = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge
                val artistStyle = if (isWide) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
                val metaStyle = if (isWide) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .clipToBounds()
                ) {
                    AsyncArtworkImage(
                        artworkUri = album.albumArtUriString,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop,
                        placeholderIcon = Icons.Rounded.Album,
                        placeholderIconSize = 48.dp,
                        containerColor = colors.surfaceContainerHighest,
                        contentColor = colors.onSurfaceVariant,
                        contentDescription = null
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.45f))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.background.copy(alpha = 0.35f),
                                        colors.background
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncArtworkImage(
                            artworkUri = album.albumArtUriString,
                            modifier = Modifier
                                .size(coverSize)
                                .clip(RoundedCornerShape(coverCorner)),
                            placeholderIcon = Icons.Rounded.Album,
                            placeholderIconSize = 52.dp,
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurfaceVariant,
                            contentDescription = "${album.title} cover"
                        )

                        Spacer(modifier = Modifier.width(if (isWide) 22.dp else 16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = album.title,
                                style = titleStyle,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(if (isWide) 6.dp else 4.dp))

                            val artistYearText = if (album.year > 0) "${album.artist} • ${album.year}" else album.artist
                            Text(
                                text = artistYearText,
                                style = artistStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(if (isWide) 4.dp else 2.dp))

                            val countText = if (albumSongs.size == 1) "1 song" else "${albumSongs.size} songs"
                            val durationText = formatTotalDuration(albumSongs)
                            Text(
                                text = if (durationText.isNotBlank()) "$countText • $durationText" else countText,
                                style = metaStyle,
                                color = colors.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isWide) 20.dp else 12.dp))

                        if (isWide) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onBatchEditMetadata != null && albumSongs.isNotEmpty()) {
                                    FilledTonalIconButton(
                                        onClick = { onBatchEditMetadata(albumSongs) },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = colors.surfaceContainerLow,
                                            contentColor = colors.onSurface
                                        ),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoFixHigh,
                                            contentDescription = "Edit Album Metadata",
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                FloatingActionButton(
                                    onClick = {
                                        if (albumSongs.isNotEmpty()) {
                                            viewModel.shuffleSongs(albumSongs)
                                        }
                                    },
                                    shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp,
                                        focusedElevation = 0.dp,
                                        hoveredElevation = 0.dp
                                    ),
                                    modifier = Modifier.size(92.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (onBatchEditMetadata != null && albumSongs.isNotEmpty()) {
                                    FilledTonalIconButton(
                                        onClick = { onBatchEditMetadata(albumSongs) },
                                        shape = CircleShape,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = colors.surfaceContainerLow,
                                            contentColor = colors.onSurface
                                        ),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoFixHigh,
                                            contentDescription = "Edit Album Metadata",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                FloatingActionButton(
                                    onClick = {
                                        if (albumSongs.isNotEmpty()) {
                                            viewModel.shuffleSongs(albumSongs)
                                        }
                                    },
                                    shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                    elevation = FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp,
                                        focusedElevation = 0.dp,
                                        hoveredElevation = 0.dp
                                    ),
                                    modifier = Modifier.size(62.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = colors.outlineVariant.copy(alpha = 0.35f)
        )

        if (albumSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No songs found in this album",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    songsByDisc.forEach { (discNumber, discSongs) ->
                        if (songsByDisc.size > 1) {
                            item(key = "disc_header_$discNumber") {
                                Text(
                                    text = "Disc $discNumber",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp)
                                )
                            }
                        }

                        itemsIndexed(
                            items = discSongs,
                            key = { _, song -> song.id },
                            contentType = { _, _ -> "album_detail_song" }
                        ) { _, song ->
                            val isCurrentPlaying = currentSong?.id == song.id
                            val globalIndex = albumSongs.indexOfFirst { it.id == song.id }
                            val effectiveIndex = if (globalIndex != -1) globalIndex else 0

                            EnhancedSongListItem(
                                song = song,
                                isPlaying = isCurrentPlaying && playerInfo.isPlaying,
                                isCurrentSong = isCurrentPlaying,
                                showAlbumArt = false,
                                showDuration = false,
                                isFavorite = favoriteIds.contains(song.id),
                                onPlayNext = { viewModel.playSongNext(song) },
                                onAddToQueue = { viewModel.addSongToQueue(song) },
                                onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                                onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                                onClick = { viewModel.playSongs(albumSongs, effectiveIndex) }
                            )
                        }
                    }
                }

                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = 24.dp),
                    listState = listState,
                    dragLabelProvider = songFastScrollLabelProvider
                )
            }
        }
    }
}
}
