package com.kayoi.pixelplayerdesktop.data.metadata

import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.generateArtistId
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_ARTIST_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DEFAULT_WORD_DELIMITERS
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import com.kayoi.pixelplayerdesktop.utils.collectArtistNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gagravarr.opus.OpusFile
import org.gagravarr.opus.OpusTags
import com.kayoi.pixelplayerdesktop.utils.AudioFileReaderUtils
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import javax.imageio.ImageIO

data class CoverArtUpdate(
    val bytes: ByteArray? = null,
    val mimeType: String = "image/jpeg",
    val isDeletion: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoverArtUpdate) return false
        if (isDeletion != other.isDeletion) return false
        if (mimeType != other.mimeType) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes?.contentHashCode() ?: 0
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + isDeletion.hashCode()
        return result
    }
}

data class EditableSongMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val composer: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val lyrics: String? = null,
    val coverArtUpdate: CoverArtUpdate? = null
)

data class AudioMetadataDetails(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val composer: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val trackTotal: Int?,
    val discNumber: Int?,
    val discTotal: Int?,
    val lyrics: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val format: String?,
    val durationMs: Long,
    val fileSize: Long,
    val artworkBytes: ByteArray?,
    val artworkMimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioMetadataDetails) return false
        return title == other.title &&
                artist == other.artist &&
                album == other.album &&
                albumArtist == other.albumArtist &&
                composer == other.composer &&
                genre == other.genre &&
                year == other.year &&
                trackNumber == other.trackNumber &&
                trackTotal == other.trackTotal &&
                discNumber == other.discNumber &&
                discTotal == other.discTotal &&
                lyrics == other.lyrics &&
                bitrate == other.bitrate &&
                sampleRate == other.sampleRate &&
                format == other.format &&
                durationMs == other.durationMs &&
                fileSize == other.fileSize &&
                (artworkBytes contentEquals other.artworkBytes) &&
                artworkMimeType == other.artworkMimeType
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (albumArtist?.hashCode() ?: 0)
        result = 31 * result + (composer?.hashCode() ?: 0)
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (year ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (trackTotal ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (discTotal ?: 0)
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (bitrate ?: 0)
        result = 31 * result + (sampleRate ?: 0)
        result = 31 * result + (format?.hashCode() ?: 0)
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        result = 31 * result + (artworkMimeType?.hashCode() ?: 0)
        return result
    }
}

data class MetadataEditResult(
    val success: Boolean,
    val updatedSong: Song? = null,
    val updatedArtworkPath: String? = null,
    val error: String? = null
)

data class BatchSongMetadataUpdate(
    val album: String? = null,
    val applyAlbum: Boolean = false,
    val artist: String? = null,
    val applyArtist: Boolean = false,
    val albumArtist: String? = null,
    val applyAlbumArtist: Boolean = false,
    val genre: String? = null,
    val applyGenre: Boolean = false,
    val year: Int? = null,
    val applyYear: Boolean = false,
    val discNumber: Int? = null,
    val applyDiscNumber: Boolean = false,
    val autoRenumberTracks: Boolean = false,
    val coverArtUpdate: CoverArtUpdate? = null,
    val filenamePattern: String? = null
)

data class BatchMetadataResult(
    val total: Int,
    val successCount: Int,
    val failureCount: Int,
    val updatedSongs: List<Song>,
    val errors: List<String>
)

