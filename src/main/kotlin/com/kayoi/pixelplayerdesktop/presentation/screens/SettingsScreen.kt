package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.ArtistSettingsDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.DuplicatesDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.MusicFoldersDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.PaletteStyleDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsCategoryDetail
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsOverviewMenu
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSearchCatalog
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSearchResults
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSection
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

enum class LibrarySubDetail {
    NONE,
    FOLDERS,
    ARTISTS,
    DUPLICATES,
    PALETTE_STYLE
}

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    isDynamicColor: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onRescanLibrary: () -> Unit,
    activeSection: SettingsSection? = null,
    activeSubDetail: LibrarySubDetail = LibrarySubDetail.NONE,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    onNavigate: (SettingsSection?, LibrarySubDetail) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (searchQuery.isNotBlank()) {
        val searchResults = remember(searchQuery) {
            SettingsSearchCatalog.search(searchQuery)
        }
        SettingsSearchResults(
            query = searchQuery,
            results = searchResults,
            isDarkTheme = isDarkTheme,
            onSelect = { item ->
                onClearSearch()
                onNavigate(item.section, item.subDetail)
            },
            modifier = modifier
        )
        return
    }

    AnimatedContent(
        targetState = Pair(activeSection, activeSubDetail),
        transitionSpec = {
            val initialDepth = when {
                initialState.first == null -> 0
                initialState.second != LibrarySubDetail.NONE -> 2
                else -> 1
            }
            val targetDepth = when {
                targetState.first == null -> 0
                targetState.second != LibrarySubDetail.NONE -> 2
                else -> 1
            }
            val isForward = targetDepth > initialDepth

            if (isForward) {
                slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(260)) + fadeIn(tween(260)) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(200)) + fadeOut(tween(200))
            } else {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(260)) + fadeIn(tween(260)) togetherWith
                        slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(200)) + fadeOut(tween(200))
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "SettingsNavigation"
    ) { (section, subDetail) ->
        when {
            section == null -> {
                SettingsOverviewMenu(
                    onSelectSection = {
                        onNavigate(it, LibrarySubDetail.NONE)
                    }
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.FOLDERS -> {
                MusicFoldersDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.ARTISTS -> {
                ArtistSettingsDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack,
                    onRescanLibrary = onRescanLibrary
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.DUPLICATES -> {
                DuplicatesDetailScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            section == SettingsSection.APPEARANCE && subDetail == LibrarySubDetail.PALETTE_STYLE -> {
                PaletteStyleDetailScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onBack = onBack
                )
            }
            else -> {
                SettingsCategoryDetail(
                    section = section,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    isDynamicColor = isDynamicColor,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onToggleDynamicColor = onToggleDynamicColor,
                    onRescanLibrary = onRescanLibrary,
                    onOpenFoldersDetail = { onNavigate(section, LibrarySubDetail.FOLDERS) },
                    onOpenArtistsDetail = { onNavigate(section, LibrarySubDetail.ARTISTS) },
                    onOpenDuplicatesDetail = { onNavigate(section, LibrarySubDetail.DUPLICATES) },
                    onOpenPaletteStyleDetail = { onNavigate(section, LibrarySubDetail.PALETTE_STYLE) },
                    onBack = onBack
                )
            }
        }
    }
}
