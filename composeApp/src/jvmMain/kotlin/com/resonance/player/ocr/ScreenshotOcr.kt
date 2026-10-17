package com.resonance.player.ocr

import com.resonance.player.model.AiImage
import com.resonance.player.model.ImportProgress

data class OcrPixels(val width: Int, val height: Int, val argb: IntArray) {
    init { require(width > 0 && height > 0 && width.toLong() * height == argb.size.toLong()) }
}

data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int, val confidence: Float = 1f) {
    val height get() = (bottom - top).coerceAtLeast(1)
}

data class OcrPage(val width: Int, val height: Int, val lines: List<OcrLine>)

fun interface ScreenshotOcr {
    suspend fun recognize(images: List<AiImage>, progress: (ImportProgress) -> Unit): List<OcrPage>
}
