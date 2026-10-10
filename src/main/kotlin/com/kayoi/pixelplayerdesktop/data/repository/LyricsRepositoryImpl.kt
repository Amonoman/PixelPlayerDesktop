package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

private val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

/**
 * Implementation of [LyricsRepository] handling LRC file parsing, ID3/Vorbis embedded lyrics, and local database caching.
 */
class LyricsRepositoryImpl(
    private val lyricsDao: LyricsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LyricsRepository {

    override suspend fun getLyrics(
        songId: String,
        songPath: String,
        title: String,
        artist: String
    ): Lyrics? = withContext(ioDispatcher) {
        // 1. Check local database cache
        val cached = lyricsDao.getLyrics(songId)
        if (cached != null) {
            return@withContext cached
        }

        // 2. Check for external sidecar .lrc file or embedded metadata tags
        val file = File(songPath)
        if (file.exists()) {
            val lrcFile = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
            if (lrcFile.exists()) {
                val lrcText = lrcFile.readText(Charsets.UTF_8)
                val parsed = parseLrc(lrcText)
                lyricsDao.insertOrUpdateLyrics(songId, parsed)
                return@withContext parsed
            }

            try {
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag
                val lyricsTag = tag?.getFirst(FieldKey.LYRICS)
                if (!lyricsTag.isNullOrBlank()) {
                    val parsed = parseLrc(lyricsTag)
                    lyricsDao.insertOrUpdateLyrics(songId, parsed)
                    return@withContext parsed
                }
            } catch (_: Exception) {
                // Ignore audio tag parsing errors
            }
        }

        null
    }

    override suspend fun saveLyrics(songId: String, lyrics: Lyrics) {
        lyricsDao.insertOrUpdateLyrics(songId, lyrics)
    }

    /**
     * Parses standard LRC formatted strings `[mm:ss.xx] Lyrics line` into synchronized timestamps.
     */
    private fun parseLrc(content: String): Lyrics {
        val lines = content.lines()
        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()

        for (line in lines) {
            val match = timestampRegex.matchEntire(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toIntOrNull() ?: 0
                val sec = match.groupValues[2].toIntOrNull() ?: 0
                val msPart = match.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toInt() * 10 else msPart.toInt()
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = match.groupValues[4].trim()

                syncedLines.add(
                    SyncedLine(
                        time = totalMs,
                        line = text
                    )
                )
            } else if (line.isNotBlank() && !line.startsWith("[")) {
                plainLines.add(line.trim())
            }
        }

        return if (syncedLines.isNotEmpty()) {
            Lyrics(synced = syncedLines, areFromRemote = false)
        } else {
            Lyrics(plain = plainLines.ifEmpty { lines.filter { it.isNotBlank() } }, areFromRemote = false)
        }
    }
}
