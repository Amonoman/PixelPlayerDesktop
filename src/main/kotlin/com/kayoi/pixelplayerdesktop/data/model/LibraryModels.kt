package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int = 0,
    val dateAdded: Long = 0L,
    val albumArtUriString: String? = null,
    val songCount: Int = 0,
    val albumArtist: String? = null
) {
    companion object {
        fun empty() = Album(
            id = -1,
            title = "",
            artist = "",
            dateAdded = 0,
            year = 0,
            albumArtUriString = null,
            songCount = 0,
            albumArtist = null
        )
    }
}

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val imageUrl: String? = null,
    val customImageUri: String? = null
) {
    companion object {
        fun empty() = Artist(
            id = -1,
            name = "",
            songCount = 0,
            imageUrl = null,
            customImageUri = null
        )
    }

    /** Returns the image URL/path to use, preferring the user's custom image. */
    val effectiveImageUrl: String?
        get() = customImageUri?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}
