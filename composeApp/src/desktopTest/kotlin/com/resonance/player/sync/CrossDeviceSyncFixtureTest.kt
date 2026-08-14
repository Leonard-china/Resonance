package com.resonance.player.sync

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertTrue

class CrossDeviceSyncFixtureTest {
    @Test
    fun createsAndroidImportFixtureWithArtwork() {
        val projectRoot = Path.of(System.getProperty("user.dir")).parent
        val media = projectRoot.resolve("build/test-media")
        val audio = media.resolve("hotel.mp3")
        val artwork = media.resolve("cover.jpg")
        assertTrue(Files.isRegularFile(audio))
        assertTrue(Files.isRegularFile(artwork))
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
        output.parent.createDirectories()
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
        assertTrue(Files.isRegularFile(Path.of(imported.tracks.single().sourceUri!!)))
        assertTrue(Files.isRegularFile(Path.of(imported.tracks.single().artworkPath!!)))
    }
}
