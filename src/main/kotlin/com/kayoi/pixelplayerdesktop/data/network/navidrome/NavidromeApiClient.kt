package com.kayoi.pixelplayerdesktop.data.network.navidrome

import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeAlbum
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeAuthMethod
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeCredentials
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromePlaylist
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID

class NavidromeApiClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun authParams(cred: NavidromeCredentials, method: NavidromeAuthMethod): Map<String, String> {
        return when (method) {
            NavidromeAuthMethod.TOKEN -> {
                val salt = UUID.randomUUID().toString().replace("-", "").take(12)
                val token = md5(cred.password + salt)
                mapOf("u" to cred.username, "t" to token, "s" to salt, "v" to "1.16.1", "c" to "PixelPlayerDesktop", "f" to "json")
            }
            NavidromeAuthMethod.PASSWORD -> {
                val hex = cred.password.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
                mapOf("u" to cred.username, "p" to "enc:$hex", "v" to "1.16.1", "c" to "PixelPlayerDesktop", "f" to "json")
            }
        }
    }

    private suspend fun executeSubsonicRequest(
        cred: NavidromeCredentials,
        endpoint: String,
        extraParams: Map<String, String> = emptyMap()
    ): Result<Pair<JsonObject, NavidromeAuthMethod>> = withContext(Dispatchers.IO) {
        var currentMethod = cred.authMethod
        var result = makeRequest(cred, endpoint, extraParams, currentMethod)

        val err = result.exceptionOrNull()
        if (err != null && currentMethod == NavidromeAuthMethod.TOKEN && err.message?.contains("code 40") != true) {
            val fallback = makeRequest(cred, endpoint, extraParams, NavidromeAuthMethod.PASSWORD)
            if (fallback.isSuccess) {
                currentMethod = NavidromeAuthMethod.PASSWORD
                result = fallback
            }
        }

        result.map { it to currentMethod }
    }

    private fun makeRequest(
        cred: NavidromeCredentials,
        endpoint: String,
        extraParams: Map<String, String>,
        method: NavidromeAuthMethod
    ): Result<JsonObject> {
        return try {
            val baseUrl = "${cred.normalizedServerUrl}/rest/$endpoint.view"
            val urlBuilder = baseUrl.toHttpUrl().newBuilder()

            authParams(cred, method).forEach { (k, v) ->
                urlBuilder.addQueryParameter(k, v)
            }
            extraParams.forEach { (k, v) ->
                urlBuilder.addQueryParameter(k, v)
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Accept", "application/json")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return Result.failure(Exception("Empty body"))
                val root = json.parseToJsonElement(body).jsonObject
                val subResp = root["subsonic-response"]?.jsonObject
                    ?: return Result.failure(Exception("Invalid Subsonic response format"))

                val status = subResp["status"]?.jsonPrimitive?.content ?: "failed"
                if (status != "ok") {
                    val err = subResp["error"]?.jsonObject
                    val code = err?.get("code")?.jsonPrimitive?.intOrNull ?: -1
                    val msg = err?.get("message")?.jsonPrimitive?.content ?: "Subsonic error"
                    return Result.failure(Exception("Subsonic error (code $code): $msg"))
                }

                Result.success(subResp)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ping(cred: NavidromeCredentials): Result<NavidromeAuthMethod> {
        return executeSubsonicRequest(cred, "ping").map { it.second }
    }

    suspend fun getAlbumList(
        cred: NavidromeCredentials,
        type: String = "newest",
        size: Int = 500,
        offset: Int = 0
    ): Result<List<NavidromeAlbum>> {
        val params = mapOf(
            "type" to type,
            "size" to size.toString(),
            "offset" to offset.toString()
        )
        return executeSubsonicRequest(cred, "getAlbumList2", params).map { (resp, _) ->
            val albumsArray = resp["albumList2"]?.jsonObject?.get("album")?.jsonArray ?: JsonArray(emptyList())
            albumsArray.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val title = obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "Unknown Album"
                val artist = obj["artist"]?.jsonPrimitive?.content ?: "Unknown Artist"
                NavidromeAlbum(
                    id = id,
                    title = title,
                    artist = artist,
                    artistId = obj["artistId"]?.jsonPrimitive?.content,
                    songCount = obj["songCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    coverArtId = obj["coverArt"]?.jsonPrimitive?.content,
                    year = obj["year"]?.jsonPrimitive?.intOrNull ?: 0,
                    genre = obj["genre"]?.jsonPrimitive?.content
                )
            }
        }
    }

    suspend fun getAlbum(cred: NavidromeCredentials, albumId: String): Result<List<NavidromeSong>> {
        return executeSubsonicRequest(cred, "getAlbum", mapOf("id" to albumId)).map { (resp, _) ->
            val songsArray = resp["album"]?.jsonObject?.get("song")?.jsonArray ?: JsonArray(emptyList())
            songsArray.mapNotNull { elem -> parseSong(elem.jsonObject) }
        }
    }

    suspend fun getPlaylists(cred: NavidromeCredentials): Result<List<NavidromePlaylist>> {
        return executeSubsonicRequest(cred, "getPlaylists").map { (resp, _) ->
            val container = resp["playlists"]?.jsonObject
            val array = container?.get("playlist")?.jsonArray ?: resp["playlist"]?.jsonArray ?: JsonArray(emptyList())
            array.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown Playlist"
                NavidromePlaylist(
                    id = id,
                    name = name,
                    songCount = obj["songCount"]?.jsonPrimitive?.intOrNull ?: 0,
                    durationMs = (obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1000L,
                    coverArtId = obj["coverArt"]?.jsonPrimitive?.content
                )
            }
        }
    }

    suspend fun getPlaylist(cred: NavidromeCredentials, playlistId: String): Result<List<NavidromeSong>> {
        return executeSubsonicRequest(cred, "getPlaylist", mapOf("id" to playlistId)).map { (resp, _) ->
            val pl = resp["playlist"]?.jsonObject
            val entries = pl?.get("entry")?.jsonArray ?: pl?.get("song")?.jsonArray ?: JsonArray(emptyList())
            entries.mapNotNull { elem -> parseSong(elem.jsonObject) }
        }
    }

    suspend fun searchSongs(cred: NavidromeCredentials, query: String, limit: Int = 30): Result<List<NavidromeSong>> {
        val params = mapOf(
            "query" to query,
            "songCount" to limit.toString(),
            "artistCount" to "0",
            "albumCount" to "0"
        )
        return executeSubsonicRequest(cred, "search3", params).map { (resp, _) ->
            val songs = resp["searchResult3"]?.jsonObject?.get("song")?.jsonArray ?: JsonArray(emptyList())
            songs.mapNotNull { elem -> parseSong(elem.jsonObject) }
        }
    }

    suspend fun scrobble(cred: NavidromeCredentials, songId: String, submission: Boolean = true): Result<Unit> {
        val params = mapOf(
            "id" to songId,
            "submission" to submission.toString()
        )
        return executeSubsonicRequest(cred, "scrobble", params).map { }
    }

    fun getStreamUrl(cred: NavidromeCredentials, songId: String): String {
        val auth = authParams(cred, cred.authMethod)
        val query = auth.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        return "${cred.normalizedServerUrl}/rest/stream.view?$query&id=${URLEncoder.encode(songId, "UTF-8")}"
    }

    fun getCoverArtUrl(cred: NavidromeCredentials, coverArtId: String, size: Int = 500): String {
        val auth = authParams(cred, cred.authMethod)
        val query = auth.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        return "${cred.normalizedServerUrl}/rest/getCoverArt.view?$query&id=${URLEncoder.encode(coverArtId, "UTF-8")}&size=$size"
    }

    suspend fun getLyrics(cred: NavidromeCredentials, songId: String): Result<String> {
        val byId = executeSubsonicRequest(cred, "getLyricsBySongId", mapOf("id" to songId))
        if (byId.isSuccess) {
            val lines = byId.getOrNull()?.first?.get("lyricsList")?.jsonObject
                ?.get("structuredLyrics")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("line")?.jsonArray
            if (lines != null && lines.isNotEmpty()) {
                val text = lines.mapNotNull { it.jsonObject["value"]?.jsonPrimitive?.content }.joinToString("\n")
                if (text.isNotBlank()) return Result.success(text)
            }
        }
        return Result.failure(Exception("No lyrics available"))
    }

    private fun parseSong(obj: JsonObject): NavidromeSong? {
        val id = obj["id"]?.jsonPrimitive?.content ?: return null
        val title = obj["title"]?.jsonPrimitive?.content ?: "Unknown Title"
        val artist = obj["artist"]?.jsonPrimitive?.content ?: "Unknown Artist"
        val album = obj["album"]?.jsonPrimitive?.content ?: "Unknown Album"
        val albumId = obj["albumId"]?.jsonPrimitive?.content ?: "unknown_album"
        val durationSec = obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L

        return NavidromeSong(
            id = id,
            title = title,
            artist = artist,
            artistId = obj["artistId"]?.jsonPrimitive?.content,
            album = album,
            albumId = albumId,
            albumArtist = obj["albumArtist"]?.jsonPrimitive?.content,
            durationMs = durationSec * 1000L,
            trackNumber = obj["track"]?.jsonPrimitive?.intOrNull ?: 0,
            discNumber = obj["discNumber"]?.jsonPrimitive?.intOrNull,
            year = obj["year"]?.jsonPrimitive?.intOrNull ?: 0,
            genre = obj["genre"]?.jsonPrimitive?.content,
            coverArtId = obj["coverArt"]?.jsonPrimitive?.content ?: id,
            path = obj["path"]?.jsonPrimitive?.content ?: "",
            bitRate = obj["bitRate"]?.jsonPrimitive?.intOrNull,
            mimeType = obj["contentType"]?.jsonPrimitive?.content
        )
    }
}
