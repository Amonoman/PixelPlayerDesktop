package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessItem
import com.kayoi.pixelplayerdesktop.data.model.QuickAccessType
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_more_vert
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AlbumOptionsDropdownMenu
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.ArtistOptionsDropdownMenu
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

private enum class MostListenedTab(val label: String, val icon: DrawableResource) {
    SONGS("Songs", Res.drawable.ic_rounded_music_note),
    ALBUMS("Albums", Res.drawable.ic_rounded_album),
    ARTISTS("Artists", Res.drawable.ic_artist)
}

@Composable
fun MostPlayedScreen(
    songs: List<Song>,
    albums: List<Album> = emptyList(),
    artists: List<Artist> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    viewModel: PlayerViewModel,
    onSelectAlbum: ((Album) -> Unit)? = null,
    onSelectArtist: ((Artist) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onAddSongsToPlaylist: ((List<Song>) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    onEditMetadata: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(MostListenedTab.SONGS) }

    val playedSongs = remember(songs) {
        songs.filter { it.playCount > 0 }
    }

    val filteredSongs = remember(playedSongs) {
        playedSongs.sortedByDescending { it.playCount }
            .take(QuickAccessItem.MAX_MOST_PLAYED_SONGS)
    }

    val filteredAlbums = remember(playedSongs, albums) {
        if (playedSongs.isEmpty() || albums.isEmpty()) return@remember emptyList()
        val albumPlayCountsById = mutableMapOf<Long, Long>()
        val albumPlayCountsByTitle = mutableMapOf<String, Long>()
        for (song in playedSongs) {
            val plays = song.playCount
            if (song.albumId != 0L) {
                albumPlayCountsById[song.albumId] = (albumPlayCountsById[song.albumId] ?: 0L) + plays
            } else if (song.album.isNotBlank()) {
                val key = "${song.album.lowercase()}::${song.artist.lowercase()}"
                albumPlayCountsByTitle[key] = (albumPlayCountsByTitle[key] ?: 0L) + plays
            }
        }
        albums.mapNotNull { album ->
            val byId = albumPlayCountsById[album.id] ?: 0L
            val byTitle = if (album.title.isNotBlank()) {
                albumPlayCountsByTitle["${album.title.lowercase()}::${album.artist.lowercase()}"] ?: 0L
            } else 0L
            val totalPlays = byId + byTitle
            if (totalPlays > 0L) Pair(album, totalPlays) else null
        }.sortedByDescending { it.second }
            .map { it.first }
            .take(QuickAccessItem.MAX_MOST_PLAYED_SONGS)
    }

    val filteredArtists = remember(playedSongs, artists) {
        if (playedSongs.isEmpty() || artists.isEmpty()) return@remember emptyList()
        val artistPlayCountsById = mutableMapOf<Long, Long>()
        val artistPlayCountsByName = mutableMapOf<String, Long>()
        for (song in playedSongs) {
            val plays = song.playCount
            if (song.artists.isNotEmpty()) {
                for (ref in song.artists) {
                    if (ref.id != 0L) {
                        artistPlayCountsById[ref.id] = (artistPlayCountsById[ref.id] ?: 0L) + plays
                    } else if (ref.name.isNotBlank()) {
                        val nameKey = ref.name.trim().lowercase()
                        artistPlayCountsByName[nameKey] = (artistPlayCountsByName[nameKey] ?: 0L) + plays
                    }
                }
            } else if (song.artistId != 0L) {
                artistPlayCountsById[song.artistId] = (artistPlayCountsById[song.artistId] ?: 0L) + plays
            } else if (song.artist.isNotBlank()) {
                val nameKey = song.artist.trim().lowercase()
                artistPlayCountsByName[nameKey] = (artistPlayCountsByName[nameKey] ?: 0L) + plays
            }
        }
        artists.mapNotNull { artist ->
            val byId = artistPlayCountsById[artist.id] ?: 0L
            val byName = if (artist.name.isNotBlank()) {
                artistPlayCountsByName[artist.name.trim().lowercase()] ?: 0L
            } else 0L
            val totalPlays = byId + byName
            if (totalPlays > 0L) Pair(artist, totalPlays) else null
        }.sortedByDescending { it.second }
            .map { it.first }
            .take(QuickAccessItem.MAX_MOST_PLAYED_SONGS)
    }

    val songsByAlbum = remember(songs) {
        val byId = mutableMapOf<Long, MutableList<Song>>()
        val byTitle = mutableMapOf<String, MutableList<Song>>()
        for (song in songs) {
            if (song.albumId != 0L) {
                byId.getOrPut(song.albumId) { mutableListOf() }.add(song)
            } else if (song.album.isNotBlank()) {
                val key = "${song.album.lowercase()}::${song.artist.lowercase()}"
                byTitle.getOrPut(key) { mutableListOf() }.add(song)
            }
        }
        Pair(byId, byTitle)
    }

    val songsByArtist = remember(songs) {
        val byId = mutableMapOf<Long, MutableList<Song>>()
        val byName = mutableMapOf<String, MutableList<Song>>()
        for (song in songs) {
            if (song.artists.isNotEmpty()) {
                for (ref in song.artists) {
                    if (ref.id != 0L) {
                        byId.getOrPut(ref.id) { mutableListOf() }.add(song)
                    } else if (ref.name.isNotBlank()) {
                        byName.getOrPut(ref.name.trim().lowercase()) { mutableListOf() }.add(song)
                    }
                }
            } else if (song.artistId != 0L) {
                byId.getOrPut(song.artistId) { mutableListOf() }.add(song)
            } else if (song.artist.isNotBlank()) {
                byName.getOrPut(song.artist.trim().lowercase()) { mutableListOf() }.add(song)
            }
        }
        Pair(byId, byName)
    }

    fun getAlbumSongs(album: Album): List<Song> {
        val list = if (album.id != 0L) {
            songsByAlbum.first[album.id]
        } else null
        val resolved = list ?: songsByAlbum.second["${album.title.lowercase()}::${album.artist.lowercase()}"] ?: emptyList()
        return resolved.sortedWith(
            compareBy<Song> { it.discNumber ?: 1 }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    fun getArtistSongs(artist: Artist): List<Song> {
        if (artist.id != 0L) {
            val byIdList = songsByArtist.first[artist.id]
            if (!byIdList.isNullOrEmpty()) return byIdList
        }
        return songsByArtist.second[artist.name.trim().lowercase()] ?: emptyList()
    }

    val quickAccessItems by viewModel.quickAccessItems.collectAsState()

    val pinnedAlbumIds = remember(quickAccessItems) {
        quickAccessItems.filter { it.type == QuickAccessType.ALBUM }.mapNotNull { it.targetKey.toLongOrNull() }.toSet()
    }
    val pinnedArtistIds = remember(quickAccessItems) {
        quickAccessItems.filter { it.type == QuickAccessType.ARTIST }.mapNotNull { it.targetKey.toLongOrNull() }.toSet()
    }
    val artistsByName = remember(artists) {
        artists.associateBy { it.name.lowercase() }
    }

    val songsListState = rememberLazyListState()
    val albumsListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        val screenWidth = maxWidth
        val isVeryCompact = screenWidth < 400.dp
        val isCompact = screenWidth < 560.dp
        val horizontalPadding = when {
            isVeryCompact -> 12.dp
            isCompact -> 16.dp
            else -> 24.dp
        }

        val activeListCount = when (selectedTab) {
            MostListenedTab.SONGS -> filteredSongs.size
            MostListenedTab.ALBUMS -> filteredAlbums.size
            MostListenedTab.ARTISTS -> filteredArtists.size
        }

        val activeListState = when (selectedTab) {
            MostListenedTab.SONGS -> songsListState
            MostListenedTab.ALBUMS -> albumsListState
            MostListenedTab.ARTISTS -> artistsListState
        }

        LazyColumn(
            state = activeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "most_listened_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPadding, end = horizontalPadding, top = 20.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Most Listened",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = colors.onSurface
                    )
                    val subtitle = when (selectedTab) {
                        MostListenedTab.SONGS -> if (activeListCount == 1) "1 song" else "$activeListCount songs"
                        MostListenedTab.ALBUMS -> if (activeListCount == 1) "1 album" else "$activeListCount albums"
                        MostListenedTab.ARTISTS -> if (activeListCount == 1) "1 artist" else "$activeListCount artists"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            item(key = "most_listened_tabs") {
                MostListenedConnectedButtons(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    isVeryCompact = isVeryCompact,
                    isCompact = isCompact,
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp)
                )
            }

            when (selectedTab) {
                MostListenedTab.SONGS -> {
                    if (filteredSongs.isNotEmpty()) {
                        itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                            val isCurrentPlaying = currentSong?.id == song.id
                            val rank = index + 1
                            EnhancedSongListItem(
                                modifier = Modifier.padding(horizontal = horizontalPadding),
                                song = song,
                                isCurrentSong = isCurrentPlaying,
                                isPlaying = isCurrentPlaying && isPlaying,
                                showDuration = !isVeryCompact,
                                leadingContent = {
                                    MostListenedRankPill(
                                        rank = rank,
                                        isHighlighted = isCurrentPlaying
                                    )
                                },
                                isFavorite = song.isFavorite,
                                onPlayNext = { viewModel.playSongNext(song) },
                                onAddToQueue = { viewModel.addSongToQueue(song) },
                                onAddToPlaylist = if (onAddToPlaylist != null) { { onAddToPlaylist(song) } } else null,
                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                onEditMetadata = if (onEditMetadata != null) { { onEditMetadata(song) } } else null,
                                onDelete = if (onDelete != null) { { onDelete(song) } } else null,
                                onClick = {
                                    viewModel.playSongs(filteredSongs, index)
                                }
                            )
                        }
                    } else {
                        item(key = "empty_songs") {
                            EmptyState(horizontalPadding = horizontalPadding, text = "No songs played yet")
                        }
                    }
                }

                MostListenedTab.ALBUMS -> {
                    if (filteredAlbums.isNotEmpty()) {
                        itemsIndexed(filteredAlbums, key = { _, album -> album.id }) { index, album ->
                            val rank = index + 1
                            val artist = remember(album.artist, artistsByName) {
                                artistsByName[album.artist.lowercase()]
                            }
                            val isPinned = album.id in pinnedAlbumIds
                            val onTogglePin = remember(isPinned, album) {
                                { viewModel.togglePin("album_${album.id}", album.title, QuickAccessType.ALBUM, album.id.toString(), isPinned) }
                            }

                            MostListenedAlbumItem(
                                album = album,
                                rank = rank,
                                modifier = Modifier.padding(horizontal = horizontalPadding),
                                onClick = { onSelectAlbum?.invoke(album) },
                                onPlay = { viewModel.playSongs(getAlbumSongs(album), 0) },
                                onPlayNext = { viewModel.playSongsNext(getAlbumSongs(album)) },
                                onAddToQueue = { viewModel.addSongsToQueue(getAlbumSongs(album)) },
                                onAddToPlaylist = {
                                    val albumSongs = getAlbumSongs(album)
                                    if (onAddSongsToPlaylist != null) {
                                        onAddSongsToPlaylist(albumSongs)
                                    } else if (onAddToPlaylist != null && albumSongs.isNotEmpty()) {
                                        onAddToPlaylist(albumSongs.first())
                                    }
                                },
                                onNavigateToArtist = if (artist != null && onSelectArtist != null) {
                                    { onSelectArtist(artist) }
                                } else null,
                                isPinned = isPinned,
                                onTogglePin = onTogglePin
                            )
                        }
                    } else {
                        item(key = "empty_albums") {
                            EmptyState(horizontalPadding = horizontalPadding, text = "No albums played yet")
                        }
                    }
                }

                MostListenedTab.ARTISTS -> {
                    if (filteredArtists.isNotEmpty()) {
                        itemsIndexed(filteredArtists, key = { _, artist -> artist.id }) { index, artist ->
                            val rank = index + 1
                            val isPinned = artist.id in pinnedArtistIds
                            val onTogglePin = remember(isPinned, artist) {
                                { viewModel.togglePin("artist_${artist.id}", artist.name, QuickAccessType.ARTIST, artist.id.toString(), isPinned) }
                            }

                            MostListenedArtistItem(
                                artist = artist,
                                rank = rank,
                                modifier = Modifier.padding(horizontal = horizontalPadding),
                                onClick = { onSelectArtist?.invoke(artist) },
                                onPlay = { viewModel.playSongs(getArtistSongs(artist), 0) },
                                onPlayNext = { viewModel.playSongsNext(getArtistSongs(artist)) },
                                onAddToQueue = { viewModel.addSongsToQueue(getArtistSongs(artist)) },
                                onAddToPlaylist = {
                                    val artistSongs = getArtistSongs(artist)
                                    if (onAddSongsToPlaylist != null) {
                                        onAddSongsToPlaylist(artistSongs)
                                    } else if (onAddToPlaylist != null && artistSongs.isNotEmpty()) {
                                        onAddToPlaylist(artistSongs.first())
                                    }
                                },
                                isPinned = isPinned,
                                onTogglePin = onTogglePin
                            )
                        }
                    } else {
                        item(key = "empty_artists") {
                            EmptyState(horizontalPadding = horizontalPadding, text = "No artists played yet")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MostListenedRankPill(
    rank: Int,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val pillBg = if (isHighlighted) colors.onPrimaryContainer.copy(alpha = 0.16f) else colors.surfaceContainerHighest.copy(alpha = 0.65f)
    val pillTextColor = if (isHighlighted) colors.onPrimaryContainer else colors.onSurfaceVariant

    Surface(
        modifier = modifier
            .width(36.dp)
            .height(48.dp),
        shape = CircleShape,
        color = pillBg
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (rank >= 100) 11.sp else 13.sp
                ),
                color = pillTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MostListenedConnectedButtons(
    selectedTab: MostListenedTab,
    onSelectTab: (MostListenedTab) -> Unit,
    isVeryCompact: Boolean = false,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tabs = MostListenedTab.entries

    val rowHeight = when {
        isVeryCompact -> 48.dp
        isCompact -> 52.dp
        else -> 56.dp
    }
    val outerCorner = when {
        isVeryCompact -> 24.dp
        isCompact -> 26.dp
        else -> 28.dp
    }
    val innerCorner = 6.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == tab
            val shape = if (isSelected) {
                CircleShape
            } else {
                when (index) {
                    0 -> RoundedCornerShape(
                        topStart = outerCorner,
                        bottomStart = outerCorner,
                        topEnd = innerCorner,
                        bottomEnd = innerCorner
                    )
                    tabs.size - 1 -> RoundedCornerShape(
                        topStart = innerCorner,
                        bottomStart = innerCorner,
                        topEnd = outerCorner,
                        bottomEnd = outerCorner
                    )
                    else -> RoundedCornerShape(innerCorner)
                }
            }

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.surfaceContainerHighest.copy(alpha = 0.65f),
                animationSpec = tween(durationMillis = 200),
                label = "tabContainerColor"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label = "tabContentColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(shape)
                    .background(containerColor)
                    .clickable { onSelectTab(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(if (isVeryCompact) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(if (isVeryCompact) 6.dp else 8.dp))
                    }
                    Text(
                        text = tab.label,
                        style = if (isVeryCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MostListenedAlbumItem(
    album: Album,
    rank: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onNavigateToArtist: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var isMenuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val surfaceShape = RoundedCornerShape(18.dp)
    val hoverColor = colors.onSurface.copy(alpha = 0.06f)
    val effectiveContainerColor = if (isHovered) lerpColor(colors.surfaceContainerLow, hoverColor, 0.45f) else colors.surfaceContainerLow

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(surfaceShape)
            .hoverable(interactionSource),
        shape = surfaceShape,
        color = effectiveContainerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MostListenedRankPill(rank = rank)

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Secondary),
                        onClick = { isMenuExpanded = true }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncArtworkImage(
                    artworkUri = album.albumArtUriString,
                    placeholderIcon = Res.drawable.ic_rounded_album,
                    placeholderIconSize = 22.dp,
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurfaceVariant,
                    contentDescription = "${album.title} cover",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subtitle = buildString {
                        if (album.artist.isNotBlank()) append(album.artist)
                        if (album.songCount > 0) {
                            if (isNotEmpty()) append(" • ")
                            append(if (album.songCount == 1) "1 song" else "${album.songCount} songs")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box {
                FilledIconButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                        contentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_more_vert),
                        contentDescription = "Album options",
                        modifier = Modifier.size(20.dp)
                    )
                }

                AlbumOptionsDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    album = album,
                    onPlay = onPlay ?: {},
                    onPlayNext = onPlayNext ?: {},
                    onAddToQueue = onAddToQueue ?: {},
                    onAddToPlaylist = onAddToPlaylist,
                    onNavigateToArtist = onNavigateToArtist,
                    isPinned = isPinned,
                    onTogglePin = onTogglePin
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MostListenedArtistItem(
    artist: Artist,
    rank: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    var isMenuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val surfaceShape = RoundedCornerShape(18.dp)
    val hoverColor = colors.onSurface.copy(alpha = 0.06f)
    val effectiveContainerColor = if (isHovered) lerpColor(colors.surfaceContainerLow, hoverColor, 0.45f) else colors.surfaceContainerLow

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(surfaceShape)
            .hoverable(interactionSource),
        shape = surfaceShape,
        color = effectiveContainerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MostListenedRankPill(rank = rank)

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Secondary),
                        onClick = { isMenuExpanded = true }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(ShapeCache.ExpressiveAvatar)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUri = artist.effectiveImageUrl
                    if (!imageUri.isNullOrBlank()) {
                        AsyncArtworkImage(
                            artworkUri = imageUri,
                            shape = ShapeCache.ExpressiveAvatar,
                            placeholderIcon = Res.drawable.ic_artist,
                            placeholderIconSize = 22.dp,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            contentDescription = "${artist.name} portrait",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.ic_artist),
                            contentDescription = artist.name,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val subtitle = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box {
                FilledIconButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.surfaceContainerHighest.copy(alpha = 0.6f),
                        contentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_more_vert),
                        contentDescription = "Artist options",
                        modifier = Modifier.size(20.dp)
                    )
                }

                ArtistOptionsDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    artist = artist,
                    onPlay = onPlay ?: {},
                    onPlayNext = onPlayNext ?: {},
                    onAddToQueue = onAddToQueue ?: {},
                    onAddToPlaylist = onAddToPlaylist,
                    isPinned = isPinned,
                    onTogglePin = onTogglePin
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    horizontalPadding: Dp,
    text: String
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant
        )
    }
}
