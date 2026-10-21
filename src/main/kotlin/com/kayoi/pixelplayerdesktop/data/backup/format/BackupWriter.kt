package com.kayoi.pixelplayerdesktop.data.backup.format

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupModuleInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupWriter(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    suspend fun write(
        targetFile: File,
        manifest: BackupManifest,
        modulePayloads: Map<String, String>,
        passphrase: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val totalSteps = modulePayloads.size + 1
            var currentStep = 0

            val modulesInfo = mutableMapOf<String, BackupModuleInfo>()
            val payloadBytes = mutableMapOf<String, ByteArray>()

            modulePayloads.forEach { (key, jsonPayload) ->
                val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
                payloadBytes[key] = bytes
                modulesInfo[key] = BackupModuleInfo(
                    checksum = "sha256:${sha256(bytes)}",
                    entryCount = countJsonArrayEntries(jsonPayload),
                    sizeBytes = bytes.size.toLong()
                )
            }

            val finalManifest = manifest.copy(modules = modulesInfo)
            val manifestJson = json.encodeToString(BackupManifest.serializer(), finalManifest)

            val parent = targetFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }

            targetFile.outputStream().use { rawOutput ->
                rawOutput.write(BackupFormatDetector.PXPL_MAGIC)

                val archiveOutput = if (passphrase != null) {
                    BackupCrypto.encryptingStream(rawOutput, passphrase)
                } else {
                    rawOutput
                }

                ZipOutputStream(archiveOutput).use { zip ->
                    zip.putNextEntry(ZipEntry(BackupManifest.MANIFEST_FILENAME))
                    zip.write(manifestJson.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    onProgress(++currentStep, totalSteps)

                    payloadBytes.forEach { (key, bytes) ->
                        zip.putNextEntry(ZipEntry("$key.json"))
                        zip.write(bytes)
                        zip.closeEntry()
                        onProgress(++currentStep, totalSteps)
                    }
                }
            }
        }
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun countJsonArrayEntries(payload: String): Int {
        return try {
            val trimmed = payload.trim()
            if (trimmed.startsWith("[")) {
                val element = json.parseToJsonElement(trimmed)
                if (element is JsonArray) element.size else 1
            } else {
                1
            }
        } catch (_: Exception) {
            0
        }
    }
}
