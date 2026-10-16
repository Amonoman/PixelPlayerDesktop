package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.components.AddToPlaylistDialog
import com.kayoi.pixelplayerdesktop.presentation.components.AppSidebar
import com.kayoi.pixelplayerdesktop.presentation.components.AppTopBar
import com.kayoi.pixelplayerdesktop.presentation.components.BatchEditMetadataDialog
import com.kayoi.pixelplayerdesktop.presentation.components.DeleteSongConfirmationDialog
import com.kayoi.pixelplayerdesktop.presentation.components.DismissUndoBar
import com.kayoi.pixelplayerdesktop.presentation.components.EditSongMetadataDialog
import com.kayoi.pixelplayerdesktop.presentation.components.MiniPlayerBar
import com.kayoi.pixelplayerdesktop.presentation.components.QueueSidePanel
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.screens.LibrarySubDetail
import com.kayoi.pixelplayerdesktop.presentation.screens.library.AlbumsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.ArtistsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.FoldersTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.PlaylistsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.library.SongsTab
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSection
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val homeStatsOverview by viewModel.homeStatsOverview.collectAsState()
    val musicDirectories by viewModel.musicDirectories.collectAsState()

    val playerInfo by viewModel.playerInfo.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isDarkTheme by viewModel.isDarkMode.collectAsState()
    val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
    val paletteStyle by viewModel.albumArtPaletteStyle.collectAsState()
    val colorAccuracy by viewModel.albumArtColorAccuracy.collectAsState()
    val navBarCompactMode by viewModel.navBarCompactMode.collectAsState()
    val defaultTabKey by viewModel.defaultTab.collectAsState()
    val showQueueHistory by viewModel.showQueueHistory.collectAsState()

    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedArtist by remember { mutableStateOf<Artist?>(null) }
    var selectedFolderDetail by remember { mutableStateOf<MusicFolder?>(null) }
    var currentFolderTreePath by remember { mutableStateOf<String?>(null) }
    var settingsInitialSection by remember { mutableStateOf<SettingsSection?>(null) }
    var settingsInitialSubDetail by remember { mutableStateOf(LibrarySubDetail.NONE) }

    val songColorScheme by produceState<ColorScheme?>(
        initialValue = if (isDynamicColor && currentSong?.albumArtUriString != null) {
            DynamicColorProcessor.getCachedColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )?.let { if (isDarkTheme) it.dark else it.light }
        } else null,
        currentSong?.albumArtUriString,
        isDarkTheme,
        isDynamicColor,
        paletteStyle,
        colorAccuracy
    ) {
        if (!isDynamicColor || currentSong?.albumArtUriString == null) {
            value = null
            return@produceState
        }
        val scheme = withContext(Dispatchers.Default) {
            val pair = DynamicColorProcessor.extractColorScheme(
                pathOrUri = currentSong?.albumArtUriString,
                paletteStyle = paletteStyle,
                colorAccuracyLevel = colorAccuracy
            )
            if (isDarkTheme) pair?.dark else pair?.light
        }
        value = scheme
    }

    val initialDestination = remember {
        DesktopNavDestination.entries.firstOrNull { it.name == viewModel.defaultTab.value } ?: DesktopNavDestination.HOME
    }
    var currentDestination by remember { mutableStateOf(initialDestination) }
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
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var songToEditMetadata by remember { mutableStateOf<Song?>(null) }
    var batchSongsToEdit by remember { mutableStateOf<List<Song>?>(null) }
    var isDailyMixDetailOpen by remember { mutableStateOf(false) }
    var isRecentlyPlayedDetailOpen by remember { mutableStateOf(false) }
    var isStatsDetailOpen by remember { mutableStateOf(false) }

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isQueueVisible = isQueueSidePanelOpen && (currentSong != null || queue.isNotEmpty())
        val isSidebarCompact = navBarCompactMode || (maxWidth < 960.dp && isQueueVisible) || (maxWidth < 740.dp)

        Row(modifier = Modifier.fillMaxSize()) {
            AppSidebar(
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    currentDestination = destination
                    previousDestinationBeforeSearch = null
                    if (destination != DesktopNavDestination.HOME) {
                        isDailyMixDetailOpen = false
                        isRecentlyPlayedDetailOpen = false
                        isStatsDetailOpen = false
                    }
                    if (destination != DesktopNavDestination.PLAYLISTS) {
                        selectedPlaylistId = null
                    }
                    if (destination != DesktopNavDestination.ALBUMS) {
                        selectedAlbum = null
                    }
                    if (destination != DesktopNavDestination.ARTISTS) {
                        selectedArtist = null
                    }
                    if (destination != DesktopNavDestination.FOLDERS) {
                        selectedFolderDetail = null
                        currentFolderTreePath = null
                    }
                    if (destination != DesktopNavDestination.SETTINGS) {
                        settingsInitialSection = null
                        settingsInitialSubDetail = LibrarySubDetail.NONE
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
                                when {
                                    isDailyMixDetailOpen -> {
                                        DailyMixScreen(
                                            songs = songs,
                                            favoriteSongs = favoriteSongs,
                                            currentSong = currentSong,
                                            isPlaying = playerInfo.isPlaying,
                                            viewModel = viewModel,
                                            onBack = { isDailyMixDetailOpen = false },
                                            onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                            onDelete = { song -> songToDelete = song },
                                            onEditMetadata = { song -> songToEditMetadata = song }
                                        )
                                    }
                                    isRecentlyPlayedDetailOpen -> {
                                        RecentlyPlayedScreen(
                                            recentlyPlayedSongs = recentlyPlayedSongs,
                                            currentSong = currentSong,
                                            isPlaying = playerInfo.isPlaying,
                                            viewModel = viewModel,
                                            onBack = { isRecentlyPlayedDetailOpen = false },
                                            onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                            onDelete = { song -> songToDelete = song },
                                            onEditMetadata = { song -> songToEditMetadata = song }
                                        )
                                    }
                                    isStatsDetailOpen -> {
                                        StatsScreen(
                                            viewModel = viewModel,
                                            songs = songs,
                                            onBack = { isStatsDetailOpen = false }
                                        )
                                    }
                                    else -> {
                                        HomeScreen(
                                            songs = songs,
                                            favoriteSongs = favoriteSongs,
                                            recentlyPlayedSongs = recentlyPlayedSongs,
                                            homeStatsOverview = homeStatsOverview,
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
                                            onPlaySongs = { songList, startIndex ->
                                                viewModel.playSongs(songList, startIndex)
                                            },
                                            onShuffleAll = { viewModel.playSongs(songs.shuffled()) },
                                            onNavigateToLibrary = { currentDestination = DesktopNavDestination.SONGS },
                                            onNavigateToDailyMix = { isDailyMixDetailOpen = true },
                                            onNavigateToRecentlyPlayed = { isRecentlyPlayedDetailOpen = true },
                                            onNavigateToStats = { isStatsDetailOpen = true },
                                            onNavigateToFolders = {
                                                settingsInitialSection = SettingsSection.LIBRARY
                                                settingsInitialSubDetail = LibrarySubDetail.FOLDERS
                                                currentDestination = DesktopNavDestination.SETTINGS
                                            },
                                            onRescanLibrary = { viewModel.rescanLibrary() },
                                            onPlayNext = { song -> viewModel.playSongNext(song) },
                                            onAddToQueue = { song -> viewModel.addSongToQueue(song) },
                                            onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                            onDelete = { song -> songToDelete = song },
                                            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                                            onEditMetadata = { song -> songToEditMetadata = song }
                                        )
                                    }
                                }
                            }

                            DesktopNavDestination.SONGS, DesktopNavDestination.LIKED -> {
                                SongsTab(
                                    filteredSongs = filteredSongs,
                                    currentSong = currentSong,
                                    isPlaying = playerInfo.isPlaying,
                                    searchQuery = searchQuery,
                                    viewModel = viewModel,
                                    onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                    onDelete = { song -> songToDelete = song },
                                    onEditMetadata = { song -> songToEditMetadata = song }
                                )
                            }

                            DesktopNavDestination.ALBUMS -> {
                                AlbumsTab(
                                    albums = albums,
                                    songs = songs,
                                    isDarkTheme = isDarkTheme,
                                    searchQuery = searchQuery,
                                    selectedAlbum = selectedAlbum,
                                    onSelectAlbum = { selectedAlbum = it },
                                    viewModel = viewModel,
                                    onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                    onDelete = { song -> songToDelete = song },
                                    onEditMetadata = { song -> songToEditMetadata = song },
                                    onBatchEditMetadata = { batchSongs -> batchSongsToEdit = batchSongs }
                                )
                            }

                            DesktopNavDestination.ARTISTS -> {
                                ArtistsTab(
                                    artists = artists,
                                    songs = songs,
                                    searchQuery = searchQuery,
                                    selectedArtist = selectedArtist,
                                    onSelectArtist = { selectedArtist = it },
                                    viewModel = viewModel,
                                    onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                    onDelete = { song -> songToDelete = song },
                                    onEditMetadata = { song -> songToEditMetadata = song }
                                )
                            }

                            DesktopNavDestination.PLAYLISTS -> {
                                PlaylistsTab(
                                    playlists = playlists,
                                    songs = songs,
                                    searchQuery = searchQuery,
                                    selectedPlaylistId = selectedPlaylistId,
                                    onSelectPlaylist = { selectedPlaylistId = it },
                                    viewModel = viewModel,
                                    onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                    onDelete = { song -> songToDelete = song },
                                    onEditMetadata = { song -> songToEditMetadata = song }
                                )
                            }

                            DesktopNavDestination.FOLDERS -> {
                                FoldersTab(
                                    songs = songs,
                                    musicDirectories = musicDirectories,
                                    currentSong = currentSong,
                                    isPlaying = playerInfo.isPlaying,
                                    favoriteSongs = favoriteSongs,
                                    searchQuery = searchQuery,
                                    selectedFolderDetail = selectedFolderDetail,
                                    onSelectFolderDetail = { selectedFolderDetail = it },
                                    currentFolderTreePath = currentFolderTreePath,
                                    onFolderTreePathChange = { currentFolderTreePath = it },
                                    viewModel = viewModel,
                                    onAddToPlaylist = { song -> songToAddToPlaylist = song },
                                    onDelete = { song -> songToDelete = song },
                                    onEditMetadata = { song -> songToEditMetadata = song },
                                    onBatchEditMetadata = { batchSongs -> batchSongsToEdit = batchSongs }
                                )
                            }

                            DesktopNavDestination.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    isDarkTheme = isDarkTheme,
                                    isDynamicColor = isDynamicColor,
                                    onToggleDarkTheme = { viewModel.toggleDarkMode() },
                                    onToggleDynamicColor = { viewModel.toggleDynamicColor() },
                                    onRescanLibrary = { viewModel.rescanLibrary() },
                                    initialSection = settingsInitialSection,
                                    initialSubDetail = settingsInitialSubDetail
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
                            showQueueHistory = showQueueHistory,
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
                            onAddToPlaylist = { song ->
                                songToAddToPlaylist = song
                            },
                            onDelete = { song ->
                                songToDelete = song
                            },
                            onToggleFavorite = { song ->
                                viewModel.toggleFavorite(song)
                            },
                            onEditMetadata = { song ->
                                songToEditMetadata = song
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
                            totalDurationMs = playerInfo.totalDurationMs,
                            currentPositionFlow = viewModel.currentPositionMs,
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
                onEditMetadata = { song -> songToEditMetadata = song },
                onAddToPlaylist = { song -> songToAddToPlaylist = song },
                onDelete = { song -> songToDelete = song },
                topBarWrapper = topBarWrapper
            )
        }

        if (songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                song = songToAddToPlaylist!!,
                playlists = playlists,
                onDismiss = { songToAddToPlaylist = null },
                onPlaylistSelected = { playlist ->
                    viewModel.addSongToPlaylist(playlist.id, songToAddToPlaylist!!.id)
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name, listOf(songToAddToPlaylist!!.id))
                }
            )
        }

        if (songToDelete != null) {
            DeleteSongConfirmationDialog(
                song = songToDelete!!,
                onDismiss = { songToDelete = null },
                onConfirm = {
                    viewModel.deleteSong(songToDelete!!)
                }
            )
        }

        if (songToEditMetadata != null) {
            EditSongMetadataDialog(
                song = songToEditMetadata!!,
                viewModel = viewModel,
                onDismiss = { songToEditMetadata = null }
            )
        }

        if (batchSongsToEdit != null) {
            BatchEditMetadataDialog(
                songs = batchSongsToEdit!!,
                viewModel = viewModel,
                onDismiss = { batchSongsToEdit = null }
            )
        }
    }
}
