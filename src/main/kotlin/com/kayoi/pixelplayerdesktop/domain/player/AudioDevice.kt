package com.kayoi.pixelplayerdesktop.domain.player

import kotlinx.serialization.Serializable

@Serializable
data class AudioDevice(
    val name: String,
    val description: String
)
