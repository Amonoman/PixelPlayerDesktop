package com.kayoi.pixelplayerdesktop.data.backup.validation

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupValidationResult
import com.kayoi.pixelplayerdesktop.data.backup.model.Severity
import com.kayoi.pixelplayerdesktop.data.backup.model.ValidationError
import java.io.File

class ValidationPipeline(
    private val fileValidator: BackupFileValidator = BackupFileValidator(),
    private val manifestValidator: ManifestValidator = ManifestValidator(),
    private val moduleSchemaValidator: ModuleSchemaValidator = ModuleSchemaValidator()
) {
    fun validateFile(file: File): BackupValidationResult {
        return fileValidator.validate(file)
    }

    fun validateManifest(manifest: BackupManifest): BackupValidationResult {
        return manifestValidator.validate(manifest)
    }

    fun validateModulePayload(
        section: BackupSection,
        payload: String,
        manifest: BackupManifest? = null
    ): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (manifest != null && !manifestValidator.verifyChecksum(section.key, payload, manifest)) {
            errors.add(ValidationError(
                code = "CHECKSUM_MISMATCH",
                message = "Checksum mismatch for module '${section.label}'. The backup may be corrupted or modified.",
                module = section.key,
                severity = Severity.ERROR
            ))
            return BackupValidationResult.Invalid(errors)
        }

        val schemaResult = moduleSchemaValidator.validate(section, payload)
        if (schemaResult is BackupValidationResult.Invalid) {
            errors.addAll(schemaResult.errors)
        }

        return if (errors.any { it.severity == Severity.ERROR }) {
            BackupValidationResult.Invalid(errors)
        } else if (errors.isNotEmpty()) {
            BackupValidationResult.Invalid(errors)
        } else {
            BackupValidationResult.Valid
        }
    }
}
