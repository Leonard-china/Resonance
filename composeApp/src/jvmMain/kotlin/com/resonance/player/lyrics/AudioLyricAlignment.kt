package com.resonance.player.lyrics

import com.resonance.player.model.*

data class SpeechWord(val text: String, val beginMs: Long, val endMs: Long)
data class SpeechRecognition(val lines: List<LyricLine>, val words: List<SpeechWord>, val seconds: Int)

/** Transfer only observed audio timestamps onto matching original text. Never distribute time uniformly. */
object AudioLyricAlignment {
    fun fromWords(words: List<SpeechWord>): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        var text = StringBuilder(); var begin = 0L; var end = 0L
        fun flush() { if (text.isNotBlank()) lines += LyricLine(begin, text.toString().trim()); text = StringBuilder() }
        for (word in words) {
            val limit = if ((text.toString() + word.text).any { it.code in 0x3400..0x9fff }) 22 else 64
            if (text.isNotEmpty() && (word.beginMs - end > 900 || word.beginMs - begin > 6000 || text.length + word.text.length > limit)) flush()
            if (text.isEmpty()) begin = word.beginMs
            else if (text.last().isLetter() && text.last().code < 128 && word.text.firstOrNull()?.let { it.isLetter() && it.code < 128 } == true) text.append(' ')
            text.append(word.text); end = word.endMs
            if (word.text.lastOrNull() in listOf('。', '！', '？', '.', '!', '?', ';', '；')) flush()
        }
        flush()
        return lines
    }
    fun align(original: Lyrics?, recognized: SpeechRecognition): List<LyricLine> {
        val source = original?.lines?.filter { it.text.isNotBlank() }.orEmpty()
        if (source.isEmpty() || recognized.words.isEmpty()) return recognized.lines
        val raw = source.map { normalize(it.text) }
        val text = raw.joinToString("")
        val heard = StringBuilder()
        val times = mutableListOf<Long>()
        recognized.words.forEach { word -> normalize(word.text).forEach { heard.append(it); times += word.beginMs } }
        if (text.isEmpty() || heard.isEmpty() || text.length > 5000 || heard.length > 7000) return recognized.lines
        val width = heard.length + 1
        val path = ByteArray((text.length + 1) * width)
        var previous = IntArray(width) { it }
        for (i in 1..text.length) {
            val row = IntArray(width); row[0] = i
            for (j in 1..heard.length) {
                val diagonal = previous[j - 1] + if (text[i - 1] == heard[j - 1]) 0 else 1
                val drop = previous[j] + 1; val insert = row[j - 1] + 1
                val best = minOf(diagonal, drop, insert); row[j] = best
                path[i * width + j] = when (best) { diagonal -> 1; drop -> 2; else -> 3 }
            }
            previous = row
        }
        val matched = IntArray(text.length) { -1 }
        var i = text.length; var j = heard.length
        while (i > 0 && j > 0) {
            when (path[i * width + j].toInt()) {
                1 -> { if (text[i - 1] == heard[j - 1]) matched[i - 1] = j - 1; i--; j-- }
                2 -> i--
                else -> j--
            }
        }
        if (matched.count { it >= 0 }.toDouble() / text.length < 0.7) return recognized.lines
        var offset = 0
        val aligned = source.mapIndexed { n, line ->
            val positions = matched.sliceArray(offset until offset + raw[n].length); offset += raw[n].length
            val first = positions.firstOrNull { it >= 0 }
            if (first == null || positions.count { it >= 0 } < (raw[n].length + 1) / 2) return recognized.lines
            line.copy(timestampMs = times[first], translation = null)
        }
        return aligned.takeIf { lines -> lines.zipWithNext().all { (a,b) -> a.timestampMs!! <= b.timestampMs!! } } ?: recognized.lines
    }
    private fun normalize(text: String) = text.lowercase().filter(Char::isLetterOrDigit)
}
