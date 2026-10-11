package com.kayoi.pixelplayerdesktop.data.preferences

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val musicDirectories: List<String> = emptyList(),
    val isDynamicColorEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val albumArtPaletteStyle: String = "tonal_spot",
    val albumArtColorAccuracy: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0: OFF, 1: ALL, 2: ONE
    val fetchDeezerArtistImages: Boolean = true,
    val excludedDirectories: List<String> = emptyList(),
    val artistDelimiters: List<String> = listOf("/", ";", ",", "+", "&"),
    val wordDelimiters: List<String> = listOf("featuring", "feat.", "feat", "ft.", "ft", "vs.", "vs", "versus", "with", "prod.", "prod"),
    val extractArtistFromTitle: Boolean = true,
    val artistSettingsRescanRequired: Boolean = false,
    val isAlbumsListView: Boolean = false,
    val isArtistsListView: Boolean = false,
    val isFoldersPlaylistView: Boolean = false,
    val minSongDurationMs: Int = 10000,
    val minTracksPerAlbum: Int = 1,
    val albumArtCacheLimitMb: Int = 200,
    val autoScanLrcFiles: Boolean = false,
    val lyricsSourcePreference: String = "embedded_first",
    val showPlayerFileInfo: Boolean = true,
    val navBarCompactMode: Boolean = false,
    val defaultTab: String = "HOME"
)
