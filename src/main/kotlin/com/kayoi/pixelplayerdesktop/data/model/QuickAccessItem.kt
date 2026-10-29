package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class QuickAccessType {
    SMART_PRESET,
    ALBUM,
    ARTIST,
    PLAYLIST
}

@Serializable
@Immutable
data class QuickAccessItem(
    val id: String,
    val title: String,
    val type: QuickAccessType,
    val targetKey: String,
    val isEnabled: Boolean = true,
    val order: Int = 0
) {
    companion object {
        const val MAX_PINNED_CUSTOM_ITEMS = 10
        const val MAX_RECENTLY_ADDED_SONGS = 50
        const val MAX_MOST_LISTENED_SONGS = 50

        const val PRESET_RECENTLY_ADDED = "recently_added"
        const val PRESET_MOST_LISTENED = "most_listened"
        const val PRESET_RECENTLY_PLAYED = "recently_played"

        val DEFAULT_ITEMS: List<QuickAccessItem> = listOf(
            QuickAccessItem(
                id = PRESET_RECENTLY_ADDED,
                title = "Recently Added",
                type = QuickAccessType.SMART_PRESET,
                targetKey = PRESET_RECENTLY_ADDED,
                isEnabled = true,
                order = 0
            ),
            QuickAccessItem(
                id = PRESET_MOST_LISTENED,
                title = "Most Listened",
                type = QuickAccessType.SMART_PRESET,
                targetKey = PRESET_MOST_LISTENED,
                isEnabled = true,
                order = 1
            ),
            QuickAccessItem(
                id = PRESET_RECENTLY_PLAYED,
                title = "Recently Played",
                type = QuickAccessType.SMART_PRESET,
                targetKey = PRESET_RECENTLY_PLAYED,
                isEnabled = true,
                order = 2
            )
        )
    }
}
