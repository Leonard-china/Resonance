package com.resonance.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.features.androidScreenshotOcr
import com.resonance.player.features.AndroidAssistantImages
import com.resonance.player.model.AiImage
import com.resonance.player.ocr.ScreenshotPlaylistParser
import kotlinx.coroutines.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class LocalScreenshotOcrTest {
    @Test fun wideScreenshotsUseRealDecoderScaleWithinSliceBounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("ocr-wide-fixture-", ".jpg", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(3840, 128, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) } } finally { bitmap.recycle() }
            val slices = AndroidAssistantImages.slices(context, listOf(Uri.fromFile(file)))
            assertTrue(slices.isNotEmpty())
            slices.forEach { image ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, bounds)
                assertTrue("${bounds.outWidth}x${bounds.outHeight}", bounds.outWidth in 1..1600 && bounds.outHeight in 1..1800)
            }
        } finally { file.delete() }
    }

    @Test fun bundledOfflineOcrReadsBilingualPlaylistAndReleasesSessions() = runBlocking {
        val bitmap = Bitmap.createBitmap(900, 780, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        paint.textSize = 38f; canvas.drawText("晴天", 100f, 180f, paint)
        paint.textSize = 26f; canvas.drawText("周杰伦 - 叶惠美", 100f, 225f, paint)
        paint.textSize = 36f; canvas.drawText("See You Again", 100f, 340f, paint)
        paint.textSize = 26f; canvas.drawText("Wiz Khalifa - Furious 7", 100f, 385f, paint)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        bitmap.recycle()
        fun pss(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss
        val before = pss(); var peak = before
        val sampler = launch(Dispatchers.IO) { while (isActive) { peak = maxOf(peak, pss()); delay(100) } }
        val started = System.nanoTime()
        try {
            val recognizer = androidScreenshotOcr(InstrumentationRegistry.getInstrumentation().targetContext)
            repeat(2) {
                val pages = recognizer.recognize(listOf(AiImage(bytes, "image/png"))) {}
                val text = pages.flatMap { page -> page.lines }.joinToString("\n") { line -> line.text }
                assertTrue(text, text.contains("晴天") && text.contains("周杰伦") && text.contains("See You Again"))
                val songs = ScreenshotPlaylistParser.parse(pages).flatMap { playlist -> playlist.songs }
                assertTrue(songs.toString(), songs.any { song -> song.title == "晴天" && song.artist == "周杰伦" })
            }
        } finally {
            sampler.cancelAndJoin()
            InstrumentationRegistry.getInstrumentation().addResults(Bundle().apply {
                putString("ocr_memory_kib", "before=$before; peak=$peak; after=${pss()}; two_runs_ms=${(System.nanoTime() - started) / 1_000_000}")
            })
        }
    }
}
