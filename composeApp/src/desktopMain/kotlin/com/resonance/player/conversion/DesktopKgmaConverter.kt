package com.resonance.player.conversion

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/** Decrypts KGMA in fixed-size chunks so large lossless files never need to fit in memory. */
class DesktopKgmaConverter {
    fun decrypt(input: Path, outputDirectory: Path): DecryptedAudio {
        val header = ByteArray(MINIMUM_HEADER_SIZE)
        BufferedInputStream(Files.newInputStream(input)).use { source ->
            readFully(source, header)
        }
        require(header.copyOfRange(0, KgmaCipher.header.size).contentEquals(KgmaCipher.header)) {
            "不是有效的 KGM/KGMA 文件"
        }
        val audioOffset = KgmaCipher.littleEndianInt(header, 0x10)
        val version = KgmaCipher.littleEndianInt(header, 0x14)
        val slot = KgmaCipher.littleEndianInt(header, 0x18)
        require(version == 3) { "暂不支持 KGMA v$version（当前支持 v3）" }
        require(slot == 1) { "暂不支持 KGMA 加密槽位 $slot（当前支持 slot 1）" }
        require(audioOffset >= MINIMUM_HEADER_SIZE) { "KGMA 音频偏移无效" }

        val boxes = KgmaCipher.createBoxes(header.copyOfRange(0x2c, 0x3c)) { value ->
            MessageDigest.getInstance("MD5").digest(value)
        }
        val prefix = decryptPrefix(input, audioOffset.toLong(), boxes)
        val format = KgmaCipher.detectAudioFormat(prefix)
        require(format != NativeAudioFormat.Unknown) { "KGMA 解密后不是可识别的音频格式" }

        Files.createDirectories(outputDirectory)
        val base = input.fileName.toString().substringBeforeLast('.')
        val temporary = outputDirectory.resolve(".$base.${format.extension}.part")
        val output = outputDirectory.resolve("$base.${format.extension}")
        BufferedInputStream(Files.newInputStream(input)).use { source ->
            skipFully(source, audioOffset.toLong())
            BufferedOutputStream(Files.newOutputStream(temporary)).use { target ->
                val buffer = ByteArray(BUFFER_SIZE)
                var position = 0L
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    KgmaCipher.decryptInPlace(buffer, read, position, boxes)
                    target.write(buffer, 0, read)
                    position += read
                }
            }
        }
        Files.move(temporary, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        return DecryptedAudio(output, format)
    }

    private fun decryptPrefix(input: Path, audioOffset: Long, boxes: KgmaCipher.Boxes): ByteArray {
        val prefix = ByteArray(16)
        BufferedInputStream(Files.newInputStream(input)).use { source ->
            skipFully(source, audioOffset)
            val count = source.read(prefix).coerceAtLeast(0)
            KgmaCipher.decryptInPlace(prefix, count, 0, boxes)
            return prefix.copyOf(count)
        }
    }

    private fun readFully(source: java.io.InputStream, output: ByteArray) {
        var offset = 0
        while (offset < output.size) {
            val read = source.read(output, offset, output.size - offset)
            require(read >= 0) { "KGMA 文件头不完整" }
            offset += read
        }
    }

    private fun skipFully(source: java.io.InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = source.skip(remaining)
            if (skipped > 0) remaining -= skipped else require(source.read() >= 0) { "KGMA 音频数据不完整" }.also { remaining-- }
        }
    }

    data class DecryptedAudio(val path: Path, val format: NativeAudioFormat)

    private companion object {
        const val MINIMUM_HEADER_SIZE = 0x3c
        const val BUFFER_SIZE = 256 * 1024
    }
}
