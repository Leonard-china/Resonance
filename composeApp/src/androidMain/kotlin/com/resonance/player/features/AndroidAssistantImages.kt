package com.resonance.player.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import com.resonance.player.model.AiImage
import java.io.ByteArrayOutputStream

object AndroidAssistantImages {
    @Suppress("DEPRECATION")
    fun slices(context: Context, uris: List<Uri>): List<AiImage> {
        val result = mutableListOf<AiImage>()
        require(uris.size <= 8) { "一次最多选择 8 张截图，请分批识别" }
        uris.forEach { uri ->
            context.contentResolver.openInputStream(uri).use { stream ->
                val decoder = requireNotNull(BitmapRegionDecoder.newInstance(requireNotNull(stream), false)) { "图片格式不支持分段读取" }
                try {
                    val width = decoder.width; val height = decoder.height
                    require(width.toLong() * height <= 120_000_000) { "截图像素过多，请分段选择" }
                    val sample = ((width + 1599) / 1600).coerceAtLeast(1)
                    var y = 0
                    while (y < height) {
                        require(result.size < 40) { "截图过长，请分批导入" }
                        val bottom = minOf(y + 1800 * sample, height)
                        val bitmap = decoder.decodeRegion(Rect(0, y, width, bottom), BitmapFactory.Options().apply { inSampleSize = sample })
                        val output = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output); bitmap.recycle()
                        result += AiImage(output.toByteArray())
                        require(result.sumOf { it.bytes.size.toLong() } <= 30_000_000) { "截图内容过大，请分批识别" }
                        if (bottom == height) break
                        y += 1680 * sample
                    }
                } finally { decoder.recycle() }
            }
        }
        return result
    }
}
