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
)
