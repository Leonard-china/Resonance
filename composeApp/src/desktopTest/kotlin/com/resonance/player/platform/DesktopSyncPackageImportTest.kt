package com.resonance.player.platform

import com.resonance.player.io.ImportControl
import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import com.resonance.player.sync.PlainSyncPackage
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.*

class DesktopSyncPackageImportTest {
    @Test fun pickerIncludesPackagesAndAudioWithoutExposingUnrelatedFiles() = fixture { root ->
        val filter = desktopMusicImportFilter()
        assertTrue(filter.accept(root.toFile()))
        for (name in listOf("歌单.resonance", "歌单.RESONANCE", "song.mp3", "song.FLAC", "song.kgma")) {
            assertTrue(filter.accept(root.resolve(name).toFile()), name)
        }
        assertFalse(filter.accept(root.resolve("notes.txt").toFile()))
    }

    @Test fun selectingPackageRestoresMetadataAndPlaylistsAcrossRestart() = fixture { root ->
        val source = archive(root, "夜间 歌单 100% #.resonance")
        val before = Files.readAllBytes(source)
        val report = scanner(root).scan(source, false)
        assertEquals(1, report.importedPackageCount)
        assertEquals(0, report.skippedCount)
        assertEquals("原始标题", report.tracks.single().title)
        assertTrue(report.tracks.single().isFavorite)
        assertEquals("audio/flac", report.tracks.single().mimeType)
        assertEquals("测试歌单", report.playlists.single().name)
        assertContentEquals(before, Files.readAllBytes(source))

        val profile = root.resolve("profile")
        val store = DesktopLibraryStore(profile)
        store.savePlaylists(listOf(Playlist("existing", "已有歌单", "0 首", emptyList(), 0)))
        store.mergeImported(report.tracks, report.playlists)
        store.mergeImported(report.tracks, report.playlists)
        val restarted = DesktopLibraryStore(profile)
        assertEquals(1, restarted.load().size)
        assertEquals(setOf("existing", "playlist-archive"), restarted.loadPlaylists().map { it.id }.toSet())
        assertEquals("archived-track", restarted.loadPlaylists().first { it.id == "playlist-archive" }.tracks.single().id)
        assertContentEquals("synthetic-flac-payload".toByteArray(), Files.readAllBytes(Path.of(restarted.load().single().sourceUri!!)))
    }

    @Test fun folderAndUriDropFindUppercasePackageAndDeduplicateSelection() = fixture { root ->
        val source = archive(root, "中文 空格 100% #.RESONANCE")
        val flavor = java.awt.datatransfer.DataFlavor("text/uri-list;class=java.lang.String")
        val payload = object : java.awt.datatransfer.Transferable {
            override fun getTransferDataFlavors() = arrayOf(flavor)
            override fun isDataFlavorSupported(candidate: java.awt.datatransfer.DataFlavor) = candidate == flavor
            override fun getTransferData(candidate: java.awt.datatransfer.DataFlavor): Any = "${source.toUri()}\r\n${root.toUri()}"
        }
        val report = scanner(root).scan(droppedMusicPaths(payload) + source, false)
        assertEquals(1, report.importedPackageCount)
        assertEquals(1, report.tracks.size)
        assertEquals(1, report.playlists.size)
    }

    @Test fun conversionSelectionKeepsPackagedAudioFormatAndBytes() = fixture { root ->
        val report = scanner(root).scan(archive(root), true)
        val track = report.tracks.single()
        assertEquals("audio/flac", track.mimeType)
        assertTrue(track.sourceUri!!.endsWith(".flac"))
        assertContentEquals("synthetic-flac-payload".toByteArray(), Files.readAllBytes(Path.of(track.sourceUri)))
    }

