package com.kayoi.pixelplayerdesktop.platform.mediasession

interface MediaSessionController {
    fun updateMetadata(metadata: MediaMetadata)
    fun updatePlaybackStatus(status: MediaPlaybackStatus)
    fun updatePosition(positionMs: Long)
    fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    )
    fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean)
    fun updateVolume(volume: Double)
    fun release()
}
