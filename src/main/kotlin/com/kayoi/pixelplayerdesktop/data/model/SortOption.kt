package com.kayoi.pixelplayerdesktop.data.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class SortOption(
    val storageKey: String,
    val displayName: String
) {
    data object SongDefaultOrder : SortOption("song_default_order", "Default Order")
    data object SongTitleAZ : SortOption("song_title_az", "Title (A-Z)")
    data object SongTitleZA : SortOption("song_title_za", "Title (Z-A)")
    data object SongArtist : SortOption("song_artist", "Artist")
    data object SongArtistDesc : SortOption("song_artist_desc", "Artist (Z-A)")
    data object SongAlbum : SortOption("song_album", "Album")
    data object SongAlbumDesc : SortOption("song_album_desc", "Album (Z-A)")
    data object SongDateAdded : SortOption("song_date_added", "Date Added")
    data object SongDateAddedAsc : SortOption("song_date_added_asc", "Date Added (Oldest First)")
    data object SongDuration : SortOption("song_duration", "Duration")
    data object SongDurationAsc : SortOption("song_duration_asc", "Duration (Shortest First)")

    data object AlbumTitleAZ : SortOption("album_title_az", "Title (A-Z)")
    data object AlbumTitleZA : SortOption("album_title_za", "Title (Z-A)")
    data object AlbumArtist : SortOption("album_artist", "Artist")
    data object AlbumArtistDesc : SortOption("album_artist_desc", "Artist (Z-A)")
    data object AlbumReleaseYear : SortOption("album_release_year", "Release Year")
    data object AlbumReleaseYearAsc : SortOption("album_release_year_asc", "Release Year (Oldest First)")
    data object AlbumDateAdded : SortOption("album_date_added", "Date Added")
    data object AlbumSizeAsc : SortOption("album_size_asc", "Fewest Songs")
    data object AlbumSizeDesc : SortOption("album_size_desc", "Most Songs")

    data object ArtistNameAZ : SortOption("artist_name_az", "Name (A-Z)")
    data object ArtistNameZA : SortOption("artist_name_za", "Name (Z-A)")
    data object ArtistNumSongsDesc : SortOption("artist_num_songs_desc", "Number of Songs (Most)")
    data object ArtistNumSongsAsc : SortOption("artist_num_songs_asc", "Number of Songs (Fewest)")

    data object PlaylistNameAZ : SortOption("playlist_name_az", "Name (A-Z)")
    data object PlaylistNameZA : SortOption("playlist_name_za", "Name (Z-A)")
    data object PlaylistDateCreated : SortOption("playlist_date_created", "Date Created")
    data object PlaylistDateCreatedAsc : SortOption("playlist_date_created_asc", "Date Created (Oldest First)")

    data object FolderNameAZ : SortOption("folder_name_az", "Name (A-Z)")
    data object FolderNameZA : SortOption("folder_name_za", "Name (Z-A)")
    data object FolderSongCountAsc : SortOption("folder_song_count_asc", "Fewest Songs")
    data object FolderSongCountDesc : SortOption("folder_song_count_desc", "Most Songs")
    data object FolderSubdirCountAsc : SortOption("folder_subdir_count_asc", "Fewest Subfolders")
    data object FolderSubdirCountDesc : SortOption("folder_subdir_count_desc", "Most Subfolders")

    companion object {
        val SONGS: List<SortOption> by lazy {
            listOf(
                SongDefaultOrder,
                SongTitleAZ,
                SongTitleZA,
                SongArtist,
                SongArtistDesc,
                SongAlbum,
                SongAlbumDesc,
                SongDateAdded,
                SongDateAddedAsc,
                SongDuration,
                SongDurationAsc
            )
        }

        val ALBUMS: List<SortOption> by lazy {
            listOf(
                AlbumTitleAZ,
                AlbumTitleZA,
                AlbumArtist,
                AlbumArtistDesc,
                AlbumReleaseYear,
                AlbumReleaseYearAsc,
                AlbumDateAdded,
                AlbumSizeAsc,
                AlbumSizeDesc
            )
        }

        val ARTISTS: List<SortOption> by lazy {
            listOf(
                ArtistNameAZ,
                ArtistNameZA,
                ArtistNumSongsDesc,
                ArtistNumSongsAsc
            )
        }

        val PLAYLISTS: List<SortOption> by lazy {
            listOf(
                PlaylistNameAZ,
                PlaylistNameZA,
                PlaylistDateCreated,
                PlaylistDateCreatedAsc
            )
        }

        val FOLDERS: List<SortOption> by lazy {
            listOf(
                FolderNameAZ,
                FolderNameZA,
                FolderSongCountAsc,
                FolderSongCountDesc,
                FolderSubdirCountAsc,
                FolderSubdirCountDesc
            )
        }
    }
}
