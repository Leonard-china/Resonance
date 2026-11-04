package com.resonance.player.together

import com.resonance.player.platform.TogetherPartner
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.*

class TogetherRendezvousTest {
    @Test fun productionUrlsRequireHttpsWithoutCredentialsQueriesOrFragments() {
        for (url in listOf("http://127.0.0.1:8765", "http://example.com", "https://user:pass@example.com", "https://example.com?q=x", "https://example.com/#x", "file:///tmp/a")) {
            assertFails { TogetherRendezvousClient.checkedUrl(url) }
        }
        assertEquals("https://example.com/prefix/", TogetherRendezvousClient.checkedUrl("https://example.com/prefix").toString())
        assertEquals("http://127.0.0.1:8765/", TogetherRendezvousClient.checkedUrl("http://127.0.0.1:8765", true).toString())
        assertFails { TogetherRendezvousClient.checkedUrl("http://192.168.1.2:8765", true) }
    }
    private fun key() = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private fun id(key: KeyPair) = MessageDigest.getInstance("SHA-256").digest(key.public.encoded).joinToString("") { "%02x".format(it.toInt() and 255) }
    @Test fun javaSignaturesInteroperateAndChangedPartnerEndpointCanBeInvited() = runBlocking {
        val python = System.getenv("RESONANCE_RENDEZVOUS_PYTHON")
        assumeTrue(!python.isNullOrEmpty())
        val script = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .map { it.resolve("services/rendezvous/server.py") }.first { Files.isRegularFile(it) }
        val process = ProcessBuilder(python!!, script.toString(), "--host", "127.0.0.1", "--port", "0").redirectErrorStream(true).start()
        var host: JvmListenTogetherService? = null; var guest: JvmListenTogetherService? = null
        try {
            val line = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readLine() }.get(10, TimeUnit.SECONDS)
            val port = Regex("port ([0-9]+)").find(line ?: "")?.groupValues?.get(1)?.toInt() ?: error("Local test service did not start")
            val url = "http://127.0.0.1:$port"
            val a = key(); val b = key()
            val aPartner = TogetherPartner(id(a), "A"); val bPartner = TogetherPartner(id(b), "B")
            val clientA = TogetherRendezvousClient(url, a, true); val clientB = TogetherRendezvousClient(url, b, true)
            fun contact(port: Int) = JSONObject().put("addresses", JSONArray(listOf("127.0.0.1"))).put("port", port)
            clientA.register(contact(4567), listOf(bPartner)); clientB.register(contact(4568), listOf(aPartner))
            assertEquals(4568, clientA.lookup(bPartner)?.port)
            clientB.withdraw(); assertNull(clientA.lookup(bPartner))

            val root = Files.createTempDirectory("together-rendezvous-native-")
            fun store(name: String): TogetherCompanionStore {
                val vault = com.resonance.player.features.DesktopCredentialVault(root.resolve(name))
                return TogetherCompanionStore(root.resolve(name), vault::encrypt, vault::decrypt)
            }
            val hostStore = store("host"); val guestStore = store("guest")
            val hostId = id(hostStore.identity()); val guestId = id(guestStore.identity())
            // Deliberately stale endpoint. Success must come from fresh signed reachability metadata.
            hostStore.pair(guestId, "伙伴", listOf("127.0.0.2"), 1)
            guestStore.pair(hostId, "伙伴", listOf("127.0.0.2"), 1)
            val runningHost = JvmListenTogetherService(root.resolve("h-audio"), { error("No audio") }, companionStore = hostStore,
                companionPort = 0, companionAddresses = { listOf("127.0.0.1") }, allowTestRendezvousHttp = true)
            val runningGuest = JvmListenTogetherService(root.resolve("g-audio"), { error("No audio") }, companionStore = guestStore,
                companionPort = 0, companionAddresses = { listOf("127.0.0.1") }, allowTestRendezvousHttp = true)
            suspend fun until(condition: () -> Boolean) = withTimeout(15_000) { while (!condition()) delay(20) }
            host = runningHost; guest = runningGuest
            runningHost.configureRendezvous(url); runningGuest.configureRendezvous(url)
            until { runningHost.companion.value.rendezvousStatus.contains("已连接") && runningGuest.companion.value.rendezvousStatus.contains("已连接") }
            runningHost.host(0); runningHost.invitePartner(guestId)
            until { runningGuest.companion.value.incomingPartnerId == hostId }
            assertFalse(runningGuest.companion.value.connected)
            runningGuest.respondPartnerInvitation(true)
            until { runningHost.companion.value.connected && runningGuest.companion.value.connected }
            assertEquals(hostId, runningGuest.companion.value.peerId)
        } finally { guest?.close(); host?.close(); process.destroy(); if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly() }
    }
}
