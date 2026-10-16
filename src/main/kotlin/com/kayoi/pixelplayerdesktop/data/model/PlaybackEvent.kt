package com.kayoi.pixelplayerdesktop.data.model

data class PlaybackEvent(
    val id: Long = 0L,
    val songId: String,
    val timestamp: Long,
    val durationMs: Long = 0L
)
