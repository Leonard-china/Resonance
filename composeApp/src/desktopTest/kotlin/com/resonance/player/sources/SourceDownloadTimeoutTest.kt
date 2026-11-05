package com.resonance.player.sources

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import kotlin.test.*

class SourceDownloadTimeoutTest {
    @Test fun progressingAudioOutlivesMetadataDeadlineWithoutChangingPublicAddressGuards() = runBlocking {
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/") { exchange ->
            try {
                exchange.sendResponseHeaders(200, 4)
                exchange.responseBody.use { output ->
                    output.write(byteArrayOf(1)); output.flush()
                    Thread.sleep(250)
                    output.write(byteArrayOf(2, 3, 4))
                }
            } catch (_: java.io.IOException) { /* Metadata intentionally times out. */ }
            finally { exchange.close() }
        }
        server.start()
        val directory = Files.createTempDirectory("resonance-download-timeout-")
        try {
            // Only this injected test transport rewrites to loopback, after the production URL check.
            val client = OkHttpClient.Builder().callTimeout(100, TimeUnit.MILLISECONDS)
                .readTimeout(2, TimeUnit.SECONDS).addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").build())
                }.build()
            val network = PublicSourceNetwork(client)
            assertFails { network.request(JSONObject().put("url", "https://fixture.example/metadata")) }
            val target = directory.resolve("fixture.bin")
            assertEquals(4L, network.download("https://fixture.example/audio", target))
            assertContentEquals(byteArrayOf(1, 2, 3, 4), Files.readAllBytes(target))
            assertFailsWith<IllegalArgumentException> { network.download("http://localhost/audio", target) }
            assertFalse(PublicSourceNetwork.publicAddress(java.net.InetAddress.getByName("127.0.0.1")))
        } finally { server.stop(0); executor.shutdownNow(); directory.toFile().deleteRecursively() }
    }
}
