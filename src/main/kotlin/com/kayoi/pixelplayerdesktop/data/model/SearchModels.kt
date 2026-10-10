package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class SearchFilterType {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}

@Immutable
sealed interface SearchResultItem {
    data class SongItem(val song: Song) : SearchResultItem
    data class AlbumItem(val album: Album) : SearchResultItem
    data class ArtistItem(val artist: Artist) : SearchResultItem
}
