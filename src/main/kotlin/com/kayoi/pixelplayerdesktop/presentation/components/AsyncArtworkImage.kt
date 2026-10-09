package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File
import java.util.Collections
import java.util.LinkedHashMap

private const val MAX_MEMORY_CACHE_ENTRIES = 500

private val memoryArtworkCache = Collections.synchronizedMap(
    object : LinkedHashMap<String, ImageBitmap>(MAX_MEMORY_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_MEMORY_CACHE_ENTRIES
        }
    }
)

@Composable
fun AsyncArtworkImage(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    placeholderIcon: ImageVector = Icons.Rounded.MusicNote,
    placeholderIconSize: Dp = 24.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    var imageBitmap by remember(artworkUri) {
        mutableStateOf(artworkUri?.let { memoryArtworkCache[it] })
    }

    LaunchedEffect(artworkUri) {
        if (artworkUri.isNullOrBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }

        val cached = memoryArtworkCache[artworkUri]
        if (cached != null) {
            imageBitmap = cached
            return@LaunchedEffect
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val file = File(artworkUri)
                if (file.exists() && file.isFile && file.length() > 0) {
                    val bytes = file.readBytes()
                    val skiaImage = Image.makeFromEncoded(bytes)
                    skiaImage.toComposeImageBitmap()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        if (loaded != null) {
            memoryArtworkCache[artworkUri] = loaded
            imageBitmap = loaded
        } else {
            imageBitmap = null
        }
    }

    val clipModifier = if (shape != null) modifier.clip(shape) else modifier

    Box(
        modifier = clipModifier.background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = imageBitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = contentDescription,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(placeholderIconSize)
            )
        }
    }
}
