package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.network.jellyfin.model.JellyfinCredentials
import kotlinx.coroutines.flow.StateFlow

interface JellyfinRepository {
    val isLoggedInFlow: StateFlow<Boolean>
    val syncProgressFlow: StateFlow<Pair<Float, String>?>

    fun getCredentials(): JellyfinCredentials?
    suspend fun login(serverUrl: String, username: String, password: String): Result<String>
    suspend fun logout()
    suspend fun syncLibrary(onProgress: ((Float, String) -> Unit)? = null): Result<Int>
    suspend fun syncPlaylists(): Result<Int>
    fun getStreamUrl(itemId: String): String?
    fun getImageUrl(itemId: String, maxWidth: Int = 500): String?
}
