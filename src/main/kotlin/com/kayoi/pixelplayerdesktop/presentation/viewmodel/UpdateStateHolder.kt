package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import java.util.concurrent.atomic.AtomicBoolean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.kayoi.pixelplayerdesktop.BuildConfig
import com.kayoi.pixelplayerdesktop.data.network.github.GitHubReleaseClient
import com.kayoi.pixelplayerdesktop.domain.model.AppReleaseInfo
import com.kayoi.pixelplayerdesktop.domain.model.SemanticVersion
import com.kayoi.pixelplayerdesktop.domain.model.UpdateCheckResult
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.utils.BrowserUtils

class UpdateStateHolder(
    private val releaseClient: GitHubReleaseClient,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope
) {
    private val startupCheckTriggered = AtomicBoolean(false)

    private val _checkResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val checkResult: StateFlow<UpdateCheckResult> = _checkResult.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _availableRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val availableRelease: StateFlow<AppReleaseInfo?> = _availableRelease.asStateFlow()

    val checkUpdatesOnStartup: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.checkUpdatesOnStartup }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    fun runStartupCheckIfNeeded() {
        if (!startupCheckTriggered.compareAndSet(false, true)) {
            return
        }

        coroutineScope.launch {
            val settings = settingsRepository.getSettings()
            if (!settings.checkUpdatesOnStartup) {
                return@launch
            }

            performCheck(isManual = false)
        }
    }

    fun checkForUpdates() {
        coroutineScope.launch {
            performCheck(isManual = true)
        }
    }

    private suspend fun performCheck(isManual: Boolean) {
        if (isManual) {
            _checkResult.value = UpdateCheckResult.Checking
        }

        val release = releaseClient.fetchLatestRelease()
        val currentVersion = BuildConfig.APP_VERSION_NAME

        if (release == null) {
            if (isManual) {
                _checkResult.value = UpdateCheckResult.Error("Could not reach GitHub Releases.")
            }
            return
        }

        val isNewer = SemanticVersion.isNewer(
            remoteVersion = release.versionTag,
            currentVersion = currentVersion
        )

        if (isNewer) {
            _availableRelease.value = release
            _checkResult.value = UpdateCheckResult.UpdateAvailable(
                release = release,
                currentVersion = currentVersion
            )

            val settings = settingsRepository.getSettings()
            val alreadyDismissed = settings.lastDismissedUpdateVersion == release.versionTag

            if (isManual || !alreadyDismissed) {
                _showUpdateDialog.value = true
            }
        } else {
            _checkResult.value = UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    fun dismissUpdateDialog(skipThisVersion: Boolean = false) {
        _showUpdateDialog.value = false
        if (skipThisVersion) {
            val release = _availableRelease.value ?: return
            coroutineScope.launch {
                settingsRepository.setLastDismissedUpdateVersion(release.versionTag)
            }
        }
    }

    fun openReleasePage(url: String) {
        BrowserUtils.openBrowser(url)
    }

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setCheckUpdatesOnStartup(enabled)
        }
    }
}
