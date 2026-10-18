package com.kayoi.pixelplayerdesktop.platform.mediasession

data class MediaMetadata(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val artUri: String? = null,
    val durationMs: Long = 0L
)
