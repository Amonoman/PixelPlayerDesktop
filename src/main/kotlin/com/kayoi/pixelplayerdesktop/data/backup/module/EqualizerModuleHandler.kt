package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.model.EqualizerPreset
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EqualizerBackupPayload(
    val customPresets: List<EqualizerPreset> = emptyList(),
    val pinnedPresets: List<String> = emptyList(),
    val enabled: Boolean = false,
    val currentPreset: String = "flat",
    val bands: List<Float> = emptyList(),
    val preamp: Float = 0f,
    val viewMode: String = "SLIDERS"
)

class EqualizerModuleHandler(
    private val settingsRepository: SettingsRepository,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.EQUALIZER

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        val payload = EqualizerBackupPayload(
            customPresets = settings.customEqualizerPresets,
            pinnedPresets = settings.pinnedEqualizerPresets,
            enabled = settings.equalizerEnabled,
            currentPreset = settings.equalizerPreset,
            bands = settings.equalizerBands,
            preamp = settings.equalizerPreamp,
            viewMode = settings.equalizerViewMode
        )
        json.encodeToString(EqualizerBackupPayload.serializer(), payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        settings.customEqualizerPresets.size + settings.pinnedEqualizerPresets.size
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            return@withContext
        }
        val imported = json.decodeFromString(EqualizerBackupPayload.serializer(), payload)
        settingsRepository.updateSettings { current ->
            current.copy(
                customEqualizerPresets = imported.customPresets,
                pinnedEqualizerPresets = if (imported.pinnedPresets.isNotEmpty()) imported.pinnedPresets else current.pinnedEqualizerPresets,
                equalizerEnabled = imported.enabled,
                equalizerPreset = imported.currentPreset,
                equalizerBands = if (imported.bands.isNotEmpty()) imported.bands else current.equalizerBands,
                equalizerPreamp = imported.preamp,
                equalizerViewMode = imported.viewMode
            )
        }
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)
}
