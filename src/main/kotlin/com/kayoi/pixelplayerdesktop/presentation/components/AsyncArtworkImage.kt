package com.kayoi.pixelplayerdesktop.presentation.components

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_music_note
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.network.RemoteArtworkResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.koin.core.context.GlobalContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

enum class ArtworkSize {
    SMALL,
    LARGE
}

data class ArtworkCacheKey(
    val uri: String,
    val size: ArtworkSize
)

internal class ArtworkMemoryCache(
    private val maxSizeBytes: Long = 32L * 1024L * 1024L
) {
    private val lock = Any()
    private val map = LinkedHashMap<ArtworkCacheKey, ImageBitmap>(64, 0.75f, true)
    private var currentSizeBytes: Long = 0L

    fun get(key: ArtworkCacheKey): ImageBitmap? = synchronized(lock) {
        map[key]
    }

    fun put(key: ArtworkCacheKey, bitmap: ImageBitmap) {
        synchronized(lock) {
            val entrySize = (bitmap.width * bitmap.height * 4).toLong()
            if (entrySize > maxSizeBytes) return

            map.remove(key)?.let { old ->
                currentSizeBytes -= (old.width * old.height * 4).toLong()
            }

            map[key] = bitmap
            currentSizeBytes += entrySize

            val iterator = map.entries.iterator()
            while (currentSizeBytes > maxSizeBytes && iterator.hasNext()) {
                val entry = iterator.next()
                currentSizeBytes -= (entry.value.width * entry.value.height * 4).toLong()
                iterator.remove()
            }
        }
    }


    fun sizeBytes(): Long = synchronized(lock) { currentSizeBytes }

    fun entryCount(): Int = synchronized(lock) { map.size }

    fun clear() = synchronized(lock) {
        map.clear()
        currentSizeBytes = 0L
    }
}

internal object ArtworkLoader {
    private const val MAX_FILE_SIZE_BYTES = 30 * 1024 * 1024
    private const val MAX_DIMENSION_CAP = 8192

    val memoryCache = ArtworkMemoryCache()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightLock = Any()

    private class InFlightState(
        val deferred: Deferred<ImageBitmap?>,
        var refCount: Int
    )

    private val inFlight = HashMap<ArtworkCacheKey, InFlightState>()

    fun getCached(key: ArtworkCacheKey): ImageBitmap? = memoryCache.get(key)

    suspend fun loadArtwork(key: ArtworkCacheKey, targetDimension: Int): ImageBitmap? {
        memoryCache.get(key)?.let { return it }

        val state = synchronized(inFlightLock) {
            memoryCache.get(key)?.let { return it }
            val existing = inFlight[key]
            if (existing != null) {
                existing.refCount++
                existing
            } else {
                val deferred = scope.async(Dispatchers.IO) {
                    try {
                        coroutineContext.ensureActive()
                        val bytes = resolveBytes(key.uri)
                        coroutineContext.ensureActive()
                        val bitmap = if (bytes != null) {
                            decodeAndScaleArtwork(bytes, targetDimension)
                        } else null
                        if (bitmap != null) {
                            memoryCache.put(key, bitmap)
                        }
                        bitmap
                    } finally {
                        synchronized(inFlightLock) {
                            inFlight.remove(key)
                        }
                    }
                }
                val newState = InFlightState(deferred, refCount = 1)
                inFlight[key] = newState
                newState
            }
        }

        return try {
            state.deferred.await()
        } catch (e: CancellationException) {
            synchronized(inFlightLock) {
                state.refCount--
                if (state.refCount <= 0) {
                    state.deferred.cancel()
                    inFlight.remove(key)
                }
            }
            throw e
        }
    }

