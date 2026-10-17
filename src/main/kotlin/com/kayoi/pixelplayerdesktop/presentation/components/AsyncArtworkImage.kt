package com.kayoi.pixelplayerdesktop.presentation.components

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
import com.kayoi.pixelplayerdesktop.data.network.RemoteArtworkResolver
import org.jetbrains.skia.Image
import org.koin.core.context.GlobalContext
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import java.util.Collections
import java.util.LinkedHashMap
import kotlin.math.roundToInt

private const val MAX_MEMORY_CACHE_ENTRIES = 80
private const val MAX_ARTWORK_DIMENSION = 512

private val memoryArtworkCache = Collections.synchronizedMap(
    object : LinkedHashMap<String, ImageBitmap>(MAX_MEMORY_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_MEMORY_CACHE_ENTRIES
        }
    }
)

private fun decodeAndScaleArtwork(bytes: ByteArray, maxDimension: Int = MAX_ARTWORK_DIMENSION): ImageBitmap? {
    return try {
        val rawImage = Image.makeFromEncoded(bytes)
        val origW = rawImage.width
        val origH = rawImage.height
        if (origW <= 0 || origH <= 0) {
            rawImage.close()
            return null
        }

        if (origW > maxDimension || origH > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(origW, origH)
            val targetW = (origW * scale).roundToInt().coerceAtLeast(1)
            val targetH = (origH * scale).roundToInt().coerceAtLeast(1)

            val surface = Surface.makeRasterN32Premul(targetW, targetH)
            surface.canvas.drawImageRect(
                rawImage,
                Rect.makeWH(origW.toFloat(), origH.toFloat()),
                Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                SamplingMode.MITCHELL,
                null,
                true
            )
            val scaledSnapshot = surface.makeImageSnapshot()
            surface.close()
            rawImage.close()
            scaledSnapshot.toComposeImageBitmap()
        } else {
            rawImage.toComposeImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}

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
            if (imageBitmap != cached) {
                imageBitmap = cached
            }
            return@LaunchedEffect
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val resolver = GlobalContext.getOrNull()?.getOrNull<RemoteArtworkResolver>()
                val bytes = if (resolver != null) {
                    resolver.resolveArtworkBytes(artworkUri)
                } else {
                    val file = File(artworkUri)
                    if (file.exists() && file.isFile && file.length() > 0) file.readBytes() else null
                }
                if (bytes != null) decodeAndScaleArtwork(bytes) else null
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
