package com.resonance.player.together

import com.resonance.player.audio.DesktopPcmPlayer
import com.resonance.player.model.*
import com.resonance.player.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assume.assumeTrue
import java.nio.file.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

/** Opt-in real decoder/output bridge. Uses only a caller-supplied original test audio file. */
class TogetherNativeBridgeTest {
    @Test fun windowsNativeHostForMuMu() = runBlocking<Unit> {
        assumeTrue(System.getenv("RESONANCE_NATIVE_BRIDGE") == "1")
        val audio = Path.of(requireNotNull(System.getenv("RESONANCE_NATIVE_AUDIO"))).toAbsolutePath()
        require(Files.isRegularFile(audio))
        val directory = Path.of(System.getenv("RESONANCE_BRIDGE_OUTPUT") ?: "build/together-native-bridge").toAbsolutePath()
        Files.createDirectories(directory)
        Files.deleteIfExists(directory.resolve("finished"))
        val vault = com.resonance.player.features.DesktopCredentialVault(directory)
        val errors = CopyOnWriteArrayList<String>()
        val player = DesktopPcmPlayer({}, {}, {}, { errors += it })
        val relayAcceptance = System.getenv("RESONANCE_NATIVE_RELAY_ACCEPTANCE") == "1"
        val companionEnabled = System.getenv("RESONANCE_BRIDGE_COMPANIONS") == "1"
        val host = JvmListenTogetherService(directory.resolve("host-cache"), { Files.newInputStream(Path.of(it.sourceUri!!)) },
            companionStore = if (companionEnabled || relayAcceptance) TogetherCompanionStore(directory.resolve("companions"),
                vault::encrypt, vault::decrypt,
                defaultRelay = if (relayAcceptance) "wss://leonard.asia/resonance-relay/v1" else "") else null,
            companionPort = 0)
        val duration = System.getenv("RESONANCE_NATIVE_DURATION")?.toIntOrNull()?.coerceIn(120, 14_400) ?: 960
        val track = Track("native-original", "同频 · 原创演示", "Resonance Studio", "一起听", "%d:%02d".format(duration / 60, duration % 60), 7, sourceUri = audio.toString(), mimeType = "audio/mpeg")
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val log = StringBuilder("elapsed_ms,position_ms,playing,peer_ready\n")
        fun guestCompleted() = host.companion.value.messages.any {
            !it.outgoing && it.text == "[resonance-native-acceptance-complete]"
        }
        try {
            host.publishQueue(listOf(track))
            player.setVolume(if (relayAcceptance) 0f else .08f)
            player.play(track, autoPlay = false)
            withTimeout(15_000) { while (!player.isPrepared(track.id)) { check(errors.isEmpty()) { "Native decoder failed" }; delay(50) } }
            host.publishPlayback(player.snapshot())
            host.host(0)
            Files.writeString(directory.resolve("port.txt"), host.state.value.port.toString())
            // Android loopback port 1 has no listener or adb reverse mapping in this acceptance.
            // No alternate addresses: joining cannot silently pass through a LAN direct connection.
            val invitation = host.invitation("127.0.0.1", if (relayAcceptance) 1 else host.state.value.port)
            if (relayAcceptance) check(host.inspectInvitation(invitation)?.relayHost == "leonard.asia")
            Files.writeString(directory.resolve("invite.txt"), invitation)
            owner.launch { while (isActive) { host.publishNativePlayback(player.snapshot()); delay(50) } }
            withTimeout(120_000) { host.state.first { it.peerReady } }
            player.setPlaying(true)
            val seconds = System.getenv("RESONANCE_NATIVE_SECONDS")?.toIntOrNull()?.coerceIn(30, 8_000) ?: 60
            val promo = System.getenv("RESONANCE_PROMO_SCENE") == "1"
            var replied = false
            if (companionEnabled) {
                withTimeout(15_000) { host.companion.first { it.connected } }
                check(host.sendChat(if (promo) "今天也辛苦了，陪你再听一会儿。" else "原生稳定性测试已开始"))
                withTimeout(15_000) { host.companion.first { it.messages.any { message -> message.outgoing && message.delivered } } }
            }
            val start = System.nanoTime()
            for (second in 0 until seconds) {
                if (!promo && second == 15) player.setPlaying(false)
                if (!promo && second == 18) player.seekTo(.1f)
                if (!promo && second == 20) player.setPlaying(true)
                delay(1_000)
                if (relayAcceptance && companionEnabled && guestCompleted()) break
                val snapshot = player.snapshot()
                check(errors.isEmpty()) { "Native decoder failed" }
                check(host.state.value.status == TogetherStatus.Connected) { "Native peer disconnected" }
                if (companionEnabled) {
                    if (promo && host.companion.value.pairingIncoming) host.respondPairing(true)
                    if (promo && !replied && host.companion.value.messages.any { !it.outgoing && it.text == "有你在，晚一点也没关系。" }) {
                        check(host.sendChat("那就不着急，下一首也一起听。")); replied = true
                    }
                    check(host.companion.value.connected) { "Authenticated chat disconnected" }
                    if (second > 0 && second % 600 == 0) check(host.sendChat("稳定性测试：已运行 ${second / 60} 分钟"))
                }
                log.append("${(System.nanoTime() - start) / 1_000_000},${snapshot.positionMs},${snapshot.isPlaying},${host.state.value.peerReady}\n")
            }
            if (relayAcceptance && companionEnabled) withTimeout(30_000) {
                host.companion.first { guestCompleted() }
            }
            Files.writeString(directory.resolve("finished"), "ok")
            withTimeout(30_000) { host.state.first { it.status == TogetherStatus.Listening } }
        } finally {
            owner.cancel(); player.close(); host.close()
            Files.writeString(directory.resolve("windows-native.csv"), log)
            Files.deleteIfExists(directory.resolve("invite.txt"))
        }
    }
}
