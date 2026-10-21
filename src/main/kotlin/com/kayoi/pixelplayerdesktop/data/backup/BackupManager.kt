package com.kayoi.pixelplayerdesktop.data.backup

import com.kayoi.pixelplayerdesktop.BuildConfig
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupFormatDetector
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupReader
import com.kayoi.pixelplayerdesktop.data.backup.format.BackupWriter
import com.kayoi.pixelplayerdesktop.data.backup.model.*
import com.kayoi.pixelplayerdesktop.data.backup.module.BackupModuleHandler
import com.kayoi.pixelplayerdesktop.data.backup.restore.RestoreExecutor
import com.kayoi.pixelplayerdesktop.data.backup.restore.RestorePlanner
import com.kayoi.pixelplayerdesktop.data.backup.validation.ValidationPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val backupWriter: BackupWriter,
    private val backupReader: BackupReader,
    private val restorePlanner: RestorePlanner,
    private val restoreExecutor: RestoreExecutor,
    private val validationPipeline: ValidationPipeline,
    private val handlers: Map<BackupSection, BackupModuleHandler>
) {
    private val _progressFlow = MutableStateFlow<BackupTransferProgressUpdate?>(null)
    val progressFlow: StateFlow<BackupTransferProgressUpdate?> = _progressFlow.asStateFlow()

    suspend fun export(
        targetFile: File,
        sections: Set<BackupSection>,
        passphrase: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val selectedSections = sections.toList()
            val totalSteps = selectedSections.size + 3
            var step = 0

            reportProgress(BackupOperationType.EXPORT, ++step, totalSteps,
                "Preparing backup", "Building your selected backup sections.")

            val modulePayloads = mutableMapOf<String, String>()
            selectedSections.forEach { section ->
                val handler = handlers[section] ?: return@forEach
                reportProgress(BackupOperationType.EXPORT, ++step, totalSteps,
                    "Collecting ${section.label}", section.description, section)
                modulePayloads[section.key] = handler.export()
            }

            val manifest = BackupManifest(
                schemaVersion = BackupManifest.CURRENT_SCHEMA_VERSION,
                appVersion = BuildConfig.APP_VERSION_NAME,
                appVersionCode = BuildConfig.APP_VERSION_CODE,
                createdAt = System.currentTimeMillis(),
                deviceInfo = DeviceInfo(
                    manufacturer = System.getProperty("os.name") ?: "Desktop",
                    model = System.getProperty("os.arch") ?: "x86_64"
                )
            )

            reportProgress(BackupOperationType.EXPORT, ++step, totalSteps,
                "Packaging backup", "Creating .pxpl archive.")

            backupWriter.write(
                targetFile = targetFile,
                manifest = manifest,
                modulePayloads = modulePayloads,
                passphrase = passphrase
            ).getOrThrow()

            reportProgress(BackupOperationType.EXPORT, ++step, totalSteps,
                "Backup complete", "Your PixelPlayer backup was created successfully.")
        }
    }

    suspend fun isBackupEncrypted(file: File): Boolean {
        return backupReader.detectFormat(file).getOrNull() == BackupFormatDetector.Format.PXPL_V4_ENCRYPTED
    }

    suspend fun inspectBackup(file: File, passphrase: String? = null): Result<RestorePlan> = withContext(Dispatchers.IO) {
        runCatching {
            val fileValidation = validationPipeline.validateFile(file)
            if (fileValidation is BackupValidationResult.Invalid && fileValidation.fatalErrors.isNotEmpty()) {
                throw IllegalArgumentException(fileValidation.fatalErrors.first().message)
            }

            val plan = restorePlanner.buildRestorePlan(file, passphrase).getOrThrow()
            val manifestValidation = validationPipeline.validateManifest(plan.manifest)
            if (manifestValidation is BackupValidationResult.Invalid && manifestValidation.fatalErrors.isNotEmpty()) {
                throw IllegalArgumentException(manifestValidation.fatalErrors.first().message)
            }

            plan
        }
    }

    suspend fun restore(
        file: File,
        plan: RestorePlan,
        passphrase: String? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        restoreExecutor.execute(
            file = file,
            plan = plan,
            passphrase = passphrase,
            onProgress = { _progressFlow.value = it }
        )
    }

    private fun reportProgress(
        operation: BackupOperationType,
        step: Int,
        totalSteps: Int,
        title: String,
        detail: String,
        section: BackupSection? = null
    ) {
        _progressFlow.value = BackupTransferProgressUpdate(operation, step, totalSteps, title, detail, section)
    }
}
