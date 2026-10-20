package com.kayoi.pixelplayerdesktop.utils

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine
import com.kayoi.pixelplayerdesktop.data.model.SyncedWord

object LrcParser {
    private val timestampRegex = Regex("""^\[(\d{2}):(\d{2})[.:](\d{2,3})\](.*)""")
    private val wordTagRegex = Regex("""<\d{2}:\d{2}[.:]\d{2,3}>""")
    private val wordSplitRegex = Regex("""(?=<\d{2}:\d{2}[.:]\d{2,3}>)""")
    private val wordMatchRegex = Regex("""^<(\d{2}):(\d{2})[.:](\d{2,3})>(.*)""")

    fun parse(content: String): Lyrics {
        val lines = content.lines()
        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        val entireLyricsHasKana = content.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }

        for (line in lines) {
            val trimmedLine = line.trim()
            val match = timestampRegex.matchEntire(trimmedLine)
            if (match != null) {
                val min = match.groupValues[1].toIntOrNull() ?: 0
                val sec = match.groupValues[2].toIntOrNull() ?: 0
                val msPart = match.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toInt() * 10 else msPart.toInt()
                val totalMs = (min * 60 + sec) * 1000 + ms
                val rawText = match.groupValues[4]

                val hasWordTags = rawText.contains(wordTagRegex)
                val displayText = if (hasWordTags) wordTagRegex.replace(rawText, "").trim() else rawText.trim()
                val words = if (hasWordTags) parseWords(rawText, totalMs) else null

                val romanized = when {
                    MultiLangRomanizer.isJapanese(displayText, entireLyricsHasKana) -> MultiLangRomanizer.romanizeJapanese(displayText)
                    MultiLangRomanizer.isChinese(displayText) -> MultiLangRomanizer.romanizeChinese(displayText)
                    MultiLangRomanizer.isKorean(displayText) -> MultiLangRomanizer.romanizeKorean(displayText)
                    MultiLangRomanizer.isHindi(displayText) -> MultiLangRomanizer.romanizeHindi(displayText)
                    MultiLangRomanizer.isPunjabi(displayText) -> MultiLangRomanizer.romanizePunjabi(displayText)
                    MultiLangRomanizer.isCyrillic(displayText) -> MultiLangRomanizer.romanizeCyrillic(displayText)
                    else -> null
                }?.let { MultiLangRomanizer.capitalizeFirstLetter(it) }?.trim()

                syncedLines.add(
                    SyncedLine(
                        time = totalMs,
                        line = displayText,
                        words = words?.takeIf { it.isNotEmpty() },
                        romanization = romanized
                    )
                )
            } else if (trimmedLine.isNotBlank() && !trimmedLine.startsWith("[")) {
                plainLines.add(trimmedLine)
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

    private fun parseWords(rawText: String, lineStartMs: Int): List<SyncedWord> {
        val words = mutableListOf<SyncedWord>()
        val parts = rawText.split(wordSplitRegex)
        var pendingWordBoundary = false

        for (part in parts) {
            if (part.isEmpty()) continue
            val wordMatch = wordMatchRegex.matchEntire(part)
            if (wordMatch != null) {
                val min = wordMatch.groupValues[1].toIntOrNull() ?: 0
                val sec = wordMatch.groupValues[2].toIntOrNull() ?: 0
                val msPart = wordMatch.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toInt() * 10 else msPart.toInt()
                val wordTimestamp = (min * 60 + sec) * 1000 + ms
                val wordTextRaw = wordMatch.groupValues[4]
                    .substringBefore('\n')
                    .substringBefore('\r')
                    .substringBefore("\\n")
                    .substringBefore("\\r")

                val startsNewWord = words.isEmpty() ||
                    pendingWordBoundary ||
                    wordTextRaw.firstOrNull()?.isWhitespace() == true
                val trimmed = wordTextRaw.trim()
                pendingWordBoundary = wordTextRaw.lastOrNull()?.isWhitespace() == true

                if (trimmed.isNotEmpty()) {
                    words.add(
                        SyncedWord(
                            time = wordTimestamp,
                            word = trimmed,
                            startsNewWord = startsNewWord
                        )
                    )
                }
            } else {
                if (words.isEmpty()) {
                    val leading = part.trim()
                    if (leading.isNotEmpty()) {
                        words.add(
                            SyncedWord(
                                time = lineStartMs,
                                word = leading,
                                startsNewWord = true
                            )
                        )
                    }
                }
                if (part.any { it.isWhitespace() }) {
                    pendingWordBoundary = true
                }
            }
        }
        return words
    }

    fun toLrcString(lyrics: Lyrics, preferSynced: Boolean = true): String {
        if (preferSynced && !lyrics.synced.isNullOrEmpty()) {
            return lyrics.synced.joinToString("\n") { line ->
                val totalMs = line.time
                val minutes = totalMs / 60000
                val seconds = (totalMs % 60000) / 1000
                val hundredths = (totalMs % 1000) / 10
                val timestamp = "[%02d:%02d.%02d]".format(minutes, seconds, hundredths)
                if (!line.words.isNullOrEmpty()) {
                    val wordsStr = line.words.joinToString(separator = "") { word ->
                        val wMin = word.time / 60000
                        val wSec = (word.time % 60000) / 1000
                        val wHun = (word.time % 1000) / 10
                        val wTimestamp = "<%02d:%02d.%02d>".format(wMin, wSec, wHun)
                        val prefix = if (word.startsNewWord && word != line.words.first()) " " else ""
                        "$prefix$wTimestamp${word.word}"
                    }
                    "$timestamp$wordsStr"
                } else {
                    "$timestamp${line.line}"
                }
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
