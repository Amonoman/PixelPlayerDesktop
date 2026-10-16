package com.kayoi.pixelplayerdesktop.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EqualizerPreset(
    val name: String,
    val displayName: String,
    val bandLevels: List<Float>,
    val preamp: Float = 0f,
    val isCustom: Boolean = false
) {
    companion object {
        const val NUM_BANDS = 10
        const val MIN_LEVEL = -15f
        const val MAX_LEVEL = 15f
        const val MIN_PREAMP = -15f
        const val MAX_PREAMP = 15f

        val BAND_FREQUENCIES = listOf("60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz", "14kHz", "16kHz")

        val FLAT = EqualizerPreset(
            name = "flat",
            displayName = "Flat",
            bandLevels = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )

        val ROCK = EqualizerPreset(
            name = "rock",
            displayName = "Rock",
            bandLevels = listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f)
        )

        val POP = EqualizerPreset(
            name = "pop",
            displayName = "Pop",
            bandLevels = listOf(-1f, 2f, 4f, 5f, 5f, 4f, 2f, 1f, 2f, 2f)
        )

        val HIP_HOP = EqualizerPreset(
            name = "hip_hop",
            displayName = "Hip Hop",
            bandLevels = listOf(6f, 8f, 4f, 1f, -1f, -1f, 1f, 1f, 3f, 4f)
        )

        val JAZZ = EqualizerPreset(
            name = "jazz",
            displayName = "Jazz",
            bandLevels = listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)
        )

        val CLASSICAL = EqualizerPreset(
            name = "classical",
            displayName = "Classical",
            bandLevels = listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 4f, 4f)
        )

        val ELECTRONIC = EqualizerPreset(
            name = "electronic",
            displayName = "Electronic",
            bandLevels = listOf(5f, 6f, 2f, 0f, -1f, 1f, 0f, 2f, 6f, 7f)
        )

        val BASS_BOOST = EqualizerPreset(
            name = "bass_boost",
            displayName = "Bass Boost",
            bandLevels = listOf(7f, 9f, 6f, 3f, 0f, 0f, 0f, 0f, 0f, 0f)
        )

        val TREBLE_BOOST = EqualizerPreset(
            name = "treble_boost",
            displayName = "Treble Boost",
            bandLevels = listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 6f, 8f, 9f)
        )

        val VOCAL = EqualizerPreset(
            name = "vocal",
            displayName = "Vocal",
            bandLevels = listOf(-3f, -2f, -1f, 2f, 5f, 6f, 5f, 3f, 1f, 0f)
        )

        val ALL_PRESETS = listOf(
            FLAT, ROCK, POP, HIP_HOP, JAZZ, CLASSICAL, ELECTRONIC, BASS_BOOST, TREBLE_BOOST, VOCAL
        )

        val DEFAULT_PINNED = ALL_PRESETS.map { it.name }

        fun custom(
            bandLevels: List<Float>,
            preamp: Float = 0f,
            name: String = "custom",
            displayName: String = "Custom"
        ) = EqualizerPreset(
            name = name,
            displayName = displayName,
            bandLevels = bandLevels,
            preamp = preamp,
            isCustom = true
        )

        fun fromName(name: String, customPresets: List<EqualizerPreset> = emptyList()): EqualizerPreset {
            return customPresets.find { it.name.equals(name, ignoreCase = true) }
                ?: ALL_PRESETS.find { it.name.equals(name, ignoreCase = true) }
                ?: FLAT
        }
    }
}
