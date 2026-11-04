package com.resonance.player.together

import com.resonance.player.model.*
import com.resonance.player.platform.*
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import kotlin.test.*

class ListenTogetherTest {
    @Test fun protocolRejectsDeepJsonDuplicateKeysAndMalformedUtf8() {
        assertEquals("[{quoted}]", parseTogetherJson("{\"text\":\"[{quoted}]\"}".toByteArray()).getString("text"))
        assertFails { parseTogetherJson(("{\"a\":".repeat(40) + "1" + "}".repeat(40)).toByteArray()) }
        assertFails { parseTogetherJson("{\"type\":\"state\",\"type\":\"ping\"}".toByteArray()) }
        assertFails { parseTogetherJson(byteArrayOf(0xC3.toByte(), 0x28)) }
    }
    private fun track(id: String = "a") = Track(id, "测试音乐 $id", "本机夹具", "加密直连", "4:00", 7, sourceUri = "fixture-$id")
    private suspend fun ready(service: ListenTogetherService, title: String) = withTimeout(12_000) {
        service.remotePlayback.first { it?.track?.title == title }!!
    }

    @Test fun invitationRoundTripsIpv6AndRejectsAmbiguousCapabilities() {
        val invite = TogetherInvitation("::1", 45873, "ab".repeat(32), Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))
        assertEquals(invite, TogetherInvitation.parse(invite.encode()))
        val wrappedInChatMessage = "🎵【Resonance 一起听】我发起了一起听，来跟我听同一首歌吧！打开 Resonance 即可自动加入：\n" + invite.encode() + "\n快来听！"
        assertEquals(invite, TogetherInvitation.parse(wrappedInChatMessage))
        for (bad in listOf(invite.encode() + "#fragment", invite.encode() + "&key=duplicate", invite.encode().replace("/v1", "/v2"),
            invite.encode().replace("resonance-listen", "http"), "resonance-listen://user:password@localhost:80/v1?pin=x&key=y")) {
            assertFails { TogetherInvitation.parse(bad) }
        }
    }

    @Test fun authenticatedAudioPauseSeekAndNextTrackStayWithinTemporaryCache() = runBlocking {
        val directory = Files.createTempDirectory("together-test-")
        val payloadA = ByteArray(130_001) { (it * 13).toByte() }
        val payloadB = ByteArray(98_003) { (it * 7).toByte() }
        val host = JvmListenTogetherService(directory.resolve("host"), { ByteArrayInputStream(if (it.id == "a") payloadA else payloadB) })
        val stopped = AtomicInteger()
        val guestCache = Files.createDirectories(directory.resolve("guest"))
        val sentinel = Files.write(guestCache.resolve("keep.txt"), byteArrayOf(1, 2, 3))
        val guest = JvmListenTogetherService(guestCache, { error("Guest must not expose local audio") }, { stopped.incrementAndGet() })
        try {
            host.publishPlayback(PlayerState(currentTrack = track(), isPlaying = true, progress = .2f))
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            val first = ready(guest, track().title)
            assertContentEquals(payloadA, Files.readAllBytes(java.nio.file.Path.of(first.track!!.sourceUri!!)))
            assertTrue(guest.state.value.tlsVersion in setOf("TLSv1.3", "TLSv1.2"))
            assertEquals(host.state.value.verificationCode, guest.state.value.verificationCode)
            host.publishPlayback(PlayerState(currentTrack = track(), isPlaying = false, progress = .4f))
            val paused = withTimeout(4_000) { guest.remotePlayback.first { it != null && !it.playing && kotlin.math.abs(it.progress - .4f) < .001f }!! }
            assertEquals(first.mediaId, paused.mediaId)
            host.publishPlayback(PlayerState(currentTrack = track("b"), isPlaying = true, progress = .6f))
            val second = ready(guest, track("b").title)
            assertNotEquals(first.mediaId, second.mediaId)
            assertContentEquals(payloadB, Files.readAllBytes(java.nio.file.Path.of(second.track!!.sourceUri!!)))
            host.leave()
            withTimeout(4_000) { guest.state.first { it.status == TogetherStatus.Reconnecting } }
            withTimeout(12_000) { guest.state.first { it.status == TogetherStatus.Error } }
            assertNull(guest.remotePlayback.value)
            assertTrue(stopped.get() > 0)
            guest.leave()
            delay(2_200)
            assertTrue(Files.exists(sentinel))
            assertFalse(Files.exists(java.nio.file.Path.of(second.track!!.sourceUri!!)))
        } finally { guest.close(); host.close() }
    }

    @Test fun wrongPinAndWrongKeyCannotReadAudioOrOccupyTheRoom() = runBlocking {
        val directory = Files.createTempDirectory("together-auth-")
        val reads = AtomicInteger()
        val host = JvmListenTogetherService(directory.resolve("host"), { reads.incrementAndGet(); ByteArrayInputStream(byteArrayOf(1)) })
        val guest = JvmListenTogetherService(directory.resolve("guest"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            val invitation = TogetherInvitation.parse(host.invitation("127.0.0.1"))
            val wrongPin = invitation.copy(pin = if (invitation.pin[0] == '0') "1" + invitation.pin.drop(1) else "0" + invitation.pin.drop(1))
            guest.join(wrongPin.encode())
            assertEquals(TogetherStatus.Error, guest.state.value.status)
            guest.join(invitation.copy(key = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32))).encode())
            assertEquals(TogetherStatus.Error, guest.state.value.status)
            assertEquals(0, reads.get())
            assertEquals(TogetherStatus.Listening, host.state.value.status)
            guest.join(invitation.encode())
            ready(guest, track().title)
            assertEquals(1, reads.get())
        } finally { guest.close(); host.close() }
    }

    @Test fun leavingAndImmediatelyRejoiningDoesNotDeleteNewSessionMedia() = runBlocking {
        val directory = Files.createTempDirectory("together-rejoin-")
        val host = JvmListenTogetherService(directory.resolve("host"), { ByteArrayInputStream(ByteArray(512) { 42 }) })
        val guest = JvmListenTogetherService(directory.resolve("guest"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            ready(guest, track().title)
            guest.leave()
            withTimeout(3_000) { host.state.first { it.status == TogetherStatus.Listening } }
            guest.join(host.invitation("127.0.0.1"))
            val second = ready(guest, track().title)
            delay(2_500)
            assertTrue(Files.exists(java.nio.file.Path.of(second.track!!.sourceUri!!)))
        } finally { guest.close(); host.close() }
    }

    @Test fun malformedAuthenticatedFramesNeverBecomePlayableFiles() = runBlocking {
        val directory = Files.createTempDirectory("together-framing-")
        val identity = TogetherTls.identity()
        val listener = (identity.context.serverSocketFactory.createServerSocket(0) as SSLServerSocket).apply { TogetherTls.configure(this) }
        val server = launch(Dispatchers.IO) {
            (listener.accept() as SSLSocket).use { socket ->
                TogetherTls.configure(socket); socket.startHandshake()
                val input = DataInputStream(socket.inputStream)
                assertEquals(1, input.readUnsignedByte())
                val size = input.readInt()
                val hello = JSONObject(ByteArray(size).also(input::readFully).toString(Charsets.UTF_8))
                assertEquals(identity.key, hello.getString("key"))
                val output = DataOutputStream(socket.outputStream)
                val welcome = "{\"type\":\"welcome\",\"version\":1}".toByteArray()
                output.writeByte(1); output.writeInt(welcome.size); output.write(welcome); output.flush()
                output.writeByte(2); output.writeInt(Int.MAX_VALUE); output.flush()
                delay(100)
            }
        }
        val guest = JvmListenTogetherService(directory, { error("Unused") })
        try {
            guest.join(TogetherInvitation("127.0.0.1", listener.localPort, identity.pin, identity.key).encode())
            withTimeout(4_000) { guest.state.first { it.status == TogetherStatus.Error } }
            assertNull(guest.remotePlayback.value)
            assertEquals(0L, Files.list(directory).use { it.count() })
        } finally { guest.close(); listener.close(); server.cancelAndJoin() }
    }

    @Test fun ipv6LoopbackCanCarryTheEncryptedSession() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("together-ipv6-")
        val host = JvmListenTogetherService(directory.resolve("host"), { ByteArrayInputStream(byteArrayOf(1, 2, 3)) })
        val guest = JvmListenTogetherService(directory.resolve("guest"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            guest.join(host.invitation("::1"))
            ready(guest, track().title)
            withTimeout(3_000) { guest.state.first { it.status == TogetherStatus.Connected } }
        } finally { guest.close(); host.close() }
    }

    @Test fun roomCapacityRejectionAndEndedInvitationCannotJoinANewRoom() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("together-membership-")
        val host = JvmListenTogetherService(directory.resolve("host"), { ByteArrayInputStream(byteArrayOf(1, 2, 3)) })
        val guests = (1..7).map { JvmListenTogetherService(directory.resolve("guest-$it"), { error("Unused") }) }
        val eighth = JvmListenTogetherService(directory.resolve("eighth"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            val original = TogetherInvitation.parse(host.invitation("127.0.0.1"))
            for (g in guests) {
                g.join(original.encode())
                ready(g, track().title)
            }
            eighth.join(original.encode())
            assertEquals(TogetherStatus.Error, eighth.state.value.status)
            assertNotNull(guests.first().remotePlayback.value?.track)
            host.leave()
            host.host(0)
            val fresh = TogetherInvitation.parse(host.invitation("127.0.0.1"))
            assertNotEquals(original.pin, fresh.pin)
            assertNotEquals(original.key, fresh.key)
            eighth.join(original.copy(port = fresh.port).encode())
            assertEquals(TogetherStatus.Error, eighth.state.value.status)
            eighth.join(fresh.encode())
            ready(eighth, track().title)
        } finally {
            eighth.close()
            guests.forEach { it.close() }
            host.close()
        }
    }

    @Test fun invitationSupportsMultipleCandidatesAndRoundTrips() {
        val invite = TogetherInvitation(
            "2409:8a00:1234::1",
            45873,
            "ab".repeat(32),
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }),
            listOf("192.168.1.100", "192.168.43.1")
        )
        val encoded = invite.encode()
        assertTrue(encoded.contains("cands=192.168.1.100,192.168.43.1"))
        val parsed = TogetherInvitation.parse(encoded)
        assertEquals(invite, parsed)
        assertEquals(listOf("2409:8a00:1234::1", "192.168.1.100", "192.168.43.1"), parsed.allHosts)
    }

    @Test fun racedConnectionSuccessfullySelectsReachableCandidate() = runBlocking {
        val directory = Files.createTempDirectory("together-race-test-")
        val host = JvmListenTogetherService(directory.resolve("host"), { ByteArrayInputStream(byteArrayOf(1, 2, 3)) })
        val guest = JvmListenTogetherService(directory.resolve("guest"), { error("Unused") })
        try {
            host.publishPlayback(PlayerState(currentTrack = track()))
            host.host(0)
            val realPort = host.state.value.port
            val keys = TogetherInvitation.parse(host.invitation("127.0.0.1"))
            // 包含一个不可达的保留地址 192.0.2.1 和真实的 127.0.0.1
            val multiInvite = TogetherInvitation("192.0.2.1", realPort, keys.pin, keys.key, listOf("127.0.0.1"))
            guest.join(multiInvite.encode())
            val received = withTimeout(6_000) { guest.remotePlayback.first { it?.track != null } }
            assertNotNull(received)
            assertEquals("127.0.0.1", guest.state.value.peer)
            assertEquals(TogetherStatus.Connected, guest.state.value.status)
        } finally {
            guest.close()
            host.close()
        }
    }
}
