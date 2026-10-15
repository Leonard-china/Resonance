package com.resonance.player.features

import com.resonance.player.model.AiImage
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

object DesktopAssistantImages {
    fun select(multiple: Boolean): List<Path> {
        var files = emptyList<Path>()
        SwingUtilities.invokeAndWait {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = multiple
                fileFilter = if (multiple) javax.swing.filechooser.FileNameExtensionFilter("歌单截图 PNG/JPG", "png", "jpg", "jpeg")
                    else javax.swing.filechooser.FileNameExtensionFilter("封面 PNG/JPG/WebP", "png", "jpg", "jpeg", "webp")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) files = if (multiple) chooser.selectedFiles.map { it.toPath() } else listOf(chooser.selectedFile.toPath())
        }
        return files
    }
    fun slices(paths: List<Path>): List<AiImage> {
        require(paths.size <= 8) { "一次最多选择 8 张截图，请分批识别" }
        val all = paths.flatMap { path ->
        require(java.nio.file.Files.size(path) <= 30_000_000) { "截图超过 30 MB" }
        ImageIO.createImageInputStream(path.toFile()).use { stream ->
        val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: error("图片格式无法读取")
        try {
        reader.input = stream
        val originalWidth = reader.getWidth(0); val originalHeight = reader.getHeight(0)
        require(originalWidth.toLong() * originalHeight <= 120_000_000) { "截图像素过多，请分段选择" }
        val width = minOf(originalWidth, 1600)
        val scale = width.toDouble() / originalWidth
        val height = (originalHeight * scale).toInt()
        require(height <= 1800 + 39 * 1680) { "截图过长，请分批识别，避免漏掉末尾" }
        val result = mutableListOf<AiImage>(); var y = 0
        while (y < height) {
            val h = minOf(1800, height - y)
            val sourceY = (y / scale).toInt()
            val sourceHeight = minOf(originalHeight - sourceY, kotlin.math.ceil(h / scale).toInt())
            val sample = (originalWidth / 1600).coerceAtLeast(1)
            val parameters = reader.defaultReadParam.apply { sourceRegion = java.awt.Rectangle(0, sourceY, originalWidth, sourceHeight); setSourceSubsampling(sample, sample, 0, 0) }
            val region = reader.read(0, parameters)
            val image = BufferedImage(width, h, BufferedImage.TYPE_INT_RGB)
            image.createGraphics().apply { setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); drawImage(region, 0, 0, width, h, null); dispose() }
            val bytes = ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
            result += AiImage(bytes)
            if (y + h >= height) break
            y += 1680
        }
        result
        } finally { reader.dispose() }
        }
        }
        require(all.size <= 40 && all.sumOf { it.bytes.size.toLong() } <= 30_000_000) { "截图片段过多，请分批识别" }
        return all
    }
}
