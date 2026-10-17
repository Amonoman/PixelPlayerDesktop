package com.kayoi.pixelplayerdesktop.data.network.navidrome.model

enum class NavidromeAuthMethod {
    TOKEN,
    PASSWORD;

    companion object {
        fun fromStorageKey(value: String?): NavidromeAuthMethod =
            entries.firstOrNull { it.name == value } ?: TOKEN
    }
}

data class NavidromeCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val authMethod: NavidromeAuthMethod = NavidromeAuthMethod.TOKEN
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
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

data class NavidromeSong(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String,
    val albumId: String,
    val albumArtist: String? = null,
    val durationMs: Long = 0L,
    val trackNumber: Int = 0,
    val discNumber: Int? = null,
    val year: Int = 0,
    val genre: String? = null,
    val coverArtId: String? = null,
    val path: String = "",
    val bitRate: Int? = null,
    val mimeType: String? = null,
    val dateAdded: Long = 0L
)

data class NavidromeAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val songCount: Int = 0,
    val coverArtId: String? = null,
    val year: Int = 0,
    val genre: String? = null
)

data class NavidromePlaylist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val durationMs: Long = 0L,
    val coverArtId: String? = null
)
