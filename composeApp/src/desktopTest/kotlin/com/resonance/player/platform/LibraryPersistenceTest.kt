package com.resonance.player.platform

import com.resonance.player.model.Track
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.test.*

class LibraryPersistenceTest {
    @Test fun relinkingMissingAudioPreservesIdentityFavoriteAndPlaylistAfterRestart() {
        val directory = Files.createTempDirectory("resonance-relink-test-").toRealPath()
        try {
            val previousFile = directory.resolve("previous.mp3")
            Files.write(previousFile, byteArrayOf(1, 2, 3))
            val previous = Track("original-id", "Song", "Artist", "Album", "3:00", 1, isFavorite = true, sourceUri = previousFile.toString())
            val store = DesktopLibraryStore(directory)
            store.save(listOf(previous))
            store.savePlaylists(listOf(com.resonance.player.model.Playlist("mine", "Keep", "", listOf(previous), 0)))
            Files.delete(previousFile)
            val replacementFile = directory.resolve("replacement.flac")
            val bytes = byteArrayOf(4, 5, 6)
            Files.write(replacementFile, bytes)
            store.replaceAudio(previous.copy(sourceUri = replacementFile.toString(), mimeType = "audio/flac"))
            val restarted = DesktopLibraryStore(directory)
            val restored = restarted.load().single()
            assertEquals(previous.id, restored.id)
            assertTrue(restored.isFavorite)
            assertEquals(replacementFile.toString(), restored.sourceUri)
            assertEquals("audio/flac", restored.mimeType)
            assertEquals(restored, restarted.loadPlaylists().single().tracks.single())
            assertContentEquals(bytes, Files.readAllBytes(replacementFile))
        } finally {
            check(directory.fileName.toString().startsWith("resonance-relink-test-"))
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
    @Test fun concurrentMergesAndFavoritesPreserveEveryTrack() {
        val directory = Files.createTempDirectory("resonance-store-test-")
        val store = DesktopLibraryStore(directory)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val tracks = (0 until 30).map { id ->
                val source = directory.resolve("$id.mp3")
                Files.write(source, byteArrayOf(1, 2, 3))
                Track("$id", "song $id", "artist", "album", "3:00", id, sourceUri = source.toString())
            }
            val writes = tracks.map { track -> executor.submit { store.merge(listOf(track)); store.setFavorite(track.id, true) } }
            writes.forEach { it.get() }
            assertEquals(30, store.load().size)
            assertTrue(store.load().all { it.isFavorite })
        } finally { executor.shutdownNow(); directory.toFile().deleteRecursively() }
    }

    @Test fun corruptedPrimaryRecoversThePreviousValidRevision() {
        val directory = Files.createTempDirectory("resonance-store-recovery-")
        try {
            val source = directory.resolve("audio.mp3")
            Files.write(source, byteArrayOf(1))
            val store = DesktopLibraryStore(directory)
            val track = Track("one", "before", "artist", "album", "3:00", 1, sourceUri = source.toString())
            store.save(listOf(track))
            store.save(listOf(track.copy(title = "after")))
            Files.writeString(directory.resolve("library.properties"), "count=2\ntrack.0.source=missing\n")
            assertEquals("before", store.load().single().title)
            assertEquals("before", store.load().single().title)
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun unreadableUiSettingsRecoverTheBackupBeforeTheNextWrite() {
        val directory = Files.createTempDirectory("resonance-ui-recovery-")
        try {
            val store = DesktopLibraryStore(directory)
            store.writeUiValue("vol.global", "0.25")
            store.writeUiValue("vol.global", "0.5")
            Files.writeString(directory.resolve("ui-state.properties"), "broken=\\uZZZZ")
            assertEquals("0.25", store.readUiValue("vol.global"))
            store.writeUiValue("gain.track.a", "3.0")
            assertEquals("0.25", store.readUiValue("vol.global"))
            assertEquals("3.0", store.readUiValue("gain.track.a"))
        } finally { directory.toFile().deleteRecursively() }
    }
}
