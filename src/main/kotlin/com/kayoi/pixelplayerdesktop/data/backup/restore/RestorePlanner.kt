package com.kayoi.pixelplayerdesktop.data.backup.restore

import com.kayoi.pixelplayerdesktop.data.backup.format.BackupReader
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.ModuleRestoreDetail
import com.kayoi.pixelplayerdesktop.data.backup.model.RestorePlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RestorePlanner(
    private val backupReader: BackupReader
) {
    suspend fun buildRestorePlan(file: File, passphrase: String? = null): Result<RestorePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val manifest = backupReader.readManifest(file, passphrase).getOrThrow()

            val availableModules = manifest.modules.keys.mapNotNull { key ->
                BackupSection.fromKey(key)
            }.toSet()

            val moduleDetails = mutableMapOf<BackupSection, ModuleRestoreDetail>()
            availableModules.forEach { section ->
                val info = manifest.modules[section.key]
                moduleDetails[section] = ModuleRestoreDetail(
                    entryCount = info?.entryCount ?: 0,
                    sizeBytes = info?.sizeBytes ?: 0,
                    willOverwrite = true
                )
            }

            val warnings = mutableListOf<String>()
            if (manifest.deviceInfo.androidVersion > 0) {
                warnings.add("This backup originated from mobile. Platform-specific settings and unmapped song IDs will be safely skipped.")
            } else if (manifest.schemaVersion < 3) {
                warnings.add("This is an older backup (v${manifest.schemaVersion}). Some modern modules may not be available.")
            }

            RestorePlan(
                manifest = manifest,
                backupFilePath = file.absolutePath,
                availableModules = availableModules,
                selectedModules = availableModules,
                moduleDetails = moduleDetails,
                warnings = warnings
            )
        }
    }
}
