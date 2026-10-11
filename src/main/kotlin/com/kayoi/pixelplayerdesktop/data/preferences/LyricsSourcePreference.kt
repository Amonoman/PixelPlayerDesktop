package com.kayoi.pixelplayerdesktop.data.preferences

enum class LyricsSourcePreference(val storageKey: String, val label: String) {
    EMBEDDED_FIRST("embedded_first", "Embedded First"),
    API_FIRST("api_first", "Online First"),
    LOCAL_FIRST("local_first", "Local First");

    companion object {
        fun fromStorageKey(key: String?): LyricsSourcePreference {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: EMBEDDED_FIRST
        }
    }
}
