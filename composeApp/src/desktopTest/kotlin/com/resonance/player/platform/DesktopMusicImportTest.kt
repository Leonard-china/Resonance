package com.resonance.player.platform

import com.resonance.player.io.ImportControl
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class DesktopMusicImportTest {
    @Test fun mixedFilesAndOverlappingFoldersImportOnceAndPreserveSources() = fixture { root ->
        val folder = Files.createDirectories(root.resolve("音乐 100% #"))
        val audio = folder.resolve("歌手 - 音乐.WAV")
        writeWave(audio)
        val bytes = Files.readAllBytes(audio)
        Files.writeString(folder.resolve("ignore.txt"), "not music")
        val report = DesktopMusicScanner(root.resolve("art"), root.resolve("managed"))
            .scan(listOf(folder, audio, folder), false)
        assertEquals(1, report.tracks.size)
        assertEquals(0, report.skippedCount)
        assertEquals("音乐", report.tracks.single().title)
        assertEquals("歌手", report.tracks.single().artist)
        assertContentEquals(bytes, Files.readAllBytes(audio))
        val store = DesktopLibraryStore(root.resolve("profile"))
        store.merge(report.tracks)
        assertEquals(report.tracks.single().id, DesktopLibraryStore(root.resolve("profile")).load().single().id)
    }

    @Test fun badInputsDoNotPreventHealthyFilesFromImporting() = fixture { root ->
        val audio = root.resolve("valid.wav").also(::writeWave)
        val broken = root.resolve("broken.mp3").also { Files.writeString(it, "broken") }
        val report = DesktopMusicScanner(root.resolve("art"), root.resolve("managed"))
            .scan(listOf(root.resolve("missing"), broken, audio), false)
        assertEquals(1, report.tracks.size)
        assertEquals(2, report.skippedCount)
        assertEquals(2, report.warnings.size)
        assertTrue(Files.exists(audio))
    }

    @Test fun unsupportedSelectionExplainsWhyNothingWasImported() = fixture { root ->
        val text = root.resolve("notes.txt").also { Files.writeString(it, "notes") }
        val report = DesktopMusicScanner(root.resolve("art"), root.resolve("managed")).scan(text, false)
        assertTrue(report.tracks.isEmpty())
        assertTrue(report.warnings.single().contains("未找到支持的音频文件"))
    }

    @Test fun cancellationDuringEnumerationKeepsSourceUntouched() = fixture { root ->
        val audio = root.resolve("source.wav").also(::writeWave)
        val bytes = Files.readAllBytes(audio)
        val control = ImportControl().apply { cancel() }
        val report = DesktopMusicScanner(root.resolve("art"), root.resolve("managed")).scan(root, false, control)
        assertTrue(report.cancelled)
        assertTrue(report.tracks.isEmpty())
        assertContentEquals(bytes, Files.readAllBytes(audio))
    }

    @Test fun fileManagerPayloadsHandleUnicodeSpacesAndUriEscapes() = fixture { root ->
        val audio = root.resolve("中文 100% #.wav")
        val fileList = transfer(DataFlavor.javaFileListFlavor, listOf(audio.toFile(), audio.toFile(), "ignored"))
        assertTrue(supportsMusicDrop(fileList))
        assertEquals(listOf(audio), droppedMusicPaths(fileList))
        val uriFlavor = DataFlavor("text/uri-list;class=java.lang.String")
        val uriList = "# Nautilus\r\n${audio.toUri()}\r\nhttps://example.com/song.mp3\nfile://remote/music.mp3\ninvalid\n"
        assertEquals(listOf(audio), droppedMusicPaths(transfer(uriFlavor, uriList)))
        assertFalse(supportsMusicDrop(transfer(DataFlavor.stringFlavor, audio.toString())))
        assertTrue(droppedMusicPaths(transfer(DataFlavor.stringFlavor, "https://example.com")).isEmpty())
    }

    @Test fun oversizedUriPayloadIsRejected() {
        assertTrue(droppedMusicPaths(transfer(DataFlavor("text/uri-list;class=java.lang.String"), "x".repeat(1_048_577))).isEmpty())
    }

    private fun transfer(flavor: DataFlavor, value: Any) = object : Transferable {
        override fun getTransferDataFlavors() = arrayOf(flavor)
        override fun isDataFlavorSupported(candidate: DataFlavor) = candidate == flavor
        override fun getTransferData(candidate: DataFlavor): Any {
            require(candidate == flavor)
            return value
        }
    }

    private fun fixture(test: (Path) -> Unit) {
        val root = Files.createTempDirectory("resonance-drop-test-").toRealPath()
        try { test(root) } finally { root.toFile().deleteRecursively() }
    }

    private fun writeWave(path: Path) {
        val samples = 44_100
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            fun int(value: Int) = output.writeInt(Integer.reverseBytes(value))
            fun short(value: Int) = output.writeShort(java.lang.Short.reverseBytes(value.toShort()).toInt())
            output.writeBytes("RIFF"); int(36 + samples * 2); output.writeBytes("WAVEfmt ")
            int(16); short(1); short(1); int(44_100); int(88_200); short(2); short(16)
            output.writeBytes("data"); int(samples * 2)
            repeat(samples) { short((kotlin.math.sin(it * 2.0 * Math.PI * 440 / 44_100) * 8_000).toInt()) }
        }
    }
}
