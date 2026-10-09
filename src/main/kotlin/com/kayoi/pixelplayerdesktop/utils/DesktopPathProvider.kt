package com.kayoi.pixelplayerdesktop.utils

import java.io.File

object DesktopPathProvider {

    private val osName = System.getProperty("os.name").lowercase()
    private val userHome = System.getProperty("user.home") ?: "."

    val configDir: File by lazy {
        val dir = when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                File(appData, "PixelPlayer")
            }
            osName.contains("mac") -> {
                File(userHome, "Library/Application Support/PixelPlayer")
            }
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config"
                File(xdgConfig, "pixelplayer")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val cacheDir: File by lazy {
        val dir = when {
            osName.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                File(localAppData, "PixelPlayer/Cache")
            }
            osName.contains("mac") -> {
                File(userHome, "Library/Caches/PixelPlayer")
            }
            else -> {
                val xdgCache = System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache"
                File(xdgCache, "pixelplayer")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val artworkCacheDir: File by lazy {
        val dir = File(cacheDir, "artworks")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val databaseFile: File by lazy {
        File(configDir, "pixelplayer.db")
    }

    val settingsFile: File by lazy {
        File(configDir, "settings.json")
    }

    val defaultMusicDirectories: List<String> by lazy {
        val defaultMusic = File(userHome, "Music")
        if (defaultMusic.exists() && defaultMusic.isDirectory) {
            listOf(defaultMusic.absolutePath)
        } else {
            listOf(userHome)
        }
    }
}
