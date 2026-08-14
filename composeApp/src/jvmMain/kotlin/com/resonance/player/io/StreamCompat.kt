package com.resonance.player.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Android-safe replacement for the Java 9 InputStream.readNBytes API. */
internal fun InputStream.readUpTo(limit: Int): ByteArray {
    require(limit >= 0) { "读取上限不能为负数" }
    if (limit == 0) return ByteArray(0)
    val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(minOf(limit, DEFAULT_BUFFER_SIZE))
    var remaining = limit
    while (remaining > 0) {
        val read = read(buffer, 0, minOf(buffer.size, remaining))
        if (read < 0) break
        if (read == 0) {
            val single = read()
            if (single < 0) break
            output.write(single)
            remaining -= 1
        } else {
            output.write(buffer, 0, read)
            remaining -= read
        }
    }
    return output.toByteArray()
}
