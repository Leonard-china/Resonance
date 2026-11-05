package com.resonance.player.sync

import com.resonance.player.model.*
import java.nio.file.*
import java.util.Properties
import java.util.zip.*
import kotlin.test.*

class ArchiveRollbackTest {
    private fun archive(root: Path, name: String, payload: String): Path {
        val audio = Files.writeString(root.resolve("$name.mp3"), payload)
        val art = Files.writeString(root.resolve("$name.cover"), "cover-$payload")
        val track = Track("stable", "Song", "Artist", "Album", "3:00", 0, sourceUri = audio.toString(), artworkPath = art.toString())
        return root.resolve("$name.resonance").also {
            PlainSyncPackage().export(it, listOf(track), listOf(Playlist("mine", "Keep", "", listOf(track), 0)),
                { Files.newInputStream(audio) }, { Files.newInputStream(art) })
        }
    }
    private fun mutate(source: Path, target: Path, change: (Properties) -> Unit): Path {
        ZipFile(source.toFile()).use { input ->
            ZipOutputStream(Files.newOutputStream(target)).use { output ->
                input.entries().asSequence().forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    val bytes = input.getInputStream(entry).use { it.readBytes() }
                    if (entry.name == "manifest.properties") {
                        val props = Properties().apply { bytes.inputStream().use(::load) }; change(props); props.store(output, "fixture")
                    } else output.write(bytes)
                    output.closeEntry()
                }
            }
        }
        return target
    }
    private fun fixture(block: (Path, Path, PlainSyncPackage.Imported) -> Unit) {
        val root = Files.createTempDirectory("archive-rollback-")
        try {
            val managed = root.resolve("managed")
            val before = PlainSyncPackage().import(archive(root, "before", "original"), managed)
            block(root, managed, before)
        } finally { root.toFile().deleteRecursively() }
    }
    private fun assertOriginal(before: PlainSyncPackage.Imported) {
        assertEquals("original", Files.readString(Path.of(before.tracks.single().sourceUri!!)))
        assertEquals("cover-original", Files.readString(Path.of(before.tracks.single().artworkPath!!)))
    }
    @Test fun badPlaylistMetadataNeverMovesAudioOrCallsPersistence() = fixture { root, managed, before ->
        val invalid = mutate(archive(root, "incoming", "replacement"), root.resolve("invalid.resonance")) { it.remove("playlist.0.name") }
        var commits = 0
        assertFails { PlainSyncPackage().import(invalid, managed, onCommit = { commits++ }) }
        assertEquals(0, commits); assertOriginal(before)
    }
    @Test fun persistenceFailureRollsBackBothReplacedAudioAndArtwork() = fixture { root, managed, before ->
        val incoming = archive(root, "incoming", "replacement")
        assertFailsWith<java.io.IOException> {
            PlainSyncPackage().import(incoming, managed, onCommit = { changed ->
                assertEquals("replacement", Files.readString(Path.of(changed.tracks.single().sourceUri!!)))
                throw java.io.IOException("simulated metadata write failure")
            })
        }
        assertOriginal(before)
        assertTrue(Files.list(managed).use { it.noneMatch { p -> p.fileName.toString().startsWith(".sync-import-") || p.fileName.toString().startsWith(".rollback-") } })
    }
    @Test fun referenceBudgetsAndDuplicateIdsAreCheckedBeforeReplacement() = fixture { root, managed, before ->
        val incoming = archive(root, "incoming", "replacement")
        for ((key, value) in listOf("playlist.0.track.count" to "1000000", "playlist.0.track.count" to "-1", "playlist.0.track.count" to "not-a-number", "playlist.count" to "not-a-number")) {
            val invalid = mutate(incoming, root.resolve("invalid.resonance")) { it.setProperty(key, value) }
            assertFails { PlainSyncPackage().import(invalid, managed) }; assertOriginal(before)
        }
        val duplicate = mutate(incoming, root.resolve("duplicate.resonance")) { props ->
            props.setProperty("track.count", "2")
            props.stringPropertyNames().filter { it.startsWith("track.0.") }.forEach { props.setProperty(it.replace("track.0.", "track.1."), props.getProperty(it)) }
        }
        assertFailsWith<IllegalArgumentException> { PlainSyncPackage().import(duplicate, managed) }; assertOriginal(before)
    }
    @Test fun cancellationAfterMovingFilesRestoresOriginalBytes() = fixture { root, managed, before ->
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            PlainSyncPackage().import(archive(root, "incoming", "replacement"), managed, onCommit = { throw kotlinx.coroutines.CancellationException("fixture") })
        }
        assertOriginal(before)
    }
    @Test fun fileTransactionRestoresMetadataAndRemovesOnlyNewFiles() {
        val root = Files.createTempDirectory("metadata-rollback-")
        try {
            val old = Files.writeString(root.resolve("library.properties"), "original")
            val fresh = root.resolve("playlists.properties")
            assertFails {
                com.resonance.player.io.FileRollback.run(listOf(old, fresh)) {
                    Files.writeString(old, "changed"); Files.writeString(fresh, "new"); error("fixture")
                }
            }
            assertEquals("original", Files.readString(old)); assertFalse(Files.exists(fresh))
        } finally { root.toFile().deleteRecursively() }
    }
}