class DesktopMetadataEditor(
    private val musicDao: MusicDao,
    private val artworkCacheDir: File = DesktopPathProvider.artworkCacheDir,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun readMetadata(file: File): Result<AudioMetadataDetails> = withContext(ioDispatcher) {
        if (!file.exists() || !file.isFile) {
            return@withContext Result.failure(FileNotFoundException("File does not exist: ${file.absolutePath}"))
        }

        if (isOpusFile(file)) {
            return@withContext readOpusMetadata(file)
        }

        try {
            java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF
            val audioFile = AudioFileReaderUtils.readAudioFile(file)
            val tag = audioFile.tag
            val header = audioFile.audioHeader

            val title = tag?.getFirst(FieldKey.TITLE)?.trim()?.ifBlank { null } ?: file.nameWithoutExtension
            val artist = tag?.getFirst(FieldKey.ARTIST)?.trim()?.ifBlank { null } ?: "Unknown Artist"
            val album = tag?.getFirst(FieldKey.ALBUM)?.trim()?.ifBlank { null } ?: "Unknown Album"
            val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.trim()?.ifBlank { null }
            val composer = tag?.getFirst(FieldKey.COMPOSER)?.trim()?.ifBlank { null }
            val genre = tag?.getFirst(FieldKey.GENRE)?.trim()?.ifBlank { null }
            val lyrics = tag?.getFirst(FieldKey.LYRICS)?.trim()?.ifBlank { null }

            val yearStr = tag?.getFirst(FieldKey.YEAR) ?: tag?.getFirst(FieldKey.ORIGINAL_YEAR)
            val year = parseYear(yearStr)

            val trackStr = tag?.getFirst(FieldKey.TRACK)
            val trackNumber = trackStr?.substringBefore('/')?.trim()?.toIntOrNull()
            val trackTotal = tag?.getFirst(FieldKey.TRACK_TOTAL)?.trim()?.toIntOrNull()
                ?: trackStr?.substringAfter('/', "")?.trim()?.toIntOrNull()

            val discStr = tag?.getFirst(FieldKey.DISC_NO)
            val discNumber = discStr?.substringBefore('/')?.trim()?.toIntOrNull()
            val discTotal = tag?.getFirst(FieldKey.DISC_TOTAL)?.trim()?.toIntOrNull()
                ?: discStr?.substringAfter('/', "")?.trim()?.toIntOrNull()

            val artwork = tag?.firstArtwork
            val artworkBytes = artwork?.binaryData
            val artworkMimeType = artwork?.mimeType

            val durationMs = (header?.trackLength?.toLong() ?: 0L) * 1000L
            val bitrate = header?.bitRateAsNumber?.toInt()
            val sampleRate = header?.sampleRateAsNumber
            val format = header?.format

            Result.success(
                AudioMetadataDetails(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    composer = composer,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    trackTotal = trackTotal,
                    discNumber = discNumber,
                    discTotal = discTotal,
                    lyrics = lyrics,
                    bitrate = bitrate,
                    sampleRate = sampleRate,
                    format = format,
                    durationMs = durationMs,
                    fileSize = file.length(),
                    artworkBytes = artworkBytes,
                    artworkMimeType = artworkMimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeMetadata(
        song: Song,
        metadata: EditableSongMetadata,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true
    ): MetadataEditResult = withContext(ioDispatcher) {
        val targetFile = File(song.path)
        if (!targetFile.exists()) {
            return@withContext MetadataEditResult(success = false, error = "File not found: ${song.path}")
        }
        if (!targetFile.canWrite()) {
            return@withContext MetadataEditResult(success = false, error = "Permission denied or file is read-only: ${song.path}")
        }

        val writeSuccess = if (isOpusFile(targetFile)) {
            writeOpusMetadata(targetFile, metadata)
        } else {
            writeJAudioTaggerMetadata(targetFile, metadata)
        }

        if (!writeSuccess) {
            return@withContext MetadataEditResult(success = false, error = "Failed to write tags to audio file")
        }

        val updatedArtworkPath = when {
            metadata.coverArtUpdate?.isDeletion == true -> null
            metadata.coverArtUpdate?.bytes != null -> saveArtworkToCache(metadata.coverArtUpdate.bytes)
            else -> song.albumArtUriString
        }

        val title = metadata.title.trim().ifBlank { targetFile.nameWithoutExtension }
        val rawArtist = metadata.artist.trim().ifBlank { "Unknown Artist" }
        val parsedArtists = collectArtistNames(
            rawArtistName = rawArtist,
            title = title,
            artistDelimiters = artistDelimiters,
            wordDelimiters = wordDelimiters,
            extractFromTitle = extractArtistFromTitle
        )
        val primaryArtist = parsedArtists.firstOrNull()?.ifBlank { null } ?: rawArtist
        val artistRefs = parsedArtists.mapIndexed { index, name ->
            ArtistRef(
                id = generateArtistId(name),
                name = name.trim(),
                isPrimary = (index == 0)
            )
        }
        val album = metadata.album.trim().ifBlank { "Unknown Album" }
        val artistId = generateArtistId(primaryArtist)
        val albumId = "$album-$primaryArtist".hashCode().toLong()

        val updatedSong = song.copy(
            title = title,
            artist = primaryArtist,
            artistId = artistId,
            artists = artistRefs,
            album = album,
            albumId = albumId,
            albumArtist = metadata.albumArtist?.trim()?.ifBlank { null },
            genre = metadata.genre?.trim()?.ifBlank { null },
            lyrics = metadata.lyrics?.trim()?.ifBlank { null },
            trackNumber = metadata.trackNumber ?: 0,
            discNumber = metadata.discNumber,
            year = metadata.year ?: song.year,
            albumArtUriString = updatedArtworkPath,
            dateModified = targetFile.lastModified()
        )

        try {
            musicDao.upsertSongs(listOf(updatedSong))
        } catch (e: Exception) {
            return@withContext MetadataEditResult(
                success = false,
                updatedSong = updatedSong,
                updatedArtworkPath = updatedArtworkPath,
                error = "File saved but database update failed: ${e.message}"
            )
        }

        MetadataEditResult(
            success = true,
            updatedSong = updatedSong,
            updatedArtworkPath = updatedArtworkPath
        )
    }

    suspend fun writeSongLyrics(song: Song, lyricsText: String): MetadataEditResult = withContext(ioDispatcher) {
        val targetFile = File(song.path)
        if (!targetFile.exists()) {
            return@withContext MetadataEditResult(success = false, error = "File not found: ${song.path}")
        }
        if (!targetFile.canWrite()) {
            return@withContext MetadataEditResult(success = false, error = "Permission denied or file is read-only: ${song.path}")
        }

        val writeSuccess = if (isOpusFile(targetFile)) {
            writeOpusLyrics(targetFile, lyricsText)
        } else {
            writeJAudioTaggerLyrics(targetFile, lyricsText)
        }

        if (!writeSuccess) {
            return@withContext MetadataEditResult(success = false, error = "Failed to write lyrics to audio file")
        }

        val updatedSong = song.copy(
            lyrics = lyricsText.trim().ifBlank { null },
            dateModified = targetFile.lastModified()
        )

        try {
            musicDao.upsertSongs(listOf(updatedSong))
        } catch (e: Exception) {
            return@withContext MetadataEditResult(
                success = false,
                updatedSong = updatedSong,
                error = "Lyrics saved to file but database update failed: ${e.message}"
            )
        }

        MetadataEditResult(
            success = true,
            updatedSong = updatedSong
        )
    }

    private fun writeJAudioTaggerLyrics(audioFile: File, lyricsText: String): Boolean {
        return try {
            java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF
            val fileObj = AudioFileReaderUtils.readAudioFile(audioFile)
            val tag = fileObj.tag ?: fileObj.createDefaultTag()
            if (lyricsText.isNotBlank()) {
                tag.setField(FieldKey.LYRICS, lyricsText)
            } else {
                tag.deleteField(FieldKey.LYRICS)
            }
            fileObj.commit()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun writeOpusLyrics(audioFile: File, lyricsText: String): Boolean {
        var opusFile: OpusFile? = null
        var tempFile: File? = null
        return try {
            val sourceOpusFile = OpusFile(audioFile)
            opusFile = sourceOpusFile
            val tags = sourceOpusFile.tags ?: OpusTags()
            tags.replaceSingleComment("LYRICS", lyricsText.takeIf { it.isNotBlank() })

            val cacheDir = DesktopPathProvider.cacheDir
            tempFile = File(cacheDir, "metadata_edit_opus_${System.nanoTime()}.opus")
            FileOutputStream(tempFile).use { fos ->
                OpusFile(fos, sourceOpusFile.info, tags).use { newOpusFile ->
                    var packet = sourceOpusFile.nextAudioPacket
                    while (packet != null) {
                        newOpusFile.writeAudioData(packet)
                        packet = sourceOpusFile.nextAudioPacket
                    }
                }
            }
            sourceOpusFile.close()
            opusFile = null

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return false
            }

            try {
                Files.move(
                    tempFile.toPath(),
                    audioFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(
                    tempFile.toPath(),
                    audioFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            try { opusFile?.close() } catch (_: Exception) {}
            try {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        artistDelimiters: List<String> = DEFAULT_ARTIST_DELIMITERS,
        wordDelimiters: List<String> = DEFAULT_WORD_DELIMITERS,
        extractArtistFromTitle: Boolean = true,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult = withContext(ioDispatcher) {
        val total = songs.size
        val updatedSongs = mutableListOf<Song>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        songs.forEachIndexed { index, song ->
            val file = File(song.path)
            if (!file.exists() || !file.canWrite()) {
                failureCount++
                errors.add("${song.title}: File not accessible or read-only")
                onProgress(index + 1, total)
                return@forEachIndexed
            }

            val trackNumber = if (update.autoRenumberTracks) {
                index + 1
            } else {
                if (song.trackNumber > 0) song.trackNumber else null
            }

            val metadata = EditableSongMetadata(
                title = song.title,
                artist = if (update.applyArtist && update.artist != null) update.artist else song.artist,
                album = if (update.applyAlbum && update.album != null) update.album else song.album,
                albumArtist = if (update.applyAlbumArtist) update.albumArtist else song.albumArtist,
                genre = if (update.applyGenre) update.genre else song.genre,
                year = if (update.applyYear) update.year else if (song.year > 0) song.year else null,
                discNumber = if (update.applyDiscNumber) update.discNumber else song.discNumber,
                trackNumber = trackNumber,
                lyrics = song.lyrics,
                coverArtUpdate = update.coverArtUpdate
            )

            val editResult = writeMetadata(
                song = song,
                metadata = metadata,
                artistDelimiters = artistDelimiters,
                wordDelimiters = wordDelimiters,
                extractArtistFromTitle = extractArtistFromTitle
            )

            if (!editResult.success || editResult.updatedSong == null) {
                failureCount++
                errors.add("${song.title}: ${editResult.error ?: "Failed to write tags"}")
                onProgress(index + 1, total)
                return@forEachIndexed
            }

            var currentSong = editResult.updatedSong

            if (!update.filenamePattern.isNullOrBlank()) {
                try {
                    val ext = file.extension
                    val targetName = generateNewFilename(
                        pattern = update.filenamePattern,
                        song = currentSong,
                        trackNumber = trackNumber,
                        extension = ext
                    )
                    val targetFile = File(file.parentFile, targetName)

                    if (targetFile.canonicalPath != file.canonicalPath) {
                        var candidateFile = targetFile
                        var counter = 1
                        val baseName = candidateFile.nameWithoutExtension
                        while (candidateFile.exists() && candidateFile.canonicalPath != file.canonicalPath) {
                            candidateFile = File(file.parentFile, "$baseName ($counter).$ext")
                            counter++
                        }

                        if (file.renameTo(candidateFile)) {
                            musicDao.updateSongPath(
                                id = currentSong.id,
                                newPath = candidateFile.absolutePath,
                                newContentUri = candidateFile.toURI().toString()
                            )
                            currentSong = currentSong.copy(
                                path = candidateFile.absolutePath,
                                contentUriString = candidateFile.toURI().toString()
                            )
                        } else {
                            errors.add("${song.title}: File rename failed (tags were saved)")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("${song.title}: Error renaming file: ${e.message}")
                }
            }

            successCount++
            updatedSongs.add(currentSong)
            onProgress(index + 1, total)
        }

        BatchMetadataResult(
            total = total,
            successCount = successCount,
            failureCount = failureCount,
            updatedSongs = updatedSongs,
            errors = errors
        )
    }

    private fun isOpusFile(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext == "opus") return true
        if (ext == "ogg") {
            try {
                file.inputStream().use { input ->
                    val header = ByteArray(512)
                    val read = input.read(header)
                    if (read >= 36) {
                        val text = String(header, 0, read, Charsets.ISO_8859_1)
                        if (text.contains("OpusHead")) return true
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun readOpusMetadata(file: File): Result<AudioMetadataDetails> {
        return try {
            val opusFile = OpusFile(file)
            val tags = opusFile.tags

            fun getComment(key: String): String? {
                if (tags == null) return null
                return tags.allComments.entries
                    .firstOrNull { it.key.equals(key, ignoreCase = true) }
                    ?.value
                    ?.firstOrNull()
                    ?.trim()
                    ?.ifBlank { null }
            }

            val title = getComment("TITLE") ?: file.nameWithoutExtension
            val artist = getComment("ARTIST") ?: "Unknown Artist"
            val album = getComment("ALBUM") ?: "Unknown Album"
            val albumArtist = getComment("ALBUMARTIST") ?: getComment("ALBUM ARTIST")
            val composer = getComment("COMPOSER")
            val genre = getComment("GENRE")
            val lyrics = getComment("LYRICS") ?: getComment("UNSYNCEDLYRICS")
            val year = parseYear(getComment("DATE") ?: getComment("YEAR"))

            val trackStr = getComment("TRACKNUMBER")
            val trackNumber = trackStr?.substringBefore('/')?.trim()?.toIntOrNull()
            val trackTotal = getComment("TRACKTOTAL")?.toIntOrNull()
                ?: trackStr?.substringAfter('/', "")?.trim()?.toIntOrNull()

            val discStr = getComment("DISCNUMBER")
            val discNumber = discStr?.substringBefore('/')?.trim()?.toIntOrNull()
            val discTotal = getComment("DISCTOTAL")?.toIntOrNull()
                ?: discStr?.substringAfter('/', "")?.trim()?.toIntOrNull()

            var artworkBytes: ByteArray? = null
            var artworkMimeType: String? = null
            val pictureBlock = getComment("metadata_block_picture")
            if (!pictureBlock.isNullOrBlank()) {
                val parsed = parseFlacPictureBlock(pictureBlock)
                artworkBytes = parsed?.first
                artworkMimeType = parsed?.second
            }
            if (artworkBytes == null) {
                val coverArt = getComment("coverart")
                if (!coverArt.isNullOrBlank()) {
                    try {
                        artworkBytes = Base64.getDecoder().decode(coverArt.trim())
                        artworkMimeType = getComment("coverartmime") ?: "image/jpeg"
                    } catch (_: Exception) {}
                }
            }

            val durationMs = getOpusDurationMs(file)
            opusFile.close()

            Result.success(
                AudioMetadataDetails(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    composer = composer,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    trackTotal = trackTotal,
                    discNumber = discNumber,
                    discTotal = discTotal,
                    lyrics = lyrics,
                    bitrate = null,
                    sampleRate = 48000,
                    format = "Opus",
                    durationMs = durationMs,
                    fileSize = file.length(),
                    artworkBytes = artworkBytes,
                    artworkMimeType = artworkMimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeOpusMetadata(
        audioFile: File,
        metadata: EditableSongMetadata
    ): Boolean {
        var opusFile: OpusFile? = null
        var tempFile: File? = null
        return try {
            val sourceOpusFile = OpusFile(audioFile)
            opusFile = sourceOpusFile
            val tags = sourceOpusFile.tags ?: OpusTags()

            tags.replaceSingleComment("TITLE", metadata.title)
            tags.replaceSingleComment("ARTIST", metadata.artist)
            tags.replaceSingleComment("ALBUM", metadata.album)
            tags.replaceSingleComment("ALBUMARTIST", metadata.albumArtist?.takeIf { it.isNotBlank() })
            tags.replaceSingleComment("COMPOSER", metadata.composer?.takeIf { it.isNotBlank() })
            tags.replaceSingleComment("GENRE", metadata.genre?.takeIf { it.isNotBlank() })
            tags.replaceSingleComment("LYRICS", metadata.lyrics?.takeIf { it.isNotBlank() })

            if (metadata.year != null && metadata.year > 0) {
                tags.replaceSingleComment("DATE", metadata.year.toString())
            } else {
                tags.removeComments("DATE")
                tags.removeComments("YEAR")
            }

            if (metadata.trackNumber != null && metadata.trackNumber > 0) {
                tags.replaceSingleComment("TRACKNUMBER", metadata.trackNumber.toString())
            } else {
                tags.removeComments("TRACKNUMBER")
            }
            if (metadata.trackTotal != null && metadata.trackTotal > 0) {
                tags.replaceSingleComment("TRACKTOTAL", metadata.trackTotal.toString())
            } else {
                tags.removeComments("TRACKTOTAL")
            }

            if (metadata.discNumber != null && metadata.discNumber > 0) {
                tags.replaceSingleComment("DISCNUMBER", metadata.discNumber.toString())
            } else {
                tags.removeComments("DISCNUMBER")
            }
            if (metadata.discTotal != null && metadata.discTotal > 0) {
                tags.replaceSingleComment("DISCTOTAL", metadata.discTotal.toString())
            } else {
                tags.removeComments("DISCTOTAL")
            }

            metadata.coverArtUpdate?.let { update ->
                tags.applyCoverArtUpdate(update)
            }

            val cacheDir = DesktopPathProvider.cacheDir
            tempFile = File(cacheDir, "metadata_edit_opus_${System.nanoTime()}.opus")
            FileOutputStream(tempFile).use { fos ->
                OpusFile(fos, sourceOpusFile.info, tags).use { newOpusFile ->
                    var packet = sourceOpusFile.nextAudioPacket
                    while (packet != null) {
                        newOpusFile.writeAudioData(packet)
                        packet = sourceOpusFile.nextAudioPacket
                    }
                }
            }
            sourceOpusFile.close()
            opusFile = null

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return false
            }

            try {
                Files.move(
                    tempFile.toPath(),
                    audioFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(
                    tempFile.toPath(),
                    audioFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            try { opusFile?.close() } catch (_: Exception) {}
            try {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    private fun writeJAudioTaggerMetadata(
        audioFile: File,
        metadata: EditableSongMetadata
    ): Boolean {
        return try {
            java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF
            val fileObj = AudioFileReaderUtils.readAudioFile(audioFile)
            val tag = fileObj.tag ?: fileObj.createDefaultTag()

            tag.setField(FieldKey.TITLE, metadata.title)
            tag.setField(FieldKey.ARTIST, metadata.artist)
            tag.setField(FieldKey.ALBUM, metadata.album)

            if (!metadata.albumArtist.isNullOrBlank()) {
                tag.setField(FieldKey.ALBUM_ARTIST, metadata.albumArtist)
            } else {
                tag.deleteField(FieldKey.ALBUM_ARTIST)
            }

            if (!metadata.composer.isNullOrBlank()) {
                tag.setField(FieldKey.COMPOSER, metadata.composer)
            } else {
                tag.deleteField(FieldKey.COMPOSER)
            }

            if (!metadata.genre.isNullOrBlank()) {
                tag.setField(FieldKey.GENRE, metadata.genre)
            } else {
                tag.deleteField(FieldKey.GENRE)
            }

            if (!metadata.lyrics.isNullOrBlank()) {
                tag.setField(FieldKey.LYRICS, metadata.lyrics)
            } else {
                tag.deleteField(FieldKey.LYRICS)
            }

            if (metadata.year != null && metadata.year > 0) {
                tag.setField(FieldKey.YEAR, metadata.year.toString())
            } else {
                tag.deleteField(FieldKey.YEAR)
            }

            if (metadata.trackNumber != null && metadata.trackNumber > 0) {
                tag.setField(FieldKey.TRACK, metadata.trackNumber.toString())
            } else {
                tag.deleteField(FieldKey.TRACK)
            }
            if (metadata.trackTotal != null && metadata.trackTotal > 0) {
                tag.setField(FieldKey.TRACK_TOTAL, metadata.trackTotal.toString())
            } else {
                tag.deleteField(FieldKey.TRACK_TOTAL)
            }

            if (metadata.discNumber != null && metadata.discNumber > 0) {
                tag.setField(FieldKey.DISC_NO, metadata.discNumber.toString())
            } else {
                tag.deleteField(FieldKey.DISC_NO)
            }
            if (metadata.discTotal != null && metadata.discTotal > 0) {
                tag.setField(FieldKey.DISC_TOTAL, metadata.discTotal.toString())
            } else {
                tag.deleteField(FieldKey.DISC_TOTAL)
            }

            metadata.coverArtUpdate?.let { update ->
                if (update.isDeletion) {
                    tag.deleteArtworkField()
                } else if (update.bytes != null && update.bytes.isNotEmpty()) {
                    val artwork = ArtworkFactory.getNew()
                    artwork.binaryData = update.bytes
                    artwork.mimeType = update.mimeType
                    artwork.pictureType = 3
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                }
            }

            fileObj.commit()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun OpusTags.replaceSingleComment(key: String, value: String?) {
        removeComments(key)
        if (!value.isNullOrBlank()) {
            addComment(key, value)
        }
    }

    private fun OpusTags.applyCoverArtUpdate(update: CoverArtUpdate) {
        removeComments("METADATA_BLOCK_PICTURE")
        removeComments("COVERART")
        removeComments("COVERARTMIME")

        if (update.isDeletion) {
            return
        }

        val imageBytes = update.bytes ?: return
        addComment("METADATA_BLOCK_PICTURE", buildVorbisPictureBlock(imageBytes, update.mimeType))
    }

    private fun buildVorbisPictureBlock(imageBytes: ByteArray, mimeType: String): String {
        val safeMimeType = mimeType.takeIf { it.isNotBlank() } ?: "image/jpeg"
        val mimeBytes = safeMimeType.toByteArray(Charsets.UTF_8)
        val descriptionBytes = "Front Cover".toByteArray(Charsets.UTF_8)
        val (width, height) = getImageDimensions(imageBytes)

        val output = ByteArrayOutputStream()
        output.writeIntBigEndian(3)
        output.writeIntBigEndian(mimeBytes.size)
        output.write(mimeBytes)
        output.writeIntBigEndian(descriptionBytes.size)
        output.write(descriptionBytes)
        output.writeIntBigEndian(width)
        output.writeIntBigEndian(height)
        output.writeIntBigEndian(0)
        output.writeIntBigEndian(0)
        output.writeIntBigEndian(imageBytes.size)
        output.write(imageBytes)
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }

    private fun getImageDimensions(imageBytes: ByteArray): Pair<Int, Int> {
        return try {
            val iis = ImageIO.createImageInputStream(ByteArrayInputStream(imageBytes)) ?: return Pair(0, 0)
            val readers = ImageIO.getImageReaders(iis)
            if (readers.hasNext()) {
                val reader = readers.next()
                try {
                    reader.input = iis
                    Pair(reader.getWidth(0), reader.getHeight(0))
                } finally {
                    reader.dispose()
                }
            } else {
                Pair(0, 0)
            }
        } catch (_: Exception) {
            Pair(0, 0)
        }
    }

    private fun ByteArrayOutputStream.writeIntBigEndian(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun parseFlacPictureBlock(base64Data: String): Pair<ByteArray, String>? {
        return try {
            val bytes = Base64.getDecoder().decode(base64Data.trim())
            if (bytes.size < 32) return null
            val buffer = ByteBuffer.wrap(bytes)
            buffer.int // type
            val mimeLen = buffer.int
            if (mimeLen < 0 || mimeLen > bytes.size - 8) return null
            val mimeBytes = ByteArray(mimeLen)
            buffer.get(mimeBytes)
            val mime = String(mimeBytes, Charsets.US_ASCII)
            val descLen = buffer.int
            if (descLen < 0 || descLen > bytes.size - buffer.position()) return null
            val descBytes = ByteArray(descLen)
            buffer.get(descBytes)
            buffer.int // width
            buffer.int // height
            buffer.int // depth
            buffer.int // colors
            val dataLen = buffer.int
            if (dataLen <= 0 || dataLen > bytes.size - buffer.position()) return null
            val imgData = ByteArray(dataLen)
            buffer.get(imgData)
            Pair(imgData, mime)
        } catch (_: Exception) {
            null
        }
    }

    private fun getOpusDurationMs(file: File): Long {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                if (length < 28) return 0L
                val searchSize = minOf(65536L, length).toInt()
                val buffer = ByteArray(searchSize)
                raf.seek(length - searchSize)
                raf.readFully(buffer)

                for (i in (searchSize - 14) downTo 0) {
                    if (buffer[i] == 'O'.code.toByte() &&
                        buffer[i + 1] == 'g'.code.toByte() &&
                        buffer[i + 2] == 'g'.code.toByte() &&
                        buffer[i + 3] == 'S'.code.toByte()
                    ) {
                        val granulePos = (buffer[i + 6].toLong() and 0xFF) or
                                ((buffer[i + 7].toLong() and 0xFF) shl 8) or
                                ((buffer[i + 8].toLong() and 0xFF) shl 16) or
                                ((buffer[i + 9].toLong() and 0xFF) shl 24) or
                                ((buffer[i + 10].toLong() and 0xFF) shl 32) or
                                ((buffer[i + 11].toLong() and 0xFF) shl 40) or
                                ((buffer[i + 12].toLong() and 0xFF) shl 48) or
                                ((buffer[i + 13].toLong() and 0xFF) shl 56)

                        if (granulePos > 0) {
                            return (granulePos * 1000L) / 48000L
                        }
                    }
                }
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseYear(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(raw) ?: Regex("""\d{4}""").find(raw)
        return match?.value?.toIntOrNull() ?: raw.trim().take(4).toIntOrNull()
    }

    fun saveArtworkToCache(binaryData: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(binaryData).joinToString("") { "%02x".format(it) }
            val cachedFile = File(artworkCacheDir, "$hash.jpg")
            if (!cachedFile.exists()) {
                cachedFile.writeBytes(binaryData)
            }
            cachedFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun generateNewFilename(
            pattern: String,
            song: Song,
            trackNumber: Int?,
            extension: String
        ): String {
            val trackStr = trackNumber?.let { String.format("%02d", it) } ?: "01"
            val artistStr = song.artist
            val titleStr = song.title
            val albumStr = song.album

            var result = pattern
                .replace("{track}", trackStr)
                .replace("{title}", titleStr)
                .replace("{artist}", artistStr)
                .replace("{album}", albumStr)

            result = result.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            if (result.isBlank()) {
                result = song.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            }
            return "$result.$extension"
        }
    }
}