    private suspend fun resolveBytes(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val resolver = GlobalContext.getOrNull()?.getOrNull<RemoteArtworkResolver>()
            if (resolver != null) {
                resolver.resolveArtworkBytes(uri)
            } else {
                val file = File(uri)
                if (file.exists() && file.isFile && file.length() > 0 && file.length() <= MAX_FILE_SIZE_BYTES) {
                    file.readBytes()
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    internal fun decodeAndScaleArtwork(bytes: ByteArray, targetDimension: Int): ImageBitmap? {
        if (bytes.isEmpty() || bytes.size > MAX_FILE_SIZE_BYTES) return null

        val data = try {
            Data.makeFromBytes(bytes)
        } catch (_: Throwable) {
            return null
        }

        val codec = try {
            Codec.makeFromData(data)
        } catch (_: Throwable) {
            data.close()
            return null
        }

        val origW = codec.width
        val origH = codec.height
        if (origW <= 0 || origH <= 0 || origW > MAX_DIMENSION_CAP || origH > MAX_DIMENSION_CAP) {
            codec.close()
            data.close()
            return null
        }

        val maxOrig = maxOf(origW, origH)
        val sample = when {
            maxOrig >= targetDimension * 8 -> 8
            maxOrig >= targetDimension * 4 -> 4
            maxOrig >= targetDimension * 2 -> 2
            else -> 1
        }

        var decodedBitmap: Bitmap? = null
        val decodeW = (origW / sample).coerceAtLeast(1)
        val decodeH = (origH / sample).coerceAtLeast(1)

        try {
            val bm = Bitmap()
            val info = ImageInfo.makeN32Premul(decodeW, decodeH)
            if (bm.allocPixels(info)) {
                codec.readPixels(bm)
                decodedBitmap = bm
            } else {
                bm.close()
            }
        } catch (_: Throwable) {
            decodedBitmap?.close()
            decodedBitmap = null
        }

        if (decodedBitmap == null && sample > 1 && origW <= 2048 && origH <= 2048) {
            try {
                val bm = Bitmap()
                val info = ImageInfo.makeN32Premul(origW, origH)
                if (bm.allocPixels(info)) {
                    codec.readPixels(bm)
                    decodedBitmap = bm
                } else {
                    bm.close()
                }
            } catch (_: Throwable) {
                decodedBitmap?.close()
                decodedBitmap = null
            }
        }

        codec.close()
        data.close()

        val bm = decodedBitmap ?: return null

        return try {
            if (bm.width <= targetDimension && bm.height <= targetDimension) {
                bm.asComposeImageBitmap()
            } else {
                val scale = targetDimension.toFloat() / maxOf(bm.width, bm.height)
                val targetW = (bm.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (bm.height * scale).roundToInt().coerceAtLeast(1)

                val surface = Surface.makeRasterN32Premul(targetW, targetH)
                val downsampledImage = Image.makeFromBitmap(bm)
                surface.canvas.drawImageRect(
                    downsampledImage,
                    Rect.makeWH(bm.width.toFloat(), bm.height.toFloat()),
                    Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                    SamplingMode.MITCHELL,
                    null,
                    true
                )
                val snapshot = surface.makeImageSnapshot()
                surface.close()
                downsampledImage.close()
                bm.close()
                snapshot.toComposeImageBitmap()
            }
        } catch (_: Throwable) {
            bm.close()
            null
        }
    }
}

@Composable
fun AsyncArtworkImage(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    targetSize: ArtworkSize = ArtworkSize.SMALL,
    shape: Shape? = null,
    placeholderIcon: DrawableResource = Res.drawable.ic_rounded_music_note,
    placeholderIconSize: Dp = 24.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val density = LocalDensity.current.density
    val targetDimensionPx = remember(targetSize, density) {
        when (targetSize) {
            ArtworkSize.SMALL -> (128 * density).roundToInt().coerceAtLeast(128)
            ArtworkSize.LARGE -> 512
        }
    }

    val cacheKey = remember(artworkUri, targetSize) {
        artworkUri?.takeIf { it.isNotBlank() }?.let {
            ArtworkCacheKey(it, targetSize)
        }
    }

    var imageBitmap by remember(cacheKey) {
        mutableStateOf(cacheKey?.let { ArtworkLoader.getCached(it) })
    }

    LaunchedEffect(cacheKey, targetDimensionPx) {
        if (cacheKey == null) {
            imageBitmap = null
            return@LaunchedEffect
        }

        val cached = ArtworkLoader.getCached(cacheKey)
        if (cached != null) {
            if (imageBitmap != cached) {
                imageBitmap = cached
            }
            return@LaunchedEffect
        }

        val loaded = ArtworkLoader.loadArtwork(cacheKey, targetDimensionPx)
        imageBitmap = loaded
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
                painter = painterResource(placeholderIcon),
                contentDescription = contentDescription,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(placeholderIconSize)
            )
        }
    }
}
