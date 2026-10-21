package com.kayoi.pixelplayerdesktop.data.backup.validation

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupValidationResult
import com.kayoi.pixelplayerdesktop.data.backup.model.Severity
import com.kayoi.pixelplayerdesktop.data.backup.model.ValidationError
import java.security.MessageDigest

class ManifestValidator {

    fun validate(manifest: BackupManifest): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (manifest.schemaVersion < BackupManifest.MIN_SUPPORTED_VERSION) {
            errors.add(ValidationError(
                code = "UNSUPPORTED_VERSION",
                message = "Backup version ${manifest.schemaVersion} is too old and not supported.",
                severity = Severity.ERROR
            ))
        }

        if (manifest.schemaVersion > BackupManifest.CURRENT_SCHEMA_VERSION) {
            errors.add(ValidationError(
                code = "FUTURE_VERSION",
                message = "Backup was created with a newer version of PixelPlayer (v${manifest.schemaVersion}). Please update your app.",
                severity = Severity.ERROR
            ))
        }

        if (manifest.createdAt <= 0L) {
            errors.add(ValidationError(
                code = "INVALID_TIMESTAMP",
                message = "Backup manifest contains an invalid creation timestamp.",
                severity = Severity.WARNING
            ))
        }

        if (manifest.modules.isEmpty()) {
            errors.add(ValidationError(
                code = "EMPTY_MANIFEST",
                message = "Backup contains no modules to restore.",
                severity = Severity.ERROR
            ))
        }

        return if (errors.any { it.severity == Severity.ERROR }) {
            BackupValidationResult.Invalid(errors)
        } else if (errors.isNotEmpty()) {
            BackupValidationResult.Invalid(errors)
        } else {
            BackupValidationResult.Valid
        }
    }

    fun verifyChecksum(moduleKey: String, payload: String, manifest: BackupManifest): Boolean {
        val moduleInfo = manifest.modules[moduleKey] ?: return false
        val expected = moduleInfo.checksum.trim().removePrefix("sha256:")
        if (expected.isEmpty()) return true

        val actual = sha256(payload.toByteArray(Charsets.UTF_8))
        return actual.equals(expected, ignoreCase = true)
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
