package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.StorageFilter
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons

enum class LibraryTabKind {
    SONGS,
    LIKED,
    ALBUMS,
    ARTISTS
}

@Composable
fun LibraryEmptyState(
    tabKind: LibraryTabKind,
    searchQuery: String,
    storageFilter: StorageFilter,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val icon: ImageVector
    val title: String
    val subtitle: String

    if (searchQuery.isNotBlank()) {
        icon = Icons.Rounded.SearchOff
        title = when (tabKind) {
            LibraryTabKind.SONGS -> "No songs matching \"$searchQuery\""
            LibraryTabKind.LIKED -> "No liked songs matching \"$searchQuery\""
            LibraryTabKind.ALBUMS -> "No albums matching \"$searchQuery\""
            LibraryTabKind.ARTISTS -> "No artists matching \"$searchQuery\""
        }
        subtitle = "Try searching with a different keyword"
    } else {
        when (storageFilter) {
            StorageFilter.ALL -> {
                when (tabKind) {
                    LibraryTabKind.SONGS -> {
                        icon = Icons.Rounded.MusicNote
                        title = "No songs in library"
                        subtitle = "Add music directories in Settings to scan"
                    }
                    LibraryTabKind.LIKED -> {
                        icon = Icons.Rounded.FavoriteBorder
                        title = "No liked songs yet"
                        subtitle = "Favorite tracks by clicking the heart icon to see them here"
                    }
                    LibraryTabKind.ALBUMS -> {
                        icon = Icons.Rounded.Album
                        title = "No albums in library"
                        subtitle = "Add music directories in Settings to scan"
                    }
                    LibraryTabKind.ARTISTS -> {
                        icon = PixelPlayerIcons.Artist
                        title = "No artists in library"
                        subtitle = "Add music directories in Settings to scan"
                    }
                }
            }
            StorageFilter.ONLINE -> {
                icon = Icons.Rounded.CloudOff
                when (tabKind) {
                    LibraryTabKind.SONGS -> {
                        title = "No cloud songs"
                        subtitle = "Connect and sync with Navidrome or Jellyfin in Settings"
                    }
                    LibraryTabKind.LIKED -> {
                        title = "No cloud liked songs"
                        subtitle = "Favorite songs synced from Navidrome or Jellyfin will appear here"
                    }
                    LibraryTabKind.ALBUMS -> {
                        title = "No cloud albums"
                        subtitle = "Connect and sync with Navidrome or Jellyfin in Settings"
                    }
                    LibraryTabKind.ARTISTS -> {
                        title = "No cloud artists"
                        subtitle = "Connect and sync with Navidrome or Jellyfin in Settings"
                    }
                }
            }
            StorageFilter.OFFLINE -> {
                when (tabKind) {
                    LibraryTabKind.SONGS -> {
                        icon = Icons.Rounded.Computer
                        title = "No local songs"
                        subtitle = "Add local music directories in Settings to scan"
                    }
                    LibraryTabKind.LIKED -> {
                        icon = Icons.Rounded.FavoriteBorder
                        title = "No local liked songs"
                        subtitle = "Favorite your offline local songs to see them here"
                    }
                    LibraryTabKind.ALBUMS -> {
                        icon = Icons.Rounded.Computer
                        title = "No local albums"
                        subtitle = "Add local music directories in Settings to scan"
                    }
                    LibraryTabKind.ARTISTS -> {
                        icon = Icons.Rounded.Computer
                        title = "No local artists"
                        subtitle = "Add local music directories in Settings to scan"
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(54.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}
