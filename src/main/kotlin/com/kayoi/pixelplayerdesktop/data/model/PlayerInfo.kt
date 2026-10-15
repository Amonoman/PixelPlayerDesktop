package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class QueueItem(
    val id: Long,
    val albumArtUri: String? = null
)

@Immutable
@Serializable
data class PlayerInfo(
    val songTitle: String = "",
    val artistName: String = "",
    val isPlaying: Boolean = false,
    val albumArtUri: String? = null,
    val albumArtBitmapData: ByteArray? = null,
    val totalDurationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val lyrics: Lyrics? = null,
    val isLoadingLyrics: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerInfo

        if (songTitle != other.songTitle) return false
        if (artistName != other.artistName) return false
        if (isPlaying != other.isPlaying) return false
        if (albumArtUri != other.albumArtUri) return false
        if (albumArtBitmapData != null) {
            if (other.albumArtBitmapData == null) return false
            if (!albumArtBitmapData.contentEquals(other.albumArtBitmapData)) return false
        } else if (other.albumArtBitmapData != null) return false
        if (totalDurationMs != other.totalDurationMs) return false
        if (isFavorite != other.isFavorite) return false
        if (queue != other.queue) return false
        if (lyrics != other.lyrics) return false
        if (isLoadingLyrics != other.isLoadingLyrics) return false
        if (isShuffleEnabled != other.isShuffleEnabled) return false
        if (repeatMode != other.repeatMode) return false
        if (volume != other.volume) return false
        if (isMuted != other.isMuted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songTitle.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + (albumArtUri?.hashCode() ?: 0)
        result = 31 * result + (albumArtBitmapData?.contentHashCode() ?: 0)
        result = 31 * result + totalDurationMs.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + queue.hashCode()
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + isLoadingLyrics.hashCode()
        result = 31 * result + isShuffleEnabled.hashCode()
        result = 31 * result + repeatMode
        result = 31 * result + volume.hashCode()
        result = 31 * result + isMuted.hashCode()
        return result
    }
}
