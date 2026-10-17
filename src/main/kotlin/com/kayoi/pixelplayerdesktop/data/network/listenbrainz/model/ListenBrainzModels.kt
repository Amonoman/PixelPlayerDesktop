package com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzSubmission(
    @SerialName("listen_type") val listenType: String,
    @SerialName("payload") val payload: List<ListenBrainzListen>
) {
    companion object {
        const val TYPE_PLAYING_NOW = "playing_now"
        const val TYPE_IMPORT = "import"
        const val TYPE_SINGLE = "single"
    }
}

@Serializable
data class ListenBrainzListen(
    @SerialName("listened_at") val listenedAt: Long? = null,
    @SerialName("track_metadata") val trackMetadata: ListenBrainzTrackMetadata
)

@Serializable
data class ListenBrainzTrackMetadata(
    @SerialName("artist_name") val artistName: String,
    @SerialName("track_name") val trackName: String,
    @SerialName("release_name") val releaseName: String? = null,
    @SerialName("additional_info") val additionalInfo: ListenBrainzAdditionalInfo? = null
)

@Serializable
data class ListenBrainzAdditionalInfo(
    @SerialName("media_player") val mediaPlayer: String? = "PixelPlayerDesktop",
    @SerialName("submission_client") val submissionClient: String? = "PixelPlayerDesktop",
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("recording_mbid") val recordingMbid: String? = null
)

@Serializable
data class ListenBrainzTokenValidation(
    val valid: Boolean = false,
    @SerialName("user_name") val userName: String? = null
)

@Serializable
data class ListenBrainzListenCountResponse(
    val payload: ListenBrainzListenCountPayload? = null
)

@Serializable
data class ListenBrainzListenCountPayload(
    val count: Long = 0
)

@Serializable
data class ListenBrainzPlayingNowResponse(
    val payload: ListenBrainzPlayingNowPayload? = null
)

@Serializable
data class ListenBrainzPlayingNowPayload(
    val listens: List<ListenBrainzListen> = emptyList()
)

data class ListenBrainzPendingListen(
    val id: Long = 0L,
    val listenedAtMs: Long,
    val trackName: String,
    val artistName: String,
    val releaseName: String? = null,
    val durationMs: Long? = null,
    val recordingMbid: String? = null,
    val source: String = "LOCAL",
    val createdAtMs: Long = System.currentTimeMillis()
)

data class ListenBrainzAccountState(
    val isConnected: Boolean = false,
    val userName: String? = null,
    val needsReauth: Boolean = false,
    val serverUrl: String? = null,
    val pendingCount: Int = 0,
    val lastError: String? = null
)
