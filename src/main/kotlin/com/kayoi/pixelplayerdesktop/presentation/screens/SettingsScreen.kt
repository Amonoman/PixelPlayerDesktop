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
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.MusicFoldersDetailScreen
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsCategoryDetail
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsOverviewMenu
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSection
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

enum class LibrarySubDetail {
    NONE,
    FOLDERS,
    ARTISTS
}

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    isDarkTheme: Boolean,
    isDynamicColor: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onRescanLibrary: () -> Unit,
    initialSection: SettingsSection? = null,
    initialSubDetail: LibrarySubDetail = LibrarySubDetail.NONE,
    modifier: Modifier = Modifier
) {
    var activeSection by remember(initialSection) { mutableStateOf<SettingsSection?>(initialSection) }
    var activeSubDetail by remember(initialSubDetail) { mutableStateOf(initialSubDetail) }

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
                        activeSection = it
                        activeSubDetail = LibrarySubDetail.NONE
                    }
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.FOLDERS -> {
                MusicFoldersDetailScreen(
                    viewModel = viewModel,
                    onBack = { activeSubDetail = LibrarySubDetail.NONE }
                )
            }
            section == SettingsSection.LIBRARY && subDetail == LibrarySubDetail.ARTISTS -> {
                ArtistSettingsDetailScreen(
                    viewModel = viewModel,
                    onBack = { activeSubDetail = LibrarySubDetail.NONE },
                    onRescanLibrary = onRescanLibrary
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
                    onOpenFoldersDetail = { activeSubDetail = LibrarySubDetail.FOLDERS },
                    onOpenArtistsDetail = { activeSubDetail = LibrarySubDetail.ARTISTS },
                    onBack = { activeSection = null }
                )
            }
        }
    }
}
