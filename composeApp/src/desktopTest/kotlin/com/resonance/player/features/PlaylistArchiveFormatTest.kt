package com.resonance.player.features

import com.resonance.player.model.Track
import com.resonance.player.sync.PlainSyncPackage
import java.nio.file.Files
import kotlin.test.*

class PlaylistArchiveFormatTest {
    @Test fun flacAudioKeepsEncodingExtensionMimeAndArtwork() {
        val root = Files.createTempDirectory("resonance-archive-formats-")
        try {
            val audio = root.resolve("source.flac"); Files.write(audio, "fLaC-fixture-content".toByteArray())
            val cover = root.resolve("cover.jpg"); Files.write(cover, byteArrayOf(1, 2, 3))
            val track = Track("id", "Test", "Artist", "Album", "0:01", 0, sourceUri = audio.toString(), artworkPath = cover.toString(), mimeType = "audio/flac")
            val archive = root.resolve("playlist.resonance")
            PlainSyncPackage().export(archive, listOf(track), emptyList(), { Files.newInputStream(audio) }, { Files.newInputStream(cover) })
            val result = PlainSyncPackage().import(archive, root.resolve("imported")).tracks.single()
            assertTrue(result.sourceUri!!.endsWith(".flac"))
            assertEquals("audio/flac", result.mimeType)
            assertContentEquals(Files.readAllBytes(audio), Files.readAllBytes(java.nio.file.Path.of(result.sourceUri)))
            assertContentEquals(Files.readAllBytes(cover), Files.readAllBytes(java.nio.file.Path.of(result.artworkPath!!)))
        } finally { Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }
}
