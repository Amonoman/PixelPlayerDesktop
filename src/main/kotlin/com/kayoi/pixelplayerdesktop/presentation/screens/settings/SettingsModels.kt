package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
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
            SettingsSection.ABOUT -> Color(0xFF3F474D) to Color(0xFFDEE3EB)
        }
    } else {
        when (category) {
            SettingsSection.LIBRARY -> Color(0xFFD7E3FF) to Color(0xFF005AC1)
            SettingsSection.APPEARANCE -> Color(0xFFFFD8E4) to Color(0xFF631835)
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
