package com.resonance.player.playback

import com.resonance.player.audio.DesktopPcmPlayer
import com.resonance.player.audio.DesktopPlaybackEvent
import com.resonance.player.model.RepeatMode
import com.resonance.player.model.Track
import com.resonance.player.platform.DesktopPlatformServices
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.*

/** Opt-in: actual FFmpeg + Java Sound, muted output, generated audio and isolated storage only. */
class DesktopAudioOutputIntegrationTest {
    private fun enabled() = assumeTrue("Requires a local audio device and FFmpeg", System.getenv("RESONANCE_DESKTOP_AUDIO_TEST") == "1")
    private fun await(message: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(20)
        assertTrue(condition(), message)
    }
    private fun fixture(directory: Path): Track {
        val pcmBytes = 48_000 * 60 * 4
        val wave = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        wave.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(2).putInt(48_000).putInt(192_000).putShort(4).putShort(16)
            .put("data".toByteArray()).putInt(pcmBytes)
        repeat(48_000 * 60) { frame ->
            val sample = (kotlin.math.sin(frame * 2 * Math.PI * 220 / 48_000) * 1000).toInt().toShort()
            wave.putShort(sample).putShort(sample)
        }
        val path = directory.resolve("generated-tone.wav")
        Files.write(path, wave.array())
        return Track("native-fixture-a", "Generated test tone", "", "", "1:00", 0, sourceUri = path.toString())
    }
    private fun removeFixture(directory: Path) {
        require(directory.toAbsolutePath().normalize().startsWith(Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()))
        require(directory.fileName.toString().startsWith("resonance-native-test-"))
        directory.toFile().deleteRecursively()
    }

    @Test fun pauseSeekSpeedAndRapidReplacementReachRealAudioOutput() {
        enabled()
        val directory = Files.createTempDirectory("resonance-native-test-")
        val events = CopyOnWriteArrayList<DesktopPlaybackEvent>()
        val player = DesktopPcmPlayer({}, {}, {}, {}, events::add)
        try {
            val track = fixture(directory)
            player.setVolume(0f)
            player.play(track)
            await("Native output must advance") { player.isPrepared(track.id) && (player.positionMs ?: 0) >= 1000 }
            player.setPlaying(false)
            Thread.sleep(100)
            val paused = player.positionMs!!
            Thread.sleep(300)
            assertFalse(player.snapshot().isPlaying)
            assertTrue(player.positionMs!! - paused < 100, "Paused output must not advance")
            player.setPlaying(true)
            await("Resume must advance actual output") { player.positionMs!! >= paused + 300 }
            val oldRequest = player.currentRequestId
            player.seekTo(.5f)
            await("Seek must prepare a new decoder at the requested position") { player.isPrepared(track.id) && (player.positionMs ?: 0) >= 30_000 }
            assertNotEquals(oldRequest, player.currentRequestId)
            player.setPlaybackSpeed(2f)
            await("Speed change must become ready") { player.isPrepared(track.id) }
            assertEquals(2f, player.snapshot().playbackSpeed)
            val accelerated = player.positionMs!!
            await("Changed-speed audio must advance") { (player.positionMs ?: 0) >= accelerated + 800 }
            repeat(6) { player.play(track.copy(id = if (it % 2 == 0) "native-fixture-b" else track.id)) }
            val finalRequest = player.currentRequestId
            await("Last A request must own playback") { player.isPrepared(track.id) && events.any { it.requestId == finalRequest && it.kind == DesktopPlaybackEvent.Kind.Progress } }
            assertEquals(track.id, player.snapshot().currentTrack?.id)
            assertTrue(events.none { it.requestId == finalRequest && it.kind == DesktopPlaybackEvent.Kind.Error })
        } finally { player.close(); removeFixture(directory) }
    }

