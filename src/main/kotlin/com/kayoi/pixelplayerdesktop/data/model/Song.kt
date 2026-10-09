package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ArtistRef(
    val id: Long,
    val name: String,
    val isPrimary: Boolean = false
)

@Immutable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: Long = 0L,
    val artists: List<ArtistRef> = emptyList(),
    val album: String = "",
    val albumId: Long = 0L,
    val albumArtist: String? = null,
    val path: String = "",
    val contentUriString: String = "",
    val albumArtUriString: String? = null,
    val duration: Long = 0L,
    val genre: String? = null,
    val lyrics: String? = null,
    val isFavorite: Boolean = false,
    val trackNumber: Int = 0,
    val discNumber: Int? = null,
    val year: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val navidromeId: String? = null,
    val jellyfinId: String? = null
) {
    val displayArtist: String
        get() {
            if (artists.isNotEmpty()) {
                return artists.sortedByDescending { it.isPrimary }.joinToString(", ") { it.name }
            }
            return artist
        }
}
