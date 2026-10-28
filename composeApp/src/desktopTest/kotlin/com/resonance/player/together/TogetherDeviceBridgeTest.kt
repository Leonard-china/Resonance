package com.resonance.player.together

import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.platform.TogetherStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assume.assumeTrue
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

/** Opt-in host for the physical Android TLS interoperability test, never a default waiting test. */
class TogetherDeviceBridgeTest {
    @Test fun windowsHostForPhysicalAndroidPeer() = runBlocking<Unit> {
        assumeTrue(System.getenv("RESONANCE_DEVICE_BRIDGE") == "1")
        val directory = Path.of("build/together-bridge").toAbsolutePath()
        Files.createDirectories(directory)
        val invitation = directory.resolve("phone-invite.txt")
        val port = directory.resolve("port.txt")
        val first = Track("bridge-a", "加密直连测试 A", "测试夹具", "Windows 与 Android", "0:04", 7, sourceUri = "fixture-a", mimeType = "audio/wav")
        val second = first.copy(id = "bridge-b", title = "加密直连测试 B", durationText = "0:06", sourceUri = "fixture-b")
        val host = JvmListenTogetherService(directory.resolve("host-cache"), { ByteArrayInputStream(wave(if (it.id == "bridge-a") 4 else 6)) })
        try {
            host.publishPlayback(PlayerState(currentTrack = first, isPlaying = false, progress = .2f))
            host.host(0)
            Files.writeString(invitation, host.invitation("127.0.0.1"))
            Files.writeString(port, host.state.value.port.toString())
            withTimeout(90_000) { host.state.first { it.status == TogetherStatus.Connected } }
            delay(1_000)
            host.publishPlayback(PlayerState(currentTrack = first, isPlaying = false, progress = .4f))
            delay(1_000)
            host.publishPlayback(PlayerState(currentTrack = first, isPlaying = true, progress = .6f))
            delay(1_000)
            host.publishPlayback(PlayerState(currentTrack = second, isPlaying = false, progress = .1f))
            withTimeout(25_000) { host.state.first { it.status == TogetherStatus.Listening } }
        } finally {
            host.close()
            Files.deleteIfExists(invitation)
            Files.deleteIfExists(port)
        }
    }

    private fun wave(seconds: Int): ByteArray {
        val dataBytes = 8000 * seconds * 2
        return ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVEfmt ".toByteArray()); putInt(16)
            putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes)
        }.array()
    }
}
