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
data class SearchHistoryItem(
    val id: Long? = null,
    val query: String,
    val timestamp: Long
)

@Immutable
sealed interface SearchResultItem {
    data class SongItem(val song: Song) : SearchResultItem
    data class AlbumItem(val album: Album) : SearchResultItem
    data class ArtistItem(val artist: Artist) : SearchResultItem
}

enum class AudioOutputMode(val storageKey: String) {
    SYSTEM_DEFAULT("system_default"),
    DIRECT("direct"),
    PCM_FLOAT("pcm_float");

    val usesFloatOutput: Boolean
        get() = this == PCM_FLOAT

    companion object {
        fun fromStorageKey(value: String?): AudioOutputMode =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM_DEFAULT
    }
}

enum class FolderSource(val storageKey: String, val displayName: String) {
    INTERNAL("internal", "Internal Storage"),
    EXTERNAL("external", "External Storage");

    companion object {
        fun fromStorageKey(rawValue: String?): FolderSource =
            entries.firstOrNull { it.storageKey == rawValue } ?: INTERNAL
    }
}

data class MusicFolder(
    val path: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val subFolders: List<MusicFolder> = emptyList()
) {
    val totalSongCount: Int by lazy {
        songs.size + subFolders.sumOf { it.totalSongCount }
    }

    val totalSubFolderCount: Int by lazy {
        subFolders.size + subFolders.sumOf { it.totalSubFolderCount }
    }

    fun collectAllSongs(): List<Song> = songs + subFolders.flatMap { it.collectAllSongs() }
}

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    val title: String,
    val defaultSort: SortOption
) {
    SONGS("SONGS", "Songs", SortOption.SongTitleAZ),
    ALBUMS("ALBUMS", "Albums", SortOption.AlbumTitleAZ),
    ARTISTS("ARTIST", "Artists", SortOption.ArtistNameAZ),
    PLAYLISTS("PLAYLISTS", "Playlists", SortOption.PlaylistNameAZ),
    FOLDERS("FOLDERS", "Folders", SortOption.FolderNameAZ),
    LIKED("LIKED", "Liked", SortOption.LikedSongDateLiked);

    companion object {
        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}
