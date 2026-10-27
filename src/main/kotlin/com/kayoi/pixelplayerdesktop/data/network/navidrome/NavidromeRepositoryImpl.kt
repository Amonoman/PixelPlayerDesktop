package com.kayoi.pixelplayerdesktop.data.network.navidrome

import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.model.ArtistRef
import com.kayoi.pixelplayerdesktop.data.model.generateArtistId
import com.kayoi.pixelplayerdesktop.data.model.Playlist
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeAuthMethod
import com.kayoi.pixelplayerdesktop.data.network.navidrome.model.NavidromeCredentials
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class NavidromeRepositoryImpl(
    private val apiClient: NavidromeApiClient,
    private val musicDao: MusicDao,
    private val playlistDao: LocalPlaylistDao,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : NavidromeRepository {

    private val _isLoggedInFlow = MutableStateFlow(false)
    override val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _syncProgressFlow = MutableStateFlow<Pair<Float, String>?>(null)
    override val syncProgressFlow: StateFlow<Pair<Float, String>?> = _syncProgressFlow.asStateFlow()

    init {
        coroutineScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _isLoggedInFlow.value = settings.navidromeEnabled &&
                    !settings.navidromeServerUrl.isNullOrBlank() &&
                    !settings.navidromeUsername.isNullOrBlank() &&
                    !settings.navidromePassword.isNullOrBlank()
            }
        }
    }

    override fun getCredentials(): NavidromeCredentials? {
        val s = settingsRepository.settingsFlow.value
        if (!s.navidromeEnabled || s.navidromeServerUrl.isNullOrBlank() || s.navidromeUsername.isNullOrBlank() || s.navidromePassword.isNullOrBlank()) {
            return null
        }
        return NavidromeCredentials(
            serverUrl = s.navidromeServerUrl,
            username = s.navidromeUsername,
            password = s.navidromePassword,
            authMethod = NavidromeAuthMethod.fromStorageKey(s.navidromeAuthMethod)
        )
    }

    override suspend fun login(serverUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val cred = NavidromeCredentials(serverUrl, username, password)
        val pingResult = apiClient.ping(cred)

        pingResult.fold(
            onSuccess = { effectiveAuthMethod ->
                settingsRepository.updateSettings { current ->
                    current.copy(
                        navidromeServerUrl = cred.normalizedServerUrl,
                        navidromeUsername = username.trim(),
                        navidromePassword = password,
                        navidromeAuthMethod = effectiveAuthMethod.name,
                        navidromeEnabled = true
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
                navidromeServerUrl = null,
                navidromeUsername = null,
                navidromePassword = null,
                navidromeEnabled = false
            )
        }
        musicDao.clearNavidromeSongs()
        playlistDao.clearPlaylistsBySource("NAVIDROME")
        runCatching {
            DesktopPathProvider.artworkCacheDir.listFiles { file ->
                file.name.startsWith("nd_")
            }?.forEach { it.delete() }
        }
        Unit
    }

    override suspend fun syncLibrary(onProgress: ((Float, String) -> Unit)?): Result<Int> {
        val cred = getCredentials() ?: return Result.failure(Exception("Not logged into Navidrome"))
        if (activeSyncJob?.isActive == true) {
            return Result.failure(Exception("Sync already in progress"))
        }

        val deferred = coroutineScope.async {
            try {
                onProgress?.invoke(0.05f, "Fetching album catalog...")
                _syncProgressFlow.value = 0.05f to "Fetching album catalog..."

                val albumsResult = apiClient.getAlbumList(cred, size = 1000)
                if (albumsResult.isFailure) {
                    _syncProgressFlow.value = null
                    return@async Result.failure(albumsResult.exceptionOrNull() ?: Exception("Failed to fetch albums"))
                }

                val albums = albumsResult.getOrThrow()
                val totalAlbums = albums.size
                val concurrencyLimit = 5
                val semaphore = Semaphore(concurrencyLimit)
                val processedCount = AtomicInteger(0)

                val albumSongLists = coroutineScope {
                    albums.map { album ->
                        async {
                            semaphore.withPermit {
                                val songsResult = apiClient.getAlbum(cred, album.id)
                                val currentProcessed = processedCount.incrementAndGet()
                                val progress = 0.05f + (currentProcessed.toFloat() / totalAlbums.coerceAtLeast(1)) * 0.85f
                                val message = "Syncing ${album.title}..."
                                onProgress?.invoke(progress, message)
                                _syncProgressFlow.value = progress to message

                                if (songsResult.isSuccess) {
                                    val albumSongs = songsResult.getOrThrow()
                                    albumSongs.map { ns ->
                                        Song(
                                            id = "navidrome_${ns.id}",
                                            title = ns.title,
                                            artist = ns.artist,
                                            artistId = generateArtistId(ns.artist),
                                            artists = listOf(ArtistRef(id = generateArtistId(ns.artist), name = ns.artist, isPrimary = true)),
                                            album = ns.album,
                                            albumId = album.id.hashCode().toLong(),
                                            albumArtist = ns.albumArtist,
                                            path = "navidrome://${ns.id}",
                                            contentUriString = "navidrome://${ns.id}",
                                            albumArtUriString = "navidrome_cover://${ns.coverArtId ?: ns.id}",
                                            duration = ns.durationMs,
                                            genre = ns.genre ?: album.genre,
                                            lyrics = null,
                                            isFavorite = false,
                                            trackNumber = ns.trackNumber,
                                            discNumber = ns.discNumber,
                                            year = ns.year.takeIf { it > 0 } ?: album.year,
                                            dateAdded = ns.dateAdded.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                            dateModified = System.currentTimeMillis(),
                                            navidromeId = ns.id
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            }
                        }
                    }.awaitAll()
                }

                val convertedSongs = albumSongLists.flatten()

                onProgress?.invoke(0.92f, "Saving songs to database...")
                _syncProgressFlow.value = 0.92f to "Saving songs to database..."

                musicDao.clearNavidromeSongs()
                musicDao.upsertSongs(convertedSongs)

                syncPlaylists()

                onProgress?.invoke(1.0f, "Sync complete!")
                _syncProgressFlow.value = null

                Result.success(convertedSongs.size)
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
        val cred = getCredentials() ?: return@withContext Result.failure(Exception("Not logged into Navidrome"))
        try {
            val playlistsResult = apiClient.getPlaylists(cred)
            if (playlistsResult.isFailure) return@withContext Result.failure(playlistsResult.exceptionOrNull()!!)

            val remotePlaylists = playlistsResult.getOrThrow()
            for (rp in remotePlaylists) {
                val detailResult = apiClient.getPlaylist(cred, rp.id)
                if (detailResult.isSuccess) {
                    val songs = detailResult.getOrThrow()
                    val songIds = songs.map { "navidrome_${it.id}" }
                    val playlist = Playlist(
                        id = "navidrome_pl_${rp.id}",
                        name = rp.name,
                        songIds = songIds,
                        createdAt = System.currentTimeMillis(),
                        lastModified = System.currentTimeMillis(),
                        source = "NAVIDROME"
                    )
                    playlistDao.insertPlaylist(playlist)
                }
            }
            Result.success(remotePlaylists.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStreamUrl(navidromeSongId: String): String? {
        val cred = getCredentials() ?: return null
        return apiClient.getStreamUrl(cred, navidromeSongId)
    }

    override fun getCoverArtUrl(coverArtId: String, size: Int): String? {
        val cred = getCredentials() ?: return null
        return apiClient.getCoverArtUrl(cred, coverArtId, size)
    }

    override suspend fun scrobble(navidromeSongId: String, submission: Boolean): Result<Unit> {
        val cred = getCredentials() ?: return Result.failure(Exception("Not logged into Navidrome"))
        return apiClient.scrobble(cred, navidromeSongId, submission)
    }
}
