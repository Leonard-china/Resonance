package com.resonance.player.sync

import com.resonance.player.model.Playlist
import com.resonance.player.model.Track
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlainSyncPackageTest {
    @Test
    fun roundTripPreservesMetadataAudioAndArtwork() {
        val root = Files.createTempDirectory("resonance-plain-sync-test-")
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
                isFavorite = true,
                sourceUri = audio.toString(),
                artworkPath = artwork.toString(),
            )
            val playlist = Playlist("playlist-stable-id", "Favorites", "1 song", listOf(track), 11)
            val pkg = root.resolve("library.resonance")

            PlainSyncPackage().export(
                target = pkg,
                tracks = listOf(track),
                playlists = listOf(playlist),
                openAudio = { Files.newInputStream(audio) },
                openArtwork = { Files.newInputStream(artwork) },
            )

            // 明文 Zip：可直接用标准 Zip 工具打开，且清单可见
            ZipFile(pkg.toFile()).use { zip ->
                assertTrue(zip.entries().toList().any { it.name == "manifest.properties" })
                val manifest = zip.getInputStream(zip.getEntry("manifest.properties")).readBytes().decodeToString()
                assertTrue(manifest.contains("Song"), "明文包清单应包含曲目标题")
            }

            val imported = PlainSyncPackage().import(pkg, root.resolve("managed"))
            assertEquals("Song", imported.tracks.single().title)
            assertEquals("Artist", imported.tracks.single().artist)
            assertEquals(true, imported.tracks.single().isFavorite)
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
    fun rejectsCorruptedAudio() {
        val root = Files.createTempDirectory("resonance-plain-sync-test-")
        try {
            val audio = root.resolve("sample.mp3")
            Files.write(audio, "ID3-fake-audio-payload".encodeToByteArray())
            val track = Track("id", "Song", "Artist", "Album", "0:01", 1, sourceUri = audio.toString())
            val pkg = root.resolve("library.resonance")
            PlainSyncPackage().export(pkg, listOf(track), emptyList(), { Files.newInputStream(audio) }, { null })

            // 篡改音频条目内容，导入时 SHA-256 校验必须失败
            val staging = root.resolve("tamper")
            Files.createDirectories(staging)
            java.util.zip.ZipInputStream(Files.newInputStream(pkg)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = staging.resolve(entry.name)
                    target.parent?.let(Files::createDirectories)
                    val bytes = zip.readBytes()
                    Files.write(
                        target,
                        if (entry.name.startsWith("audio/")) bytes.copyOf(bytes.size - 1) else bytes,
                    )
                    zip.closeEntry()
                }
            }
            val tampered = root.resolve("tampered.resonance")
            java.util.zip.ZipOutputStream(Files.newOutputStream(tampered)).use { zip ->
                Files.walk(staging).use { paths ->
                    paths.filter(Files::isRegularFile).forEach { path ->
                        zip.putNextEntry(java.util.zip.ZipEntry(staging.relativize(path).toString().replace('\\', '/')))
                        Files.copy(path, zip)
                        zip.closeEntry()
                    }
                }
            }
            assertFailsWith<IllegalArgumentException> {
                PlainSyncPackage().import(tampered, root.resolve("managed-tampered"))
            }
        } finally {
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }

    @Test
    fun rejectsLegacyEncryptedPackage() {
        val root = Files.createTempDirectory("resonance-plain-sync-test-")
        try {
            val audio = root.resolve("sample.mp3")
            Files.write(audio, "ID3-fake-audio-payload".encodeToByteArray())
            val track = Track("id", "Song", "Artist", "Album", "0:01", 1, sourceUri = audio.toString())
            val pkg = root.resolve("library.resonance")
            EncryptedSyncPackage().export(
                pkg,
                "correct horse battery staple",
                listOf(track),
                emptyList(),
                { Files.newInputStream(audio) },
                { null },
            )
            assertFailsWith<IllegalArgumentException> {
                PlainSyncPackage().import(pkg, root.resolve("managed-encrypted"))
            }
        } finally {
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
}
