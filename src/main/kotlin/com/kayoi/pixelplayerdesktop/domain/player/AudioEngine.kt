package com.kayoi.pixelplayerdesktop.domain.player

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

interface AudioEngine {
    val playbackState: StateFlow<PlaybackState>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val volume: StateFlow<Float> // 0.0f to 1.0f
    val isMuted: StateFlow<Boolean>
    val errorEvents: SharedFlow<String>
    val completionEvents: SharedFlow<Unit>

    fun play(mediaPathOrUri: String)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setMute(muted: Boolean)
    fun release()
}
