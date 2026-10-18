package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class SettingsSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    LIBRARY(
        id = "library",
        title = "Music Management",
        subtitle = "Manage folders, refresh library, parsing options",
        icon = Icons.Rounded.LibraryMusic
    ),
    APPEARANCE(
        id = "appearance",
        title = "Appearance",
        subtitle = "Themes, layout, and visual styles",
        icon = Icons.Rounded.Palette
    ),
    PLAYBACK(
        id = "playback",
        title = "Playback",
        subtitle = "Audio behavior, crossfade, and background play",
        icon = Icons.Rounded.MusicNote
    ),
    EQUALIZER(
        id = "equalizer",
        title = "Equalizer",
        subtitle = "10-band frequencies, preamp, and audio presets",
        icon = Icons.Rounded.GraphicEq
    ),
    ACCOUNTS(
        id = "accounts",
        title = "Accounts",
        subtitle = "Manage Navidrome and Jellyfin services",
        icon = Icons.Rounded.AccountCircle
    ),
    SHORTCUTS(
        id = "shortcuts",
        title = "Keyboard Shortcuts",
        subtitle = "Hotkeys, playback controls, and navigation cheatsheet",
        icon = Icons.Rounded.Keyboard
    ),
    ABOUT(
        id = "about",
        title = "About",
        subtitle = "App info, version, and credits",
        icon = Icons.Rounded.Info
    )
}

fun getCategoryColors(category: SettingsSection, isDark: Boolean): Pair<Color, Color> {
    return if (isDark) {
        when (category) {
            SettingsSection.LIBRARY -> Color(0xFF004A77) to Color(0xFFC2E7FF)
            SettingsSection.APPEARANCE -> Color(0xFF7D5260) to Color(0xFFFFD8E4)
            SettingsSection.PLAYBACK -> Color(0xFF633B48) to Color(0xFFFFD8EC)
            SettingsSection.EQUALIZER -> Color(0xFF6E4E13) to Color(0xFFFFDEAC)
            SettingsSection.ACCOUNTS -> Color(0xFF37474F) to Color(0xFFBBD9E8)
            SettingsSection.SHORTCUTS -> Color(0xFF2E4369) to Color(0xFFBFD2F8)
            SettingsSection.ABOUT -> Color(0xFF3F474D) to Color(0xFFDEE3EB)
        }
    } else {
        when (category) {
            SettingsSection.LIBRARY -> Color(0xFFD7E3FF) to Color(0xFF005AC1)
            SettingsSection.APPEARANCE -> Color(0xFFFFD8E4) to Color(0xFF631835)
            SettingsSection.PLAYBACK -> Color(0xFFFFD8EC) to Color(0xFF631B4B)
            SettingsSection.EQUALIZER -> Color(0xFFFFDEAC) to Color(0xFF281900)
            SettingsSection.ACCOUNTS -> Color(0xFFD6EAF5) to Color(0xFF103548)
            SettingsSection.SHORTCUTS -> Color(0xFFD6E3FF) to Color(0xFF285EA7)
            SettingsSection.ABOUT -> Color(0xFFEFF1F7) to Color(0xFF44474F)
        }
    }
}

fun getGroupShape(index: Int, totalItems: Int): Shape = when {
    totalItems <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}
