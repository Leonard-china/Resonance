package com.resonance.player.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtLoggingLevel
import com.resonance.player.model.AiImage
import com.resonance.player.model.ImportProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.FloatBuffer
import java.security.MessageDigest
import kotlin.math.*

/** CPU-only PP-OCRv5. Sessions, image pixels and tensors live only for this import. */
class PaddleScreenshotOcr(
    private val asset: (String) -> InputStream,
    private val decode: (ByteArray) -> OcrPixels,
) : ScreenshotOcr {
    override suspend fun recognize(images: List<AiImage>, progress: (ImportProgress) -> Unit): List<OcrPage> = try { withContext(Dispatchers.Default) {
        require(images.size in 1..40 && images.sumOf { it.bytes.size.toLong() } <= 30_000_000) { "截图过多或过大，请分批选择" }
        val environment = OrtEnvironment.getEnvironment()
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(2)
            options.setInterOpNumThreads(1)
            options.setCPUArenaAllocator(false)
            options.setMemoryPatternOptimization(false)
            options.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_ERROR)
            val dictionary = listOf("") + asset("ocr/characters.txt").bufferedReader(Charsets.UTF_8).use { it.readLines() } + " "
            progress(ImportProgress("加载本地 OCR", detail = "图片在本机处理，不调用识图 API"))
            environment.createSession(model("det", "a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d"), options).use { detector ->
                environment.createSession(model("rec", "da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092"), options).use { recognizer ->
                    images.mapIndexed { index, image ->
                        currentCoroutineContext().ensureActive()
                        progress(ImportProgress("本地识别截图", "第 ${index + 1} / ${images.size} 片", index, images.size, index.toFloat() / images.size))
                        val pixels = decode(image.bytes)
                        require(pixels.width <= 2000 && pixels.height <= 2200) { "截图分片尺寸过大，请重新选择图片" }
                        val lines = mutableListOf<OcrLine>()
                        for (box in detect(environment, detector, pixels)) {
                            currentCoroutineContext().ensureActive()
                            val (text, confidence) = read(environment, recognizer, pixels, box, dictionary)
                            if (text.isNotBlank() && confidence >= .5f) lines += box.copy(text = text.trim(), confidence = confidence)
                        }
                        OcrPage(pixels.width, pixels.height, lines.sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left }))
                    }
                }
            }
        }
    } } catch (error: LinkageError) {
        throw IllegalStateException("本机无法加载本地 OCR 引擎，请安装完整正式版或在设置切换到云端 API", error)
    }

    private fun model(name: String, expected: String): ByteArray {
        val bytes = asset("ocr/$name.onnx").use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(hash == expected) { "本地 OCR 模型损坏，请重新安装正式版，或在设置切换到云端 API" }
        return bytes
    }

    private fun detect(env: OrtEnvironment, session: OrtSession, image: OcrPixels): List<OcrLine> {
        val ratio = min(1.0, 960.0 / max(image.width, image.height))
        val w = ((image.width * ratio / 32).roundToInt() * 32).coerceAtLeast(32)
        val h = ((image.height * ratio / 32).roundToInt() * 32).coerceAtLeast(32)
        val full = OcrLine("", 0, 0, image.width, image.height)
        val input = tensor(image, full, w, h, w, true)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 3, h.toLong(), w.toLong())).use { value ->
            session.run(mapOf(session.inputNames.first() to value)).use { result ->
                val output = result[0] as OnnxTensor
                val shape = output.info.shape
                val mh = shape[shape.size - 2].toInt(); val mw = shape.last().toInt()
                val scores = output.floatBuffer
                val visited = BooleanArray(mw * mh)
                val queue = IntArray(mw * mh)
                val boxes = mutableListOf<OcrLine>()
                for (start in visited.indices) {
                    if (visited[start] || scores[start] < .3f) continue
                    var head = 0; var tail = 1; queue[0] = start; visited[start] = true
                    var x0 = mw; var y0 = mh; var x1 = 0; var y1 = 0; var total = 0f
                    while (head < tail) {
                        val p = queue[head++]; val x = p % mw; val y = p / mw
                        x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y)
                        total += scores[p]
                        for (dy in -1..1) for (dx in -1..1) {
                            val nx = x + dx; val ny = y + dy
                            if (nx !in 0 until mw || ny !in 0 until mh) continue
                            val n = ny * mw + nx
                            if (!visited[n] && scores[n] >= .3f) { visited[n] = true; queue[tail++] = n }
                        }
                    }
                    if (x1 - x0 < 3 || y1 - y0 < 2 || total / tail < .6f) continue
                    // Screenshots contain horizontal text. DB's axis-aligned unclip preserves its line padding.
                    val pad = (x1 - x0 + 1f) * (y1 - y0 + 1f) * 1.5f / (2 * (x1 - x0 + y1 - y0 + 2))
                    boxes += OcrLine("", ((x0 - pad) * image.width / mw).toInt().coerceAtLeast(0),
                        ((y0 - pad) * image.height / mh).toInt().coerceAtLeast(0),
                        ((x1 + pad + 1) * image.width / mw).toInt().coerceAtMost(image.width),
                        ((y1 + pad + 1) * image.height / mh).toInt().coerceAtMost(image.height))
                    check(boxes.size <= 500) { "截图文字过于密集，请分段选择" }
                }
                return boxes.sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left })
            }
        }
    }

    private fun read(env: OrtEnvironment, session: OrtSession, image: OcrPixels, box: OcrLine, dictionary: List<String>): Pair<String, Float> {
        val resized = ceil(48.0 * (box.right - box.left) / box.height).toInt().coerceIn(8, 1280)
        val width = max(320, resized)
        val input = tensor(image, box, resized, 48, width, false)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 3, 48, width.toLong())).use { value ->
            session.run(mapOf(session.inputNames.first() to value)).use { result ->
                val output = result[0] as OnnxTensor
                val shape = output.info.shape; val count = shape.last().toInt(); val steps = shape[shape.size - 2].toInt()
                check(count == dictionary.size) { "本地 OCR 字典与模型不匹配" }
                val scores = output.floatBuffer
                val text = StringBuilder(); var previous = -1; var confidence = 0f; var letters = 0
                for (t in 0 until steps) {
                    var best = 0; var score = scores[t * count]
                    for (c in 1 until count) if (scores[t * count + c] > score) { best = c; score = scores[t * count + c] }
                    if (best != 0 && best != previous) { text.append(dictionary[best]); confidence += score; letters++ }
                    previous = best
                }
                return text.toString() to if (letters == 0) 0f else confidence / letters
            }
        }
    }

    /** BGR and normalization match the official inference.yml; padding stays zero after normalization. */
    private fun tensor(image: OcrPixels, crop: OcrLine, width: Int, height: Int, paddedWidth: Int, detector: Boolean): FloatArray {
        val data = FloatArray(3 * height * paddedWidth)
        val means = floatArrayOf(.485f, .456f, .406f); val std = floatArrayOf(.229f, .224f, .225f)
        for (y in 0 until height) {
            val sy = (crop.top + (y + .5) * crop.height / height - .5).coerceIn(0.0, (image.height - 1).toDouble())
            val y0 = sy.toInt(); val y1 = min(y0 + 1, image.height - 1); val fy = (sy - y0).toFloat()
            for (x in 0 until width) {
                val sx = (crop.left + (x + .5) * (crop.right - crop.left) / width - .5).coerceIn(0.0, (image.width - 1).toDouble())
                val x0 = sx.toInt(); val x1 = min(x0 + 1, image.width - 1); val fx = (sx - x0).toFloat()
                for (channel in 0..2) {
                    fun v(xx: Int, yy: Int) = ((image.argb[yy * image.width + xx] ushr (8 * channel)) and 255).toFloat()
                    val a = v(x0, y0) * (1 - fx) + v(x1, y0) * fx
                    val b = v(x0, y1) * (1 - fx) + v(x1, y1) * fx
                    val pixel = (a * (1 - fy) + b * fy) / 255f
                    data[channel * height * paddedWidth + y * paddedWidth + x] = if (detector) (pixel - means[channel]) / std[channel] else (pixel - .5f) / .5f
                }
            }
        }
        return data
    }
}
