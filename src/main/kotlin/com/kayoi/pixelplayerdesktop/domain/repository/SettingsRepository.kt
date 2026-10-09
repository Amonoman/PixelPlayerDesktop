package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settingsFlow: StateFlow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings)
    suspend fun addMusicDirectory(path: String)
    suspend fun removeMusicDirectory(path: String)
    suspend fun setDynamicColoringEnabled(enabled: Boolean)
    suspend fun setDarkMode(isDark: Boolean)
    suspend fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle)
    suspend fun setAlbumArtColorAccuracy(accuracy: Int)
    suspend fun setVolume(volume: Float)
    suspend fun setMuted(muted: Boolean)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun setRepeatMode(repeatMode: Int)
    suspend fun setFetchDeezerArtistImages(enabled: Boolean)
}
