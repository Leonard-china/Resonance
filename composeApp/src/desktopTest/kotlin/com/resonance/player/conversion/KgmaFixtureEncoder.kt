package com.resonance.player.conversion

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal object KgmaFixtureEncoder {
    fun write(source: Path, target: Path) {
        val cryptoKey = ByteArray(16) { (it * 17 + 3).toByte() }
        val boxes = KgmaCipher.createBoxes(cryptoKey) { MessageDigest.getInstance("MD5").digest(it) }
        val header = ByteArray(0x3c)
        KgmaCipher.header.copyInto(header)
        writeLeInt(header, 0x10, 0x3c)
        writeLeInt(header, 0x14, 3)
        writeLeInt(header, 0x18, 1)
        cryptoKey.copyInto(header, 0x2c)
        Files.newOutputStream(target).buffered().use { output ->
            output.write(header)
            Files.newInputStream(source).buffered().use { input ->
                val buffer = ByteArray(256 * 1024)
                var absoluteOffset = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    for (index in 0 until read) {
                        val targetByte = buffer[index]
                        for (candidate in 0..255) {
                            val probe = byteArrayOf(candidate.toByte())
                            KgmaCipher.decryptInPlace(probe, 1, absoluteOffset + index, boxes)
                            if (probe[0] == targetByte) {
                                buffer[index] = candidate.toByte()
                                break
                            }
                        }
                    }
                    output.write(buffer, 0, read)
                    absoluteOffset += read
                }
            }
        }
    }

    private fun writeLeInt(target: ByteArray, offset: Int, value: Int) {
        repeat(4) { index -> target[offset + index] = (value ushr (index * 8)).toByte() }
    }
}
