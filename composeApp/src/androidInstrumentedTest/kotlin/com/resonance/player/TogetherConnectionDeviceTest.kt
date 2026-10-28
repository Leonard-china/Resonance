package com.resonance.player

import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.model.PlayerState
import com.resonance.player.model.Track
import com.resonance.player.platform.TogetherStatus
import com.resonance.player.together.JvmListenTogetherService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

class TogetherConnectionDeviceTest {
    @Test fun androidPeersCanCreateAndValidateEphemeralTlsCertificates() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "together-native-")
        val audio = ByteArray(99_123) { (it * 3).toByte() }
        val host = JvmListenTogetherService(root.resolve("host"), { ByteArrayInputStream(audio) })
        val guest = JvmListenTogetherService(root.resolve("guest"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = Track("native-tls", "原生加密测试", "测试夹具", "本机", "0:04", 7, sourceUri = "fixture")))
            host.host(0)
            assertEquals(TogetherStatus.Listening, host.state.value.status)
            guest.join(host.invitation("127.0.0.1"))
            val received = withTimeout(15_000) { guest.remotePlayback.first { it?.track != null }!! }
            assertContentEquals(audio, Files.readAllBytes(Path.of(received.track!!.sourceUri!!)))
            assertTrue(guest.state.value.tlsVersion in setOf("TLSv1.3", "TLSv1.2"))
        } finally { guest.close(); host.close() }
    }

    @Test fun physicalAndroidFollowsWindowsAudioPauseSeekAndReplacement() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val invitation = arguments.getString("togetherInvite")
        assumeTrue(!invitation.isNullOrBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(context.cacheDir.toPath(), "together-windows-")
        val guest = JvmListenTogetherService(directory, { error("Guest must not read local audio") })
        try {
            guest.join(invitation!!)
            val first = withTimeout(20_000) { guest.remotePlayback.first { it?.track?.title == "加密直连测试 A" }!! }
            val bytes = Files.readAllBytes(Path.of(first.track!!.sourceUri!!))
            assertEquals(64_044, bytes.size)
            assertEquals("RIFF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
            withTimeout(10_000) { guest.remotePlayback.first { it != null && !it.playing && kotlin.math.abs(it.progress - .4f) < .01f } }
            withTimeout(10_000) { guest.remotePlayback.first { it?.playing == true && it.mediaId == first.mediaId } }
            val second = withTimeout(10_000) { guest.remotePlayback.first { it?.track?.title == "加密直连测试 B" }!! }
            assertNotEquals(first.mediaId, second.mediaId)
            assertEquals(96_044L, Files.size(Path.of(second.track!!.sourceUri!!)))
            assertEquals("TLSv1.3", guest.state.value.tlsVersion)
        } finally { guest.close() }
    }

    private fun assertContentEquals(expected: ByteArray, actual: ByteArray) = assertTrue(expected.contentEquals(actual))
}
