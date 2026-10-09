package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.model.Lyrics

interface LyricsRepository {
    suspend fun getLyrics(songId: String, songPath: String, title: String, artist: String): Lyrics?
    suspend fun saveLyrics(songId: String, lyrics: Lyrics)
}
