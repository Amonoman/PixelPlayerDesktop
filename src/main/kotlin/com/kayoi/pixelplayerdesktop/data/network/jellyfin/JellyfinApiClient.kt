package com.kayoi.pixelplayerdesktop.data.network.jellyfin

import com.kayoi.pixelplayerdesktop.BuildConfig
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.model.JellyfinCredentials
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.model.JellyfinPlaylist
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.model.JellyfinSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class JellyfinApiClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    companion object {
        private const val CLIENT_NAME = BuildConfig.APP_NAME
        private const val CLIENT_VERSION = BuildConfig.APP_VERSION_NAME
        private const val DEVICE_NAME = "Desktop"
        private const val DEVICE_ID = "PixelPlayerDesktop-PC"
    }

    private fun buildAuthHeader(token: String? = null): String {
        val tokenPart = if (!token.isNullOrBlank()) ", Token=\"$token\"" else ""
        return "MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"$DEVICE_ID\", Version=\"$CLIENT_VERSION\"$tokenPart"
    }

    suspend fun authenticateByName(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = serverUrl.trim().trimEnd('/')
            val url = "$normalizedUrl/Users/AuthenticateByName"

            val bodyJson = """{"Username":${json.encodeToString(serializer(), username)},"Pw":${json.encodeToString(serializer(), password)}}"""
            val requestBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", buildAuthHeader())
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val root = json.parseToJsonElement(body).jsonObject
                val token = root["AccessToken"]?.jsonPrimitive?.content ?: ""
                val userId = root["User"]?.jsonObject?.get("Id")?.jsonPrimitive?.content ?: ""

                if (token.isBlank() || userId.isBlank()) {
                    return@withContext Result.failure(Exception("Invalid authentication response"))
                }
                Result.success(token to userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeGet(
        cred: JellyfinCredentials,
        path: String,
        params: Map<String, String> = emptyMap()
    ): Result<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = "${cred.normalizedServerUrl}$path"
            val urlBuilder = baseUrl.toHttpUrl().newBuilder()
            params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Authorization", buildAuthHeader(cred.accessToken))
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val element = json.parseToJsonElement(body)
                if (element is JsonObject) {
                    Result.success(element)
                } else {
                    Result.failure(Exception("Expected JSON object response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ping(cred: JellyfinCredentials): Result<Boolean> {
        return executeGet(cred, "/System/Ping").map { true }
    }

    suspend fun getMusicItems(
        cred: JellyfinCredentials,
        startIndex: Int = 0,
        limit: Int = 500
    ): Result<Pair<Int, List<JellyfinSong>>> {
        val userId = cred.userId ?: return Result.failure(Exception("Missing userId"))
        val params = mapOf(
            "IncludeItemTypes" to "Audio",
            "Recursive" to "true",
            "Fields" to "MediaSources,Genres,Path",
            "StartIndex" to startIndex.toString(),
            "Limit" to limit.toString(),
            "SortBy" to "SortName",
            "SortOrder" to "Ascending"
        )
        return executeGet(cred, "/Users/$userId/Items", params).map { root ->
            val total = root["TotalRecordCount"]?.jsonPrimitive?.intOrNull ?: 0
            val items = root["Items"]?.jsonArray ?: JsonArray(emptyList())
            val list = items.mapNotNull { parseSong(it.jsonObject) }
            total to list
        }
    }

    suspend fun getPlaylists(cred: JellyfinCredentials): Result<List<JellyfinPlaylist>> {
        val userId = cred.userId ?: return Result.failure(Exception("Missing userId"))
        val params = mapOf(
            "IncludeItemTypes" to "Playlist",
            "Recursive" to "true",
            "Fields" to "ChildCount"
        )
        return executeGet(cred, "/Users/$userId/Items", params).map { root ->
            val items = root["Items"]?.jsonArray ?: JsonArray(emptyList())
            items.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["Id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = obj["Name"]?.jsonPrimitive?.content ?: "Unknown Playlist"
                val count = obj["ChildCount"]?.jsonPrimitive?.intOrNull ?: 0
                JellyfinPlaylist(id = id, name = name, songCount = count)
            }
        }
    }

    suspend fun getPlaylistItems(cred: JellyfinCredentials, playlistId: String): Result<List<JellyfinSong>> {
        val userId = cred.userId ?: return Result.failure(Exception("Missing userId"))
        val params = mapOf(
            "Fields" to "MediaSources,Genres,Path",
            "UserId" to userId
        )
        return executeGet(cred, "/Playlists/$playlistId/Items", params).map { root ->
            val items = root["Items"]?.jsonArray ?: JsonArray(emptyList())
            items.mapNotNull { parseSong(it.jsonObject) }
        }
    }

    fun getStreamUrl(cred: JellyfinCredentials, itemId: String): String {
        return "${cred.normalizedServerUrl}/Audio/$itemId/stream" +
            "?static=true" +
            "&api_key=${cred.accessToken}" +
            "&UserId=${cred.userId}" +
            "&DeviceId=$DEVICE_ID"
    }

    fun getImageUrl(cred: JellyfinCredentials, itemId: String, maxWidth: Int = 500): String {
        return "${cred.normalizedServerUrl}/Items/$itemId/Images/Primary?maxWidth=$maxWidth&quality=90"
    }

    private fun parseSong(obj: JsonObject): JellyfinSong? {
        val id = obj["Id"]?.jsonPrimitive?.content ?: return null
        val type = obj["Type"]?.jsonPrimitive?.content
        if (type != null && type != "Audio") return null

        val name = obj["Name"]?.jsonPrimitive?.content ?: "Unknown Title"
        val artistsArray = obj["Artists"]?.jsonArray
        val artist = artistsArray?.firstOrNull()?.jsonPrimitive?.content
            ?: obj["AlbumArtist"]?.jsonPrimitive?.content
            ?: "Unknown Artist"
        val album = obj["Album"]?.jsonPrimitive?.content ?: "Unknown Album"
        val albumId = obj["AlbumId"]?.jsonPrimitive?.content
        val albumArtist = obj["AlbumArtist"]?.jsonPrimitive?.content
        val runTimeTicks = obj["RunTimeTicks"]?.jsonPrimitive?.longOrNull ?: 0L
        val durationMs = runTimeTicks / 10_000L

        val genres = obj["Genres"]?.jsonArray
        val genre = genres?.firstOrNull()?.jsonPrimitive?.content

        return JellyfinSong(
            id = id,
            name = name,
            artist = artist,
            artistId = obj["ArtistItems"]?.jsonArray?.firstOrNull()?.jsonObject?.get("Id")?.jsonPrimitive?.content,
            album = album,
            albumId = albumId,
            albumArtist = albumArtist,
            durationMs = durationMs,
            trackNumber = obj["IndexNumber"]?.jsonPrimitive?.intOrNull ?: 0,
            discNumber = obj["ParentIndexNumber"]?.jsonPrimitive?.intOrNull,
            year = obj["ProductionYear"]?.jsonPrimitive?.intOrNull ?: 0,
            genre = genre,
            container = obj["Container"]?.jsonPrimitive?.content
        )
    }
}
