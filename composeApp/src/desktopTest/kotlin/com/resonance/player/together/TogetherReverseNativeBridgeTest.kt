package com.resonance.player.together

import com.resonance.player.audio.DesktopPcmPlayer
import com.resonance.player.playback.TogetherPlaybackDriver
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assume.assumeTrue
import java.nio.file.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

class TogetherReverseNativeBridgeTest {
    @Test fun windowsNativeGuestFollowsMuMuHost() = runBlocking<Unit> {
        assumeTrue(System.getenv("RESONANCE_REVERSE_BRIDGE") == "1")
        val directory = Path.of(System.getenv("RESONANCE_REVERSE_OUTPUT") ?: "build/together-reverse-bridge").toAbsolutePath()
        Files.createDirectories(directory)
        println("[ReverseBridge] Working directory: $directory")
        val vault = com.resonance.player.features.DesktopCredentialVault(directory)
        val errors = CopyOnWriteArrayList<String>()
        val player = DesktopPcmPlayer({}, {}, {}, { errors += it })
        val guest = JvmListenTogetherService(directory.resolve("cache"), { error("Guest never reads local songs") }, onGuestStopped = { player.setPlaying(false) },
            companionStore = TogetherCompanionStore(directory.resolve("companions"),
                vault::encrypt, vault::decrypt), companionPort = 0)
        guest.trackPlayerReadiness(true)
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val driver = TogetherPlaybackDriver(guest, owner, player::snapshot,
            prepare = { frame ->
                println("[ReverseBridge] Preparing track: ${frame.track?.title}")
                player.setPlaybackSpeed(frame.speed)
                player.play(requireNotNull(frame.track), false, frame.progress)
            },
            seek = {
                println("[ReverseBridge] Seek to $it")
                player.seekTo(it)
            },
            playing = {
                println("[ReverseBridge] Set playing: $it")
                player.setPlaying(it)
            },
            speed = player::setPlaybackSpeed,
            ready = { id ->
                withTimeoutOrNull(15_000) {
                    while (!player.isPrepared(id)) delay(25)
                    true
                } ?: false
            })
        try {
            player.setVolume(.08f)
            val invitationFile = directory.resolve("invite.txt")
            println("[ReverseBridge] Waiting for invitation file: $invitationFile")
            withTimeout(100_000) { while (!Files.isRegularFile(invitationFile)) delay(100) }
            val inviteContent = Files.readString(invitationFile)
            println("[ReverseBridge] Got invitation, joining room...")
            guest.join(inviteContent)
            println("[ReverseBridge] Waiting for status=Connected and peerReady...")
            withTimeout(35_000) {
                guest.state.first {
                    println("[ReverseBridge] Current state: status=${it.status}, peerReady=${it.peerReady}, message=${it.message}")
                    it.status == TogetherStatus.Connected && it.peerReady
                }
            }
            println("[ReverseBridge] Connection ready! Beginning 65s trace...")
            withTimeout(15_000) { guest.companion.first { it.connected } }
            assertTrue(guest.sendChat("Windows 原生反向跟听已建立"))
            withTimeout(15_000) { guest.companion.first { it.messages.any { message -> message.outgoing && message.delivered } } }
            val trace = StringBuilder("elapsed_ms,position_ms,playing\n")
            val start = System.nanoTime()
            var paused = false; var resumed = false
            repeat(65) { second ->
                delay(1_000)
                assertEquals(TogetherStatus.Connected, guest.state.value.status)
                check(errors.isEmpty()) { "Native guest decoder failed: $errors" }
                val snapshot = player.snapshot()
                if (second % 5 == 0 || second in 15..25) {
                    println("[ReverseBridge] Sec $second: pos=${snapshot.positionMs}ms, isPlaying=${snapshot.isPlaying}")
                }
                if (second in 16..19 && !snapshot.isPlaying) paused = true
                if (second > 22 && snapshot.isPlaying) resumed = true
                trace.append("${(System.nanoTime()-start)/1_000_000},${snapshot.positionMs},${snapshot.isPlaying}\n")
            }
            val csvFile = directory.resolve("windows-native.csv")
            Files.writeString(csvFile, trace.toString())
            println("[ReverseBridge] Wrote CSV trace to $csvFile")
            assertTrue(paused, "Expected playback to have paused between second 16-19")
            assertTrue(resumed, "Expected playback to have resumed after second 22")
            assertTrue(player.snapshot().positionMs!! > 120_000, "Expected seek position > 120000ms")
            println("[ReverseBridge] Test completed successfully!")
        } finally {
            guest.close()
            driver.close()
            owner.cancel()
            player.close()
            Files.deleteIfExists(directory.resolve("invite.txt"))
        }
    }
}
