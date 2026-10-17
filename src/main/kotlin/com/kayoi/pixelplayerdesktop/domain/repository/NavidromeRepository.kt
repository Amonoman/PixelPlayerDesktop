package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeCredentials
import kotlinx.coroutines.flow.StateFlow

interface NavidromeRepository {
    val isLoggedInFlow: StateFlow<Boolean>
    val syncProgressFlow: StateFlow<Pair<Float, String>?>

    fun getCredentials(): NavidromeCredentials?
    suspend fun login(serverUrl: String, username: String, password: String): Result<String>
    suspend fun logout()
    suspend fun syncLibrary(onProgress: ((Float, String) -> Unit)? = null): Result<Int>
    suspend fun syncPlaylists(): Result<Int>
    fun getStreamUrl(navidromeSongId: String): String?
    fun getCoverArtUrl(coverArtId: String, size: Int = 500): String?
    suspend fun scrobble(navidromeSongId: String, submission: Boolean = true): Result<Unit>
}
