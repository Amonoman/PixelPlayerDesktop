package com.kayoi.pixelplayerdesktop.data.backup.format

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class BackupReader(
    private val formatDetector: BackupFormatDetector = BackupFormatDetector(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    companion object {
        const val MAX_MANIFEST_BYTES = 512 * 1024
        const val MAX_MODULE_PAYLOAD_BYTES = 16 * 1024 * 1024
    }

    suspend fun detectFormat(file: File): Result<BackupFormatDetector.Format> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.exists()) { "Backup file does not exist: ${file.absolutePath}" }
            file.inputStream().use { input ->
                val header = formatDetector.readHeader(input)
                formatDetector.detect(header)
            }
        }
    }

    suspend fun readManifest(file: File, passphrase: String? = null): Result<BackupManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormat(file).getOrThrow()
            when (format) {
                BackupFormatDetector.Format.PXPL_V4_ENCRYPTED -> {
                    if (passphrase == null) throw BackupEncryptedException()
                    readManifestFromZip(file, passphrase)
                }
                BackupFormatDetector.Format.PXPL_V3_ZIP -> {
                    readManifestFromZip(file, null)
                }
                else -> throw IllegalArgumentException("Unsupported backup format: $format")
            }
        }
    }

    suspend fun readModulePayload(
        file: File,
        moduleKey: String,
        passphrase: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormat(file).getOrThrow()
            when (format) {
                BackupFormatDetector.Format.PXPL_V4_ENCRYPTED -> {
                    if (passphrase == null) throw BackupEncryptedException()
                    readEntryFromZip(file, "$moduleKey.json", MAX_MODULE_PAYLOAD_BYTES, passphrase)
                        ?: throw IllegalArgumentException("Module '$moduleKey' not found in backup")
                }
                BackupFormatDetector.Format.PXPL_V3_ZIP -> {
                    readEntryFromZip(file, "$moduleKey.json", MAX_MODULE_PAYLOAD_BYTES, null)
                        ?: throw IllegalArgumentException("Module '$moduleKey' not found in backup")
                }
                else -> throw IllegalArgumentException("Unsupported backup format: $format")
            }
        }
    }

    suspend fun readAllModulePayloads(
        file: File,
        passphrase: String? = null
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormat(file).getOrThrow()
            when (format) {
                BackupFormatDetector.Format.PXPL_V4_ENCRYPTED -> {
                    if (passphrase == null) throw BackupEncryptedException()
                    readAllEntriesFromZip(file, MAX_MODULE_PAYLOAD_BYTES, passphrase)
                }
                BackupFormatDetector.Format.PXPL_V3_ZIP -> {
                    readAllEntriesFromZip(file, MAX_MODULE_PAYLOAD_BYTES, null)
                }
                else -> throw IllegalArgumentException("Unsupported backup format: $format")
            }
        }
    }

    private fun readManifestFromZip(file: File, passphrase: String?): BackupManifest {
        val jsonString = readEntryFromZip(
            file = file,
            entryName = BackupManifest.MANIFEST_FILENAME,
            maxBytes = MAX_MANIFEST_BYTES,
            passphrase = passphrase
        ) ?: throw IllegalArgumentException("Manifest not found in backup archive")

        return json.decodeFromString(BackupManifest.serializer(), jsonString)
    }

    private fun openZipStream(file: File, passphrase: String?): ZipInputStream {
        val raw = file.inputStream()
        try {
            skipFully(raw, BackupFormatDetector.PXPL_MAGIC_SIZE)
            val stream = if (passphrase != null) {
                BackupCrypto.decryptingStream(raw, passphrase)
            } else {
                raw
            }
            return ZipInputStream(stream)
        } catch (e: Exception) {
            raw.close()
            throw e
        }
    }

    private fun readEntryFromZip(
        file: File,
        entryName: String,
        maxBytes: Int,
        passphrase: String?
    ): String? {
        openZipStream(file, passphrase).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (isSuspiciousEntryName(entry.name)) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (entry.name == entryName) {
                    return readCurrentEntryText(zip, maxBytes, "Backup entry '$entryName'")
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun readAllEntriesFromZip(
        file: File,
        maxBytes: Int,
        passphrase: String?
    ): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        openZipStream(file, passphrase).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!isSuspiciousEntryName(name) &&
                    name != BackupManifest.MANIFEST_FILENAME &&
                    name.endsWith(".json")
                ) {
                    val key = name.removeSuffix(".json")
                    entries[key] = readCurrentEntryText(zip, maxBytes, "Backup entry '$name'")
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun isSuspiciousEntryName(name: String): Boolean {
        return name.contains("..") ||
            name.contains('/') ||
            name.contains('\\') ||
            name.contains(' ')
    }

    private fun readCurrentEntryText(
        input: InputStream,
        maxBytes: Int,
        sourceLabel: String
    ): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break

            totalBytes += read
            if (totalBytes > maxBytes) {
                throw IllegalArgumentException(
                    "$sourceLabel exceeds the ${maxBytes / (1024 * 1024)}MB safety limit."
                )
            }

            out.write(buffer, 0, read)
        }

        return out.toString(Charsets.UTF_8.name())
    }

    private fun skipFully(input: InputStream, byteCount: Int) {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = input.skip(remaining.toLong())
            if (skipped > 0) {
                remaining -= skipped.toInt()
                continue
            }

            if (input.read() == -1) {
                throw IllegalArgumentException("Backup file is truncated.")
            }
            remaining--
        }
    }
}
