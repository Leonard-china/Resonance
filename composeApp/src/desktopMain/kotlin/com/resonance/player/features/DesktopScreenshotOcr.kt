package com.resonance.player.features

import com.resonance.player.ocr.OcrPixels
import com.resonance.player.ocr.PaddleScreenshotOcr
import com.resonance.player.ocr.ScreenshotOcr
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

fun desktopScreenshotOcr(): ScreenshotOcr = PaddleScreenshotOcr(
    asset = { requireNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(it)) { "安装包缺少本地 OCR 模型，请重新安装" } },
    decode = { bytes ->
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: error("无法读取截图")
            try {
                reader.input = input
                val w = reader.getWidth(0); val h = reader.getHeight(0)
                require(w in 1..2000 && h in 1..2200) { "截图分片尺寸无效，请重新选择图片" }
                val image = reader.read(0)
                try { OcrPixels(w, h, image.getRGB(0, 0, w, h, null, 0, w)) } finally { image.flush() }
            } finally { reader.dispose() }
        }
    },
)
