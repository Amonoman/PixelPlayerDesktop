package com.kayoi.pixelplayerdesktop.domain.repository

import com.kayoi.pixelplayerdesktop.data.model.Lyrics
import com.kayoi.pixelplayerdesktop.data.network.lyrics.LyricsSearchResultItem

interface LyricsRepository {
    suspend fun getLyrics(songId: String, songPath: String, title: String, artist: String): Lyrics?
    suspend fun saveLyrics(songId: String, lyrics: Lyrics)
    suspend fun deleteLyrics(songId: String)
    suspend fun resetAllLyrics()
    suspend fun searchOnlineLyrics(query: String): List<LyricsSearchResultItem>
}
