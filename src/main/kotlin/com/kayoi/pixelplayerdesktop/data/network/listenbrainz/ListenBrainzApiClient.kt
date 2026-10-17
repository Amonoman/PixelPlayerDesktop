package com.kayoi.pixelplayerdesktop.data.network.listenbrainz

import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzListenCountResponse
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzPlayingNowResponse
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzSubmission
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzTokenValidation
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzTrackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ListenBrainzApiClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }
) {

    suspend fun validateToken(
        token: String,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<ListenBrainzTokenValidation> = withContext(Dispatchers.IO) {
        try {
            val url = "${normalizeBaseUrl(baseUrl)}1/validate-token"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token $token")
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val validation = json.decodeFromString<ListenBrainzTokenValidation>(body)
                Result.success(validation)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitListens(
        token: String,
        submission: ListenBrainzSubmission,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${normalizeBaseUrl(baseUrl)}1/submit-listens"
            val jsonString = json.encodeToString(ListenBrainzSubmission.serializer(), submission)
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token $token")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string()
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message} - $errorMsg"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListenCount(
        token: String,
        userName: String,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val url = "${normalizeBaseUrl(baseUrl)}1/user/$userName/listen-count"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token $token")
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<ListenBrainzListenCountResponse>(body)
                Result.success(res.payload?.count ?: 0L)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayingNow(
        token: String,
        userName: String,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<ListenBrainzTrackMetadata?> = withContext(Dispatchers.IO) {
        try {
            val url = "${normalizeBaseUrl(baseUrl)}1/user/$userName/playing-now"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token $token")
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val res = json.decodeFromString<ListenBrainzPlayingNowResponse>(body)
                Result.success(res.payload?.listens?.firstOrNull()?.trackMetadata)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.listenbrainz.org/"
    }
}
