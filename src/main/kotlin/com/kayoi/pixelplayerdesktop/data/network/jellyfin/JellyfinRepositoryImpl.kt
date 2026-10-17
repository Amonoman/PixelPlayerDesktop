package com.kayoi.pixelplayerdesktop.data.network.jellyfin

import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.model.JellyfinCredentials
import com.kayoi.pixelplayerdesktop.domain.repository.JellyfinRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JellyfinRepositoryImpl(
    private val apiClient: JellyfinApiClient,
    private val musicDao: MusicDao,
    private val playlistDao: LocalPlaylistDao,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : JellyfinRepository {

    private val _isLoggedInFlow = MutableStateFlow(false)
    override val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _syncProgressFlow = MutableStateFlow<Pair<Float, String>?>(null)
    override val syncProgressFlow: StateFlow<Pair<Float, String>?> = _syncProgressFlow.asStateFlow()

    init {
        coroutineScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _isLoggedInFlow.value = settings.jellyfinEnabled &&
                    !settings.jellyfinServerUrl.isNullOrBlank() &&
                    !settings.jellyfinUsername.isNullOrBlank() &&
                    !settings.jellyfinAccessToken.isNullOrBlank() &&
                    !settings.jellyfinUserId.isNullOrBlank()
            }
        }
    }

    override fun getCredentials(): JellyfinCredentials? {
        val s = settingsRepository.settingsFlow.value
        if (!s.jellyfinEnabled || s.jellyfinServerUrl.isNullOrBlank() || s.jellyfinUsername.isNullOrBlank() || s.jellyfinAccessToken.isNullOrBlank() || s.jellyfinUserId.isNullOrBlank()) {
            return null
        }
        return JellyfinCredentials(
            serverUrl = s.jellyfinServerUrl,
            username = s.jellyfinUsername,
            accessToken = s.jellyfinAccessToken,
            userId = s.jellyfinUserId
        )
    }

    override suspend fun login(serverUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val authResult = apiClient.authenticateByName(serverUrl, username, password)

        authResult.fold(
            onSuccess = { (token, userId) ->
                val normalizedUrl = serverUrl.trim().trimEnd('/')
                val urlWithScheme = if (normalizedUrl.startsWith("http://", ignoreCase = true) || normalizedUrl.startsWith("https://", ignoreCase = true)) {
                    normalizedUrl
                } else {
                    "https://$normalizedUrl"
                }

                settingsRepository.updateSettings { current ->
                    current.copy(
                        jellyfinServerUrl = urlWithScheme,
                        jellyfinUsername = username.trim(),
                        jellyfinAccessToken = token,
                        jellyfinUserId = userId,
                        jellyfinEnabled = true
                    )
                }
                Result.success(username)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private var activeSyncJob: Job? = null

    override suspend fun logout() = withContext(Dispatchers.IO) {
        activeSyncJob?.cancel()
        activeSyncJob = null
        _syncProgressFlow.value = null
        settingsRepository.updateSettings { current ->
            current.copy(
                jellyfinServerUrl = null,
                jellyfinUsername = null,
                jellyfinAccessToken = null,
                jellyfinUserId = null,
                jellyfinEnabled = false
            )
        }
        musicDao.clearJellyfinSongs()
        playlistDao.clearPlaylistsBySource("JELLYFIN")
        runCatching {
            DesktopPathProvider.artworkCacheDir.listFiles { file ->
                file.name.startsWith("jf_")
            }?.forEach { it.delete() }
        }
        Unit
    }

    override suspend fun syncLibrary(onProgress: ((Float, String) -> Unit)?): Result<Int> {
        val cred = getCredentials() ?: return Result.failure(Exception("Not logged into Jellyfin"))
        if (activeSyncJob?.isActive == true) {
            return Result.failure(Exception("Sync already in progress"))
        }

        val deferred = coroutineScope.async {
            try {
                onProgress?.invoke(0.05f, "Connecting to Jellyfin...")
                _syncProgressFlow.value = 0.05f to "Connecting to Jellyfin..."

                val allSongs = mutableListOf<Song>()
                var startIndex = 0
                val pageSize = 500
                var totalCount = 1

                while (startIndex < totalCount) {
                    val progress = 0.05f + (startIndex.toFloat() / totalCount.coerceAtLeast(1)) * 0.80f
                    val message = "Fetching songs ($startIndex / $totalCount)..."
                    onProgress?.invoke(progress, message)
                    _syncProgressFlow.value = progress to message

                    val pageResult = apiClient.getMusicItems(cred, startIndex = startIndex, limit = pageSize)
                    if (pageResult.isFailure) {
                        _syncProgressFlow.value = null
                        return@async Result.failure(pageResult.exceptionOrNull() ?: Exception("Failed to fetch music items"))
                    }

                    val (total, items) = pageResult.getOrThrow()
                    totalCount = total
                    if (items.isEmpty()) break

                    for (js in items) {
                        allSongs.add(
                            Song(
                                id = "jellyfin_${js.id}",
                                title = js.name,
                                artist = js.artist,
                                artistId = js.artist.hashCode().toLong(),
                                artists = listOf(ArtistRef(id = js.artist.hashCode().toLong(), name = js.artist, isPrimary = true)),
                                album = js.album,
                                albumId = (js.albumId ?: js.album).hashCode().toLong(),
                                albumArtist = js.albumArtist,
                                path = "jellyfin://${js.id}",
                                contentUriString = "jellyfin://${js.id}",
                                albumArtUriString = "jellyfin_cover://${js.albumId ?: js.id}",
                                duration = js.durationMs,
                                genre = js.genre,
                                lyrics = null,
                                isFavorite = false,
                                trackNumber = js.trackNumber,
                                discNumber = js.discNumber,
                                year = js.year,
                                dateAdded = System.currentTimeMillis(),
                                dateModified = System.currentTimeMillis(),
                                jellyfinId = js.id
                            )
                        )
                    }

                    startIndex += items.size
                }

                onProgress?.invoke(0.90f, "Saving songs to database...")
                _syncProgressFlow.value = 0.90f to "Saving songs to database..."

                musicDao.clearJellyfinSongs()
                musicDao.upsertSongs(allSongs)

                syncPlaylists()

                onProgress?.invoke(1.0f, "Sync complete!")
                _syncProgressFlow.value = null

                Result.success(allSongs.size)
            } catch (e: Exception) {
                _syncProgressFlow.value = null
                Result.failure(e)
            } finally {
                _syncProgressFlow.value = null
            }
        }

        activeSyncJob = deferred
        return try {
            deferred.await()
        } finally {
            if (activeSyncJob == deferred) {
                activeSyncJob = null
            }
        }
    }

    override suspend fun syncPlaylists(): Result<Int> = withContext(Dispatchers.IO) {
        val cred = getCredentials() ?: return@withContext Result.failure(Exception("Not logged into Jellyfin"))
        try {
            val playlistsResult = apiClient.getPlaylists(cred)
            if (playlistsResult.isFailure) return@withContext Result.failure(playlistsResult.exceptionOrNull()!!)

            val remotePlaylists = playlistsResult.getOrThrow()
            for (rp in remotePlaylists) {
                val detailResult = apiClient.getPlaylistItems(cred, rp.id)
                if (detailResult.isSuccess) {
                    val songs = detailResult.getOrThrow()
                    val songIds = songs.map { "jellyfin_${it.id}" }
                    val playlist = Playlist(
                        id = "jellyfin_pl_${rp.id}",
                        name = rp.name,
                        songIds = songIds,
                        createdAt = System.currentTimeMillis(),
                        lastModified = System.currentTimeMillis(),
                        source = "JELLYFIN"
                    )
                    playlistDao.insertPlaylist(playlist)
                }
            }
            Result.success(remotePlaylists.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStreamUrl(itemId: String): String? {
        val cred = getCredentials() ?: return null
        return apiClient.getStreamUrl(cred, itemId)
    }

    override fun getImageUrl(itemId: String, maxWidth: Int): String? {
        val cred = getCredentials() ?: return null
        return apiClient.getImageUrl(cred, itemId, maxWidth)
    }
}
