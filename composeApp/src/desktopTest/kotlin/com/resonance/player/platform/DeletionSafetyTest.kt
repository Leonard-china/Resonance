package com.resonance.player.platform

import com.resonance.player.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.*

class DeletionSafetyTest {
    private fun fixture(block: suspend (Path, DesktopLibraryStore, DesktopPlatformServices) -> Unit) = runBlocking {
        val root = Files.createTempDirectory("deletion-safety-")
        val old = System.getProperty("RESONANCE_DATA")
        System.setProperty("RESONANCE_DATA", root.resolve("data").toString())
        val service = DesktopPlatformServices()
        try { block(root, DesktopLibraryStore(root.resolve("data")), service) }
        finally {
            service.close()
            if (old == null) System.clearProperty("RESONANCE_DATA") else System.setProperty("RESONANCE_DATA", old)
            root.toFile().deleteRecursively()
        }
    }
    @Test fun removingOneTrackRetainsTheSharedArtworkOfAnotherTrack() = fixture { root, store, service ->
        val cover = Files.writeString(root.resolve("shared.cover"), "generated cover")
        val a = Track("a", "A", "Fixture", "Album", "1:00", 0, sourceUri = Files.writeString(root.resolve("a.mp3"), "a").toString(), artworkPath = cover.toString())
        val b = a.copy(id = "b", title = "B", sourceUri = Files.writeString(root.resolve("b.mp3"), "b").toString())
        store.save(listOf(a, b)); store.savePlaylists(listOf(Playlist("mine", "Keep", "", listOf(a, b), 0)))
        val report = service.deleteLocalTrack(a)
        assertTrue(report.success); assertTrue(report.fileDeleted); assertEquals(setOf("a"), report.removedTrackIds)
        assertEquals(listOf("b"), store.load().map { it.id })
        assertEquals("generated cover", Files.readString(cover))
        assertEquals(cover.toString(), store.load().single().artworkPath)
        assertEquals(listOf("b"), store.loadPlaylists().single().tracks.map { it.id })
    }
    @Test fun deniedDeletionPreservesRecordsAndPartialBatchReportsOnlyActualRemovals() = fixture { root, store, service ->
        assumeTrue(System.getProperty("os.name").startsWith("Linux"))
        val parent = Files.createDirectories(root.resolve("locked"))
        val blocked = Track("blocked", "Locked", "Fixture", "Album", "1:00", 0, true,
            Files.writeString(parent.resolve("locked.mp3"), "keep").toString())
        val free = blocked.copy(id = "free", sourceUri = Files.writeString(root.resolve("free.mp3"), "remove").toString())
        store.save(listOf(blocked, free)); store.savePlaylists(listOf(Playlist("mine", "Keep", "", listOf(blocked, free), 0)))
        Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("r-x------"))
        try {
            assumeTrue(!parent.toFile().canWrite())
            val report = service.batchDeleteTracks(listOf(blocked, free))
            assertFalse(report.success); assertEquals(setOf("free"), report.removedTrackIds)
            assertEquals(listOf(blocked), store.load())
            assertEquals(listOf(blocked), store.loadPlaylists().single().tracks)
            assertTrue(Files.exists(Path.of(blocked.sourceUri!!))); assertFalse(Files.exists(Path.of(free.sourceUri!!)))
        } finally { Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwx------")) }
    }
}
