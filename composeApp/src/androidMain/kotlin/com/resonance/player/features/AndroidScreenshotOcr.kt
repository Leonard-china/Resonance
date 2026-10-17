package com.resonance.player.features

import android.content.Context
import android.graphics.BitmapFactory
import com.resonance.player.ocr.OcrPixels
import com.resonance.player.ocr.PaddleScreenshotOcr
import com.resonance.player.ocr.ScreenshotOcr

fun androidScreenshotOcr(context: Context): ScreenshotOcr = PaddleScreenshotOcr(
    asset = { context.applicationContext.assets.open(it) },
    decode = { bytes ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth in 1..2000 && options.outHeight in 1..2200) { "截图分片尺寸无效，请重新选择图片" }
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "无法读取截图" }
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            OcrPixels(bitmap.width, bitmap.height, pixels)
        } finally { bitmap.recycle() }
    },
)
