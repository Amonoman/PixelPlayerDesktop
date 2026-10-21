package com.kayoi.pixelplayerdesktop.data.backup.model

enum class BackupSection(
    val key: String,
    val label: String,
    val description: String,
    val sinceVersion: Int = 1
) {
    PLAYLISTS(
        key = "playlists",
        label = "Playlists",
        description = "Your custom playlists and ordering preferences."
    ),
    GLOBAL_SETTINGS(
        key = "global_settings",
        label = "Global Settings",
        description = "Themes, behavior, playback, and app preferences."
    ),
    FAVORITES(
        key = "favorites",
        label = "Favorites",
        description = "Songs marked as favorite."
    ),
    LYRICS(
        key = "lyrics",
        label = "Saved Lyrics",
        description = "Lyrics you've saved or cached."
    ),
    SEARCH_HISTORY(
        key = "search_history",
        label = "Search History",
        description = "Recent search terms in the app."
    ),
    TRANSITIONS(
        key = "transitions",
        label = "Transition Rules",
        description = "Custom transition settings between songs."
    ),
    ENGAGEMENT_STATS(
        key = "engagement_stats",
        label = "Engagement Stats",
        description = "Play count and listening duration per song."
    ),
    PLAYBACK_HISTORY(
        key = "playback_history",
        label = "Playback History",
        description = "Timeline-based listening history for stats."
    ),
    QUICK_FILL(
        key = "quick_fill",
        label = "QuickFill Genres",
        description = "Custom genres and their icons.",
        sinceVersion = 3
    ),
    ARTIST_IMAGES(
        key = "artist_images",
        label = "Artist Images",
        description = "Cached artist images and custom artwork.",
        sinceVersion = 3
    ),
    EQUALIZER(
        key = "equalizer",
        label = "Equalizer",
        description = "Your custom equalizer presets and audio profiles.",
        sinceVersion = 3
    ),
    AUDIO_BOOKMARKS(
        key = "audio_bookmarks",
        label = "Audio Bookmarks",
        description = "Saved timestamps inside songs and audiobooks.",
        sinceVersion = 4
    );

    companion object {
        val defaultSelection: Set<BackupSection> = setOf(
            PLAYLISTS,
            GLOBAL_SETTINGS,
            FAVORITES,
            LYRICS,
            ENGAGEMENT_STATS,
            PLAYBACK_HISTORY,
            ARTIST_IMAGES,
            EQUALIZER
        )

        fun fromKey(key: String): BackupSection? = entries.find { it.key == key }
    }
}
