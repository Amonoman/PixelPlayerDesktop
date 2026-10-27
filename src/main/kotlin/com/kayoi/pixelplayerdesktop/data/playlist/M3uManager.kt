package com.kayoi.pixelplayerdesktop.data.playlist

import java.io.File
import java.io.IOException
import java.io.Reader
import java.net.URLDecoder
import kotlin.math.abs
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song

internal const val MAX_M3U_CHARACTERS = 8 * 1024 * 1024

internal fun readM3uTextBounded(
    reader: Reader,
    maxCharacters: Int = MAX_M3U_CHARACTERS
): String {
    require(maxCharacters >= 0) { "maxCharacters must not be negative" }
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    val result = StringBuilder(minOf(maxCharacters, DEFAULT_BUFFER_SIZE))
    while (true) {
        val count = reader.read(buffer)
        if (count < 0) break
        if (count > maxCharacters - result.length) {
            throw IOException("Playlist file exceeds size limit")
        }
        result.append(buffer, 0, count)
    }
    return result.toString()
}

data class M3uParseResult(
    val playlistId: String?,
    val songIds: List<String>,
    val unresolvedEntries: List<String>,
    val ambiguousEntries: List<String>
)

private data class ExtInfMetadata(
    val durationSec: Long,
    val title: String,
    val artist: String
)

class M3uManager {

    fun parseM3uFile(file: File, allSongs: List<Song>): Pair<String, M3uParseResult> {
        val defaultName = file.nameWithoutExtension.ifBlank { "Imported Playlist" }
        val content = file.bufferedReader(Charsets.UTF_8).use(::readM3uTextBounded)
        val parseResult = parseContent(content, allSongs, baseDirectory = file.parentFile)
        return Pair(defaultName, parseResult)
    }

    fun generateM3uContent(playlist: Playlist, songs: List<Song>, targetFile: File? = null): String {
        return generateContent(playlist, songs, targetFile)
    }

    companion object {
        private const val PLAYLIST_ID_MARKER = "#PIXELPLAYER-PLAYLIST-ID:"
        private const val EXTINF_PREFIX = "#EXTINF:"

        fun parseContent(
            content: String,
            songs: List<Song>,
            baseDirectory: File? = null
        ): M3uParseResult {
            val isWindows = System.getProperty("os.name")?.lowercase()?.contains("windows") == true

            fun normalizePath(p: String): String {
                var clean = p.trim().replace('\\', '/')
                if (clean.startsWith("file://", ignoreCase = true)) {
                    clean = clean.substring(7).removePrefix("localhost")
                    if (clean.length >= 3 && clean[0] == '/' && clean[1].isLetter() && clean[2] == ':') {
                        clean = clean.substring(1)
                    }
                }
                if (clean.contains('%')) {
                    clean = runCatching { URLDecoder.decode(clean, "UTF-8") }.getOrDefault(clean)
                }
                return if (isWindows) clean.lowercase() else clean
            }

            val exactMatches = songs
                .flatMap { song ->
                    listOf(song.path, song.contentUriString)
                        .filter(String::isNotBlank)
                        .map { entry -> normalizePath(entry) to song }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, matches) -> matches.distinctBy(Song::id) }

            val basenameMatches = songs
                .flatMap { song ->
                    listOf(song.path, song.contentUriString)
                        .filter(String::isNotBlank)
                        .map { entry -> normalizePath(entry).substringAfterLast('/') to song }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, matches) -> matches.distinctBy(Song::id) }

            val songsByTitle = songs
                .filter { it.title.isNotBlank() }
                .groupBy { it.title.lowercase() }

            var playlistId: String? = null
            val resolvedIds = mutableListOf<String>()
            val unresolved = mutableListOf<String>()
            val ambiguous = mutableListOf<String>()

            var lastExtInf: ExtInfMetadata? = null

