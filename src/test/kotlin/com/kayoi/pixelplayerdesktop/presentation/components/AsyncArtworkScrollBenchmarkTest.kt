package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.management.ManagementFactory
import java.util.Collections
import java.util.LinkedHashMap
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class AsyncArtworkScrollBenchmarkTest {

    private fun getProcessRssKb(): Long {
        return try {
            val statusFile = File("/proc/self/status")
            if (statusFile.exists()) {
                val line = statusFile.readLines().firstOrNull { it.startsWith("VmRSS:") }
                line?.substringAfter(":")?.trim()?.substringBefore(" ")?.toLongOrNull() ?: 0L
            } else 0L
        } catch (_: Throwable) {
            0L
        }
    }

    private fun getGcStats(): Pair<Long, Long> {
        var count = 0L
        var timeMs = 0L
        for (gcBean in ManagementFactory.getGarbageCollectorMXBeans()) {
            val c = gcBean.collectionCount
            val t = gcBean.collectionTime
            if (c > 0) count += c
            if (t > 0) timeMs += t
        }
        return Pair(count, timeMs)
    }

    // Previous implementation logic (before our changes)
    private fun oldDecodeAndScaleArtwork(bytes: ByteArray, maxDimension: Int = 512): ImageBitmap? {
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

    @Test
    fun benchmarkComparison5kTrackScroll() {
        val tempDir = File.createTempFile("artwork_bench", "dir")
        tempDir.delete()
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        // Create 100 unique high-res covers (1500x1500px JPEG) shared across 5,000 tracks
        val coverFiles = (0 until 100).map { i ->
            val img = BufferedImage(1500, 1500, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.color = Color(i * 12 % 255, (i * 35) % 255, (i * 70) % 255)
            g.fillRect(0, 0, 1500, 1500)
            g.dispose()

            val f = File(tempDir, "cover_$i.jpg")
            val baos = ByteArrayOutputStream()
            ImageIO.write(img, "jpeg", baos)
            f.writeBytes(baos.toByteArray())
            f.deleteOnExit()
            f.absolutePath
        }

        val trackArtworks = (0 until 5000).map { i ->
            coverFiles[i % coverFiles.size]
        }

        // Warm up Skiko and JVM so native libraries are loaded
        ArtworkLoader.decodeAndScaleArtwork(File(coverFiles[0]).readBytes(), 128)
        oldDecodeAndScaleArtwork(File(coverFiles[0]).readBytes(), 512)
        System.gc()
        Thread.sleep(300)

        // ---------------------------------------------------------
        // 1. BENCHMARK BEFORE (Old: decode to 512, 80 entry count cache, no in-flight deduplication, no cancellation)
        // ---------------------------------------------------------
        val oldCache = Collections.synchronizedMap(
            object : LinkedHashMap<String, ImageBitmap>(80, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
                    return size > 80
                }
            }
        )

        System.gc()
        Thread.sleep(200)
        val oldRssBefore = getProcessRssKb() / 1024.0
        val oldHeapBefore = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0)
        val (oldGcCountBefore, oldGcTimeBefore) = getGcStats()

        val oldJobs = mutableListOf<Job>()
        val oldElapsed = measureTimeMillis {
            runBlocking {
                val windowSize = 12
                val step = 15
                var currentTop = 0
                while (currentTop < trackArtworks.size) {
                    val visibleIndices = currentTop until minOf(currentTop + windowSize, trackArtworks.size)
                    for (idx in visibleIndices) {
                        val uri = trackArtworks[idx]
                        val job = launch(Dispatchers.IO) {
                            val cached = oldCache[uri]
                            if (cached == null) {
                                val bytes = File(uri).readBytes()
                                val decoded = oldDecodeAndScaleArtwork(bytes, 512)
                                if (decoded != null) oldCache[uri] = decoded
                            }
                        }
                        oldJobs.add(job)
                    }
                    delay(5)
                    currentTop += step
                }
                oldJobs.forEach { it.join() }
            }
        }

        val oldRssAfter = getProcessRssKb() / 1024.0
        val oldHeapAfter = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0)
        val (oldGcCountAfter, oldGcTimeAfter) = getGcStats()

        // Approximate old pixel memory: 80 entries * 512 * 512 * 4 = 83.88 MB
        val oldPixelMemoryMb = oldCache.size * (512 * 512 * 4) / (1024 * 1024.0)

        oldCache.clear()
        System.gc()
        Thread.sleep(500)

        // ---------------------------------------------------------
        // 2. BENCHMARK AFTER (New: downsampled decode to 128, 32MB byte limit, deduplication, cancellation)
        // ---------------------------------------------------------
        ArtworkLoader.memoryCache.clear()
        System.gc()
        Thread.sleep(200)
        val newRssBefore = getProcessRssKb() / 1024.0
        val newHeapBefore = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0)
        val (newGcCountBefore, newGcTimeBefore) = getGcStats()

        val newJobs = mutableListOf<Job>()
        val newElapsed = measureTimeMillis {
            runBlocking {
                val windowSize = 12
                val step = 15
                var currentTop = 0
                while (currentTop < trackArtworks.size) {
                    val visibleIndices = currentTop until minOf(currentTop + windowSize, trackArtworks.size)
                    // Cancel scrolled-away jobs
                    newJobs.forEach { it.cancel() }
                    newJobs.clear()

                    for (idx in visibleIndices) {
                        val uri = trackArtworks[idx]
                        val key = ArtworkCacheKey(uri, ArtworkSize.SMALL)
                        val job = launch {
                            try {
                                ArtworkLoader.loadArtwork(key, 128)
                            } catch (_: CancellationException) {
                                // Scrolled away
                            }
                        }
                        newJobs.add(job)
                    }
                    delay(5)
                    currentTop += step
                }
                newJobs.forEach { it.join() }
            }
        }

        val newRssAfter = getProcessRssKb() / 1024.0
        val newHeapAfter = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0)
        val (newGcCountAfter, newGcTimeAfter) = getGcStats()

        val newPixelMemoryMb = ArtworkLoader.memoryCache.sizeBytes() / (1024 * 1024.0)

        println("\n=======================================================")
        println("       5K-TRACK FAST SCROLL PERFORMANCE COMPARISON      ")
        println("=======================================================")
        println("METRIC                   | BEFORE (512px) | AFTER (128px)")
        println("-------------------------+----------------+--------------")
        println("Total Scroll Time        | %7d ms      | %7d ms".format(oldElapsed, newElapsed))
        println("Process RSS Peak/After   | %7.1f MB      | %7.1f MB".format(oldRssAfter, newRssAfter))
        println("Process RSS Delta        | %+7.1f MB      | %+7.1f MB".format(oldRssAfter - oldRssBefore, newRssAfter - newRssBefore))
        println("JVM Heap Delta           | %+7.1f MB      | %+7.1f MB".format(oldHeapAfter - oldHeapBefore, newHeapAfter - newHeapBefore))
        println("GC Collections           | %7d         | %7d".format(oldGcCountAfter - oldGcCountBefore, newGcCountAfter - newGcCountBefore))
        println("GC Pause Time            | %7d ms      | %7d ms".format(oldGcTimeAfter - oldGcTimeBefore, newGcTimeAfter - newGcTimeBefore))
        println("Pixel Memory in Cache    | %7.1f MB      | %7.1f MB".format(oldPixelMemoryMb, newPixelMemoryMb))
        println("Cache Entries Held       | %7d         | %7d".format(oldCache.size, ArtworkLoader.memoryCache.entryCount()))
        println("=======================================================\n")

        tempDir.deleteRecursively()
    }
}
