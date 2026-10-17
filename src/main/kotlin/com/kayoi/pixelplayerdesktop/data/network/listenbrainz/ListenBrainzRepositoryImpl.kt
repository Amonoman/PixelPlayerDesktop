package com.kayoi.pixelplayerdesktop.data.network.listenbrainz

import com.kayoi.pixelplayerdesktop.data.database.ListenBrainzDao
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzAccountState
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzAdditionalInfo
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzListen
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzSubmission
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.model.ListenBrainzTrackMetadata
import com.kayoi.pixelplayerdesktop.domain.repository.ListenBrainzRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ListenBrainzRepositoryImpl(
    private val apiClient: ListenBrainzApiClient,
    private val dao: ListenBrainzDao,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ListenBrainzRepository {

    private val flushMutex = Mutex()
    private val _accountState = MutableStateFlow(ListenBrainzAccountState())
    override val accountState: StateFlow<ListenBrainzAccountState> = _accountState.asStateFlow()

    init {
        coroutineScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                val hasToken = !settings.listenBrainzToken.isNullOrBlank()
                _accountState.update {
                    it.copy(
                        isConnected = hasToken && settings.listenBrainzEnabled,
                        userName = settings.listenBrainzUserName,
                        needsReauth = false,
                        serverUrl = settings.listenBrainzServerUrl
                    )
                }
            }
        }

        coroutineScope.launch {
            dao.countFlow().collectLatest { count ->
                _accountState.update { it.copy(pendingCount = count) }
            }
        }

        coroutineScope.launch {
            flushQueue()
        }
    }

    override fun hasToken(): Boolean {
        val settings = settingsRepository.settingsFlow.value
        return settings.listenBrainzEnabled && !settings.listenBrainzToken.isNullOrBlank()
    }

    override suspend fun connect(token: String, customServerUrl: String?): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = customServerUrl?.trim()?.takeIf { it.isNotEmpty() } ?: ListenBrainzApiClient.DEFAULT_BASE_URL
        val validationResult = apiClient.validateToken(token.trim(), targetUrl)

        validationResult.fold(
            onSuccess = { validation ->
                if (validation.valid && !validation.userName.isNullOrBlank()) {
                    settingsRepository.updateSettings { current ->
                        current.copy(
                            listenBrainzToken = token.trim(),
                            listenBrainzUserName = validation.userName,
                            listenBrainzServerUrl = customServerUrl?.trim()?.takeIf { it.isNotEmpty() },
                            listenBrainzEnabled = true
                        )
                    }
                    _accountState.update { it.copy(lastError = null) }
                    launchFlush()
                    Result.success(validation.userName)
                } else {
                    Result.failure(Exception("Invalid ListenBrainz token"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        settingsRepository.updateSettings { current ->
            current.copy(
                listenBrainzToken = null,
                listenBrainzUserName = null,
                listenBrainzServerUrl = null,
                listenBrainzEnabled = false
            )
        }
        _accountState.update { it.copy(lastError = null) }
    }

    override suspend fun submitPlayingNow(
        trackName: String,
        artistName: String,
        releaseName: String?,
        durationMs: Long?,
        recordingMbid: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settingsFlow.value
        val token = settings.listenBrainzToken?.takeIf { it.isNotBlank() && settings.listenBrainzEnabled }
            ?: return@withContext Result.failure(Exception("Not connected to ListenBrainz"))

        val submission = ListenBrainzSubmission(
            listenType = ListenBrainzSubmission.TYPE_PLAYING_NOW,
            payload = listOf(
                ListenBrainzListen(
                    listenedAt = null,
                    trackMetadata = ListenBrainzTrackMetadata(
                        artistName = artistName,
                        trackName = trackName,
                        releaseName = releaseName,
                        additionalInfo = ListenBrainzAdditionalInfo(
                            durationMs = durationMs,
                            recordingMbid = recordingMbid
                        )
                    )
                )
            )
        )

        val result = apiClient.submitListens(
            token = token,
            submission = submission,
            baseUrl = settings.listenBrainzServerUrl ?: ListenBrainzApiClient.DEFAULT_BASE_URL
        )
        if (result.isFailure) {
            updateErrorState(result.exceptionOrNull()?.message)
        }
        result
    }

    override suspend fun enqueueListen(listen: ListenBrainzPendingListen): Boolean = withContext(Dispatchers.IO) {
        val inserted = dao.insert(listen)
        if (inserted) {
            dao.trimQueue()
            launchFlush()
        }
        inserted
    }

    override fun launchFlush() {
        coroutineScope.launch {
            flushQueue()
        }
    }

    override suspend fun flushQueue(): Result<Int> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settingsFlow.value
        val token = settings.listenBrainzToken?.takeIf { it.isNotBlank() && settings.listenBrainzEnabled }
            ?: return@withContext Result.success(0)

        flushMutex.withLock {
            val pending = dao.getPendingListens(limit = 100)
            if (pending.isEmpty()) return@withContext Result.success(0)

            val payload = pending.map { item ->
                ListenBrainzListen(
                    listenedAt = item.listenedAtMs / 1000L,
                    trackMetadata = ListenBrainzTrackMetadata(
                        artistName = item.artistName,
                        trackName = item.trackName,
                        releaseName = item.releaseName,
                        additionalInfo = ListenBrainzAdditionalInfo(
                            durationMs = item.durationMs,
                            recordingMbid = item.recordingMbid
                        )
                    )
                )
            }

            val listenType = if (payload.size == 1) {
                ListenBrainzSubmission.TYPE_SINGLE
            } else {
                ListenBrainzSubmission.TYPE_IMPORT
            }

            val submission = ListenBrainzSubmission(
                listenType = listenType,
                payload = payload
            )

            val result = apiClient.submitListens(
                token = token,
                submission = submission,
                baseUrl = settings.listenBrainzServerUrl ?: ListenBrainzApiClient.DEFAULT_BASE_URL
            )

            if (result.isSuccess) {
                _accountState.update { it.copy(lastError = null) }
                val deletedCount = dao.deleteListens(pending.map { it.id })
                Result.success(deletedCount)
            } else {
                val ex = result.exceptionOrNull() ?: Exception("Failed to flush queue")
                updateErrorState(ex.message)
                Result.failure(ex)
            }
        }
    }

    private fun updateErrorState(errorMessage: String?) {
        if (errorMessage == null) return
        val friendlyMsg = when {
            errorMessage.contains("verified email address", ignoreCase = true) ->
                "MetaBrainz requires a verified email address to submit listens. Please verify your email at metabrainz.org/profile"
            errorMessage.contains("401", ignoreCase = true) ->
                "ListenBrainz authorization failed. Please check your user token."
            else -> errorMessage
        }
        _accountState.update { it.copy(lastError = friendlyMsg) }
    }
}
