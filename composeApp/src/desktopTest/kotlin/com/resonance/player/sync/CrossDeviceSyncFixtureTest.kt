package com.resonance.player.sync

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import java.nio.file.Files
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class CrossDeviceSyncFixtureTest {
    @Test
    fun createsAndroidImportFixtureWithArtwork() {
        val media = Files.createTempDirectory("resonance-cross-device-")
        try {
            val audio = media.resolve("hotel.mp3")
            val artwork = media.resolve("cover.jpg")
            Files.write(audio, ByteArray(16_384).also { Random(42).nextBytes(it) })
            Files.write(artwork, ByteArray(2_048).also { Random(84).nextBytes(it) })
            val track = Track(
                id = "cross-device-hotel",
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California",
                durationText = "0:08",
                artworkSeed = 13,
                sourceUri = audio.toString(),
                artworkPath = artwork.toString(),
            )
            val output = media.resolve("cross-device-sync.resonance")
            EncryptedSyncPackage().export(
                output,
                "resonance-test-013",
                listOf(track),
                listOf(Playlist("cross-device-list", "Cross Device", "1 song", listOf(track), 13)),
                { Files.newInputStream(audio) },
                { Files.newInputStream(artwork) },
            )
            assertTrue(Files.size(output) > 10_000)
            val imported = EncryptedSyncPackage().import(output, "resonance-test-013", media.resolve("cross-device-managed"))
            assertTrue(Files.isRegularFile(java.nio.file.Path.of(imported.tracks.single().sourceUri!!)))
            assertTrue(Files.isRegularFile(java.nio.file.Path.of(imported.tracks.single().artworkPath!!)))
        } finally {
            Files.walk(media).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
}
