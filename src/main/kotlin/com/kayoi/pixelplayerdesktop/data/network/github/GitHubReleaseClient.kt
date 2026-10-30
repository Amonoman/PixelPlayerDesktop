package com.kayoi.pixelplayerdesktop.data.network.github

import com.kayoi.pixelplayerdesktop.BuildConfig
import com.kayoi.pixelplayerdesktop.domain.model.AppReleaseInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

class GitHubReleaseClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val owner: String = "adlifarizi",
    private val repo: String = "PixelPlayerDesktop"
) {
    suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(ioDispatcher) {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "${BuildConfig.APP_NAME}/${BuildConfig.APP_VERSION_NAME}")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val dto = json.decodeFromString<GitHubReleaseDto>(bodyString)
                if (dto.draft) {
                    return@withContext null
                }
                val cleanReleaseNotes = dto.body
                    ?.substringBefore("---")
                    ?.replace(Regex("""^#{1,6}\s*""", RegexOption.MULTILINE), "")
                    ?.trim()
                    .orEmpty()

                AppReleaseInfo(
                    versionTag = dto.tagName,
                    versionName = dto.name ?: dto.tagName,
                    releaseNotes = cleanReleaseNotes,
                    htmlUrl = dto.htmlUrl,
                    publishedAt = dto.publishedAt,
                    isPrerelease = dto.prerelease
                )
            }
        }.getOrNull()
    }
}
