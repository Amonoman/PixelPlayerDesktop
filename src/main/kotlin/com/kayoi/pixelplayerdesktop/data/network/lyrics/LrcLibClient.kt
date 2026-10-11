package com.kayoi.pixelplayerdesktop.data.network.lyrics

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.utils.LrcParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

@Serializable
data class LrcLibResponse(
    val id: Int? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricsSearchResultItem(
    val id: Int?,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSeconds: Double?,
    val isSynced: Boolean,
    val lyrics: Lyrics
)

class LrcLibClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSeconds: Int = 0
    ): Lyrics? = withContext(ioDispatcher) {
        val directLyrics = getExactMatch(title, artist, album, durationSeconds)
        if (directLyrics != null) {
            return@withContext directLyrics
        }

        searchMatch(title, artist)
    }

    suspend fun searchCandidates(query: String): List<LyricsSearchResultItem> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query.trim(), "UTF-8")
        val responseText = executeRawRequest(url) ?: return@withContext emptyList()
        runCatching {
            val list = json.decodeFromString<List<LrcLibResponse>>(responseText)
            list.mapNotNull { resp ->
                val lyrics = parseResponse(resp) ?: return@mapNotNull null
                LyricsSearchResultItem(
                    id = resp.id,
                    trackName = resp.trackName ?: resp.name ?: query,
                    artistName = resp.artistName.orEmpty(),
                    albumName = resp.albumName,
                    durationSeconds = resp.duration,
                    isSynced = !resp.syncedLyrics.isNullOrBlank(),
                    lyrics = lyrics.copy(areFromRemote = true)
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun getExactMatch(
        title: String,
        artist: String,
        album: String,
        durationSeconds: Int
    ): Lyrics? {
        val queryBuilder = StringBuilder("https://lrclib.net/api/get?")
        queryBuilder.append("track_name=").append(URLEncoder.encode(title.trim(), "UTF-8"))
        queryBuilder.append("&artist_name=").append(URLEncoder.encode(artist.trim(), "UTF-8"))
        if (album.isNotBlank()) {
            queryBuilder.append("&album_name=").append(URLEncoder.encode(album.trim(), "UTF-8"))
        }
        if (durationSeconds > 0) {
            queryBuilder.append("&duration=").append(durationSeconds)
        }

        val response = executeRequest(queryBuilder.toString()) ?: return null
        return parseResponse(response)
    }

    private fun searchMatch(title: String, artist: String): Lyrics? {
        val cleanArtist = artist.split(",", "/", "&", "feat.", "ft.").firstOrNull()?.trim().orEmpty()
        val query = if (cleanArtist.isNotBlank()) "$cleanArtist $title" else title
        val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query.trim(), "UTF-8")

        val responseText = executeRawRequest(url) ?: return null
        return runCatching {
            val list = json.decodeFromString<List<LrcLibResponse>>(responseText)
            list.firstNotNullOfOrNull { parseResponse(it) }
        }.getOrNull()
    }

    private fun executeRequest(urlString: String): LrcLibResponse? {
        val responseText = executeRawRequest(urlString) ?: return null
        return runCatching {
            json.decodeFromString<LrcLibResponse>(responseText)
        }.getOrNull()
    }

    private fun executeRawRequest(urlString: String): String? {
        return runCatching {
            val uri = URI(urlString)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "PixelPlayerDesktop/1.0 (https://github.com/adlifarizi/PixelPlayerDesktop)")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        }.getOrNull()
    }

    private fun parseResponse(response: LrcLibResponse): Lyrics? {
        val synced = response.syncedLyrics?.trim()
        if (!synced.isNullOrBlank()) {
            val parsed = LrcParser.parse(synced)
            if (!parsed.synced.isNullOrEmpty() || !parsed.plain.isNullOrEmpty()) {
                return parsed
            }
        }

        val plain = response.plainLyrics?.trim()
        if (!plain.isNullOrBlank()) {
            return LrcParser.parse(plain)
        }

        return null
    }
}
