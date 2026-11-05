package com.resonance.player.together

import com.resonance.player.platform.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import org.junit.Test
import kotlin.test.*

class TogetherCompanionTest {
    @Test fun incomingMessageEventFiresAfterPersistenceButNotForReplayOrClearedHistory() = runBlocking {
        val root = Files.createTempDirectory("together-message-events-")
        val store = Vault().store(root)
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = TogetherCompanionSession(store, owner)
        val observed = Channel<Unit>(Channel.UNLIMITED)
        val peer = "a".repeat(64)
        val wire = Any()
        try {
            val observer = owner.launch(start = CoroutineStart.UNDISPATCHED) { session.chatMessageEvents.collect { observed.send(it) } }
            session.attach(peer, wire, sender = { _ -> })
            val message = JSONObject().put("type", "chat").put("sequence", 1).put("text", "新消息")
            assertTrue(session.receive(wire, message))
            withTimeout(5_000) { observed.receive() }
            assertEquals("新消息", store.messages(peer).single().text)
            assertTrue(session.receive(wire, message))
            assertNull(withTimeoutOrNull(250) { observed.receive() })
            store.clear(peer)
            assertTrue(session.receive(wire, message))
            assertNull(withTimeoutOrNull(250) { observed.receive() })
            assertTrue(store.messages(peer).isEmpty())
            observer.cancel()
        } finally { owner.cancel(); observed.close() }
    }
    private class Vault {
        val key = KeyGenerator.getInstance("AES").apply { init(128) }.generateKey()
        fun seal(value: String): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
            return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()))
        }
        fun open(value: String): String {
            val bytes = Base64.getDecoder().decode(value)
            return String(Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }.doFinal(bytes.copyOfRange(12, bytes.size)))
        }
        fun store(path: Path) = TogetherCompanionStore(path, ::seal, ::open)
    }
    @Test fun encryptedIdentityAndHistorySurviveRestartAndClearDoesNotReplay() {
        val directory = Files.createTempDirectory("together-local-chat-")
        val vault = Vault()
        val store = vault.store(directory)
        val peer = "a".repeat(64)
        val identity = store.identity()
        assertContentEquals(identity.private.encoded, vault.store(directory).identity().private.encoded)
        store.enqueue(peer, "私密测试文本")
        store.receive(peer, 1, "朋友的消息")
        store.receive(peer, 1, "朋友的消息")
        assertEquals(2, vault.store(directory).messages(peer).size)
        Files.list(directory).use { paths -> paths.filter(Files::isRegularFile).forEach { file ->
            assertFalse(Files.readString(file).contains("私密测试文本"))
            assertFalse(Files.readString(file).contains("朋友的消息"))
        } }
        val oldData = Files.readAllBytes(directory.resolve("chat-$peer.properties"))
        store.clear(peer)
        // Simulate restoration of pre-clear main and backup; the independent no-rollback marker wins.
        Files.write(directory.resolve("chat-$peer.properties"), oldData)
        Files.write(directory.resolve("chat-$peer.properties.bak"), oldData)
        val restarted = vault.store(directory)
        assertTrue(restarted.messages(peer).isEmpty())
        restarted.receive(peer, 1, "朋友的消息")
        assertTrue(restarted.messages(peer).isEmpty())
        assertEquals(2, restarted.enqueue(peer, "新的消息").sequence)
        restarted.receive(peer, 2, "第二条")
        assertEquals(2, restarted.messages(peer).size)
        restarted.clear(peer)
        Files.writeString(directory.resolve("chat-$peer.properties.clear"), "broken")
        assertFails { vault.store(directory).messages(peer) }
        assertFails { store.messages("../outside") }
        assertFails { store.enqueue(peer, "x".repeat(2001)) }
    }
    @Test fun boundedPendingQueueNeverSilentlyDropsUndeliveredMessages() {
        val store = Vault().store(Files.createTempDirectory("together-outbox-"))
        val peer = "b".repeat(64)
        repeat(100) { store.enqueue(peer, "消息 $it") }
        assertFails { store.enqueue(peer, "超过上限") }
        store.acknowledge(peer, 1)
        assertEquals(101, store.enqueue(peer, "确认后可以继续").sequence)
        assertEquals(100, store.messages(peer).count { it.outgoing && !it.delivered })
    }
    private fun service(root: Path, vault: Vault? = Vault()) = JvmListenTogetherService(root.resolve("audio"), { error("No audio expected") },
        companionStore = vault?.store(root.resolve("companions")), companionPort = 0, companionAddresses = { listOf("127.0.0.1") })
    private suspend fun waitFor(condition: () -> Boolean) = withTimeout(10_000) { while (!condition()) delay(20) }
    @Test fun firstLaunchDoesNotListenUntilRoomActionButRememberedPartnersCanReceiveInvites() = runBlocking {
        val root = Files.createTempDirectory("together-startup-listen-")
        for (remembered in listOf(false, true)) {
            val store = Vault().store(root.resolve("store-$remembered"))
            if (remembered) store.pair("b".repeat(64), "已配对设备")
            val service = JvmListenTogetherService(root.resolve("cache-$remembered"), { error("No audio") },
                companionStore = store, companionPort = 0, companionAddresses = { listOf("127.0.0.1") })
            try {
                withTimeout(5_000) { service.companionStartup.await() }
                assertEquals(remembered, service.listeningPartnerPort > 0)
                if (!remembered) {
                    service.host(0)
                    assertEquals(0, service.listeningPartnerPort)
                    val guest = this@TogetherCompanionTest.service(root.resolve("first-guest"))
                    try {
                        guest.join(service.invitation("127.0.0.1"))
                        waitFor { service.companion.value.connected && guest.companion.value.connected }
                        assertEquals(0, guest.listeningPartnerPort)
                        service.requestPairing()
                        waitFor { guest.companion.value.pairingIncoming }
                        assertTrue(service.listeningPartnerPort > 0)
                        assertEquals(0, guest.listeningPartnerPort)
                        guest.respondPairing(true)
                        waitFor { service.companion.value.paired && guest.companion.value.paired }
                        assertTrue(guest.listeningPartnerPort > 0)
                    } finally { guest.close() }
                    assertTrue(service.listeningPartnerPort > 0)
                }
            } finally { service.close() }
        }
    }
    @Test fun realTlsChatPairingLocalClearAndStableDeviceIdentityAcrossNewRooms() = runBlocking {
        val root = Files.createTempDirectory("together-chat-tls-")
        val hostVault = Vault(); val guestVault = Vault()
        var host = service(root.resolve("host"), hostVault)
        var guest = service(root.resolve("guest"), guestVault)
        try {
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            waitFor { host.companion.value.connected && guest.companion.value.connected }
            val hostId = guest.companion.value.peerId
            val guestId = host.companion.value.peerId
            host.sendChat("晚安，明天还一起听")
            guest.sendChat("好，房间里见")
            waitFor { host.companion.value.messages.size == 2 && guest.companion.value.messages.size == 2 &&
                host.companion.value.messages.all { it.delivered } && guest.companion.value.messages.all { it.delivered } }
            assertFalse(host.sendChat("错误目标不能接收", "0".repeat(64)))
            assertEquals(2, host.companion.value.messages.size)
            assertEquals(2, guest.companion.value.messages.size)
            host.requestPairing()
            waitFor { guest.companion.value.pairingIncoming }
            assertTrue(host.companion.value.partners.isEmpty())
            assertTrue(guest.companion.value.partners.isEmpty())
            guest.respondPairing(true)
            waitFor { host.companion.value.paired && guest.companion.value.paired }
            guest.clearChat(hostId)
            waitFor { guest.companion.value.messages.isEmpty() }
            assertEquals(2, host.companion.value.messages.size)
            host.close(); guest.close()
            host = service(root.resolve("host"), hostVault)
            guest = service(root.resolve("guest"), guestVault)
            host.host(0)
            guest.join(host.invitation("127.0.0.1"))
            waitFor { host.companion.value.connected && guest.companion.value.connected }
            // attach publishes the connection before refreshing persisted partner/history state.
            waitFor { host.companion.value.paired && guest.companion.value.paired &&
                host.companion.value.messages.size == 2 }
            assertEquals(hostId, guest.companion.value.peerId)
            assertEquals(guestId, host.companion.value.peerId)
            assertTrue(host.companion.value.paired && guest.companion.value.paired)
            assertTrue(guest.companion.value.messages.isEmpty())
            assertEquals(2, host.companion.value.messages.size)
            guest.sendChat("今天的新消息")
            waitFor { host.companion.value.messages.size == 3 && guest.companion.value.messages.size == 1 && guest.companion.value.messages.single().delivered }
            host.removePartner(guestId)
            waitFor { host.companion.value.partners.isEmpty() }
            assertTrue(guest.companion.value.paired)
        } finally { host.close(); guest.close() }
    }
    @Test fun legacyAnonymousRoomsRemainCompatibleBothWays() = runBlocking {
        val root = Files.createTempDirectory("together-legacy-chat-")
        for (newHost in listOf(true, false)) {
            val host = service(root.resolve("h-$newHost"), if (newHost) Vault() else null)
            val guest = service(root.resolve("g-$newHost"), if (newHost) null else Vault())
            try {
                host.host(0); guest.join(host.invitation("127.0.0.1"))
                waitFor { host.state.value.status == TogetherStatus.Connected && guest.state.value.status == TogetherStatus.Connected }
                assertFalse(host.companion.value.connected)
                assertFalse(guest.companion.value.connected)
            } finally { guest.close(); host.close() }
        }
    }
    @Test fun rememberedPartnerInviteRequiresAcceptanceAndRemovalRevokesOffers() = runBlocking {
        val root = Files.createTempDirectory("together-partner-invite-")
        val host = service(root.resolve("host")); val guest = service(root.resolve("guest"))
        try {
            host.host(0); guest.join(host.invitation("127.0.0.1"))
            waitFor { host.companion.value.connected && guest.companion.value.connected }
            host.requestPairing(); waitFor { guest.companion.value.pairingIncoming }; guest.respondPairing(true)
            waitFor { host.companion.value.paired && guest.companion.value.paired }
            val hostId = guest.companion.value.peerId; val guestId = host.companion.value.peerId
            guest.leave(); host.leave()
            waitFor { !host.companion.value.connected && !guest.companion.value.connected }
            host.requireJoinApproval(true)
            host.host(0)
            host.invitePartner(guestId)
            waitFor { guest.companion.value.incomingPartnerId == hostId }
            assertEquals(TogetherRole.None, guest.state.value.role)
            assertEquals(TogetherStatus.Listening, host.state.value.status)
            guest.respondPartnerInvitation(false)
            waitFor { guest.companion.value.incomingPartnerId.isEmpty() }
            assertEquals(TogetherRole.None, guest.state.value.role)
            waitFor { host.companion.value.message.contains("邀请已送达") }
            host.invitePartner(guestId)
            waitFor { guest.companion.value.incomingPartnerId == hostId }
            guest.respondPartnerInvitation(true)
            waitFor { guest.companion.value.connected && host.companion.value.connected }
            assertEquals(hostId, guest.companion.value.peerId)
            assertFalse(host.state.value.pendingJoin) // The host's explicit invite already approved this exact device.
            guest.leave(); host.leave()
            waitFor { !host.companion.value.connected && !guest.companion.value.connected }
            guest.removePartner(hostId)
            waitFor { guest.companion.value.partners.isEmpty() }
            host.host(0); host.invitePartner(guestId)
            waitFor { host.companion.value.message.contains("未能送达") }
            assertTrue(guest.companion.value.incomingPartnerId.isEmpty())
            assertEquals(TogetherRole.None, guest.state.value.role)
        } finally { guest.close(); host.close() }
    }
}
