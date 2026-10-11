package com.kayoi.pixelplayerdesktop.utils

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.model.SyncedLine

object LrcParser {
    private val timestampRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun parse(content: String): Lyrics {
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
