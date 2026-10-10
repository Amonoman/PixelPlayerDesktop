package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val imageUrl: String? = null,
    val customImageUri: String? = null
) {
    /** Returns the image URL/path to use, preferring the user's custom image. */
    val effectiveImageUrl: String?
        get() = customImageUri?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}