    @Test fun backgroundPlaybackSoakRetainsProgressAndAccountingAcrossInterruptions() = runBlocking {
        enabled()
        val seconds = System.getenv("RESONANCE_PLAYBACK_SOAK_SECONDS")?.toIntOrNull() ?: 0
        assumeTrue("Set RESONANCE_PLAYBACK_SOAK_SECONDS for a bounded background soak", seconds in 60..7200)
        val directory = Files.createTempDirectory("resonance-native-test-")
        val previous = System.getProperty("RESONANCE_DATA")
        var services: DesktopPlatformServices? = null
        try {
            System.setProperty("RESONANCE_DATA", directory.toString())
            val track = fixture(directory)
            val native = DesktopPlatformServices().also { services = it }
            native.setVolume(0f); native.foreground.value = false
            val started = System.nanoTime()
            var cycle = 0
            while ((System.nanoTime() - started) / 1_000_000_000 < seconds) {
                native.play(track, listOf(track), false, RepeatMode.Off)
                assertTrue(native.awaitPlaybackReady(track.id))
                await("Background output must advance") { (native.playbackPositionMs ?: 0) >= 1000 }
                native.setPlaying(false)
                val paused = native.playbackPositionMs ?: 0
                Thread.sleep(800)
                assertTrue((native.playbackPositionMs ?: 0) - paused < 100)
                native.seekTo(.25f)
                assertTrue(native.awaitPlaybackReady(track.id))
                Thread.sleep(300)
                native.setPlaying(true)
                val mark = native.playbackPositionMs ?: 0
                await("Output resumes after pause and seek") { (native.playbackPositionMs ?: 0) >= mark + 1000 }
                Thread.sleep(5000)
                native.flushListeningStats()
                val elapsed = (System.nanoTime() - started) / 1_000_000_000
                assertTrue((native.getTrackListeningStats(track.id)?.totalSeconds ?: 0) <= elapsed + 2, "Seek must not inflate background listening")
                cycle++
            }
            native.setPlaying(false); native.flushListeningStats()
            assertTrue((native.getTrackListeningStats(track.id)?.totalSeconds ?: 0) >= seconds / 2)
            assertTrue(cycle >= 5)
            println("Background soak: requestedSeconds=$seconds, cycles=$cycle, listeningSeconds=${native.getTrackListeningStats(track.id)?.totalSeconds}")
        } finally {
            services?.close(); ListeningStatsRepository.release(directory)
            if (previous == null) System.clearProperty("RESONANCE_DATA") else System.setProperty("RESONANCE_DATA", previous)
            removeFixture(directory)
        }
    }

    @Test fun platformRecordsWithoutComposeAndDeduplicatesNearEndPauseResume() = runBlocking {
        enabled()
        val directory = Files.createTempDirectory("resonance-native-test-")
        val previous = System.getProperty("RESONANCE_DATA")
        var services: DesktopPlatformServices? = null
        try {
            System.setProperty("RESONANCE_DATA", directory.toString())
            val track = fixture(directory)
            val native = DesktopPlatformServices().also { services = it }
            native.setVolume(0f)
            val started = System.nanoTime()
            native.play(track, listOf(track), false, RepeatMode.Off)
            assertTrue(native.awaitPlaybackReady(track.id), "Native decoder/output must become ready before measuring accounting")
            try {
                await("Statistics must advance with no App composition") { (native.getTrackListeningStats(track.id)?.totalSeconds ?: 0) >= 2 }
            } catch (failure: AssertionError) {
                throw AssertionError("Native position=${native.playbackPositionMs}; stats=${native.getTrackListeningStats(track.id)}; save warning=${ListeningStatsRepository.get(directory).errors.value}", failure)
            }
            native.setPlaying(false)
            native.flushListeningStats()
            val pausedSeconds = native.getTrackListeningStats(track.id)!!.totalSeconds
            Thread.sleep(1100)
            native.flushListeningStats()
            assertEquals(pausedSeconds, native.getTrackListeningStats(track.id)!!.totalSeconds)
            native.seekTo(.95f)
            native.setPlaying(true)
            await("95% threshold must complete once") { native.getTrackListeningStats(track.id)?.playCount == 1 }
            repeat(2) { native.setPlaying(false); Thread.sleep(100); native.setPlaying(true); Thread.sleep(300) }
            native.setPlaying(false)
            native.flushListeningStats()
            assertEquals(1, native.getTrackListeningStats(track.id)!!.playCount)
            val elapsedSeconds = (System.nanoTime() - started) / 1_000_000_000
            assertTrue(native.getTrackListeningStats(track.id)!!.totalSeconds <= elapsedSeconds + 1, "Seek must not add 57 seconds of listening")
        } finally {
            services?.close()
            ListeningStatsRepository.release(directory)
            if (previous == null) System.clearProperty("RESONANCE_DATA") else System.setProperty("RESONANCE_DATA", previous)
            removeFixture(directory)
        }
    }
}
