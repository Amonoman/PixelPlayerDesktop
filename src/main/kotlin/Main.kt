import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kayoi.pixelplayerdesktop.di.appModules
import com.kayoi.pixelplayerdesktop.presentation.components.InputFocusTracker
import com.kayoi.pixelplayerdesktop.presentation.components.LocalInputFocusTracker
import com.kayoi.pixelplayerdesktop.presentation.components.LocalWindowMinimized
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavDestination
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopNavigator
import com.kayoi.pixelplayerdesktop.presentation.navigation.LocalBackDispatcher
import com.kayoi.pixelplayerdesktop.presentation.navigation.toDefaultTarget
import com.kayoi.pixelplayerdesktop.presentation.screens.MainAppScaffold
import com.kayoi.pixelplayerdesktop.presentation.screens.setup.SetupScreen
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.ui.theme.PixelPlayerTheme
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.SystemAccentColorProvider
import io.github.vinceglb.filekit.FileKit
import java.awt.AWTEvent
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.system.exitProcess
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.lifecycle.AppShutdownCoordinator

fun main(args: Array<String> = emptyArray()) {
    val appStartTime = System.currentTimeMillis()
    val isBenchmark = args.contains("--benchmark-first-frame")
    AppShutdownCoordinator.registerShutdownHook()
    FileKit.init("com.kayoi.pixelplayerdesktop")
    application {
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
    val audioEngine = remember { koin.get<AudioEngine>() }
    val initialSetupDone by playerViewModel.initialSetupDone.collectAsState()
    val isDarkTheme by playerViewModel.isDarkMode.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val playerInfo by playerViewModel.playerInfo.collectAsState()
    val closeToTray by playerViewModel.closeToTray.collectAsState()
    var isWindowVisible by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }

    val exitApp: () -> Unit = remember(audioEngine) {
        {
            if (!isExiting) {
                isExiting = true
                isWindowVisible = false
                AppShutdownCoordinator.shutdownAndExit(audioEngine = audioEngine) {
                    SwingUtilities.invokeLater {
                        exitApplication()
                    }
                }
            }
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    var isInputActive by remember { mutableStateOf(false) }
    val inputFocusTracker = remember { InputFocusTracker { isInputActive = it } }
    var isQueueSidePanelOpen by remember { mutableStateOf(true) }

    val backDispatcher = remember { DesktopBackDispatcher() }
    val defaultTabKey by playerViewModel.defaultTab.collectAsState()
    val initialNavTarget = remember(defaultTabKey) {
        DesktopNavDestination.entries.firstOrNull { it.name == defaultTabKey }?.toDefaultTarget() ?: DesktopNavDestination.HOME.toDefaultTarget()
    }
    val navigator = remember { DesktopNavigator(initialNavTarget) }

    var lastNavTimestamp by remember { mutableStateOf(0L) }
    val triggerBack: () -> Unit = remember(backDispatcher) {
        {
            val now = System.currentTimeMillis()
            if (now - lastNavTimestamp > 350L) {
                lastNavTimestamp = now
                backDispatcher.dispatchBack()
            }
        }
    }
    val triggerForward: () -> Unit = remember(backDispatcher) {
        {
            val now = System.currentTimeMillis()
            if (now - lastNavTimestamp > 350L) {
                lastNavTimestamp = now
                backDispatcher.dispatchForward()
            }
        }
    }

    val isWindows = remember { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }

    DisposableEffect(triggerBack, triggerForward, isWindows) {
        val mouseListener = AWTEventListener { event ->
            if (event is MouseEvent && event.id == MouseEvent.MOUSE_RELEASED) {
                // OpenJDK on X11 maps side buttons 8/9 to AWT 6/7; buttons 4/5 are horizontal scroll.
                val isBack = if (isWindows) event.button == 4 else event.button == 6
                val isForward = if (isWindows) event.button == 5 else event.button == 7
                if (isBack) {
                    triggerBack()
                } else if (isForward) {
                    triggerForward()
                }
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(
            mouseListener,
            AWTEvent.MOUSE_EVENT_MASK
        )
        onDispose {
            Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener)
        }
    }

    // System-Based Dynamic Theme (Windows / Linux Accent Color)
    val persistedAccent = playerViewModel.themeStateHolder.lastKnownAccentColor.value
    val initialAccent = remember {
        persistedAccent?.let { Color(it) } ?: SystemAccentColorProvider.defaultAccentColor
    }
    val systemColorSchemePair by produceState(
        initialValue = remember(initialAccent) { DynamicColorProcessor.generateColorSchemePair(initialAccent) }
    ) {
        val systemSeed = withContext(Dispatchers.IO) {
            SystemAccentColorProvider.getSystemAccentColor()
        }
        if (persistedAccent == null || Color(persistedAccent) != systemSeed) {
            playerViewModel.themeStateHolder.updateLastKnownAccentColor(systemSeed.toArgb())
        }
        if (systemSeed != initialAccent) {
            value = DynamicColorProcessor.generateColorSchemePair(systemSeed)
        }
    }
    val systemColorScheme = if (isDarkTheme) systemColorSchemePair.dark else systemColorSchemePair.light

    // Scan user's library on launch if setup already completed
    LaunchedEffect(initialSetupDone) {
        if (initialSetupDone) {
            playerViewModel.scanLibrary()
        }
    }

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null && queue.isNotEmpty()) {
            isQueueSidePanelOpen = true
        }
    }

    if (isBenchmark) {
        LaunchedEffect(Unit) {
            val ttf = System.currentTimeMillis() - appStartTime
            println("TIME_TO_FIRST_FRAME: ${ttf}ms")
            exitApp()
        }
    }

    if (closeToTray) {
        Tray(
            icon = painterResource("icons/icon.png"),
            tooltip = "PixelPlayer",
            onAction = {
                isWindowVisible = true
                windowState.isMinimized = false
            },
            menu = {
                Item(
                    text = "Open PixelPlayer",
                    onClick = {
                        isWindowVisible = true
                        windowState.isMinimized = false
                    }
                )
                Item(
                    text = if (playerInfo.isPlaying) "Pause" else "Play",
                    onClick = { playerViewModel.togglePlayPause() }
                )
                Item(
                    text = "Next",
                    onClick = { playerViewModel.playNext() }
                )
                Item(
                    text = "Previous",
                    onClick = { playerViewModel.playPrevious() }
                )
                Separator()
                Item(
                    text = "Quit",
                    onClick = exitApp
                )
            }
        )
    }

    Window(
        visible = isWindowVisible,
        onCloseRequest = {
            if (closeToTray) {
                isWindowVisible = false
            } else {
                exitApp()
            }
        },
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
                        if (!isInputActive) {
                            playerViewModel.playNext()
                            true
                        } else {
                            false
                        }
                    }
                    isModifier && keyEvent.key == Key.DirectionLeft -> {
                        if (!isInputActive) {
                            playerViewModel.playPrevious()
                            true
                        } else {
                            false
                        }
                    }
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && keyEvent.key == Key.DirectionRight -> {
                        playerViewModel.seekForward(5000L)
                        true
                    }
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && keyEvent.key == Key.DirectionLeft -> {
                        playerViewModel.seekBackward(5000L)
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
                    keyEvent.key == Key.M && !isInputActive && !isModifier -> {
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
                    keyEvent.key == Key.L && !isInputActive && !isModifier -> {
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
                    keyEvent.key == Key.Spacebar && !isInputActive -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    keyEvent.isAltPressed && keyEvent.key == Key.DirectionLeft -> {
                        triggerBack()
                        true
                    }
                    keyEvent.isAltPressed && keyEvent.key == Key.DirectionRight -> {
                        triggerForward()
                        true
                    }
                    keyEvent.key == Key.Back -> {
                        triggerBack()
                        true
                    }
                    keyEvent.key == Key.Escape -> {
                        triggerBack()
                        true
                    }
                    else -> false
                }
            } else if (keyEvent.type == KeyEventType.KeyUp) {
                when {
                    keyEvent.isAltPressed && (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionRight) -> true
                    keyEvent.key == Key.Back || keyEvent.key == Key.Escape -> true
                    isModifier && (keyEvent.key == Key.K || (!isInputActive && (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.DirectionLeft)) || keyEvent.key == Key.DirectionUp || keyEvent.key == Key.DirectionDown || keyEvent.key == Key.S || keyEvent.key == Key.R || keyEvent.key == Key.Q) -> true
                    !isModifier && !keyEvent.isAltPressed && !isInputActive && (keyEvent.key == Key.DirectionRight || keyEvent.key == Key.DirectionLeft) -> true
                    keyEvent.isAltPressed && keyEvent.key == Key.Q -> true
                    keyEvent.key == Key.F11 -> true
                    (keyEvent.key == Key.M || keyEvent.key == Key.L || keyEvent.key == Key.Spacebar) && !isInputActive && !isModifier -> true
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        window.minimumSize = Dimension(760, 560)

        DisposableEffect(window) {
            fun updateMaximizedBounds() {
                val config = window.graphicsConfiguration ?: return
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
                val bounds = config.bounds
                window.maximizedBounds = Rectangle(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    bounds.width - insets.left - insets.right,
                    bounds.height - insets.top - insets.bottom
                )
            }

            updateMaximizedBounds()

            val listener = object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent?) {
                    updateMaximizedBounds()
                }
                override fun componentResized(e: ComponentEvent?) {
                    updateMaximizedBounds()
                }
            }
            window.addComponentListener(listener)

            onDispose {
                window.removeComponentListener(listener)
            }
        }

        CompositionLocalProvider(
            LocalWindowMinimized provides windowState.isMinimized,
            LocalInputFocusTracker provides inputFocusTracker,
            LocalBackDispatcher provides backDispatcher
        ) {
            PixelPlayerTheme(
                darkTheme = isDarkTheme,
                colorSchemeOverride = systemColorScheme
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!initialSetupDone) {
                        SetupScreen(
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
                            onClose = exitApp,
                            onSetupComplete = {
                                playerViewModel.setInitialSetupDone(true)
                                playerViewModel.scanLibrary(forceRescan = true)
                            },
                            topBarWrapper = { content ->
                                WindowDraggableArea {
                                    content()
                                }
                            }
                        )
                    } else {
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
                            onClose = {
                                if (closeToTray) {
                                    isWindowVisible = false
                                } else {
                                    exitApp()
                                }
                            },
                            topBarWrapper = { content ->
                                WindowDraggableArea {
                                    content()
                                }
                            },
                            searchFocusRequester = searchFocusRequester,
                            onSearchFocusChange = { inputFocusTracker.onFocusChanged(it) },
                            isQueueSidePanelOpen = isQueueSidePanelOpen,
                            onToggleQueueSidePanel = { isQueueSidePanelOpen = !isQueueSidePanelOpen },
                            navigator = navigator,
                            backDispatcher = backDispatcher
                        )
                    }
                }
            }
        }
    }
}
    AppShutdownCoordinator.performTeardown(timeoutMs = 1000L)
    exitProcess(0)
}
