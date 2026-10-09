package com.kayoi.pixelplayerdesktop.data.preferences

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

    private fun loadSettingsFromDisk(): UserSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText(Charsets.UTF_8)
                json.decodeFromString<UserSettings>(content)
            } else {
                val defaultSettings = UserSettings(
                    musicDirectories = DesktopPathProvider.defaultMusicDirectories
                )
                saveSettingsToDisk(defaultSettings)
                defaultSettings
            }
        } catch (e: Exception) {
            UserSettings(musicDirectories = DesktopPathProvider.defaultMusicDirectories)
        }
    }

    private fun saveSettingsToDisk(settings: UserSettings) {
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
