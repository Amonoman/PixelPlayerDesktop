package com.kayoi.pixelplayerdesktop.presentation.navigation

import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.presentation.screens.LibrarySubDetail
import com.kayoi.pixelplayerdesktop.presentation.screens.settings.SettingsSection

sealed interface DesktopNavTarget {
    val rootDestination: DesktopNavDestination

    data object Home : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object DailyMix : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object RecentlyPlayed : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object RecentlyAdded : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object MostListened : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object Stats : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.HOME
    }

    data object Songs : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.SONGS
    }

    data object Liked : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.LIKED
    }

    data object Albums : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.ALBUMS
    }

    data class AlbumDetail(val album: Album) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.ALBUMS
    }

    data object Artists : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.ARTISTS
    }

    data class ArtistDetail(val artist: Artist) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.ARTISTS
    }

    data object Playlists : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.PLAYLISTS
    }

    data class PlaylistDetail(val playlistId: String) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.PLAYLISTS
    }

    data class Folders(val treePath: String? = null) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.FOLDERS
    }

    data class FolderDetail(val folder: MusicFolder) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.FOLDERS
    }

    data class Settings(
        val section: SettingsSection? = null,
        val subDetail: LibrarySubDetail = LibrarySubDetail.NONE
    ) : DesktopNavTarget {
        override val rootDestination = DesktopNavDestination.SETTINGS
    }
}

fun DesktopNavDestination.toDefaultTarget(): DesktopNavTarget = when (this) {
    DesktopNavDestination.HOME -> DesktopNavTarget.Home
    DesktopNavDestination.SONGS -> DesktopNavTarget.Songs
    DesktopNavDestination.ALBUMS -> DesktopNavTarget.Albums
    DesktopNavDestination.ARTISTS -> DesktopNavTarget.Artists
    DesktopNavDestination.PLAYLISTS -> DesktopNavTarget.Playlists
    DesktopNavDestination.FOLDERS -> DesktopNavTarget.Folders()
    DesktopNavDestination.LIKED -> DesktopNavTarget.Liked
    DesktopNavDestination.SETTINGS -> DesktopNavTarget.Settings()
}
