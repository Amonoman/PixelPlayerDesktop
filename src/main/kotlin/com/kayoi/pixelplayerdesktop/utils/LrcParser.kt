package com.kayoi.pixelplayerdesktop.utils

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine

object LrcParser {
    private val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun parse(content: String): Lyrics {
        val lines = content.lines()
        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        val entireLyricsHasKana = content.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }

        for (line in lines) {
            val match = timestampRegex.matchEntire(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toIntOrNull() ?: 0
                val sec = match.groupValues[2].toIntOrNull() ?: 0
                val msPart = match.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toInt() * 10 else msPart.toInt()
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = match.groupValues[4].trim()

                val romanized = when {
                    MultiLangRomanizer.isJapanese(text, entireLyricsHasKana) -> MultiLangRomanizer.romanizeJapanese(text)
                    MultiLangRomanizer.isChinese(text) -> MultiLangRomanizer.romanizeChinese(text)
                    MultiLangRomanizer.isKorean(text) -> MultiLangRomanizer.romanizeKorean(text)
                    MultiLangRomanizer.isHindi(text) -> MultiLangRomanizer.romanizeHindi(text)
                    MultiLangRomanizer.isPunjabi(text) -> MultiLangRomanizer.romanizePunjabi(text)
                    MultiLangRomanizer.isCyrillic(text) -> MultiLangRomanizer.romanizeCyrillic(text)
                    else -> null
                }?.let { MultiLangRomanizer.capitalizeFirstLetter(it) }?.trim()

                syncedLines.add(
                    SyncedLine(
                        time = totalMs,
                        line = text,
                        romanization = romanized
                    )
                )
            } else if (line.isNotBlank() && !line.startsWith("[")) {
                plainLines.add(line.trim())
            }
        }

        return if (syncedLines.isNotEmpty()) {
            val plainVersion = syncedLines.map { line ->
                buildString {
                    append(line.line)
                    if (!line.romanization.isNullOrEmpty()) append("\n").append(line.romanization)
                }
            }
            Lyrics(synced = syncedLines, plain = plainVersion, areFromRemote = false)
        } else {
            val targetPlain = plainLines.ifEmpty { lines.filter { it.isNotBlank() } }
            val processedPlain = targetPlain.map { line ->
                val romanized = when {
                    MultiLangRomanizer.isJapanese(line, entireLyricsHasKana) -> MultiLangRomanizer.romanizeJapanese(line)
                    MultiLangRomanizer.isChinese(line) -> MultiLangRomanizer.romanizeChinese(line)
                    MultiLangRomanizer.isKorean(line) -> MultiLangRomanizer.romanizeKorean(line)
                    MultiLangRomanizer.isHindi(line) -> MultiLangRomanizer.romanizeHindi(line)
                    MultiLangRomanizer.isPunjabi(line) -> MultiLangRomanizer.romanizePunjabi(line)
                    MultiLangRomanizer.isCyrillic(line) -> MultiLangRomanizer.romanizeCyrillic(line)
                    else -> null
                }?.let { MultiLangRomanizer.capitalizeFirstLetter(it) }?.trim()

                if (!romanized.isNullOrEmpty()) "$line\n$romanized" else line
            }
            Lyrics(plain = processedPlain, areFromRemote = false)
        }
    }

    fun toLrcString(lyrics: Lyrics, preferSynced: Boolean = true): String {
        if (preferSynced && !lyrics.synced.isNullOrEmpty()) {
            return lyrics.synced.joinToString("\n") { line ->
                val totalMs = line.time
                val minutes = totalMs / 60000
                val seconds = (totalMs % 60000) / 1000
                val hundredths = (totalMs % 1000) / 10
                val timestamp = "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
                "$timestamp${line.line}"
            }
        } else if (!lyrics.plain.isNullOrEmpty()) {
            return lyrics.plain.joinToString("\n") { it.substringBefore("\n") }
        } else if (!lyrics.synced.isNullOrEmpty()) {
            return lyrics.synced.joinToString("\n") { line ->
                val totalMs = line.time
                val minutes = totalMs / 60000
                val seconds = (totalMs % 60000) / 1000
                val hundredths = (totalMs % 1000) / 10
                val timestamp = "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
                "$timestamp${line.line}"
            }
        }
        return ""
    }
}
