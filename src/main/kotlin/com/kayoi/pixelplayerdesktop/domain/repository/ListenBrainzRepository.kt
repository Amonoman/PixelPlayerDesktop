package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzAccountState
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import kotlinx.coroutines.flow.StateFlow

interface ListenBrainzRepository {
    val accountState: StateFlow<ListenBrainzAccountState>

    fun hasToken(): Boolean
    suspend fun connect(token: String, customServerUrl: String? = null): Result<String>
    suspend fun disconnect()
    suspend fun submitPlayingNow(
        trackName: String,
        artistName: String,
        releaseName: String?,
        durationMs: Long?,
        recordingMbid: String?
    ): Result<Unit>
    suspend fun enqueueListen(listen: ListenBrainzPendingListen): Boolean
    fun launchFlush()
    suspend fun flushQueue(): Result<Int>
}
