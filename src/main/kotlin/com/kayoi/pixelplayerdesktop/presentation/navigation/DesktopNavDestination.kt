package com.kayoi.pixelplayerdesktop.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons

enum class NavSection(val label: String) {
    MENU("Menu"),
    LIBRARY("Library"),
    SYSTEM("System")
}

enum class DesktopNavDestination(
    val title: String,
    val icon: ImageVector,
    val section: NavSection
) {
    HOME("Home", Icons.Rounded.Home, NavSection.MENU),

    SONGS("Songs", Icons.Rounded.MusicNote, NavSection.LIBRARY),
    ALBUMS("Albums", Icons.Rounded.Album, NavSection.LIBRARY),
    ARTISTS("Artists", PixelPlayerIcons.Artist, NavSection.LIBRARY),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, NavSection.LIBRARY),
    FOLDERS("Folders", Icons.Rounded.Folder, NavSection.LIBRARY),
    LIKED("Liked", Icons.Rounded.Favorite, NavSection.LIBRARY),

    SETTINGS("Settings", Icons.Rounded.Settings, NavSection.SYSTEM)
}
