package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.preferences.UserSettings
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GlobalSettingsModuleHandler(
    private val settingsRepository: SettingsRepository,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.GLOBAL_SETTINGS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val current = settingsRepository.getSettings()
        json.encodeToString(UserSettings.serializer(), current)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        1
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            return@withContext
        }
        val imported = json.decodeFromString(UserSettings.serializer(), payload)
        settingsRepository.updateSettings { current ->
            // Keep local machine music directories if imported is empty
            val mergedDirs = if (imported.musicDirectories.isNotEmpty()) {
                imported.musicDirectories
            } else {
                current.musicDirectories
            }

            imported.copy(
                musicDirectories = mergedDirs,
                // Retain current custom EQ presets if incoming had none
                customEqualizerPresets = if (imported.customEqualizerPresets.isNotEmpty()) {
                    imported.customEqualizerPresets
                } else {
                    current.customEqualizerPresets
                }
            )
        }
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val previous = json.decodeFromString(UserSettings.serializer(), snapshot)
        settingsRepository.updateSettings { previous }
    }
}
