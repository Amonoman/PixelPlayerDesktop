package com.kayoi.pixelplayerdesktop.presentation.navigation

import com.kayoi.pixelplayerdesktop.generated.resources.ic_artist
import org.jetbrains.compose.resources.DrawableResource
import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_album
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_favorite
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_folder
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_home
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_settings
enum class NavSection(val label: String) {
    MENU("Menu"),
    LIBRARY("Library"),
    SYSTEM("System")
}

enum class DesktopNavDestination(
    val title: String,
    val icon: DrawableResource,
    val section: NavSection
) {
    HOME("Home", Res.drawable.ic_rounded_home, NavSection.MENU),

    SONGS("Songs", Res.drawable.ic_rounded_music_note, NavSection.LIBRARY),
    ALBUMS("Albums", Res.drawable.ic_rounded_album, NavSection.LIBRARY),
    ARTISTS("Artists", Res.drawable.ic_artist, NavSection.LIBRARY),
    PLAYLISTS("Playlists", Res.drawable.ic_automirrored_rounded_playlist_play, NavSection.LIBRARY),
    FOLDERS("Folders", Res.drawable.ic_rounded_folder, NavSection.LIBRARY),
    LIKED("Liked", Res.drawable.ic_rounded_favorite, NavSection.LIBRARY),

    SETTINGS("Settings", Res.drawable.ic_rounded_settings, NavSection.SYSTEM)
}
