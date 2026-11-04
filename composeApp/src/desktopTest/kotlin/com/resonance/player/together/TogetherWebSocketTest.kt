package com.resonance.player.together

import org.json.JSONObject
import org.junit.Assume.assumeTrue
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.*

class TogetherWebSocketTest {
    @Test fun endpointRejectsInsecureOrAmbiguousTargets() {
        assertEquals("wss", TogetherRelayTransport.endpoint("wss://leonard.asia/resonance-relay/v1").scheme)
        for (url in listOf("ws://example.com/resonance-relay/v1", "wss://user@example.com/resonance-relay/v1",
            "wss://example.com/resonance-relay/v1?key=bad", "wss://example.com/other", "https://example.com/other")) {
            assertFails { TogetherRelayTransport.endpoint(url) }
        }
    }

    @Test fun defaultsRespectSavedOptOutAndCustomServer() {
        val directory = Files.createTempDirectory("relay-settings-")
        try {
            val store = TogetherCompanionStore(directory, { it }, { it }, "wss://leonard.asia/resonance-relay/v1")
            assertEquals("wss://leonard.asia/resonance-relay/v1", store.relaySettings().first)
            store.setRelaySettings("", "")
            assertEquals("", store.relaySettings().first)
            store.setRelaySettings("https://example.com", "fixture")
            assertEquals("https://example.com", store.relaySettings().first)
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun realPinnedTlsRoundTripOverWebSocket() {
        val python = System.getenv("RESONANCE_RELAY_TEST_PYTHON")
        assumeTrue(!python.isNullOrBlank())
        val script = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .map { it.resolve("services/relay/integration_fixture.py") }.first { Files.isRegularFile(it) }
        val process = ProcessBuilder(python!!, script.toString()).redirectErrorStream(true).start()
        try {
            val port = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readLine() }.get(10, TimeUnit.SECONDS).toInt()
            pinnedTlsRoundTrip("ws://127.0.0.1:$port/resonance-relay/v1")
        } finally { process.destroyForcibly(); process.waitFor(3, TimeUnit.SECONDS) }
    }

    // Explicit deployment acceptance only: ordinary tests never contact production.
    @Test fun deployedPinnedTlsRoundTripOverWebSocket() {
        val url = System.getenv("RESONANCE_RELAY_ACCEPTANCE_URL")
        assumeTrue(!url.isNullOrBlank())
        require(url == "wss://leonard.asia/resonance-relay/v1")
        pinnedTlsRoundTrip(url)
    }

    private fun pinnedTlsRoundTrip(url: String) {
        val host = TogetherWebSocket()
        val guest = TogetherWebSocket()
        try {
            val keys = TogetherTls.identity()
            val guestIdentity = TogetherTls.identity()
            fun hello(socket: TogetherWebSocket, role: String) {
                socket.open(url)
                socket.outputStream.write(JSONObject().put("version", 1).put("role", role)
                    .put("route", TogetherRelayTransport.route(keys.key)).toString().toByteArray())
            }
            fun status(socket: TogetherWebSocket): String = buildString {
                while (true) { val byte = socket.inputStream.read(); check(byte >= 0); if (byte == 10) break; append(byte.toChar()) }
            }
            hello(host, "host")
            assertTrue(status(host).contains("waiting"))
            hello(guest, "guest")
            assertTrue(status(host).contains("paired")); assertTrue(status(guest).contains("paired"))
            val server = CompletableFuture.supplyAsync {
                TogetherRelayTransport.roomSocket(host, keys, null, true).use { tls ->
                    val request = DataInputStream(tls.inputStream).readUTF()
                    DataOutputStream(tls.outputStream).apply { writeUTF("reply:$request"); flush() }
                    // Keep the TLS transport open until the peer acknowledges reading the reply.
                    DataInputStream(tls.inputStream).readBoolean()
                }
            }
            TogetherRelayTransport.roomSocket(guest, guestIdentity, keys.pin, false).use { tls ->
                DataOutputStream(tls.outputStream).apply { writeUTF("original-fixture"); flush() }
                assertEquals("reply:original-fixture", DataInputStream(tls.inputStream).readUTF())
                DataOutputStream(tls.outputStream).apply { writeBoolean(true); flush() }
                server.get(10, TimeUnit.SECONDS)
            }
        } finally { host.close(); guest.close() }
    }
}
