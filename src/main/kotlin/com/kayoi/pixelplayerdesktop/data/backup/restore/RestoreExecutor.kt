package com.kayoi.pixelplayerdesktop.data.backup.restore

import com.kayoi.pixelplayerdesktop.data.backup.format.BackupReader
import com.kayoi.pixelplayerdesktop.data.backup.model.*
import com.kayoi.pixelplayerdesktop.data.backup.module.BackupModuleHandler
import com.kayoi.pixelplayerdesktop.data.backup.validation.ValidationPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RestoreExecutor(
    private val backupReader: BackupReader,
    private val validationPipeline: ValidationPipeline,
    private val handlers: Map<BackupSection, BackupModuleHandler>
) {
    suspend fun execute(
        file: File,
        plan: RestorePlan,
        passphrase: String? = null,
        onProgress: (BackupTransferProgressUpdate) -> Unit = {}
    ): RestoreResult = withContext(Dispatchers.IO) {
        val selectedModules = plan.selectedModules.toList().sortedBy { it.key }
        val totalSteps = selectedModules.size * 2 + 3
        var step = 0

        reportProgress(onProgress, ++step, totalSteps, "Creating safety snapshots", "Capturing current state for rollback.")

        val snapshots = mutableMapOf<BackupSection, String>()
        try {
            selectedModules.forEach { section ->
                val handler = handlers[section]
                if (handler != null) {
                    snapshots[section] = handler.snapshot()
                }
            }
        } catch (e: Exception) {
            return@withContext RestoreResult.TotalFailure("Failed to capture safety snapshots: ${e.message}")
        }

        reportProgress(onProgress, ++step, totalSteps, "Preparing restore", "Selected modules will be processed one by one.")

        val restoredModules = mutableListOf<BackupSection>()
        var currentSection: BackupSection? = null

        try {
            selectedModules.forEach { section ->
                currentSection = section
                val handler = handlers[section] ?: return@forEach

                val payload = backupReader.readModulePayload(file, section.key, passphrase).getOrThrow()

                val validationResult = validationPipeline.validateModulePayload(section, payload, plan.manifest)
                if (validationResult is BackupValidationResult.Invalid && validationResult.fatalErrors.isNotEmpty()) {
                    throw IllegalStateException("Validation failed for ${section.label}: ${validationResult.fatalErrors.first().message}")
                }

                reportProgress(onProgress, ++step, totalSteps, "Restoring ${section.label}", section.description, section)

                handler.restore(payload)
                restoredModules.add(section)

                reportProgress(onProgress, ++step, totalSteps, "Restored ${section.label}", section.description, section)
            }
        } catch (e: Exception) {
            var rollbackSuccess = true
            val rollbackOrder = (restoredModules + listOfNotNull(currentSection)).distinct().asReversed()
            rollbackOrder.forEach { section ->
                try {
                    val snapshot = snapshots[section]
                    if (snapshot != null) {
                        handlers[section]?.rollback(snapshot)
                    }
                } catch (_: Exception) {
                    rollbackSuccess = false
                }
            }

            val failedReason = e.message ?: "Unknown restore error"
            val failedSection = currentSection
            val failedLabel = failedSection?.label ?: "Unknown"

            if (rollbackSuccess) {
                return@withContext RestoreResult.TotalFailure(
                    "Restore failed at $failedLabel: $failedReason. All applied changes were safely rolled back."
                )
            }

            return@withContext RestoreResult.PartialFailure(
                succeeded = restoredModules.toSet(),
                failed = failedSection?.let { mapOf(it to failedReason) }.orEmpty(),
                rolledBack = false
            )
        }

        reportProgress(onProgress, ++step, totalSteps, "Restore complete", "All selected modules were restored successfully.")
        RestoreResult.Success
    }

    private fun reportProgress(
        onProgress: (BackupTransferProgressUpdate) -> Unit,
        step: Int,
        totalSteps: Int,
        title: String,
        detail: String,
        section: BackupSection? = null
    ) {
        onProgress(
            BackupTransferProgressUpdate(
                operation = BackupOperationType.IMPORT,
                step = step,
                totalSteps = totalSteps,
                title = title,
                detail = detail,
                section = section
            )
        )
    }
}