            content.lineSequence().forEach { rawLine ->
                val line = rawLine.removePrefix("\uFEFF").trim()
                if (line.isBlank()) return@forEach

                if (line.startsWith(PLAYLIST_ID_MARKER)) {
                    playlistId = line.removePrefix(PLAYLIST_ID_MARKER)
                        .trim()
                        .takeIf { it.isNotBlank() && it.length <= 200 }
                    return@forEach
                }

                if (line.startsWith(EXTINF_PREFIX)) {
                    lastExtInf = parseExtInf(line.removePrefix(EXTINF_PREFIX))
                    return@forEach
                }

                if (line.startsWith('#')) return@forEach

                val normalizedLine = normalizePath(line)
                var resolvedSong: Song? = null

                val exact = exactMatches[normalizedLine].orEmpty()
                if (exact.size == 1) {
                    resolvedSong = exact.single()
                } else if (exact.size > 1) {
                    ambiguous += line
                    lastExtInf = null
                    return@forEach
                }

                if (resolvedSong == null && baseDirectory != null) {
                    val relativeCandidate = File(baseDirectory, line)
                    val normalizedRelative = runCatching { normalizePath(relativeCandidate.canonicalPath) }.getOrNull()
                    if (normalizedRelative != null) {
                        val relativeMatches = exactMatches[normalizedRelative].orEmpty()
                        if (relativeMatches.size == 1) {
                            resolvedSong = relativeMatches.single()
                        } else if (relativeMatches.size > 1) {
                            ambiguous += line
                            lastExtInf = null
                            return@forEach
                        }
                    }
                }

                if (resolvedSong == null) {
                    val basename = normalizedLine.substringAfterLast('/')
                    val candidates = basenameMatches[basename].orEmpty()
                    when {
                        candidates.size == 1 -> resolvedSong = candidates.single()
                        candidates.size > 1 -> {
                            val disambiguated = disambiguateByMetadata(candidates, lastExtInf)
                            if (disambiguated != null) {
                                resolvedSong = disambiguated
                            } else {
                                ambiguous += line
                                lastExtInf = null
                                return@forEach
                            }
                        }
                    }
                }

                if (resolvedSong == null && lastExtInf != null) {
                    val metadataMatch = findByMetadata(songsByTitle, lastExtInf)
                    if (metadataMatch != null) {
                        resolvedSong = metadataMatch
                    }
                }

                if (resolvedSong != null) {
                    resolvedIds += resolvedSong.id
                } else {
                    unresolved += line
                }

                lastExtInf = null
            }

            return M3uParseResult(
                playlistId = playlistId,
                songIds = resolvedIds.toList(),
                unresolvedEntries = unresolved,
                ambiguousEntries = ambiguous
            )
        }

        fun generateContent(
            playlist: Playlist,
            songs: List<Song>,
            targetFile: File? = null
        ): String {
            val sb = StringBuilder()
            sb.append("#EXTM3U\n")
            sb.append(PLAYLIST_ID_MARKER).append(playlist.id).append('\n')

            val baseDir = targetFile?.parentFile
            for (song in songs) {
                sb.append("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n")
                val location = when {
                    baseDir != null && song.path.isNotBlank() -> {
                        try {
                            File(song.path).relativeTo(baseDir).path.replace('\\', '/')
                        } catch (e: IllegalArgumentException) {
                            song.path.replace('\\', '/')
                        }
                    }
                    song.path.isNotBlank() -> song.path.replace('\\', '/')
                    else -> song.contentUriString
                }
                if (location.isNotBlank()) {
                    sb.append(location).append('\n')
                }
            }
            return sb.toString()
        }

        private fun parseExtInf(raw: String): ExtInfMetadata? {
            val commaIndex = raw.indexOf(',')
            if (commaIndex == -1) return null

            val durationPart = raw.substring(0, commaIndex).trim()
            val durationSec = durationPart.toLongOrNull() ?: 0L

            val infoPart = raw.substring(commaIndex + 1).trim()
            val hyphenIndex = infoPart.indexOf(" - ")
            val (artist, title) = if (hyphenIndex != -1) {
                val artistStr = infoPart.substring(0, hyphenIndex).trim()
                val titleStr = infoPart.substring(hyphenIndex + 3).trim()
                Pair(artistStr, titleStr)
            } else {
                Pair("", infoPart)
            }

            return ExtInfMetadata(durationSec, title, artist)
        }

        private fun disambiguateByMetadata(candidates: List<Song>, extInf: ExtInfMetadata?): Song? {
            if (extInf == null) return null
            val matches = candidates.filter { song ->
                val titleMatches = extInf.title.isNotBlank() && song.title.equals(extInf.title, ignoreCase = true)
                val artistMatches = extInf.artist.isBlank() || song.artist.contains(extInf.artist, ignoreCase = true)
                val durationMatches = extInf.durationSec <= 0L || abs((song.duration / 1000) - extInf.durationSec) <= 3
                titleMatches && artistMatches && durationMatches
            }
            return if (matches.size == 1) matches.single() else null
        }

        private fun findByMetadata(songsByTitle: Map<String, List<Song>>, extInf: ExtInfMetadata): Song? {
            if (extInf.title.isBlank()) return null
            val candidates = songsByTitle[extInf.title.lowercase()].orEmpty()
            val matches = candidates.filter { song ->
                val artistMatches = extInf.artist.isBlank() || song.artist.contains(extInf.artist, ignoreCase = true)
                val durationMatches = extInf.durationSec <= 0L || abs((song.duration / 1000) - extInf.durationSec) <= 3
                artistMatches && durationMatches
            }
            return if (matches.size == 1) matches.single() else null
        }
    }
}
