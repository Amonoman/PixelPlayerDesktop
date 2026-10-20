package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.data.network.deezer.DeezerClient
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    fetchDeezerImages: Boolean = true,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(18.dp)
    var isMenuExpanded by remember { mutableStateOf(false) }

    var resolvedImageUrl by remember(artist.name, artist.effectiveImageUrl) {
        mutableStateOf(artist.effectiveImageUrl)
    }

    LaunchedEffect(artist.name, fetchDeezerImages) {
        if (resolvedImageUrl.isNullOrBlank() && artist.name.isNotBlank()) {
            val fetched = DeezerClient.getArtistImageUrl(
                artistName = artist.name,
                fetchEnabled = fetchDeezerImages
            )
            if (!fetched.isNullOrBlank()) {
                resolvedImageUrl = fetched
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(cardShape)
            .clickable(onClick = onClick)
            .onClick(
                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                onClick = { isMenuExpanded = true }
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(ShapeCache.ExpressiveAvatar)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val imageUri = resolvedImageUrl
                if (!imageUri.isNullOrBlank()) {
                    AsyncArtworkImage(
                        artworkUri = imageUri,
                        shape = ShapeCache.ExpressiveAvatar,
                        placeholderIcon = PixelPlayerIcons.Artist,
                        placeholderIconSize = 24.dp,
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        contentDescription = "${artist.name} portrait",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = PixelPlayerIcons.Artist,
                        contentDescription = artist.name,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val songCountText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
                Text(
                    text = songCountText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(modifier = Modifier.padding(end = 4.dp)) {
                FilledTonalIconButton(
                    onClick = { isMenuExpanded = true },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Artist options",
                        modifier = Modifier.size(18.dp)
                    )
                }

                ArtistOptionsDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    artist = artist,
                    onPlay = onPlay,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist
                )
            }
        }
    }
}
