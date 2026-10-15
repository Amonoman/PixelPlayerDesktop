package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtColorAccuracy
import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle
import com.kayoi.pixelplayerdesktop.data.preferences.LyricsSourcePreference
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepositoryImpl(
    private val settingsFile: File = DesktopPathProvider.settingsFile,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : SettingsRepository {

    private val mutex = Mutex()
    private val _settingsFlow: MutableStateFlow<UserSettings>

    init {
        val initialSettings = loadSettingsFromDisk()
        _settingsFlow = MutableStateFlow(initialSettings)
    }

    override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): UserSettings = settingsFlow.value

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        mutex.withLock {
            val current = _settingsFlow.value
            val updated = transform(current)
            if (current != updated) {
                _settingsFlow.value = updated
                saveSettingsToDisk(updated)
            }
        }
    }

    override suspend fun addMusicDirectory(path: String) {
        updateSettings { settings ->
            if (settings.musicDirectories.contains(path)) {
                settings
            } else {
                settings.copy(musicDirectories = settings.musicDirectories + path)
            }
        }
    }

    override suspend fun removeMusicDirectory(path: String) {
        updateSettings { settings ->
            settings.copy(musicDirectories = settings.musicDirectories.filter { it != path })
        }
    }

    override suspend fun addExcludedDirectory(path: String) {
        updateSettings { settings ->
            if (settings.excludedDirectories.contains(path)) {
                settings
            } else {
                settings.copy(excludedDirectories = settings.excludedDirectories + path)
            }
        }
    }

    override suspend fun removeExcludedDirectory(path: String) {
        updateSettings { settings ->
            settings.copy(excludedDirectories = settings.excludedDirectories.filter { it != path })
        }
    }

    override suspend fun setDynamicColoringEnabled(enabled: Boolean) {
        updateSettings { it.copy(isDynamicColorEnabled = enabled) }
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        updateSettings { it.copy(isDarkMode = isDark) }
    }

    override suspend fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) {
        updateSettings { it.copy(albumArtPaletteStyle = style.storageKey) }
    }

    override suspend fun setAlbumArtColorAccuracy(accuracy: Int) {
        updateSettings { it.copy(albumArtColorAccuracy = AlbumArtColorAccuracy.clamp(accuracy)) }
    }

    override suspend fun setVolume(volume: Float) {
        updateSettings { it.copy(volume = volume.coerceIn(0.0f, 1.0f)) }
    }

    override suspend fun setMuted(muted: Boolean) {
        updateSettings { it.copy(isMuted = muted) }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        updateSettings { it.copy(isShuffleEnabled = enabled) }
    }

    override suspend fun setRepeatMode(repeatMode: Int) {
        updateSettings { it.copy(repeatMode = repeatMode.coerceIn(0, 2)) }
    }

    override suspend fun setFetchDeezerArtistImages(enabled: Boolean) {
        updateSettings { it.copy(fetchDeezerArtistImages = enabled) }
    }

    override suspend fun setArtistDelimiters(delimiters: List<String>) {
        updateSettings { it.copy(artistDelimiters = delimiters.distinct(), artistSettingsRescanRequired = true) }
    }

    override suspend fun addArtistDelimiter(delimiter: String) {
        val trimmed = delimiter.trim()
        if (trimmed.isEmpty()) return
        updateSettings { settings ->
            if (settings.artistDelimiters.contains(trimmed)) {
                settings
            } else {
                settings.copy(artistDelimiters = settings.artistDelimiters + trimmed, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun removeArtistDelimiter(delimiter: String) {
        updateSettings { settings ->
            if (settings.artistDelimiters.size <= 1) {
                settings
            } else {
                settings.copy(artistDelimiters = settings.artistDelimiters.filter { it != delimiter }, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun resetArtistDelimiters() {
        updateSettings { it.copy(artistDelimiters = com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS, artistSettingsRescanRequired = true) }
    }

    override suspend fun setWordDelimiters(delimiters: List<String>) {
        updateSettings { it.copy(wordDelimiters = delimiters.distinct(), artistSettingsRescanRequired = true) }
    }

    override suspend fun addWordDelimiter(delimiter: String) {
        val trimmed = delimiter.trim()
        if (trimmed.isEmpty()) return
        updateSettings { settings ->
            if (settings.wordDelimiters.any { it.equals(trimmed, ignoreCase = true) }) {
                settings
            } else {
                settings.copy(wordDelimiters = settings.wordDelimiters + trimmed, artistSettingsRescanRequired = true)
            }
        }
    }

    override suspend fun removeWordDelimiter(delimiter: String) {
        updateSettings { settings ->
            settings.copy(wordDelimiters = settings.wordDelimiters.filter { !it.equals(delimiter, ignoreCase = true) }, artistSettingsRescanRequired = true)
        }
    }

    override suspend fun resetWordDelimiters() {
        updateSettings { it.copy(wordDelimiters = com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS, artistSettingsRescanRequired = true) }
    }

    override suspend fun setExtractArtistFromTitle(enabled: Boolean) {
        updateSettings { it.copy(extractArtistFromTitle = enabled, artistSettingsRescanRequired = true) }
    }

    override suspend fun clearArtistSettingsRescanRequired() {
        updateSettings { it.copy(artistSettingsRescanRequired = false) }
    }

    override suspend fun setAlbumsListView(isListView: Boolean) {
        updateSettings { it.copy(isAlbumsListView = isListView) }
    }

    override suspend fun setArtistsListView(isListView: Boolean) {
        updateSettings { it.copy(isArtistsListView = isListView) }
    }

    override suspend fun setFoldersPlaylistView(isPlaylistView: Boolean) {
        updateSettings { it.copy(isFoldersPlaylistView = isPlaylistView) }
    }

    override suspend fun setMinSongDuration(durationMs: Int) {
        updateSettings { it.copy(minSongDurationMs = durationMs.coerceIn(0, 120000)) }
    }

    override suspend fun setMinTracksPerAlbum(minTracks: Int) {
        updateSettings { it.copy(minTracksPerAlbum = minTracks.coerceIn(1, 5)) }
    }

    override suspend fun setAlbumArtCacheLimitMb(limitMb: Int) {
        updateSettings { it.copy(albumArtCacheLimitMb = limitMb.coerceIn(50, 1500)) }
    }

    override suspend fun setAutoScanLrcFiles(enabled: Boolean) {
        updateSettings { it.copy(autoScanLrcFiles = enabled) }
    }

    override suspend fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        updateSettings { it.copy(lyricsSourcePreference = preference.storageKey) }
    }

    override suspend fun setShowPlayerFileInfo(show: Boolean) {
        updateSettings { it.copy(showPlayerFileInfo = show) }
    }

    override suspend fun setNavBarCompactMode(enabled: Boolean) {
        updateSettings { it.copy(navBarCompactMode = enabled) }
    }

    override suspend fun setDefaultTab(tab: String) {
        updateSettings { it.copy(defaultTab = tab) }
    }

    override suspend fun setLyricsAlignment(alignment: String) {
        updateSettings { it.copy(lyricsAlignment = alignment) }
    }

    override suspend fun setLyricsSyncOffset(songId: String, offsetMs: Int) {
        updateSettings { current ->
            val updatedMap = current.lyricsSyncOffsets.toMutableMap()
            if (offsetMs == 0) {
                updatedMap.remove(songId)
            } else {
                updatedMap[songId] = offsetMs
            }
            current.copy(lyricsSyncOffsets = updatedMap)
        }
    }

    override suspend fun setShowLyricsRomanization(enabled: Boolean) {
        updateSettings { it.copy(showLyricsRomanization = enabled) }
    }

    private fun loadSettingsFromDisk(): UserSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText(Charsets.UTF_8)
                json.decodeFromString(UserSettings.serializer(), content)
            } else {
                val defaultSettings = UserSettings(
                    musicDirectories = DesktopPathProvider.defaultMusicDirectories
                )
                saveSettingsToDiskSync(defaultSettings)
                defaultSettings
            }
        } catch (e: Exception) {
            UserSettings(musicDirectories = DesktopPathProvider.defaultMusicDirectories)
        }
    }

    private suspend fun saveSettingsToDisk(settings: UserSettings) = withContext(ioDispatcher) {
        saveSettingsToDiskSync(settings)
    }

    private fun saveSettingsToDiskSync(settings: UserSettings) {
        try {
            val parent = settingsFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val content = json.encodeToString(UserSettings.serializer(), settings)
            val tempFile = File(settingsFile.parentFile, "${settingsFile.name}.tmp")
            tempFile.writeText(content, Charsets.UTF_8)
            if (tempFile.exists()) {
                if (settingsFile.exists()) {
                    settingsFile.delete()
                }
                tempFile.renameTo(settingsFile)
            }
        } catch (e: Exception) {
            // Log or fallback
        }
    }
}
