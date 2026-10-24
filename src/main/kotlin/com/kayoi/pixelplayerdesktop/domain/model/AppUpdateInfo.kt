package com.kayoi.pixelplayerdesktop.domain.model

data class AppReleaseInfo(
    val versionTag: String,
    val versionName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String? = null,
    val isPrerelease: Boolean = false
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpdateAvailable(
        val release: AppReleaseInfo,
        val currentVersion: String
    ) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object SemanticVersion {
    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        val cleanRemote = cleanVersion(remoteVersion)
        val cleanCurrent = cleanVersion(currentVersion)

        val (remoteCore, remotePre) = splitCoreAndPre(cleanRemote)
        val (currentCore, currentPre) = splitCoreAndPre(cleanCurrent)

        val remoteParts = parseNumbers(remoteCore)
        val currentParts = parseNumbers(currentCore)

        val maxParts = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxParts) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        if (currentPre != null && remotePre == null) {
            return true
        }
        if (currentPre == null && remotePre != null) {
            return false
        }
        if (currentPre != null && remotePre != null) {
            return remotePre > currentPre
        }

        return false
    }

    private fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    private fun splitCoreAndPre(version: String): Pair<String, String?> {
        val idx = version.indexOf('-')
        return if (idx >= 0) {
            version.substring(0, idx) to version.substring(idx + 1)
        } else {
            version to null
        }
    }

    private fun parseNumbers(core: String): List<Int> {
        return core.split('.').map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
    }
}