    @Test fun corruptedPackageDoesNotBlockHealthyPackageOrCommitBadAudio() = fixture { root ->
        val valid = archive(root)
        val broken = root.resolve("corrupted.resonance")
        ZipFile(valid.toFile()).use { input ->
            ZipOutputStream(Files.newOutputStream(broken)).use { output ->
                input.entries().asSequence().forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    if (entry.name.startsWith("audio/")) output.write("tampered".toByteArray())
                    else input.getInputStream(entry).use { it.copyTo(output) }
                    output.closeEntry()
                }
            }
        }
        val report = scanner(root).scan(listOf(broken, valid), false)
        assertEquals(1, report.skippedCount)
        assertContains(report.warnings.single(), "corrupted.resonance")
        assertEquals(1, report.importedPackageCount)
        assertEquals(1, report.tracks.size)
        assertContentEquals("synthetic-flac-payload".toByteArray(), Files.readAllBytes(Path.of(report.tracks.single().sourceUri!!)))
        Files.list(root.resolve("managed")).use { paths -> assertFalse(paths.anyMatch { it.fileName.toString().startsWith(".sync-import-") }) }
    }

    @Test fun maliciousPackageCannotEscapeManagedDirectory() = fixture { root ->
        val malicious = root.resolve("malicious.resonance")
        ZipOutputStream(Files.newOutputStream(malicious)).use { output ->
            output.putNextEntry(ZipEntry("../escape.txt"))
            output.write("escape".toByteArray())
            output.closeEntry()
        }
        val report = scanner(root).scan(malicious, false)
        assertEquals(1, report.skippedCount)
        assertTrue(report.tracks.isEmpty())
        assertTrue(report.playlists.isEmpty())
        assertEquals(0, report.importedPackageCount)
        assertFalse(Files.exists(root.resolve("escape.txt")))
    }

    @Test fun cancellingBetweenPackagesRetainsCompletedPlaylistAndSource() = fixture { root ->
        val first = archive(root, "first.resonance")
        val second = archive(root, "second.resonance")
        val control = ImportControl()
        val report = scanner(root).scan(listOf(first, second), false, control) { progress ->
            if (progress.stage == "导入歌单包" && progress.completed == 1) control.cancel()
        }
        assertTrue(report.cancelled)
        assertEquals(1, report.importedPackageCount)
        assertEquals(1, report.playlists.size)
        assertTrue(Files.isRegularFile(first))
        assertTrue(Files.isRegularFile(second))
    }

    @Test fun emptyPlaylistPackageIsStillReportedAndSaved() = fixture { root ->
        val source = root.resolve("empty.resonance")
        PlainSyncPackage().export(source, emptyList(), listOf(Playlist("empty", "空歌单", "0 首", emptyList(), 0)), { null }, { null })
        val report = scanner(root).scan(source, false)
        assertTrue(report.tracks.isEmpty())
        assertEquals(1, report.importedPackageCount)
        val store = DesktopLibraryStore(root.resolve("profile"))
        store.mergeImported(report.tracks, report.playlists)
        assertEquals("空歌单", DesktopLibraryStore(root.resolve("profile")).loadPlaylists().single().name)
    }

    private fun scanner(root: Path) = DesktopMusicScanner(root.resolve("art"), root.resolve("managed"))

    private fun archive(root: Path, name: String = "valid.resonance"): Path {
        val source = Files.write(root.resolve("original.bin"), "synthetic-flac-payload".toByteArray())
        val track = Track("archived-track", "原始标题", "歌手", "专辑", "0:01", 7,
            isFavorite = true, sourceUri = source.toString(), mimeType = "audio/flac")
        return root.resolve(name).also { target ->
            PlainSyncPackage().export(target, listOf(track), listOf(Playlist("playlist-archive", "测试歌单", "1 首", listOf(track), 7)),
                { Files.newInputStream(source) }, { null })
        }
    }

    private fun fixture(test: (Path) -> Unit) {
        val root = Files.createTempDirectory("resonance-package-import-").toRealPath()
        try { test(root) } finally { root.toFile().deleteRecursively() }
    }
}
