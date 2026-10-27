package com.kayoi.pixelplayerdesktop.ui.theme.dynamic

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking

import com.kayoi.pixelplayerdesktop.data.preferences.AlbumArtPaletteStyle

class DynamicColorProcessorTest {

    @Test
    fun testDynamicColorExtractionFromSampleImage() {
        val image = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = image.createGraphics()
        g.color = Color(30, 30, 40)
        g.fillRect(0, 0, 256, 256)

        g.color = Color(0, 180, 216)
        g.fillOval(50, 50, 156, 156)
        g.dispose()

        val pair = DynamicColorProcessor.extractFromBufferedImage(image)
        assertNotNull(pair)
        assertNotNull(pair.dark)
        assertNotNull(pair.light)

        val primary = pair.dark.primary
        assertTrue(primary.blue > 0.4f || primary.green > 0.4f)

        val tempFile = File.createTempFile("test_artwork", ".png")
        tempFile.deleteOnExit()
        ImageIO.write(image, "png", tempFile)

        val filePair = DynamicColorProcessor.extractColorScheme(tempFile)
        assertNotNull(filePair)
        assertNotNull(filePair.dark)
    }

    @Test
    fun testNeutralWhiteAlbumArtProducesGrayscaleTheme() {
        val image = BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = image.createGraphics()
        g.color = Color(245, 245, 245)
        g.fillRect(0, 0, 128, 128)
        g.dispose()

        val pair = DynamicColorProcessor.extractFromBufferedImage(image)
        assertNotNull(pair)

        // Verify primary and surface are neutral (R == G == B)
        val primary = pair.dark.primary
        assertEquals(primary.red, primary.green, 0.01f)
        assertEquals(primary.green, primary.blue, 0.01f)
    }

    @Test
    fun testPaletteStyles() {
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(220, 50, 50)
        g.fillRect(0, 0, 64, 64)
        g.dispose()

        for (style in AlbumArtPaletteStyle.entries) {
            val pair = DynamicColorProcessor.extractFromBufferedImage(image, paletteStyle = style)
            assertNotNull(pair.light)
            assertNotNull(pair.dark)
        }
    }

    @Test
    fun testMemoryCacheBoundedCap() {
        DynamicColorProcessor.clearCache()
        val tempFiles = mutableListOf<File>()
        try {
            for (i in 0 until 300) {
                val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
                val f = File.createTempFile("art_$i", ".png")
                f.deleteOnExit()
                ImageIO.write(img, "png", f)
                tempFiles.add(f)
                DynamicColorProcessor.extractColorScheme(f)
            }
            assertTrue(DynamicColorProcessor.memoryCacheSize() <= 256)
        } finally {
            tempFiles.forEach { it.delete() }
            DynamicColorProcessor.clearCache()
        }
    }

    @Test
    fun testExtractColorSchemeSuspend() = runBlocking {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        val tempFile = File.createTempFile("suspend_art", ".png")
        tempFile.deleteOnExit()
        try {
            ImageIO.write(image, "png", tempFile)
            val scheme = DynamicColorProcessor.extractColorScheme(tempFile.absolutePath)
            assertNotNull(scheme)
            assertNotNull(scheme.dark)
        } finally {
            tempFile.delete()
        }
    }
}
