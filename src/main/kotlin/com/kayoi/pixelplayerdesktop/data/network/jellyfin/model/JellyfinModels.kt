package com.kayoi.pixelplayerdesktop.data.network.jellyfin.model

data class JellyfinCredentials(
    val serverUrl: String,
    val username: String,
    val accessToken: String? = null,
    val userId: String? = null
) {
    val normalizedServerUrl: String
        get() {
            val trimmed = serverUrl.trim().trimEnd('/')
            return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }

    val isValid: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
}

data class JellyfinSong(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String? = null,
    val album: String,
    val albumId: String? = null,
    val albumArtist: String? = null,
    val durationMs: Long = 0L,
    val trackNumber: Int = 0,
    val discNumber: Int? = null,
    val year: Int = 0,
    val genre: String? = null,
    val container: String? = null
)

data class JellyfinPlaylist(
    val id: String,
    val name: String,
    val songCount: Int = 0
)
