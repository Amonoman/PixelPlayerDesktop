package com.kayoi.pixelplayerdesktop.platform.mediasession

object NoOpMediaSessionController : MediaSessionController {
    override fun updateMetadata(metadata: MediaMetadata) {}
    override fun updatePlaybackStatus(status: MediaPlaybackStatus) {}
    override fun updatePosition(positionMs: Long) {}
    override fun updateCapabilities(
        canGoNext: Boolean,
        canGoPrevious: Boolean,
        canPlay: Boolean,
        canPause: Boolean,
        canSeek: Boolean
    ) {}
    override fun updateLoopAndShuffle(loopStatus: String, shuffle: Boolean) {}
    override fun updateVolume(volume: Double) {}
    override fun release() {}
}
