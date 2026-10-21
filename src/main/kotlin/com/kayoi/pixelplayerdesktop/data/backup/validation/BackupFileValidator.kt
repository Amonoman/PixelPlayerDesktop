package com.kayoi.pixelplayerdesktop.data.backup.validation

import com.kayoi.pixelplayerdesktop.data.backup.format.BackupFormatDetector
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupValidationResult
import com.kayoi.pixelplayerdesktop.data.backup.model.Severity
import com.kayoi.pixelplayerdesktop.data.backup.model.ValidationError
import java.io.File

class BackupFileValidator(
    private val formatDetector: BackupFormatDetector = BackupFormatDetector()
) {
    companion object {
        const val MAX_BACKUP_FILE_BYTES = 500L * 1024L * 1024L // 500MB safety ceiling
        const val MIN_BACKUP_FILE_BYTES = 8L
    }

    fun validate(file: File): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (!file.exists()) {
            errors.add(ValidationError("FILE_NOT_FOUND", "The specified backup file does not exist: ${file.name}", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        if (!file.isFile) {
            errors.add(ValidationError("NOT_A_FILE", "The specified path is a directory, not a file.", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        if (!file.canRead()) {
            errors.add(ValidationError("FILE_NOT_READABLE", "Permission denied: unable to read backup file.", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        val length = file.length()
        if (length < MIN_BACKUP_FILE_BYTES) {
            errors.add(ValidationError("FILE_EMPTY", "Backup file is empty or corrupted (size: $length bytes).", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        if (length > MAX_BACKUP_FILE_BYTES) {
            errors.add(ValidationError("FILE_TOO_LARGE", "Backup file exceeds maximum allowed size of 500MB (size: ${length / (1024 * 1024)}MB).", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        val format = try {
            file.inputStream().use { input ->
                val header = formatDetector.readHeader(input)
                formatDetector.detect(header)
            }
        } catch (e: Exception) {
            errors.add(ValidationError("READ_ERROR", "Failed to inspect backup file header: ${e.message}", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        if (format == BackupFormatDetector.Format.UNKNOWN) {
            errors.add(ValidationError("INVALID_FORMAT", "Unrecognized backup file format. Expected a .pxpl archive.", severity = Severity.ERROR))
            return BackupValidationResult.Invalid(errors)
        }

        return BackupValidationResult.Valid
    }
}
