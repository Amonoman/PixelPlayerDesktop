package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.network.lyrics.LyricsSearchResultItem

sealed interface LyricsSearchUiState {
    data object Idle : LyricsSearchUiState
    data object Loading : LyricsSearchUiState
    data class PickResult(val query: String, val results: List<LyricsSearchResultItem>) : LyricsSearchUiState
    data class NotFound(val query: String) : LyricsSearchUiState
    data class Error(val message: String) : LyricsSearchUiState
}
