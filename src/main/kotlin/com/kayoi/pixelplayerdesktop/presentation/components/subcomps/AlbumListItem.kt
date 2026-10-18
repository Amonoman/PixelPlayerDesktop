package com.kayoi.pixelplayerdesktop.presentation.components.subcomps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Album
import com.kayoi.pixelplayerdesktop.presentation.components.AsyncArtworkImage
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.ColorSchemePair
import com.kayoi.pixelplayerdesktop.ui.theme.dynamic.DynamicColorProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumListItem(
    album: Album,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val currentMaterialColorScheme = MaterialTheme.colorScheme

    var colorSchemePair by remember(
        album.albumArtUriString,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        mutableStateOf<ColorSchemePair?>(DynamicColorProcessor.getCachedColorScheme(album.albumArtUriString))
    }

    LaunchedEffect(
        album.albumArtUriString,
        DynamicColorProcessor.defaultPaletteStyle,
        DynamicColorProcessor.defaultColorAccuracyLevel
    ) {
        if (colorSchemePair == null && !album.albumArtUriString.isNullOrBlank()) {
            val extracted = DynamicColorProcessor.extractColorSchemeAsync(album.albumArtUriString)
            if (extracted != null) {
                colorSchemePair = extracted
            }
        }
    }

    val itemColorScheme = remember(colorSchemePair, isDarkTheme, currentMaterialColorScheme) {
        val pair = colorSchemePair
        if (pair != null) {
            if (isDarkTheme) pair.dark else pair.light
        } else {
            currentMaterialColorScheme
        }
    }

    val targetGradientBaseColor = itemColorScheme.primaryContainer
    val targetOnGradientColor = itemColorScheme.onPrimaryContainer

    val gradientBaseColor by animateColorAsState(
        targetValue = targetGradientBaseColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "albumListContainerColor"
    )
    val onGradientColor by animateColorAsState(
        targetValue = targetOnGradientColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "albumListOnContainerColor"
    )
    val cardCornerRadius = 18.dp
    val cardShape = RoundedCornerShape(cardCornerRadius)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = gradientBaseColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBaseColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                AsyncArtworkImage(
                    artworkUri = album.albumArtUriString,
                    placeholderIcon = Icons.Rounded.Album,
                    placeholderIconSize = 36.dp,
                    containerColor = gradientBaseColor.copy(alpha = 0.5f),
                    contentColor = onGradientColor,
                    contentDescription = "${album.title} cover",
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            remember(gradientBaseColor) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        gradientBaseColor
                                    )
                                )
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = onGradientColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = onGradientColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val songCountText = if (album.songCount == 1) " • 1 Song" else " • ${album.songCount} Songs"
                    Text(
                        text = songCountText,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.5.sp,
                        color = onGradientColor.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
