package com.kayoi.pixelplayerdesktop.data.preferences

import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
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
    val defaultTab: String = "HOME",
    val lyricsAlignment: String = "left",
    val lyricsSyncOffsets: Map<String, Int> = emptyMap(),
    val showLyricsRomanization: Boolean = true,
    val replayGainEnabled: Boolean = false,
    val replayGainMode: String = "track",
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 3,
    val showQueueHistory: Boolean = true,
    val closeToTray: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: String = "flat",
    val equalizerBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val equalizerPreamp: Float = 0f,
    val customEqualizerPresets: List<EqualizerPreset> = emptyList(),
    val pinnedEqualizerPresets: List<String> = EqualizerPreset.DEFAULT_PINNED,
    val equalizerViewMode: String = "SLIDERS"
)
