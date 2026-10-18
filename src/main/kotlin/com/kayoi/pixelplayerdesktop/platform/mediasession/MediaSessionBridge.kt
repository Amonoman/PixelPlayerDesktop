package com.kayoi.pixelplayerdesktop.platform.mediasession

import com.kayoi.pixelplayerdesktop.domain.player.PlaybackState
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlaybackStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MediaSessionBridge(
    private val playbackStateHolder: PlaybackStateHolder,
    private val coroutineScope: CoroutineScope,
    controllerFactory: (MediaSessionCommands, CoroutineScope) -> MediaSessionController = { cmd, scope ->
        MediaSessionFactory.create(cmd, scope)
    }
) : MediaSessionCommands {

    private val controller: MediaSessionController = controllerFactory(this, coroutineScope)

    init {
        startObserving()
    }

    private fun startObserving() {
        coroutineScope.launch {
            combine(
                playbackStateHolder.currentSong,
                playbackStateHolder.playerInfo
            ) { song, info ->
                if (song != null) {
                    val duration = if (song.duration > 0) song.duration else info.totalDurationMs
                    MediaMetadata(
                        trackId = song.id,
                        title = song.title.ifBlank { info.songTitle },
                        artist = song.displayArtist.ifBlank { info.artistName },
                        album = song.album,
                        albumArtist = song.albumArtist ?: "",
                        artUri = song.albumArtUriString ?: info.albumArtUri,
                        durationMs = duration
                    )
                } else if (info.songTitle.isNotBlank()) {
                    MediaMetadata(
                        trackId = "current",
                        title = info.songTitle,
                        artist = info.artistName,
                        album = "",
                        albumArtist = "",
                        artUri = info.albumArtUri,
                        durationMs = info.totalDurationMs
                    )
                } else {
                    MediaMetadata()
                }
            }.distinctUntilChanged().collectLatest { metadata ->
                controller.updateMetadata(metadata)
            }
        }

        coroutineScope.launch {
            playbackStateHolder.playbackState.collectLatest { state ->
                val status = when (state) {
                    PlaybackState.PLAYING, PlaybackState.BUFFERING -> MediaPlaybackStatus.PLAYING
                    PlaybackState.PAUSED -> MediaPlaybackStatus.PAUSED
                    PlaybackState.IDLE, PlaybackState.READY, PlaybackState.ENDED, PlaybackState.ERROR -> MediaPlaybackStatus.STOPPED
                }
                controller.updatePlaybackStatus(status)
            }
        }

        coroutineScope.launch {
            playbackStateHolder.currentPositionMs.collectLatest { positionMs ->
                controller.updatePosition(positionMs)
            }
        }

        coroutineScope.launch {
            combine(
                playbackStateHolder.queue,
                playbackStateHolder.currentSong,
                playbackStateHolder.playbackState
            ) { queue, song, state ->
                val hasTracks = queue.isNotEmpty() || song != null
                val isPlaying = state == PlaybackState.PLAYING
                val canSeek = song != null && (song.duration > 0 || playbackStateHolder.playerInfo.value.totalDurationMs > 0)
                Capabilities(
                    canGoNext = hasTracks,
                    canGoPrevious = hasTracks,
                    canPlay = hasTracks,
                    canPause = isPlaying,
                    canSeek = canSeek
                )
            }.distinctUntilChanged().collectLatest { caps ->
                controller.updateCapabilities(
                    canGoNext = caps.canGoNext,
                    canGoPrevious = caps.canGoPrevious,
                    canPlay = caps.canPlay,
                    canPause = caps.canPause,
                    canSeek = caps.canSeek
                )
            }
        }

        coroutineScope.launch {
            playbackStateHolder.playerInfo.collectLatest { info ->
                val loop = when (info.repeatMode) {
                    1 -> "Playlist"
                    2 -> "Track"
                    else -> "None"
                }
                controller.updateLoopAndShuffle(loop, info.isShuffleEnabled)
                val vol = if (info.isMuted) 0.0 else info.volume.toDouble()
                controller.updateVolume(vol)
            }
        }
    }

    override fun play() {
        if (playbackStateHolder.playbackState.value != PlaybackState.PLAYING) {
            playbackStateHolder.togglePlayPause()
        }
    }

    override fun pause() {
        if (playbackStateHolder.playbackState.value == PlaybackState.PLAYING) {
            playbackStateHolder.togglePlayPause()
        }
    }

    override fun playPause() {
        playbackStateHolder.togglePlayPause()
    }

    override fun next() {
        playbackStateHolder.playNext()
    }

    override fun previous() {
        playbackStateHolder.playPrevious()
    }

    override fun stop() {
        playbackStateHolder.stopPlayback()
    }

    override fun seek(offsetMs: Long) {
        playbackStateHolder.seekRelative(offsetMs)
    }

    override fun setPosition(positionMs: Long) {
        playbackStateHolder.seekTo(positionMs)
    }

    fun release() {
        controller.release()
    }

    private data class Capabilities(
        val canGoNext: Boolean,
        val canGoPrevious: Boolean,
        val canPlay: Boolean,
        val canPause: Boolean,
        val canSeek: Boolean
    )
}
