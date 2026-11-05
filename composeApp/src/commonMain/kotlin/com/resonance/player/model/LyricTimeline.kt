package com.resonance.player.model

/** A timestamp index is built once per lyrics instance, never once per playback tick. */
class LyricTimeline(private val lines: List<LyricLine>) {
    private val times = LongArray(lines.size) { lines[it].timestampMs ?: Long.MAX_VALUE }
    private val ordered = times.indices.drop(1).all { times[it] >= times[it - 1] }
    fun activeIndex(positionMs: Long): Int {
        if (!ordered) return lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
        var low = 0; var high = times.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (times[middle] <= positionMs) low = middle + 1 else high = middle
        }
        return low - 1
    }
}
