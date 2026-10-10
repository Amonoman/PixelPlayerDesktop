import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kayoi.pixelplayerdesktop.di.appModules
import com.kayoi.pixelplayerdesktop.presentation.screens.MainAppScaffold
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.SystemAccentColorProvider
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun main() = application {
    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
        width = 1100.dp,
        height = 800.dp
    )

    val koin = remember {
        val existing = GlobalContext.getOrNull()
        if (existing == null) {
            startKoin {
                modules(appModules)
            }.koin
        } else {
            existing.get()
        }
    }

    val playerViewModel = remember { koin.get<PlayerViewModel>() }
    val isDarkTheme by playerViewModel.isDarkMode.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isQueueSidePanelOpen by remember { mutableStateOf(true) }

    // System-Based Dynamic Theme (Windows / Linux Accent Color)
    val systemColorSchemePair = remember {
        val systemSeed = SystemAccentColorProvider.getSystemAccentColor()
        DynamicColorProcessor.generateColorSchemePair(systemSeed)
    }
    val systemColorScheme = if (isDarkTheme) systemColorSchemePair.dark else systemColorSchemePair.light

    // Scan user's library on initial launch
    LaunchedEffect(Unit) {
        playerViewModel.scanLibrary()
    }

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null && queue.isNotEmpty()) {
            isQueueSidePanelOpen = true
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "PixelPlayer",
        icon = painterResource("icons/icon.png"),
        state = windowState,
        undecorated = true,
        onPreviewKeyEvent = { keyEvent ->
            val isModifier = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    isModifier && keyEvent.key == Key.K -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionRight -> {
                        playerViewModel.playNext()
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionLeft -> {
                        playerViewModel.playPrevious()
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionUp -> {
                        val currentVol = playerViewModel.playerInfo.value.volume
                        playerViewModel.setVolume((currentVol + 0.05f).coerceIn(0f, 1f))
                        true
                    }
                    isModifier && keyEvent.key == Key.DirectionDown -> {
                        val currentVol = playerViewModel.playerInfo.value.volume
                        playerViewModel.setVolume((currentVol - 0.05f).coerceIn(0f, 1f))
                        true
                    }
                    keyEvent.key == Key.M && !isSearchFocused && !isModifier -> {
                        playerViewModel.toggleMute()
                        true
                    }
                    isModifier && keyEvent.key == Key.S -> {
                        playerViewModel.toggleShuffle()
                        true
                    }
                    isModifier && keyEvent.key == Key.R -> {
                        playerViewModel.cycleRepeatMode()
                        true
                    }
                    keyEvent.key == Key.L && !isSearchFocused && !isModifier -> {
                        playerViewModel.toggleFavoriteCurrentSong()
                        true
                    }
                    (isModifier || keyEvent.isAltPressed) && keyEvent.key == Key.Q -> {
                        isQueueSidePanelOpen = !isQueueSidePanelOpen
                        true
                    }
                    keyEvent.key == Key.F11 -> {
                        windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                            WindowPlacement.Maximized
                        } else {
                            WindowPlacement.Fullscreen
                        }
                        true
                    }
                    keyEvent.key == Key.Spacebar && !isSearchFocused -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    else -> false
                }
            } else if (keyEvent.type == KeyEventType.KeyUp) {
                when {
                    isModifier && (keyEvent.key == Key.K || keyEvent.key == Key.DirectionRight || keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown || keyEvent.key == Key.S || keyEvent.key == Key.R || keyEvent.key == Key.Q) -> true
                    keyEvent.isAltPressed && keyEvent.key == Key.Q -> true
                    keyEvent.key == Key.F11 -> true
                    (keyEvent.key == Key.M || keyEvent.key == Key.L || keyEvent.key == Key.Spacebar) && !isSearchFocused && !isModifier -> true
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        window.minimumSize = java.awt.Dimension(920, 620)

        DisposableEffect(window) {
            fun updateMaximizedBounds() {
                val config = window.graphicsConfiguration ?: return
                val insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(config)
                val bounds = config.bounds
                window.maximizedBounds = java.awt.Rectangle(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom
                )
            }

            updateMaximizedBounds()

            val listener = object : java.awt.event.ComponentAdapter() {
                override fun componentMoved(e: java.awt.event.ComponentEvent?) {
                    updateMaximizedBounds()
                }
                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    updateMaximizedBounds()
                }
            }
            window.addComponentListener(listener)

            onDispose {
                window.removeComponentListener(listener)
            }
        }

        PixelPlayerTheme(
            darkTheme = isDarkTheme,
            colorSchemeOverride = systemColorScheme
        ) {
            MainAppScaffold(
                viewModel = playerViewModel,
                isMaximized = windowState.placement == WindowPlacement.Maximized,
                onMinimize = { windowState.isMinimized = true },
                onMaximizeRestore = {
                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
                },
                onClose = ::exitApplication,
                topBarWrapper = { content ->
                    WindowDraggableArea {
                        content()
                    }
                },
                searchFocusRequester = searchFocusRequester,
                onSearchFocusChange = { isSearchFocused = it },
                isQueueSidePanelOpen = isQueueSidePanelOpen,
                onToggleQueueSidePanel = { isQueueSidePanelOpen = !isQueueSidePanelOpen }
            )
        }
    }
}
