package com.kayoi.pixelplayerdesktop.utils

import com.kayoi.pixelplayerdesktop.data.model.Song
import java.io.File
import java.util.Locale

object AudioMetaUtils {

    fun normalizeBitrateKbps(bitrate: Int?): Int? {
        if (bitrate == null || bitrate <= 0) return null
        return if (bitrate > 10_000) bitrate / 1000 else bitrate
    }

    fun formatBitrate(bitrate: Int?): String? {
        val kbps = normalizeBitrateKbps(bitrate) ?: return null
        return "$kbps kbps"
    }

    fun formatSampleRate(sampleRate: Int?): String? {
        if (sampleRate == null || sampleRate <= 0) return null
        return String.format(Locale.US, "%.1f kHz", sampleRate / 1000.0)
    }

    fun mimeTypeToFormat(mimeType: String?): String {
        val normalized = mimeType
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.substringBefore(';')
            ?: return "-"

        if (normalized.isBlank()) return "-"

        return when {
            normalized == "audio/mpeg" ||
                normalized == "audio/mp3" ||
                normalized == "audio/x-mp3" ||
                normalized == "audio/mpeg3" -> "mp3"

            normalized == "audio/flac" ||
                normalized == "audio/x-flac" -> "flac"

            normalized == "audio/wav" ||
                normalized == "audio/x-wav" ||
                normalized == "audio/wave" ||
                normalized == "audio/vnd.wave" -> "wav"

            normalized == "audio/ogg" ||
                normalized == "application/ogg" ||
                normalized == "audio/vorbis" ||
                normalized == "audio/x-vorbis" -> "ogg"

            normalized == "audio/opus" ||
                normalized == "audio/x-opus" -> "opus"

            normalized == "audio/mp4" ||
                normalized == "audio/m4a" ||
                normalized == "audio/x-m4a" ||
                normalized == "audio/mp4a-latm" -> "m4a"

            normalized == "audio/aac" ||
                normalized == "audio/aacp" -> "aac"

            normalized == "audio/alac" ||
                normalized == "audio/x-alac" -> "alac"

            normalized == "audio/aiff" ||
                normalized == "audio/x-aiff" ||
                normalized == "audio/aif" ||
                normalized == "audio/x-aifc" -> "aiff"

            normalized == "audio/x-ms-wma" ||
                normalized == "audio/wma" -> "wma"

            normalized.contains("flac") -> "flac"
            normalized.contains("opus") -> "opus"
            normalized.contains("vorbis") || normalized.contains("ogg") -> "ogg"
            normalized.contains("wav") || normalized.contains("wave") -> "wav"
            normalized.contains("aac") -> "aac"
            normalized.contains("mp4a") || normalized.contains("m4a") -> "m4a"
            normalized.contains("mpeg") || normalized.contains("mp3") -> "mp3"
            normalized.contains("alac") -> "alac"
            normalized.contains("aiff") || normalized.contains("aif") -> "aiff"
            normalized.contains("wma") -> "wma"
            normalized.startsWith("audio/") -> normalized.substringAfter("audio/").ifBlank { "-" }
            else -> "-"
        }
    }

    fun formatAudioMetaLabel(
        mimeType: String?,
        bitrate: Int?,
        sampleRate: Int?,
        path: String? = null
    ): String {
        val detectedFormat = mimeTypeToFormat(mimeType)
            .takeIf { it != "-" }
            ?: path?.let { File(it).extension.takeIf { ext -> ext.isNotBlank() } }

        val formatLabel = detectedFormat?.uppercase(Locale.getDefault())

        val parts = buildList {
            formatSampleRate(sampleRate)?.let { add(it) }
            normalizeBitrateKbps(bitrate)?.let { kbps ->
                val kbpsLabel = "$kbps kbps"
                if (formatLabel != null) {
                    add("$kbpsLabel • $formatLabel")
                } else {
                    add(kbpsLabel)
                }
            } ?: formatLabel?.let { add(it) }
        }
        return parts.joinToString(" • ")
    }

    fun formatAudioMetaLabel(song: Song): String {
        return formatAudioMetaLabel(
            mimeType = song.mimeType,
            bitrate = song.bitrate,
            sampleRate = song.sampleRate,
            path = song.path
        )
    }
}
