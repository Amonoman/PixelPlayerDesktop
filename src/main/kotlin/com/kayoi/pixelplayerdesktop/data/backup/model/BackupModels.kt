package com.kayoi.pixelplayerdesktop.data.backup.model

import kotlinx.serialization.Serializable

enum class BackupOperationType {
    EXPORT,
    IMPORT
}

data class BackupTransferProgressUpdate(
    val operation: BackupOperationType,
    val step: Int,
    val totalSteps: Int,
    val title: String,
    val detail: String,
    val section: BackupSection? = null
) {
    val progress: Float
        get() = if (totalSteps > 0) (step.toFloat() / totalSteps).coerceIn(0f, 1f) else 0f
}

@Serializable
data class BackupSongMetadata(
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L
)


data class RestorePlan(
    val manifest: BackupManifest,
    val backupFilePath: String,
    val availableModules: Set<BackupSection>,
    val selectedModules: Set<BackupSection>,
    val moduleDetails: Map<BackupSection, ModuleRestoreDetail>,
    val warnings: List<String> = emptyList()
)

data class ModuleRestoreDetail(
    val entryCount: Int,
    val sizeBytes: Long,
    val willOverwrite: Boolean = true
)

sealed class BackupValidationResult {
    data object Valid : BackupValidationResult()
    data class Invalid(val errors: List<ValidationError>) : BackupValidationResult() {
        val fatalErrors: List<ValidationError>
            get() = errors.filter { it.severity == Severity.ERROR }
        val warnings: List<ValidationError>
            get() = errors.filter { it.severity == Severity.WARNING }
    }

    fun isValid(): Boolean = this is Valid || (this is Invalid && fatalErrors.isEmpty())
}

data class ValidationError(
    val code: String,
    val message: String,
    val module: String? = null,
    val severity: Severity = Severity.ERROR
)

enum class Severity { ERROR, WARNING }

sealed class RestoreResult {
    data object Success : RestoreResult()
    data class PartialFailure(
        val succeeded: Set<BackupSection>,
        val failed: Map<BackupSection, String>,
        val rolledBack: Boolean
    ) : RestoreResult()
    data class TotalFailure(val error: String) : RestoreResult()
}
