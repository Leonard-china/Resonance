package com.resonance.player.together

import com.resonance.player.platform.TogetherStatus
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*

class TogetherConnectionBoundsTest {
    private fun invitation() = TogetherInvitation("127.0.0.1", 45873, "ab".repeat(32),
        Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))

    @Test fun candidateCountAndEveryAddressAreValidatedBeforeConnecting() {
        val base = invitation().encode()
        listOf("bad/path", "user@example.com", "127.0.0.1:99", "", "%20localhost", "[broken").forEach {
            assertFails(it) { TogetherInvitation.parse("$base&cands=$it") }
        }
        assertFails { invitation().copy(candidates = listOf("a b")).encode() }
        assertEquals(invitation(), TogetherInvitation.parse("音乐邀请：$base 请确认加入"))
        assertFails { TogetherInvitation.parse("$base&cands=" + List(8) { "127.0.0.${it+2}" }.joinToString(",")) }
        assertFails { invitation().copy(candidates = List(100) { "127.0.0.1" }).encode() }
        val bounded = invitation().copy(candidates = List(7) { "127.0.0.${it+2}" })
        assertEquals(8, TogetherInvitation.parse(bounded.encode()).allHosts.size)
        assertEquals(listOf("127.0.0.1"), TogetherInvitation.parse(base).allHosts)
    }

    @Test fun leavingDuringParallelTlsHandshakesClosesAllAttemptsPromptly() = runBlocking<Unit> {
        val listener = ServerSocket(0)
        val accepted = CopyOnWriteArrayList<Socket>()
        val server = launch(Dispatchers.IO) {
            try { repeat(2) { accepted += listener.accept() } } catch (_: Exception) { }
        }
        val guest = JvmListenTogetherService(Files.createTempDirectory("together-cancel-race-"), { error("Never read audio") })
        try {
            val invitation = invitation().copy(port = listener.localPort, candidates = listOf("127.0.0.2"))
            val joining = async(Dispatchers.IO) { guest.join(invitation.encode()) }
            withTimeout(3_000) { while (accepted.size < 2) delay(10) }
            guest.leave()
            withTimeout(2_000) {
                val cancelled = runCatching { joining.await() }.exceptionOrNull()
                assertTrue(cancelled is CancellationException && cancelled !is TimeoutCancellationException)
            }
            assertEquals(TogetherStatus.Idle, guest.state.value.status)
            accepted.forEach { socket ->
                socket.soTimeout = 1_000
                // Consume the TLS ClientHello, then confirm the client really closed the connection.
                val input = java.io.DataInputStream(socket.getInputStream())
                input.readUnsignedByte(); input.readUnsignedShort()
                val size = input.readUnsignedShort()
                input.readFully(ByteArray(size))
                val closed = try {
                    var remaining = 1_024
                    var value = input.read()
                    while (value != -1 && remaining-- > 0) value = input.read()
                    value == -1
                } catch (reset: java.net.SocketException) { true } // A reset also closes the TCP attempt; a timeout must fail.
                assertTrue(closed)
            }
        } finally { guest.close(); accepted.forEach { it.close() }; listener.close(); server.cancelAndJoin() }
    }
}
