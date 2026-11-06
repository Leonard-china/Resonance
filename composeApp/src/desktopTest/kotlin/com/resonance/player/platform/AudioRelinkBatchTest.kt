package com.resonance.player.platform

import com.resonance.player.model.*
import kotlinx.coroutines.CancellationException
import java.nio.file.Files
import kotlin.test.*

class AudioRelinkBatchTest {
    @Test fun batchPreservesLatestMetadataRejectsStaleSourcesAndWritesOneLibraryRevision() {
        val root = Files.createTempDirectory("resonance-relink-batch-")
        try {
            val tracks = (1..4).map { Track("$it", "Old $it", "Artist", "Album", "3:00", it, sourceUri = root.resolve("missing-$it.mp3").toString()) }
            val candidates = tracks.map { AudioRelinkCandidate(Files.write(root.resolve("new-${it.id}.mp3"), byteArrayOf(1, 2, 3)).toString(), "${it.id}.mp3", "${it.id}.mp3") }
            val store = DesktopLibraryStore(root)
            val current = tracks.map {
                when (it.id) {
                    "1" -> it.copy(title = "Latest edit", isFavorite = true)
                    "3" -> it.copy(sourceUri = root.resolve("changed.mp3").toString())
                    else -> it
                }
            }
            store.save(current)
            store.savePlaylists(listOf(Playlist("mine", "Keep", "", current, 0)))
            val libraryBefore = Files.readAllBytes(root.resolve("library.properties"))
            val playlistsBefore = Files.readAllBytes(root.resolve("playlists.properties"))
            Files.delete(root.resolve("new-4.mp3"))
            val (count, errors) = store.replaceAudioBatch(tracks.zip(candidates).map { AudioRelinkMatch(it.first, it.second) })
            assertEquals(2, count)
            assertEquals(2, errors.size)
            assertContentEquals(libraryBefore, Files.readAllBytes(root.resolve("library.properties.bak")), "The whole batch must produce one revision")
            assertContentEquals(playlistsBefore, Files.readAllBytes(root.resolve("playlists.properties")), "Source-only relinking must not rewrite playlist snapshots")
            val reopened = DesktopLibraryStore(root)
            val restored = reopened.load().associateBy { it.id }
            assertEquals(current[0].copy(sourceUri = candidates[0].sourceUri), restored["1"])
            assertEquals(current[1].copy(sourceUri = candidates[1].sourceUri), restored["2"])
            assertEquals(current[2], restored["3"])
            assertEquals(current[3], restored["4"])
            assertEquals(restored.values.toList(), reopened.loadPlaylists().single().tracks)
            assertContentEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(root.resolve("new-1.mp3")))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun cancellationBeforeCommitLeavesCatalogAndBackupUntouched() {
        val root = Files.createTempDirectory("resonance-relink-cancel-")
        try {
            val track = Track("id", "Song", "Artist", "Album", "3:00", 0, sourceUri = root.resolve("missing.mp3").toString())
            val source = Files.write(root.resolve("new.mp3"), byteArrayOf(1))
            val store = DesktopLibraryStore(root)
            store.save(listOf(track))
            val before = Files.readAllBytes(root.resolve("library.properties"))
            var checks = 0
            assertFailsWith<CancellationException> {
                store.replaceAudioBatch(listOf(AudioRelinkMatch(track, AudioRelinkCandidate(source.toString(), "new.mp3", "new.mp3")))) {
                    if (++checks == 2) throw CancellationException("cancel before atomic write")
                }
            }
            assertContentEquals(before, Files.readAllBytes(root.resolve("library.properties")))
            assertFalse(Files.exists(root.resolve("library.properties.bak")))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun failedAtomicWriteDoesNotClaimOrPersistReplacements() {
        val root = Files.createTempDirectory("resonance-relink-write-failure-")
        try {
            val track = Track("id", "Song", "Artist", "Album", "3:00", 0, sourceUri = root.resolve("missing.mp3").toString())
            val source = Files.write(root.resolve("new.mp3"), byteArrayOf(1))
            val store = DesktopLibraryStore(root)
            store.save(listOf(track))
            val before = Files.readAllBytes(root.resolve("library.properties"))
            // An invalid backup destination forces the real atomic writer to reject the commit.
            Files.createDirectory(root.resolve("library.properties.bak"))
            Files.write(root.resolve("library.properties.bak/block"), byteArrayOf(1))
            assertFails {
                store.replaceAudioBatch(listOf(AudioRelinkMatch(track, AudioRelinkCandidate(source.toString(), "new.mp3", "new.mp3"))))
            }
            assertContentEquals(before, Files.readAllBytes(root.resolve("library.properties")))
            assertEquals(track, store.load().single())
        } finally { root.toFile().deleteRecursively() }
    }
}
