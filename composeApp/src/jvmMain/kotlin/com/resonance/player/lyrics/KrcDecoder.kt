package com.resonance.player.lyrics

import com.resonance.player.model.LyricLine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.InflaterInputStream

object KrcDecoder {
    private val KRC_MAGIC = byteArrayOf(0x6B, 0x72, 0x63, 0x31) // "krc1"
    private val KRC_KEY = byteArrayOf(
        0x40, 0x47, 0x61, 0x77, 0x5e, 0x32, 0x74, 0x47,
        0x51, 0x36, 0x31, 0x2d, 0xce.toByte(), 0xd2.toByte(), 0x6e, 0x69
    )

    fun decode(bytes: ByteArray): List<LyricLine> {
        if (bytes.size < 4) return emptyList()
        val text = if (bytes.take(4).toByteArray().contentEquals(KRC_MAGIC)) {
            decryptKrc(bytes)
        } else {
            String(bytes, StandardCharsets.UTF_8)
        }
        return parseKrcOrLrcText(text)
    }

    private fun decryptKrc(bytes: ByteArray): String {
        val encrypted = bytes.copyOfRange(4, bytes.size)
        val decrypted = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            decrypted[i] = (encrypted[i].toInt() xor KRC_KEY[i % KRC_KEY.size].toInt()).toByte()
        }
        return try {
            InflaterInputStream(ByteArrayInputStream(decrypted)).use { inflater ->
                val buffer = ByteArray(4096)
                val out = ByteArrayOutputStream()
                var read: Int
                while (inflater.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.toString(StandardCharsets.UTF_8.name())
            }
        } catch (_: Throwable) {
            String(decrypted, StandardCharsets.UTF_8)
        }
    }

    fun parseKrcOrLrcText(text: String): List<LyricLine> {
        if (text.isBlank()) return emptyList()
        val krcLinePattern = Regex("""\[(\d+),(\d+)](.*)""")
        val wordTagPattern = Regex("""<(\d+),(\d+),\d+>""")
        val lines = mutableListOf<LyricLine>()

        var offsetMs = 0L
        val offsetMatch = Regex("""\[offset:\s*(-?\d+)]""").find(text)
        if (offsetMatch != null) {
            offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        for (rawLine in text.lineSequence()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || trimmed.startsWith("[hash:") ||
                trimmed.startsWith("[sign:") || trimmed.startsWith("[qq:") || trimmed.startsWith("[total:") ||
                trimmed.startsWith("[offset:") || trimmed.startsWith("[id:") || trimmed.startsWith("[language:")
            ) {
                continue
            }

            val krcMatch = krcLinePattern.matchEntire(trimmed)
            if (krcMatch != null) {
                val startMs = krcMatch.groupValues[1].toLongOrNull() ?: 0L
                val rawContent = krcMatch.groupValues[3]
                val cleanText = wordTagPattern.replace(rawContent, "").trim()
                if (cleanText.isNotEmpty()) {
                    lines += LyricLine(timestampMs = (startMs + offsetMs).coerceAtLeast(0L), text = cleanText)
                }
                continue
            }

            val parsedLrc = parseLrc(trimmed)
            if (parsedLrc.isNotEmpty()) {
                lines.addAll(parsedLrc)
            }
        }

        return lines.sortedBy { it.timestampMs }.distinctBy { it.timestampMs to it.text }
    }
}
