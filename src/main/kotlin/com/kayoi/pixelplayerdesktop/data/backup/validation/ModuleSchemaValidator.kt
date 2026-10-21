package com.kayoi.pixelplayerdesktop.data.backup.validation

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupValidationResult
import com.kayoi.pixelplayerdesktop.data.backup.model.Severity
import com.kayoi.pixelplayerdesktop.data.backup.model.ValidationError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class ModuleSchemaValidator(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        const val MAX_ENTRIES_PER_MODULE = 100_000
    }

    fun validate(section: BackupSection, payload: String): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        val element = try {
            json.parseToJsonElement(payload)
        } catch (e: Exception) {
            errors.add(ValidationError(
                code = "INVALID_JSON",
                message = "Module '${section.label}' contains invalid JSON: ${e.message}",
                module = section.key,
                severity = Severity.ERROR
            ))
            return BackupValidationResult.Invalid(errors)
        }

        when (section) {
            BackupSection.PLAYLISTS -> {
                if (element is JsonObject) {
                    val playlistsArray = element["playlists"]
                    if (playlistsArray != null && playlistsArray !is JsonArray) {
                        errors.add(ValidationError("INVALID_SCHEMA", "'playlists' property must be an array.", section.key))
                    } else if (playlistsArray is JsonArray && playlistsArray.size > MAX_ENTRIES_PER_MODULE) {
                        errors.add(ValidationError("TOO_MANY_ENTRIES", "Playlists exceed $MAX_ENTRIES_PER_MODULE entries limit.", section.key))
                    }
                } else if (element is JsonArray) {
                    if (element.size > MAX_ENTRIES_PER_MODULE) {
                        errors.add(ValidationError("TOO_MANY_ENTRIES", "Playlists exceed $MAX_ENTRIES_PER_MODULE entries limit.", section.key))
                    }
                } else {
                    errors.add(ValidationError("INVALID_TYPE", "Playlists payload must be a JSON object or array.", section.key))
                }
            }
            BackupSection.GLOBAL_SETTINGS,
            BackupSection.EQUALIZER,
            BackupSection.PLAYBACK_HISTORY -> {
                if (element !is JsonArray && element !is JsonObject) {
                    errors.add(ValidationError("INVALID_TYPE", "${section.label} payload must be a JSON array or object.", section.key))
                }
            }
            BackupSection.FAVORITES,
            BackupSection.LYRICS,
            BackupSection.ENGAGEMENT_STATS,
            BackupSection.ARTIST_IMAGES,
            BackupSection.SEARCH_HISTORY,
            BackupSection.TRANSITIONS,
            BackupSection.QUICK_FILL,
            BackupSection.AUDIO_BOOKMARKS -> {
                if (element !is JsonArray) {
                    errors.add(ValidationError("NOT_ARRAY", "${section.label} payload must be a JSON array.", section.key))
                } else if (element.size > MAX_ENTRIES_PER_MODULE) {
                    errors.add(ValidationError("TOO_MANY_ENTRIES", "${section.label} exceeds $MAX_ENTRIES_PER_MODULE entries limit.", section.key))
                }
            }
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
