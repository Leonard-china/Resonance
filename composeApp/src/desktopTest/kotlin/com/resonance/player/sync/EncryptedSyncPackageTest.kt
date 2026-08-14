package com.resonance.player.sync

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncryptedSyncPackageTest {
    @Test
    fun roundTripPreservesMetadataAndAudio() {
        val root = Files.createTempDirectory("resonance-sync-test-")
        try {
            val audio = root.resolve("sample.mp3")
            val artwork = root.resolve("sample.cover")
            Files.write(audio, "ID3-fake-audio-payload".encodeToByteArray())
            Files.write(artwork, byteArrayOf(1, 2, 3, 4, 5))
            val track = Track(
                id = "track-stable-id",
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationText = "3:21",
                artworkSeed = 42,
                sourceUri = audio.toString(),
                artworkPath = artwork.toString(),
            )
            val playlist = Playlist("playlist-stable-id", "Favorites", "1 song", listOf(track), 11)
            val pkg = root.resolve("library.resonance")

            EncryptedSyncPackage().export(
                target = pkg,
                passphrase = "correct horse battery staple",
                tracks = listOf(track),
                playlists = listOf(playlist),
                openAudio = { Files.newInputStream(audio) },
                openArtwork = { Files.newInputStream(artwork) },
            )

            assertTrue(Files.readAllBytes(pkg).indexOfSubArray("Song".encodeToByteArray()) < 0, "包外部不应泄露明文标签")
            val imported = EncryptedSyncPackage().import(pkg, "correct horse battery staple", root.resolve("managed"))
            assertEquals("Song", imported.tracks.single().title)
            assertEquals("Favorites", imported.playlists.single().name)
            assertEquals("track-stable-id", imported.playlists.single().tracks.single().id)
            assertTrue(Files.isRegularFile(java.nio.file.Path.of(imported.tracks.single().sourceUri!!)))
            assertContentEquals(
                byteArrayOf(1, 2, 3, 4, 5),
                Files.readAllBytes(java.nio.file.Path.of(imported.tracks.single().artworkPath!!)),
            )
        } finally {
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    @Test
    fun rejectsWrongPasswordAndTampering() {
        val root = Files.createTempDirectory("resonance-sync-test-")
        try {
            val audio = root.resolve("sample.mp3")
            Files.write(audio, "ID3-fake-audio-payload".encodeToByteArray())
            val track = Track("id", "Song", "Artist", "Album", "0:01", 1, sourceUri = audio.toString())
            val pkg = root.resolve("library.resonance")
            EncryptedSyncPackage().export(pkg, "right-password", listOf(track), emptyList(), { Files.newInputStream(audio) }, { null })

            assertFailsWith<IllegalStateException> {
                EncryptedSyncPackage().import(pkg, "wrong-password", root.resolve("wrong"))
            }

            val bytes = Files.readAllBytes(pkg)
            bytes[bytes.lastIndex - 5] = (bytes[bytes.lastIndex - 5].toInt() xor 0x40).toByte()
            Files.write(pkg, bytes)
            assertFailsWith<IllegalStateException> {
                EncryptedSyncPackage().import(pkg, "right-password", root.resolve("tampered"))
            }
        } finally {
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    private fun ByteArray.indexOfSubArray(needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        return (0..size - needle.size).firstOrNull { start ->
            needle.indices.all { index -> this[start + index] == needle[index] }
        } ?: -1
    }
}
