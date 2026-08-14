package com.resonance.player.conversion

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KgmaDeviceFixtureTest {
    @Test
    fun writesSmallFixtureForAndroidConversionSmokeTest() {
        val directory = Path.of(System.getProperty("user.dir"))
            .resolve("build/device-fixtures")
        Files.createDirectories(directory)
        val wav = directory.resolve("resonance-test.wav")
        val mp3 = directory.resolve("resonance-test.mp3")
        val kgma = directory.resolve("resonance-test.kgma")
        writeSineWave(wav)
        val ffmpeg = Path.of(
            "D:\\Software\\Github\\FlyMouseFormat\\release\\exe\\FlyingMouse Format\\resources\\ffmpeg\\ffmpeg.exe",
        )
        if (!Files.isRegularFile(ffmpeg)) return
        val process = ProcessBuilder(
            ffmpeg.toString(), "-hide_banner", "-loglevel", "error", "-y",
            "-i", wav.toString(), "-c:a", "libmp3lame", "-b:a", "320k",
            "-id3v2_version", "3", mp3.toString(),
        ).start()
        assertEquals(0, process.waitFor())
        KgmaFixtureEncoder.write(mp3, kgma)

        assertTrue(Files.size(kgma) > Files.size(mp3))
    }

    private fun writeSineWave(path: Path) {
        val sampleRate = 44_100
        val sampleCount = sampleRate / 4
        val dataSize = sampleCount * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()
        Files.newOutputStream(path).use { output ->
            output.write(header)
            repeat(sampleCount) { index ->
                val sample = (sin(2.0 * PI * 440.0 * index / sampleRate) * Short.MAX_VALUE * 0.2).toInt()
                output.write(sample and 0xff)
                output.write(sample ushr 8 and 0xff)
            }
        }
    }
}
