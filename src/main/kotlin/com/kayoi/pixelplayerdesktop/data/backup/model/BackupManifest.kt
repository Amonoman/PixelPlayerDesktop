package com.kayoi.pixelplayerdesktop.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String = "",
    val appVersionCode: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val modules: Map<String, BackupModuleInfo> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 4
        const val MIN_SUPPORTED_VERSION = 1
        const val MANIFEST_FILENAME = "manifest.json"
    }
}

@Serializable
data class DeviceInfo(
    val manufacturer: String = "",
    val model: String = "",
    val osName: String = System.getProperty("os.name") ?: "",
    val osVersion: String = System.getProperty("os.version") ?: "",
    val androidVersion: Int = 0
)

@Serializable
data class BackupModuleInfo(
    val checksum: String = "",
    val entryCount: Int = 0,
    val sizeBytes: Long = 0
)
