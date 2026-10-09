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
    val lastPlayedSongId: String? = null,
    val lastPlaybackPositionMs: Long = 0L,
    val crossfadeDurationMs: Int = 0,
    val isGaplessPlaybackEnabled: Boolean = true,
    val isReplayGainEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val songSortOption: String = "song_title_az",
    val albumSortOption: String = "album_title_az",
    val artistSortOption: String = "artist_name_az",
    val playlistSortOption: String = "playlist_name_az",
    val fetchDeezerArtistImages: Boolean = true
)
