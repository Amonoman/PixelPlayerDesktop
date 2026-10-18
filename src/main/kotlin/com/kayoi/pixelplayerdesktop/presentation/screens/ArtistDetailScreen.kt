package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar
import com.kayoi.pixelplayerdesktop.presentation.components.songFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.ColorSchemePair
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.data.network.deezer.DeezerClient
import com.kayoi.pixelplayerdesktop.utils.shapes.RoundedStarShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtistAlbumSection(
    val albumId: Long,
    val title: String,
    val year: Int? = null,
    val albumArtUriString: String? = null,
    val songs: List<Song>
)

@Composable
fun ArtistDetailScreen(
    artist: Artist,
    songs: List<Song>,
    viewModel: PlayerViewModel,
    onBack: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
    val baseColorScheme = MaterialTheme.colorScheme

    val artistSongs = remember(artist, songs) {
        songs.filter { song ->
            (song.artistId != 0L && song.artistId == artist.id) ||
            song.artists.any { it.id == artist.id || it.name.equals(artist.name, ignoreCase = true) } ||
            song.artist.contains(artist.name, ignoreCase = true)
        }
    }

    var resolvedImageUrl by remember(artist.name, artist.effectiveImageUrl) {
        mutableStateOf(artist.effectiveImageUrl)
    }

    LaunchedEffect(artist.name, fetchDeezerImages) {
        if (resolvedImageUrl.isNullOrBlank() && artist.name.isNotBlank()) {
            val fetched = DeezerClient.getArtistImageUrl(
                artistName = artist.name,
                fetchEnabled = fetchDeezerImages
            )
            if (!fetched.isNullOrBlank()) {
                resolvedImageUrl = fetched
            }
        }
    }

    var colorSchemePair by remember(
        resolvedImageUrl,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        mutableStateOf<ColorSchemePair?>(DynamicColorProcessor.getCachedColorScheme(resolvedImageUrl))
    }

    LaunchedEffect(
        resolvedImageUrl,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        if (colorSchemePair == null && !resolvedImageUrl.isNullOrBlank()) {
            val extracted = withContext(Dispatchers.IO) {
                DynamicColorProcessor.extractColorScheme(resolvedImageUrl)
            }
            if (extracted != null) {
                colorSchemePair = extracted
            }
        }
    }

    val artistColorScheme = remember(colorSchemePair, isDarkTheme, baseColorScheme) {
        val pair = colorSchemePair
        if (pair != null) {
            if (isDarkTheme) pair.dark else pair.light
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = artistColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        val colors = MaterialTheme.colorScheme
        val currentSong by viewModel.currentSong.collectAsState()
        val playerInfo by viewModel.playerInfo.collectAsState()
        val favoriteSongs by viewModel.favoriteSongs.collectAsState()
        val favoriteIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }
        val listState = rememberLazyListState()

        val albumSections = remember(artistSongs) {
            artistSongs.groupBy { it.album.ifBlank { "Unknown Album" } }
                .map { (albumTitle, songsInAlbum) ->
                    val firstSong = songsInAlbum.firstOrNull()
                    val maxYear = songsInAlbum.map { it.year }.filter { it > 0 }.maxOrNull()
                    ArtistAlbumSection(
                        albumId = firstSong?.albumId ?: 0L,
                        title = albumTitle,
                        year = maxYear,
                        albumArtUriString = songsInAlbum.firstOrNull { !it.albumArtUriString.isNullOrBlank() }?.albumArtUriString,
                        songs = songsInAlbum.sortedWith(
                            compareBy<Song> { it.discNumber ?: 1 }
                                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                .thenBy { it.title.lowercase() }
                        )
                    )
                }
                .sortedWith(
                    compareByDescending<ArtistAlbumSection> { it.year ?: 0 }
                        .thenBy { it.title.lowercase() }
                )
        }

        val allSortedSongs = remember(albumSections) {
            albumSections.flatMap { it.songs }
        }

        val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

        val songFastScrollLabelProvider = remember(allSortedSongs) {
            { index: Int ->
                songFastScrollLabel(
                    song = allSortedSongs.getOrNull(index),
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
                val titleStyle = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge
                val subtitleStyle = if (isWide) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .clipToBounds()
                ) {
                    AsyncArtworkImage(
                        artworkUri = resolvedImageUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop,
                        placeholderIcon = PixelPlayerIcons.Artist,
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
                            artworkUri = resolvedImageUrl,
                            modifier = Modifier
                                .size(coverSize)
                                .clip(ShapeCache.ExpressiveAvatar),
                            placeholderIcon = PixelPlayerIcons.Artist,
                            placeholderIconSize = 52.dp,
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurfaceVariant,
                            contentDescription = "${artist.name} portrait"
                        )

                        Spacer(modifier = Modifier.width(if (isWide) 22.dp else 16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = artist.name,
                                style = titleStyle,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(if (isWide) 6.dp else 4.dp))

                            val songsCountText = if (artistSongs.size == 1) "1 Song" else "${artistSongs.size} Songs"
                            Text(
                                text = songsCountText,
                                style = subtitleStyle,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        FloatingActionButton(
                            onClick = {
                                if (artistSongs.isNotEmpty()) {
                                    viewModel.shuffleSongs(artistSongs)
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
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = colors.outlineVariant.copy(alpha = 0.35f)
            )

            if (artistSongs.isEmpty()) {
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
                            imageVector = PixelPlayerIcons.Artist,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No songs found for this artist",
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
                        contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        albumSections.forEach { section ->
                            val isExpanded = expandedSections[section.title] ?: true

                            item(key = "album_section_${section.title}") {
                                val parentShape = RoundedCornerShape(24.dp)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(parentShape)
                                        .background(colors.surfaceContainerLow)
                                ) {
                                    CollapsibleAlbumHeader(
                                        section = section,
                                        isExpanded = isExpanded,
                                        onToggleExpand = {
                                            expandedSections[section.title] = !isExpanded
                                        },
                                        onPlayAlbum = {
                                            if (section.songs.isNotEmpty()) {
                                                viewModel.playSongs(section.songs)
                                            }
                                        }
                                    )

                                    if (isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            section.songs.forEachIndexed { songIndex, song ->
                                                val isCurrentPlaying = currentSong?.id == song.id
                                                val globalIndex = allSortedSongs.indexOfFirst { it.id == song.id }
                                                val effectiveIndex = if (globalIndex != -1) globalIndex else 0

                                                val songShape = when {
                                                    section.songs.size == 1 -> RoundedCornerShape(16.dp)
                                                    songIndex == 0 -> RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart = 4.dp,
                                                        bottomEnd = 4.dp
                                                    )
                                                    songIndex == section.songs.lastIndex -> RoundedCornerShape(
                                                        topStart = 4.dp,
                                                        topEnd = 4.dp,
                                                        bottomStart = 16.dp,
                                                        bottomEnd = 16.dp
                                                    )
                                                    else -> RoundedCornerShape(4.dp)
                                                }

                                                EnhancedSongListItem(
                                                    song = song,
                                                    isPlaying = isCurrentPlaying && playerInfo.isPlaying,
                                                    isCurrentSong = isCurrentPlaying,
                                                    showAlbumArt = false,
                                                    showDuration = false,
                                                    customShape = songShape,
                                                    containerColorOverride = colors.surfaceContainer,
                                                    isFavorite = favoriteIds.contains(song.id),
                                                    onPlayNext = { viewModel.playSongNext(song) },
                                                    onAddToQueue = { viewModel.addSongToQueue(song) },
                                                    onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                                    onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                                                    onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                                                    onClick = { viewModel.playSongs(allSortedSongs, effectiveIndex) }
                                                )
                                            }
                                        }
                                    }
                                }
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

@Composable
private fun CollapsibleAlbumHeader(
    section: ArtistAlbumSection,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayAlbum: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 240),
        label = "AlbumExpandRotation"
    )

    val subtitle = remember(section.year, section.songs.size) {
        val songCountText = if (section.songs.size == 1) "1 Song" else "${section.songs.size} Songs"
        if (section.year != null && section.year > 0) {
            "${section.year} • $songCountText"
        } else {
            songCountText
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncArtworkImage(
            artworkUri = section.albumArtUriString,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp)),
            placeholderIcon = Icons.Rounded.Album,
            placeholderIconSize = 28.dp,
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurfaceVariant,
            contentDescription = section.title
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalIconButton(
            onClick = onPlayAlbum,
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play Album",
                modifier = Modifier.size(22.dp)
            )
        }

        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = colors.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = expandIconRotation }
        )
    }
}
