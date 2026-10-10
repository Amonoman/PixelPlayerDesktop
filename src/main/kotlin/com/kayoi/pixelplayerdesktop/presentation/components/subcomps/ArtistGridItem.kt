package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Artist
import com.kayoi.pixelplayerdesktop.utils.ArtistImageService
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.ui.theme.ShapeCache

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    fallbackArtworkUri: String? = null,
    fetchDeezerImages: Boolean = true,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(20.dp)

    var resolvedImageUrl by remember(artist.name, artist.effectiveImageUrl) {
        mutableStateOf(artist.effectiveImageUrl ?: fallbackArtworkUri)
    }

    LaunchedEffect(artist.name, fetchDeezerImages) {
        if (resolvedImageUrl.isNullOrBlank() && artist.name.isNotBlank()) {
            val fetched = ArtistImageService.getArtistImageUrl(
                artistName = artist.name,
                fallbackArtworkUri = fallbackArtworkUri,
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
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(92.dp)
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
                        placeholderIconSize = 40.dp,
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
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            val songCountText = if (artist.songCount == 1) "1 track" else "${artist.songCount} tracks"
            Text(
                text = songCountText,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.5.sp,
                color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
