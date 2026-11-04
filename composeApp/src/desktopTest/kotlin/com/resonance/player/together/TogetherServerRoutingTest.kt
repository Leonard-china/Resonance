package com.resonance.player.together

import com.resonance.player.platform.TogetherStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.util.Base64
import kotlin.test.*

class TogetherServerRoutingTest {
    @Test fun serverFailureDoesNotDialReachablePeerAddress() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("together-server-route-")
        val guest = JvmListenTogetherService(directory, { error("No audio") })
        val unusedPort = ServerSocket(0).use { it.localPort }
        ServerSocket(0).use { direct ->
            try {
                val invitation = TogetherInvitation("127.0.0.1", direct.localPort, "ab".repeat(32),
                    Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }),
                    relayUrl = "https://127.0.0.1:$unusedPort")
                withTimeout(12_000) { guest.join(invitation.encode()) }
                assertEquals(TogetherStatus.Error, guest.state.value.status)
                assertTrue(guest.state.value.message.contains("服务器"))
                direct.soTimeout = 250
                assertFailsWith<SocketTimeoutException> { direct.accept().close() }
            } finally {
                guest.close()
                directory.toFile().deleteRecursively()
            }
        }
    }

    @Test fun serverRoomDoesNotPublishPeerNetworkAddresses() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("together-server-host-")
        val store = TogetherCompanionStore(directory.resolve("companions"), { it }, { it })
        store.setRelaySettings("https://127.0.0.1:1",
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 }))
        val host = JvmListenTogetherService(directory.resolve("cache"), { error("No audio") }, companionStore = store)
        try {
            host.host(0)
            assertEquals(TogetherStatus.Listening, host.state.value.status)
            val invitation = TogetherInvitation.parse(host.invitation("192.0.2.1", host.state.value.port, listOf("2001:db8::1")))
            assertEquals("127.0.0.1", invitation.host)
            assertTrue(invitation.candidates.isEmpty())
            assertEquals(listOf("127.0.0.1"), host.state.value.addresses)
            assertEquals("https://127.0.0.1:1", invitation.relayUrl)
        } finally {
            host.close()
            directory.toFile().deleteRecursively()
        }
    }
}
