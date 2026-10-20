package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.utils.AudioFileReaderUtils
import org.jaudiotagger.tag.Tag
import java.io.File
import kotlin.math.pow

data class ReplayGainValues(
    val trackGainDb: Float? = null,
    val albumGainDb: Float? = null
)

class ReplayGainManager {

    private val cache = object : LinkedHashMap<String, ReplayGainValues?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, ReplayGainValues?>) = size > 200
    }

    companion object {
        private val TRACK_GAIN_KEYS = listOf(
            "REPLAYGAIN_TRACK_GAIN",
            "replaygain_track_gain",
            "REPLAYGAIN_TRACK_GAIN_DB",
            "R128_TRACK_GAIN"
        )

        private val ALBUM_GAIN_KEYS = listOf(
            "REPLAYGAIN_ALBUM_GAIN",
            "replaygain_album_gain",
            "REPLAYGAIN_ALBUM_GAIN_DB",
            "R128_ALBUM_GAIN"
        )
    }

    fun readReplayGain(filePath: String): ReplayGainValues? {
        if (filePath.isBlank()) return null

        synchronized(cache) {
            if (cache.containsKey(filePath)) {
                return cache[filePath]
            }
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return null

        return try {
            val audioFile = AudioFileReaderUtils.readAudioFile(file)
            val tag = audioFile.tag ?: return null

            val trackGain = extractGainValue(tag, TRACK_GAIN_KEYS)
            val albumGain = extractGainValue(tag, ALBUM_GAIN_KEYS)

            val values = if (trackGain != null || albumGain != null) {
                ReplayGainValues(trackGainDb = trackGain, albumGainDb = albumGain)
            } else null

            synchronized(cache) { cache[filePath] = values }
            values
        } catch (_: Exception) {
            synchronized(cache) { cache[filePath] = null }
            null
        }
    }

    fun getVolumeMultiplier(
        values: ReplayGainValues?,
        useAlbumGain: Boolean = false
    ): Float {
        if (values == null) return 1.0f

        val gainDb = if (useAlbumGain) {
            values.albumGainDb ?: values.trackGainDb
        } else {
            values.trackGainDb ?: values.albumGainDb
        } ?: return 1.0f

        val linear = 10.0.pow(gainDb / 20.0).toFloat()
        return linear.coerceIn(0.05f, 2.0f)
    }

    private fun extractGainValue(tag: Tag, keys: List<String>): Float? {
        for (key in keys) {
            val rawValue = try {
                val value = tag.getFirst(key)
                if (!value.isNullOrBlank()) value else null
            } catch (_: Exception) {
                null
            } ?: continue

            val parsed = parseGainString(rawValue)
            if (parsed != null) return parsed
        }
        return null
    }

    internal fun parseGainString(raw: String): Float? {
        val cleaned = raw.trim()
            .replace(Regex("(?i)db"), "")
            .trim()
        return cleaned.toFloatOrNull()
    }
}
