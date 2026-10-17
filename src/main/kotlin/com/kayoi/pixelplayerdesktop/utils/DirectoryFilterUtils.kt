package com.kayoi.pixelplayerdesktop.utils

import java.io.File

object DirectoryFilterUtils {
    private val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true

    fun normalizePrefixes(excludedList: List<String>): List<String> {
        if (excludedList.isEmpty()) return emptyList()
        return excludedList.map { excluded ->
            val norm = File(excluded).absolutePath
            if (norm.endsWith(File.separator)) norm else norm + File.separator
        }
    }

    fun isPathExcluded(path: String, normalizedPrefixes: List<String>): Boolean {
        if (normalizedPrefixes.isEmpty()) return false
        val normPath = File(path).absolutePath
        val normPathWithSep = if (normPath.endsWith(File.separator)) normPath else normPath + File.separator
        return normalizedPrefixes.any { prefix ->
            if (isWindows) {
                normPathWithSep.startsWith(prefix, ignoreCase = true) || normPath.equals(prefix.trimEnd(File.separatorChar), ignoreCase = true)
            } else {
                normPathWithSep.startsWith(prefix) || normPath == prefix.trimEnd(File.separatorChar)
            }
        }
    }

    fun isDirectoryOrParentExcluded(path: String, excludedList: List<String>): Boolean {
        if (excludedList.isEmpty()) return false
        val prefixes = normalizePrefixes(excludedList)
        return isPathExcluded(path, prefixes)
    }
}

fun isDirectoryOrParentExcluded(path: String, excludedList: List<String>): Boolean =
    DirectoryFilterUtils.isDirectoryOrParentExcluded(path, excludedList)
