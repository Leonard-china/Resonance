package com.resonance.player.conversion

import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopManagedMp3ConverterTest {
    private fun ffmpegPath(): Path? = sequenceOf(
        System.getenv("RESONANCE_FFMPEG")?.let(Path::of),
        Path.of("D:\\Software\\Github\\FlyMouseFormat\\release\\exe\\FlyingMouse Format\\resources\\ffmpeg\\ffmpeg.exe"),
    ).filterNotNull().firstOrNull(Files::isRegularFile)

    @Test
    fun convertsWavToManaged320kMp3WithoutChangingSource() {
        val ffmpeg = ffmpegPath() ?: return

        val root = Files.createTempDirectory(
            Path.of(System.getProperty("user.dir")).resolve("build").createDirectories(),
            "converter-test-",
        )
        try {
            val source = root.resolve("source.wav")
            writeSineWave(source)
            val sourceHash = digest(source)

            val output = DesktopManagedMp3Converter(root.resolve("managed")).importAsMp3(source)

            assertTrue(Files.isRegularFile(output))
            assertTrue(output.fileName.toString().endsWith(".mp3"))
            assertContentEquals(sourceHash, digest(source), "转换不得修改源音频")
            val prefix = Files.newInputStream(output).use { it.readNBytes(3) }
            assertTrue(prefix.contentEquals("ID3".encodeToByteArray()) || (prefix[0].toInt() and 0xff) == 0xff)

            val secondImport = DesktopManagedMp3Converter(root.resolve("managed")).importAsMp3(source)
            assertEquals(output, secondImport, "重复导入应复用同一个托管文件")
        } finally {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun convertsFlacInsideKgmaToManagedMp3AndCleansStaging() {
        val ffmpeg = ffmpegPath() ?: return
        val root = Files.createTempDirectory(Path.of(System.getProperty("user.dir")).resolve("build").createDirectories(), "kgma-flac-test-")
        try {
            val wav = root.resolve("source.wav")
            val flac = root.resolve("source.flac")
            val kgma = root.resolve("source.kgma")
            writeSineWave(wav)
            val ffmpegResult = ProcessBuilder(ffmpeg.toString(), "-hide_banner", "-loglevel", "error", "-y", "-i", wav.toString(), "-c:a", "flac", flac.toString()).start()
            assertEquals(0, ffmpegResult.waitFor())
            KgmaFixtureEncoder.write(flac, kgma)
            val kgmaHash = digest(kgma)

            val managed = root.resolve("managed")
            val output = DesktopManagedMp3Converter(managed).importAsMp3(kgma)

            assertTrue(Files.isRegularFile(output))
            assertContentEquals(kgmaHash, digest(kgma), "转换器不得修改源 KGMA；源文件始终保留")
            Files.list(managed).use { files -> assertTrue(files.noneMatch { it.fileName.toString().startsWith(".staging-") }) }
        } finally {
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    private fun writeSineWave(path: Path) {
        val sampleRate = 44_100
        val samples = sampleRate / 4
        val dataSize = samples * 2
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeBytes("RIFF")
            output.writeIntReversed(36 + dataSize)
            output.writeBytes("WAVEfmt ")
            output.writeIntReversed(16)
            output.writeShortReversed(1)
            output.writeShortReversed(1)
            output.writeIntReversed(sampleRate)
            output.writeIntReversed(sampleRate * 2)
            output.writeShortReversed(2)
            output.writeShortReversed(16)
            output.writeBytes("data")
            output.writeIntReversed(dataSize)
            repeat(samples) { index ->
                val sample = (kotlin.math.sin(index * 2.0 * Math.PI * 440.0 / sampleRate) * 12_000).toInt()
                output.writeShortReversed(sample)
            }
        }
    }

    private fun DataOutputStream.writeIntReversed(value: Int) = writeInt(Integer.reverseBytes(value))
    private fun DataOutputStream.writeShortReversed(value: Int) = writeShort(java.lang.Short.reverseBytes(value.toShort()).toInt())
    private fun digest(path: Path): ByteArray = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
}
