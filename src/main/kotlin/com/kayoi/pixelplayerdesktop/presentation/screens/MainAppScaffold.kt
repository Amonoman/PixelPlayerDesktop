package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.presentation.components.AppSidebar
import com.kayoi.pixelplayerdesktop.presentation.components.AppTopBar
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.components.DismissUndoBar
import com.kayoi.pixelplayerdesktop.presentation.components.MiniPlayerBar
import com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AlbumGridItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.AlbumListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.ArtistGridItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.ArtistListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.EnhancedSongListItem
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import kotlinx.coroutines.launch

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.repository.FolderTreeBuilder
import com.kayoi.pixelplayerdesktop.presentation.components.folderFastScrollLabel
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.FolderListItem
import com.kayoi.pixelplayerdesktop.presentation.components.subcomps.FolderPlaylistItem
import androidx.compose.ui.focus.FocusRequester

@Composable
fun MainAppScaffold(
    viewModel: PlayerViewModel,
    isMaximized: Boolean = false,
    onMinimize: () -> Unit = {},
    onMaximizeRestore: () -> Unit = {},
    onClose: () -> Unit = {},
    topBarWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    onSearchFocusChange: (Boolean) -> Unit = {},
    isQueueSidePanelOpen: Boolean = true,
    onToggleQueueSidePanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showDismissUndoBar by remember { mutableStateOf(false) }

    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val musicDirectories by viewModel.musicDirectories.collectAsState()

    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()

    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedFolderDetail by remember { mutableStateOf<MusicFolder?>(null) }
    var currentFolderTreePath by remember { mutableStateOf<String?>(null) }
    var isFoldersPlaylistView by remember { mutableStateOf(false) }

    val songColorScheme = remember(currentSong?.albumArtUriString, isDarkTheme, isDynamicColor, paletteStyle, colorAccuracy) {
        if (isDynamicColor && currentSong?.albumArtUriString != null) {
            val pair = DynamicColorProcessor.extractColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )
            if (isDarkTheme) pair?.dark else pair?.light
        } else null
    }

    var currentDestination by remember { mutableStateOf(DesktopNavDestination.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var previousDestinationBeforeSearch by remember { mutableStateOf<DesktopNavDestination?>(null) }

    val handleSearchQueryChange: (String) -> Unit = { newQuery ->
        if (newQuery.isNotBlank()) {
            if (searchQuery.isBlank()) {
                if (currentDestination == DesktopNavDestination.HOME || currentDestination == DesktopNavDestination.SETTINGS) {
                    previousDestinationBeforeSearch = currentDestination
                    currentDestination = DesktopNavDestination.SONGS
                }
            }
        } else {
            if (previousDestinationBeforeSearch != null) {
                currentDestination = previousDestinationBeforeSearch!!
                previousDestinationBeforeSearch = null
            }
        }
        searchQuery = newQuery
        viewModel.onSearchQueryChanged(newQuery)
    }

    var isExpandedPlayerOpen by remember { mutableStateOf(false) }
    var initialExpandedTab by remember { mutableStateOf(ExpandedTab.LYRICS) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val filteredSongs = remember(songs, favoriteSongs, currentDestination, searchQuery) {
        val base = when (currentDestination) {
            DesktopNavDestination.LIKED -> favoriteSongs
            else -> songs
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val currentSongIndex = remember(filteredSongs, currentSong) {
        if (currentSong == null) -1 else filteredSongs.indexOfFirst { it.id == currentSong?.id }
    }

    val progressFraction = remember(playerInfo.currentPositionMs, playerInfo.totalDurationMs) {
        if (playerInfo.totalDurationMs > 0) {
            (playerInfo.currentPositionMs.toFloat() / playerInfo.totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isQueueVisible = isQueueSidePanelOpen && (currentSong != null || queue.isNotEmpty())
        val isSidebarCompact = (maxWidth < 1120.dp && isQueueVisible) || (maxWidth < 880.dp)

        Row(modifier = Modifier.fillMaxSize()) {

            AppSidebar(
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    currentDestination = destination
                    previousDestinationBeforeSearch = null
                    if (destination != DesktopNavDestination.PLAYLISTS) {
                        selectedPlaylistId = null
                    }
                    if (destination != DesktopNavDestination.FOLDERS) {
                        selectedFolderDetail = null
                        currentFolderTreePath = null
                    }
                },
                isCompact = isSidebarCompact
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.background)
            ) {

                topBarWrapper {
                    AppTopBar(
                        currentDestination = currentDestination,
                        searchQuery = searchQuery,
                        onSearchQueryChange = handleSearchQueryChange,
                        showQueueToggle = true,
                        isQueueOpen = isQueueSidePanelOpen,
                        queueCount = queue.size,
                        onToggleQueue = onToggleQueueSidePanel,
                        isMaximized = isMaximized,
                        onMinimize = onMinimize,
                        onMaximizeRestore = onMaximizeRestore,
                        onClose = onClose,
                        searchFocusRequester = searchFocusRequester,
                        onSearchFocusChange = onSearchFocusChange
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                    when (currentDestination) {
                        DesktopNavDestination.HOME -> {
                            HomeScreen(
                                songs = songs,
                                favoriteSongs = favoriteSongs,
                                currentSong = currentSong,
                                isPlaying = playerInfo.isPlaying,
                                onSongSelect = { song ->
                                    val idx = songs.indexOfFirst { it.id == song.id }
                                    if (idx != -1) {
                                        viewModel.playSongs(songs.drop(idx), 0)
                                    } else {
                                        viewModel.playSong(song)
                                    }
                                },
                                onShuffleAll = { viewModel.playSongs(songs.shuffled()) },
                                onNavigateToLibrary = { currentDestination = DesktopNavDestination.SONGS },
                                onPlayNext = { song -> viewModel.playSongNext(song) },
                                onAddToQueue = { song -> viewModel.addSongToQueue(song) },
                                onToggleFavorite = { song -> viewModel.toggleFavorite(song) }
                            )
                        }

                        DesktopNavDestination.SONGS, DesktopNavDestination.LIKED -> {
                            val currentSongSortOption by viewModel.libraryStateHolder.songSortOption.collectAsState()
                            val songsListState = rememberLazyListState()
                            var lastHandledSongSortKey by remember { mutableStateOf(currentSongSortOption.storageKey) }
                            var pendingSongSortScrollReset by remember { mutableStateOf(false) }

                            val songFastScrollLabelProvider = remember(filteredSongs, currentSongSortOption) {
                                { index: Int ->
                                    com.kayoi.pixelplayerdesktop.presentation.components.songFastScrollLabel(
                                        song = filteredSongs.getOrNull(index),
                                        sortOption = currentSongSortOption
                                    )
                                }
                            }

                            LaunchedEffect(currentSongSortOption) {
                                val currentSortKey = currentSongSortOption.storageKey
                                if (currentSortKey != lastHandledSongSortKey) {
                                    lastHandledSongSortKey = currentSortKey
                                    pendingSongSortScrollReset = true
                                    songsListState.scrollToItem(0)
                                }
                            }

                            LaunchedEffect(filteredSongs, pendingSongSortScrollReset) {
                                if (pendingSongSortScrollReset) {
                                    songsListState.scrollToItem(0)
                                    pendingSongSortScrollReset = false
                                }
                            }

                            val isCurrentSongVisible by remember(currentSongIndex, songsListState) {
                                derivedStateOf {
                                    if (currentSongIndex < 0) false
                                    else {
                                        val visible = songsListState.layoutInfo.visibleItemsInfo
                                        if (visible.isEmpty()) false
                                        else currentSongIndex in visible.first().index..visible.last().index
                                    }
                                }
                            }
                            val showLocateButton = currentSongIndex >= 0 && !isCurrentSongVisible

                            Column(modifier = Modifier.fillMaxSize()) {
                                com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow(
                                    itemCount = filteredSongs.size,
                                    itemCountLabel = "tracks",
                                    currentSortOption = currentSongSortOption,
                                    showLocateButton = showLocateButton,
                                    onLocateClick = {
                                        if (currentSongIndex >= 0) {
                                            scope.launch {
                                                val firstVisible = songsListState.firstVisibleItemIndex
                                                val distance = kotlin.math.abs(currentSongIndex - firstVisible)
                                                if (distance > 15) {
                                                    val preScrollIndex = if (currentSongIndex > firstVisible) {
                                                        (currentSongIndex - 8).coerceAtLeast(0)
                                                    } else {
                                                        (currentSongIndex + 8).coerceAtMost(filteredSongs.size - 1)
                                                    }
                                                    songsListState.scrollToItem(preScrollIndex)
                                                }
                                                songsListState.animateScrollToItem(currentSongIndex)
                                            }
                                        }
                                    },
                                    onSortOptionSelect = { option -> viewModel.setSongSortOption(option) },
                                    onShuffleClick = { viewModel.shuffleSongs(filteredSongs) }
                                )

                                if (filteredSongs.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
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
                                                text = if (searchQuery.isNotBlank()) "No tracks matching \"$searchQuery\"" else "No tracks in library",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "Try searching with another keyword" else "Add music directories in Settings to scan",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        LazyColumn(
                                            state = songsListState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            itemsIndexed(
                                                items = filteredSongs,
                                                key = { _, song -> song.id },
                                                contentType = { _, _ -> "song_item" }
                                            ) { index, song ->
                                                val isSelected = currentSong?.id == song.id
                                                EnhancedSongListItem(
                                                    song = song,
                                                    isCurrentSong = isSelected,
                                                    isPlaying = playerInfo.isPlaying && isSelected,
                                                    onPlayNext = { viewModel.playSongNext(song) },
                                                    onAddToQueue = { viewModel.addSongToQueue(song) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                                    onClick = { viewModel.playSongs(filteredSongs.drop(index), 0) }
                                                )
                                            }
                                        }

                                        com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                                            listState = songsListState,
                                            dragLabelProvider = songFastScrollLabelProvider
                                        )
                                    }
                                }
                            }
                        }

                        DesktopNavDestination.ALBUMS -> {
                            val currentAlbumSortOption by viewModel.libraryStateHolder.albumSortOption.collectAsState()
                            var isAlbumsListView by remember { mutableStateOf(false) }
                            val albumsGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                            val albumsListState = rememberLazyListState()
                            var lastHandledAlbumSortKey by remember { mutableStateOf(currentAlbumSortOption.storageKey) }
                            var pendingAlbumSortScrollReset by remember { mutableStateOf(false) }

                            val filteredAlbums = remember(albums, searchQuery) {
                                if (searchQuery.isBlank()) albums
                                else albums.filter {
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.artist.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            val albumFastScrollLabelProvider = remember(filteredAlbums, currentAlbumSortOption) {
                                { index: Int ->
                                    com.kayoi.pixelplayerdesktop.presentation.components.albumFastScrollLabel(
                                        album = filteredAlbums.getOrNull(index),
                                        sortOption = currentAlbumSortOption
                                    )
                                }
                            }

                            LaunchedEffect(currentAlbumSortOption) {
                                val currentSortKey = currentAlbumSortOption.storageKey
                                if (currentSortKey != lastHandledAlbumSortKey) {
                                    lastHandledAlbumSortKey = currentSortKey
                                    pendingAlbumSortScrollReset = true
                                    if (isAlbumsListView) albumsListState.scrollToItem(0) else albumsGridState.scrollToItem(0)
                                }
                            }

                            LaunchedEffect(filteredAlbums, pendingAlbumSortScrollReset) {
                                if (pendingAlbumSortScrollReset) {
                                    if (isAlbumsListView) albumsListState.scrollToItem(0) else albumsGridState.scrollToItem(0)
                                    pendingAlbumSortScrollReset = false
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow(
                                    itemCount = filteredAlbums.size,
                                    itemCountLabel = "albums",
                                    currentSortOption = currentAlbumSortOption,
                                    availableSortOptions = com.kayoi.pixelplayerdesktop.data.model.SortOption.ALBUMS,
                                    showViewModeToggle = true,
                                    isListView = isAlbumsListView,
                                    onToggleViewMode = { isAlbumsListView = !isAlbumsListView },
                                    onSortOptionSelect = { option -> viewModel.setAlbumSortOption(option) },
                                    onShuffleClick = {
                                        viewModel.shuffleRandomAlbum(filteredAlbums)
                                    }
                                )

                                if (filteredAlbums.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Album,
                                                contentDescription = null,
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "No albums matching \"$searchQuery\"" else "No albums in library",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (isAlbumsListView) {
                                            LazyColumn(
                                                state = albumsListState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(
                                                    items = filteredAlbums,
                                                    key = { it.title + it.artist },
                                                    contentType = { "album_list_item" }
                                                ) { album ->
                                                    AlbumListItem(
                                                        album = album,
                                                        isDarkTheme = isDarkTheme,
                                                        onClick = {
                                                            val albumSongs = songs.filter { it.album == album.title }
                                                            if (albumSongs.isNotEmpty()) {
                                                                viewModel.playSongs(albumSongs)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                state = albumsGridState,
                                                columns = GridCells.Adaptive(minSize = 180.dp),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(
                                                    items = filteredAlbums,
                                                    key = { it.title + it.artist },
                                                    contentType = { "album_grid_item" }
                                                ) { album ->
                                                    AlbumGridItem(
                                                        album = album,
                                                        isDarkTheme = isDarkTheme,
                                                        onClick = {
                                                            val albumSongs = songs.filter { it.album == album.title }
                                                            if (albumSongs.isNotEmpty()) {
                                                                viewModel.playSongs(albumSongs)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                                            listState = if (isAlbumsListView) albumsListState else null,
                                            gridState = if (!isAlbumsListView) albumsGridState else null,
                                            dragLabelProvider = albumFastScrollLabelProvider
                                        )
                                    }
                                }
                            }
                        }

                        DesktopNavDestination.ARTISTS -> {
                            val currentArtistSortOption by viewModel.libraryStateHolder.artistSortOption.collectAsState()
                            val fetchDeezerImages by viewModel.fetchDeezerArtistImages.collectAsState()
                            var isArtistsListView by remember { mutableStateOf(false) }
                            val artistsGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                            val artistsListState = rememberLazyListState()
                            var lastHandledArtistSortKey by remember { mutableStateOf(currentArtistSortOption.storageKey) }
                            var pendingArtistSortScrollReset by remember { mutableStateOf(false) }

                            val filteredArtists = remember(artists, searchQuery) {
                                if (searchQuery.isBlank()) artists
                                else artists.filter {
                                    it.name.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            val artistFastScrollLabelProvider = remember(filteredArtists, currentArtistSortOption) {
                                { index: Int ->
                                    com.kayoi.pixelplayerdesktop.presentation.components.artistFastScrollLabel(
                                        artist = filteredArtists.getOrNull(index),
                                        sortOption = currentArtistSortOption
                                    )
                                }
                            }

                            LaunchedEffect(currentArtistSortOption) {
                                val currentSortKey = currentArtistSortOption.storageKey
                                if (currentSortKey != lastHandledArtistSortKey) {
                                    lastHandledArtistSortKey = currentSortKey
                                    pendingArtistSortScrollReset = true
                                    if (isArtistsListView) artistsListState.scrollToItem(0) else artistsGridState.scrollToItem(0)
                                }
                            }

                            LaunchedEffect(filteredArtists, pendingArtistSortScrollReset) {
                                if (pendingArtistSortScrollReset) {
                                    if (isArtistsListView) artistsListState.scrollToItem(0) else artistsGridState.scrollToItem(0)
                                    pendingArtistSortScrollReset = false
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow(
                                    itemCount = filteredArtists.size,
                                    itemCountLabel = "artists",
                                    currentSortOption = currentArtistSortOption,
                                    availableSortOptions = com.kayoi.pixelplayerdesktop.data.model.SortOption.ARTISTS,
                                    showViewModeToggle = true,
                                    isListView = isArtistsListView,
                                    onToggleViewMode = { isArtistsListView = !isArtistsListView },
                                    onSortOptionSelect = { option -> viewModel.setArtistSortOption(option) },
                                    onShuffleClick = {
                                        viewModel.shuffleRandomArtist(filteredArtists)
                                    }
                                )

                                if (filteredArtists.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 24.dp, vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons.Artist,
                                                contentDescription = null,
                                                tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "No artists matching \"$searchQuery\"" else "No artists in library",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (isArtistsListView) {
                                            LazyColumn(
                                                state = artistsListState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(
                                                    items = filteredArtists,
                                                    key = { it.name },
                                                    contentType = { "artist_list_item" }
                                                ) { artist ->
                                                    val representativeArtwork = remember(artist.name, songs) {
                                                        songs.firstOrNull { it.artist.contains(artist.name, ignoreCase = true) }?.albumArtUriString
                                                    }
                                                    ArtistListItem(
                                                        artist = artist,
                                                        fallbackArtworkUri = representativeArtwork,
                                                        fetchDeezerImages = fetchDeezerImages,
                                                        onClick = {
                                                            val artistSongs = songs.filter { it.artist.contains(artist.name, ignoreCase = true) }
                                                            if (artistSongs.isNotEmpty()) {
                                                                viewModel.playSongs(artistSongs)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                state = artistsGridState,
                                                columns = GridCells.Adaptive(minSize = 150.dp),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                items(
                                                    items = filteredArtists,
                                                    key = { it.name },
                                                    contentType = { "artist_grid_item" }
                                                ) { artist ->
                                                    val representativeArtwork = remember(artist.name, songs) {
                                                        songs.firstOrNull { it.artist.contains(artist.name, ignoreCase = true) }?.albumArtUriString
                                                    }
                                                    ArtistGridItem(
                                                        artist = artist,
                                                        fallbackArtworkUri = representativeArtwork,
                                                        fetchDeezerImages = fetchDeezerImages,
                                                        onClick = {
                                                            val artistSongs = songs.filter { it.artist.contains(artist.name, ignoreCase = true) }
                                                            if (artistSongs.isNotEmpty()) {
                                                                viewModel.playSongs(artistSongs)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                                            listState = if (isArtistsListView) artistsListState else null,
                                            gridState = if (!isArtistsListView) artistsGridState else null,
                                            dragLabelProvider = artistFastScrollLabelProvider
                                        )
                                    }
                                }
                            }
                        }

                        DesktopNavDestination.PLAYLISTS -> {
                            if (selectedPlaylistId != null) {
                                PlaylistDetailScreen(
                                    playlistId = selectedPlaylistId!!,
                                    viewModel = viewModel,
                                    onBack = { selectedPlaylistId = null }
                                )
                            } else {
                                val currentPlaylistSortOption by viewModel.libraryStateHolder.playlistSortOption.collectAsState()
                                val playlistsListState = rememberLazyListState()
                                var lastHandledPlaylistSortKey by remember { mutableStateOf(currentPlaylistSortOption.storageKey) }
                                var pendingPlaylistSortScrollReset by remember { mutableStateOf(false) }

                                var showCreatePlaylistDialog by remember { mutableStateOf(false) }
                                var playlistToRename by remember { mutableStateOf<com.kayoi.pixelplayerdesktop.data.model.Playlist?>(null) }
                                var playlistToDelete by remember { mutableStateOf<com.kayoi.pixelplayerdesktop.data.model.Playlist?>(null) }

                                val filteredPlaylists = remember(playlists, searchQuery) {
                                    if (searchQuery.isBlank()) playlists
                                    else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }

                                val playlistFastScrollLabelProvider = remember(filteredPlaylists, currentPlaylistSortOption) {
                                    { index: Int ->
                                        com.kayoi.pixelplayerdesktop.presentation.components.playlistFastScrollLabel(
                                            playlist = filteredPlaylists.getOrNull(index),
                                            sortOption = currentPlaylistSortOption
                                        )
                                    }
                                }

                                LaunchedEffect(currentPlaylistSortOption) {
                                    val currentSortKey = currentPlaylistSortOption.storageKey
                                    if (currentSortKey != lastHandledPlaylistSortKey) {
                                        lastHandledPlaylistSortKey = currentSortKey
                                        pendingPlaylistSortScrollReset = true
                                        playlistsListState.scrollToItem(0)
                                    }
                                }

                                LaunchedEffect(filteredPlaylists, pendingPlaylistSortScrollReset) {
                                    if (pendingPlaylistSortScrollReset) {
                                        playlistsListState.scrollToItem(0)
                                        pendingPlaylistSortScrollReset = false
                                    }
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow(
                                        itemCount = filteredPlaylists.size,
                                        itemCountLabel = "playlists",
                                        currentSortOption = currentPlaylistSortOption,
                                        availableSortOptions = com.kayoi.pixelplayerdesktop.data.model.SortOption.PLAYLISTS,
                                        showCreateButton = true,
                                        createButtonLabel = "New Playlist",
                                        onCreateClick = { showCreatePlaylistDialog = true },
                                        showShuffleButton = false,
                                        onSortOptionSelect = { option -> viewModel.setPlaylistSortOption(option) }
                                    )

                                    if (filteredPlaylists.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 24.dp, vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.PlaylistPlay,
                                                    contentDescription = null,
                                                    tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(54.dp)
                                                )
                                                Text(
                                                    text = if (searchQuery.isNotBlank()) "No playlists matching \"$searchQuery\"" else "No playlists created yet",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurfaceVariant
                                                )
                                                if (searchQuery.isBlank()) {
                                                    Button(
                                                        onClick = { showCreatePlaylistDialog = true },
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = colors.primary,
                                                            contentColor = colors.onPrimary
                                                        )
                                                    ) {
                                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Create Playlist", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            LazyColumn(
                                                state = playlistsListState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(
                                                    items = filteredPlaylists,
                                                    key = { it.id },
                                                    contentType = { "playlist_list_item" }
                                                ) { pl ->
                                                    val plSongs = remember(pl.songIds, songs) {
                                                        songs.filter { pl.songIds.contains(it.id) }
                                                    }
                                                    com.kayoi.pixelplayerdesktop.presentation.components.subcomps.PlaylistListItem(
                                                        playlist = pl,
                                                        playlistSongs = plSongs,
                                                        onClick = { selectedPlaylistId = pl.id },
                                                        onPlayClick = {
                                                            if (plSongs.isNotEmpty()) {
                                                                viewModel.playSongs(plSongs, 0)
                                                            }
                                                        },
                                                        onRenameClick = { playlistToRename = pl },
                                                        onDeleteClick = { playlistToDelete = pl }
                                                    )
                                                }
                                            }

                                            com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                                                listState = playlistsListState,
                                                dragLabelProvider = playlistFastScrollLabelProvider
                                            )
                                        }
                                    }
                                }

                                if (showCreatePlaylistDialog) {
                                    com.kayoi.pixelplayerdesktop.presentation.components.CreatePlaylistDialog(
                                        title = "New Playlist",
                                        onDismiss = { showCreatePlaylistDialog = false },
                                        onConfirm = { name ->
                                            viewModel.createPlaylist(name)
                                            showCreatePlaylistDialog = false
                                        }
                                    )
                                }

                                playlistToRename?.let { pl ->
                                    com.kayoi.pixelplayerdesktop.presentation.components.CreatePlaylistDialog(
                                        title = "Rename Playlist",
                                        initialName = pl.name,
                                        confirmButtonText = "Save",
                                        onDismiss = { playlistToRename = null },
                                        onConfirm = { newName ->
                                            viewModel.renamePlaylist(pl.id, newName)
                                            playlistToRename = null
                                        }
                                    )
                                }

                                playlistToDelete?.let { pl ->
                                    AlertDialog(
                                        onDismissRequest = { playlistToDelete = null },
                                        title = { Text("Delete Playlist") },
                                        text = { Text("Are you sure you want to delete \"${pl.name}\"? This cannot be undone.") },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.deletePlaylist(pl.id)
                                                    playlistToDelete = null
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.error,
                                                    contentColor = colors.onError
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Delete")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = { playlistToDelete = null },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Cancel")
                                            }
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                }
                            }
                        }

                        DesktopNavDestination.FOLDERS -> {
                            if (selectedFolderDetail != null) {
                                FolderDetailScreen(
                                    folder = selectedFolderDetail!!,
                                    viewModel = viewModel,
                                    onBack = { selectedFolderDetail = null }
                                )
                            } else {
                                val currentFolderSortOption by viewModel.folderSortOption.collectAsState()
                                val foldersListState = rememberLazyListState()
                                var lastHandledFolderSortKey by remember { mutableStateOf(currentFolderSortOption.storageKey) }
                                var pendingFolderSortScrollReset by remember { mutableStateOf(false) }

                                val fullFolderTree = remember(songs, musicDirectories) {
                                    FolderTreeBuilder.buildFolderTree(songs, musicDirectories)
                                }

                                val activeTreeFolder = remember(fullFolderTree, currentFolderTreePath) {
                                    findFolderByPath(currentFolderTreePath, fullFolderTree)
                                }

                                val flattenedFolders = remember(fullFolderTree) {
                                    FolderTreeBuilder.flattenFolders(fullFolderTree)
                                }

                                val filteredFlattenedFolders = remember(flattenedFolders, searchQuery) {
                                    if (searchQuery.isBlank()) flattenedFolders
                                    else flattenedFolders.filter {
                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.path.contains(searchQuery, ignoreCase = true)
                                    }
                                }

                                val sortedFlattenedFolders = remember(filteredFlattenedFolders, currentFolderSortOption) {
                                    FolderTreeBuilder.sortMusicFolders(filteredFlattenedFolders, currentFolderSortOption)
                                }

                                val currentTreeSubFolders = remember(activeTreeFolder, fullFolderTree, currentFolderSortOption, searchQuery) {
                                    val list = activeTreeFolder?.subFolders ?: fullFolderTree
                                    val filtered = if (searchQuery.isBlank()) list else list.filter {
                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.path.contains(searchQuery, ignoreCase = true)
                                    }
                                    FolderTreeBuilder.sortMusicFolders(filtered, currentFolderSortOption)
                                }

                                val currentTreeSongs = remember(activeTreeFolder, currentFolderSortOption, searchQuery) {
                                    val list = activeTreeFolder?.songs ?: emptyList()
                                    val filtered = if (searchQuery.isBlank()) list else list.filter {
                                        it.title.contains(searchQuery, ignoreCase = true) ||
                                        it.artist.contains(searchQuery, ignoreCase = true) ||
                                        it.album.contains(searchQuery, ignoreCase = true)
                                    }
                                    FolderTreeBuilder.sortSongsForFolder(filtered, currentFolderSortOption)
                                }

                                val folderFastScrollLabelProvider = remember(isFoldersPlaylistView, sortedFlattenedFolders, currentTreeSubFolders, currentFolderSortOption) {
                                    { index: Int ->
                                        val folder = if (isFoldersPlaylistView) {
                                            sortedFlattenedFolders.getOrNull(index)
                                        } else {
                                            currentTreeSubFolders.getOrNull(index)
                                        }
                                        folderFastScrollLabel(
                                            folder = folder,
                                            sortOption = currentFolderSortOption
                                        )
                                    }
                                }

                                LaunchedEffect(currentFolderSortOption) {
                                    val currentSortKey = currentFolderSortOption.storageKey
                                    if (currentSortKey != lastHandledFolderSortKey) {
                                        lastHandledFolderSortKey = currentSortKey
                                        pendingFolderSortScrollReset = true
                                        foldersListState.scrollToItem(0)
                                    }
                                }

                                LaunchedEffect(sortedFlattenedFolders, currentTreeSubFolders, pendingFolderSortScrollReset) {
                                    if (pendingFolderSortScrollReset) {
                                        foldersListState.scrollToItem(0)
                                        pendingFolderSortScrollReset = false
                                    }
                                }

                                val totalItemsCount = if (isFoldersPlaylistView) {
                                    sortedFlattenedFolders.size
                                } else {
                                    currentTreeSubFolders.size + currentTreeSongs.size
                                }

                                val breadcrumbs = remember(currentFolderTreePath, fullFolderTree) {
                                    if (currentFolderTreePath == null) emptyList()
                                    else getFolderAncestors(currentFolderTreePath!!, fullFolderTree)
                                }

                                val favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    com.kayoi.pixelplayerdesktop.presentation.components.subcomps.LibraryActionRow(
                                        itemCount = totalItemsCount,
                                        itemCountLabel = if (isFoldersPlaylistView) "folders" else if (activeTreeFolder != null) "items" else "folders",
                                        currentSortOption = currentFolderSortOption,
                                        availableSortOptions = com.kayoi.pixelplayerdesktop.data.model.SortOption.FOLDERS,
                                        showViewModeToggle = true,
                                        isListView = !isFoldersPlaylistView,
                                        onToggleViewMode = { isFoldersPlaylistView = !isFoldersPlaylistView },
                                        onSortOptionSelect = { option -> viewModel.setFolderSortOption(option) },
                                        showShuffleButton = false,
                                        customLeadingContent = if (!isFoldersPlaylistView && breadcrumbs.isNotEmpty()) {
                                            {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                                ) {
                                                    Surface(
                                                        onClick = { currentFolderTreePath = null },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = colors.surfaceContainerLow,
                                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.FolderOpen,
                                                                contentDescription = null,
                                                                tint = colors.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Root",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = colors.onSurface
                                                            )
                                                        }
                                                    }

                                                    breadcrumbs.forEachIndexed { index, crumb ->
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                            contentDescription = null,
                                                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                                            modifier = Modifier.padding(horizontal = 4.dp).size(16.dp)
                                                        )
                                                        val isLast = index == breadcrumbs.lastIndex
                                                        Surface(
                                                            onClick = { currentFolderTreePath = crumb.path },
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = if (isLast) colors.primaryContainer else colors.surfaceContainerLow,
                                                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                                        ) {
                                                            Text(
                                                                text = crumb.name,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isLast) colors.onPrimaryContainer else colors.onSurface,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else null
                                    )

                                    if ((isFoldersPlaylistView && sortedFlattenedFolders.isEmpty()) ||
                                        (!isFoldersPlaylistView && currentTreeSubFolders.isEmpty() && currentTreeSongs.isEmpty())
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 24.dp, vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Folder,
                                                    contentDescription = null,
                                                    tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(54.dp)
                                                )
                                                Text(
                                                    text = if (searchQuery.isNotBlank()) "No folders matching \"$searchQuery\"" else "No folders found in library",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            LazyColumn(
                                                state = foldersListState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isFoldersPlaylistView) {
                                                    items(
                                                        items = sortedFlattenedFolders,
                                                        key = { it.path },
                                                        contentType = { "folder_playlist_item" }
                                                    ) { folder ->
                                                        FolderPlaylistItem(
                                                            folder = folder,
                                                            onClick = { selectedFolderDetail = folder },
                                                            onPlayClick = {
                                                                val fSongs = folder.collectAllSongs()
                                                                if (fSongs.isNotEmpty()) {
                                                                    viewModel.playSongs(fSongs, 0)
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    items(
                                                        items = currentTreeSubFolders,
                                                        key = { "subfolder_${it.path}" },
                                                        contentType = { "folder_tree_item" }
                                                    ) { folder ->
                                                        FolderListItem(
                                                            folder = folder,
                                                            onClick = { currentFolderTreePath = folder.path },
                                                            onPlayClick = {
                                                                val fSongs = folder.collectAllSongs()
                                                                if (fSongs.isNotEmpty()) {
                                                                    viewModel.playSongs(fSongs, 0)
                                                                }
                                                            }
                                                        )
                                                    }

                                                    if (currentTreeSongs.isNotEmpty()) {
                                                        itemsIndexed(
                                                            items = currentTreeSongs,
                                                            key = { _, song -> "song_${song.id}" },
                                                            contentType = { _, _ -> "folder_tree_song" }
                                                        ) { index, song ->
                                                            val isCurrentPlaying = currentSong?.id == song.id
                                                            EnhancedSongListItem(
                                                                song = song,
                                                                isPlaying = isCurrentPlaying && playerInfo.isPlaying,
                                                                isCurrentSong = isCurrentPlaying,
                                                                isFavorite = favoriteSongIds.contains(song.id),
                                                                onPlayNext = { viewModel.playSongNext(song) },
                                                                onAddToQueue = { viewModel.addSongToQueue(song) },
                                                                onToggleFavorite = { viewModel.toggleFavorite(song) },
                                                                onClick = { viewModel.playSongs(currentTreeSongs.drop(index), 0) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveScrollBar(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(end = 4.dp, top = 4.dp, bottom = 16.dp),
                                                listState = foldersListState,
                                                dragLabelProvider = folderFastScrollLabelProvider
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DesktopNavDestination.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                isDynamicColor = isDynamicColor,
                                onToggleDarkTheme = { viewModel.toggleDarkMode() },
                                onToggleDynamicColor = { viewModel.toggleDynamicColor() },
                                onRescanLibrary = { viewModel.scanLibrary() }
                            )
                        }
                    }
                }

                    AnimatedVisibility(
                        visible = isQueueSidePanelOpen && (currentSong != null || queue.isNotEmpty()),
                        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(tween(250)),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(tween(200))
                    ) {
                        QueueSidePanel(
                            queue = queue,
                            currentSong = currentSong,
                            isPlaying = playerInfo.isPlaying,
                            isShuffleOn = playerInfo.isShuffleEnabled,
                            repeatMode = playerInfo.repeatMode,
                            onSongClick = { song, index ->
                                viewModel.playSongAtIndex(index)
                            },
                            onToggleShuffle = {
                                viewModel.toggleShuffle()
                            },
                            onCycleRepeat = {
                                viewModel.cycleRepeatMode()
                            },
                            onClearQueue = {
                                viewModel.clearQueue()
                            },
                            onMoveItem = { from, to ->
                                viewModel.moveQueueItem(from, to)
                            },
                            onRemoveItem = { index ->
                                viewModel.removeFromQueue(index)
                            },
                            onPlayNext = { song ->
                                viewModel.playSongNext(song)
                            },
                            onToggleFavorite = { song ->
                                viewModel.toggleFavorite(song)
                            },
                            favoriteSongIds = remember(favoriteSongs) { favoriteSongs.map { it.id }.toSet() }
                        )
                    }
                }

                if (currentSong != null) {
                    PixelPlayerTheme(
                        darkTheme = isDarkTheme,
                        colorSchemeOverride = songColorScheme
                    ) {
                        MiniPlayerBar(
                            currentSong = currentSong!!,
                            isPlaying = playerInfo.isPlaying,
                            currentPositionMs = playerInfo.currentPositionMs,
                            totalDurationMs = playerInfo.totalDurationMs,
                            progress = progressFraction,
                            isShuffle = playerInfo.isShuffleEnabled,
                            repeatMode = playerInfo.repeatMode,
                            isFavorite = playerInfo.isFavorite,
                            volume = playerInfo.volume,
                            isMuted = playerInfo.isMuted,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.playPrevious() },
                            onNextClick = { viewModel.playNext() },
                            onSeek = { fraction ->
                                val targetMs = (fraction * playerInfo.totalDurationMs).toLong()
                                viewModel.seekTo(targetMs)
                            },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onExpandClick = { isExpandedPlayerOpen = true },
                            onLyricsClick = {
                                initialExpandedTab = ExpandedTab.LYRICS
                                isExpandedPlayerOpen = true
                            },
                            onQueueClick = onToggleQueueSidePanel,
                            onCloseClick = {
                                viewModel.stopPlayback()
                                showDismissUndoBar = true
                            }
                        )
                    }
                } else if (showDismissUndoBar) {
                    AnimatedVisibility(
                        visible = showDismissUndoBar,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        DismissUndoBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            message = "Playlist dismissed",
                            onUndo = {
                                showDismissUndoBar = false
                                viewModel.undoStopPlayback()
                            },
                            onClose = {
                                showDismissUndoBar = false
                            },
                            durationMillis = 4500L
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpandedPlayerOpen && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(380)) + fadeIn(tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(250))
        ) {
            ExpandedPlayerScreen(
                viewModel = viewModel,
                initialTab = initialExpandedTab,
                onClose = { isExpandedPlayerOpen = false },
                isMaximized = isMaximized,
                onMinimize = onMinimize,
                onMaximizeRestore = onMaximizeRestore,
                onCloseWindow = onClose,
                topBarWrapper = topBarWrapper
            )
        }
    }
}

private fun findFolderByPath(path: String?, folders: List<MusicFolder>): MusicFolder? {
    if (path == null) return null
    for (folder in folders) {
        if (folder.path == path) return folder
        val found = findFolderByPath(path, folder.subFolders)
        if (found != null) return found
    }
    return null
}

private fun getFolderAncestors(targetPath: String, folders: List<MusicFolder>): List<MusicFolder> {
    val result = mutableListOf<MusicFolder>()
    fun traverse(current: MusicFolder): Boolean {
        result.add(current)
        if (current.path == targetPath) return true
        for (sub in current.subFolders) {
            if (traverse(sub)) return true
        }
        result.removeAt(result.size - 1)
        return false
    }
    for (root in folders) {
        if (traverse(root)) return result
    }
    return emptyList()
}

