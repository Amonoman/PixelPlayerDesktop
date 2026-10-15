package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settingsFlow: StateFlow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings)
    suspend fun addMusicDirectory(path: String)
    suspend fun removeMusicDirectory(path: String)
    suspend fun addExcludedDirectory(path: String)
    suspend fun removeExcludedDirectory(path: String)
    suspend fun setDynamicColoringEnabled(enabled: Boolean)
    suspend fun setDarkMode(isDark: Boolean)
    suspend fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle)
    suspend fun setAlbumArtColorAccuracy(accuracy: Int)
    suspend fun setVolume(volume: Float)
    suspend fun setMuted(muted: Boolean)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun setRepeatMode(repeatMode: Int)
    suspend fun setFetchDeezerArtistImages(enabled: Boolean)
    suspend fun setArtistDelimiters(delimiters: List<String>)
    suspend fun addArtistDelimiter(delimiter: String)
    suspend fun removeArtistDelimiter(delimiter: String)
    suspend fun resetArtistDelimiters()
    suspend fun setWordDelimiters(delimiters: List<String>)
    suspend fun addWordDelimiter(delimiter: String)
    suspend fun removeWordDelimiter(delimiter: String)
    suspend fun resetWordDelimiters()
    suspend fun setExtractArtistFromTitle(enabled: Boolean)
    suspend fun clearArtistSettingsRescanRequired()
    suspend fun setAlbumsListView(isListView: Boolean)
    suspend fun setArtistsListView(isListView: Boolean)
    suspend fun setFoldersPlaylistView(isPlaylistView: Boolean)
    suspend fun setMinSongDuration(durationMs: Int)
    suspend fun setMinTracksPerAlbum(minTracks: Int)
    suspend fun setAlbumArtCacheLimitMb(limitMb: Int)
    suspend fun setAutoScanLrcFiles(enabled: Boolean)
    suspend fun setLyricsSourcePreference(preference: LyricsSourcePreference)
    suspend fun setShowPlayerFileInfo(show: Boolean)
    suspend fun setNavBarCompactMode(enabled: Boolean)
    suspend fun setDefaultTab(tab: String)
    suspend fun setLyricsAlignment(alignment: String)
    suspend fun setLyricsSyncOffset(songId: String, offsetMs: Int)
}
