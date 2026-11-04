package com.resonance.player

import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.features.AndroidCredentialVault
import com.resonance.player.platform.TogetherRole
import com.resonance.player.together.JvmListenTogetherService
import com.resonance.player.together.TogetherCompanionStore
import java.nio.file.Files
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TogetherCompanionDeviceTest {
    @Test fun androidKeystoreMutualTlsChatPairAndDirectInvite() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "companion-device-")
        fun service(name: String) = JvmListenTogetherService(root.resolve("$name-audio"), { error("No audio expected") },
            companionStore = TogetherCompanionStore(root.resolve(name), AndroidCredentialVault::encrypt, AndroidCredentialVault::decrypt),
            companionPort = 0, companionAddresses = { listOf("127.0.0.1") })
        suspend fun until(condition: () -> Boolean) = withTimeout(15_000) { while (!condition()) delay(25) }
        val host = service("host"); val guest = service("guest")
        try {
            host.host(0); guest.join(host.invitation("127.0.0.1"))
            until { host.companion.value.connected && guest.companion.value.connected }
            assertTrue(host.sendChat("模拟器真实加密聊天"))
            until { guest.companion.value.messages.size == 1 && host.companion.value.messages.single().delivered }
            val hostId = guest.companion.value.peerId; val guestId = host.companion.value.peerId
            host.requestPairing(); until { guest.companion.value.pairingIncoming }; guest.respondPairing(true)
            until { host.companion.value.paired && guest.companion.value.paired }
            guest.clearChat(hostId); until { guest.companion.value.messages.isEmpty() }
            assertEquals(1, host.companion.value.messages.size)
            guest.leave(); host.leave()
            until { !host.companion.value.connected && !guest.companion.value.connected }
            host.host(0); host.requireJoinApproval(true); host.invitePartner(guestId)
            until { guest.companion.value.incomingPartnerId == hostId }
            assertEquals(TogetherRole.None, guest.state.value.role)
            guest.respondPartnerInvitation(true)
            until { guest.companion.value.connected && host.companion.value.connected }
            assertEquals(hostId, guest.companion.value.peerId)
        } finally {
            guest.close(); host.close()
            // Only files below this test-owned cache directory are removed.
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }
}
