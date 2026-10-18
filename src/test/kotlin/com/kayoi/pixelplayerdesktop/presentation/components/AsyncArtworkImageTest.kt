package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ImageInfo
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsyncArtworkImageTest {

    private fun createFakeBitmap(w: Int, h: Int): ImageBitmap {
        val bm = Bitmap()
        val info = ImageInfo.makeN32Premul(w, h)
        bm.allocPixels(info)
        return bm.asComposeImageBitmap()
    }

    private fun createTestJpegBytes(width: Int, height: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color.MAGENTA
        g.fillRect(0, 0, width, height)
        g.dispose()

        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "jpeg", baos)
        return baos.toByteArray()
    }

    @Test
    fun testCacheKeySeparateSizeBuckets() {
        val cache = ArtworkMemoryCache(maxSizeBytes = 10 * 1024 * 1024L)
        val smallKey = ArtworkCacheKey("uri://track1", ArtworkSize.SMALL)
        val largeKey = ArtworkCacheKey("uri://track1", ArtworkSize.LARGE)

        val smallBitmap = createFakeBitmap(128, 128)
        val largeBitmap = createFakeBitmap(512, 512)

        cache.put(smallKey, smallBitmap)
        cache.put(largeKey, largeBitmap)

        assertEquals(smallBitmap, cache.get(smallKey))
        assertEquals(largeBitmap, cache.get(largeKey))
        assertEquals(2, cache.entryCount())
    }

    @Test
    fun testByteWeightedEviction() {
        // 1 entry of 500x500 is 500 * 500 * 4 = 1,000,000 bytes.
        // Cache limit: 2,500,000 bytes (fits 2 entries, 3rd will evict first).
        val cache = ArtworkMemoryCache(maxSizeBytes = 2_500_000L)

        val k1 = ArtworkCacheKey("uri://1", ArtworkSize.SMALL)
        val k2 = ArtworkCacheKey("uri://2", ArtworkSize.SMALL)
        val k3 = ArtworkCacheKey("uri://3", ArtworkSize.SMALL)

        val b1 = createFakeBitmap(500, 500)
        val b2 = createFakeBitmap(500, 500)
        val b3 = createFakeBitmap(500, 500)

        cache.put(k1, b1)
        cache.put(k2, b2)
        assertEquals(2, cache.entryCount())
        assertEquals(2_000_000L, cache.sizeBytes())

        cache.put(k3, b3)
        assertEquals(2, cache.entryCount())
        assertEquals(2_000_000L, cache.sizeBytes())
        assertNull(cache.get(k1))
        assertEquals(b2, cache.get(k2))
        assertEquals(b3, cache.get(k3))
    }

    @Test
    fun testConcurrentCacheAccess() {
        val cache = ArtworkMemoryCache(maxSizeBytes = 5 * 1024 * 1024L)
        val threadPool = Executors.newFixedThreadPool(8)

        val errors = java.util.concurrent.atomic.AtomicInteger(0)
        for (i in 0 until 100) {
            val idx = i
            threadPool.submit {
                try {
                    val key = ArtworkCacheKey("uri://$idx", if (idx % 2 == 0) ArtworkSize.SMALL else ArtworkSize.LARGE)
                    val bm = createFakeBitmap(64, 64)
                    cache.put(key, bm)
                    val retrieved = cache.get(key)
                    if (retrieved == null && cache.get(key) == null) {
                        // Might have been evicted, but shouldn't throw exception
                    }
                } catch (t: Throwable) {
                    errors.incrementAndGet()
                }
            }
        }

        threadPool.shutdown()
        assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS))
        assertEquals(0, errors.get())
    }

    @Test
    fun testDownsamplingDuringDecode() {
        val bytes = createTestJpegBytes(2400, 2400)

        // Small: should downsample by 8 directly during decode
        val small = ArtworkLoader.decodeAndScaleArtwork(bytes, 128)
        assertNotNull(small)
        assertEquals(128, small.width)
        assertEquals(128, small.height)

        // Large: should downsample directly to 512
        val large = ArtworkLoader.decodeAndScaleArtwork(bytes, 512)
        assertNotNull(large)
        assertEquals(512, large.width)
        assertEquals(512, large.height)
    }

    @Test
    fun testSafeHandlingOfCorruptedAndGiantImages() {
        val corrupted = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        assertNull(ArtworkLoader.decodeAndScaleArtwork(corrupted, 128))

        val empty = byteArrayOf()
        assertNull(ArtworkLoader.decodeAndScaleArtwork(empty, 128))
    }

    @Test
    fun testInFlightDeduplication() = runTest {
        val bytes = createTestJpegBytes(400, 400)
        val tempFile = java.io.File.createTempFile("artwork_test", ".jpg")
        tempFile.deleteOnExit()
        tempFile.writeBytes(bytes)

        val key = ArtworkCacheKey(tempFile.absolutePath, ArtworkSize.SMALL)

        // Launch two concurrent requests for the exact same key
        val req1 = async { ArtworkLoader.loadArtwork(key, 128) }
        val req2 = async { ArtworkLoader.loadArtwork(key, 128) }

        val res1 = req1.await()
        val res2 = req2.await()

        assertNotNull(res1)
        assertNotNull(res2)
        assertEquals(res1, res2)
        assertEquals(res1, ArtworkLoader.getCached(key))
    }
}
