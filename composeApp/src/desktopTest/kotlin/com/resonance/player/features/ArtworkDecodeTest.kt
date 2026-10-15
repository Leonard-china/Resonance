package com.resonance.player.features

import com.resonance.player.platform.decodeArtwork
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class ArtworkDecodeTest {
    @Test fun pngAndWebpCoversAreSupportedWithoutKeepingFullResolution() {
        val root = Files.createTempDirectory("artwork-decode-")
        try {
            val png = root.resolve("cover.png")
            ImageIO.write(BufferedImage(1700, 1100, BufferedImage.TYPE_INT_RGB), "png", png.toFile())
            val decoded = assertNotNull(decodeArtwork(png.toString()))
            assertTrue(decoded.width <= 768 && decoded.height <= 768)
            val webp = root.resolve("cover.webp")
            org.jetbrains.skia.Image.makeFromEncoded(Files.readAllBytes(png)).use { image ->
                image.encodeToData(org.jetbrains.skia.EncodedImageFormat.WEBP, 90)!!.use { Files.write(webp, it.bytes) }
            }
            val decodedWebp = assertNotNull(decodeArtwork(webp.toString()))
            assertTrue(decodedWebp.width <= 768 && decodedWebp.height <= 768)
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
}
