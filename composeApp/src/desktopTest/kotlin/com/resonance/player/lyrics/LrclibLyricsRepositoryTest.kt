package com.resonance.player.lyrics

import com.resonance.player.model.LyricsFetchResult
import com.resonance.player.model.Track
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LrclibLyricsRepositoryTest {
    @Test
    fun parsesMultipleTimestampsAndFractions() {
        val lines = parseLrc("[00:01.2][00:02.345] First\n[01:03] Second\n[ar:Artist]")

        assertEquals(listOf(1_200L, 2_345L, 63_000L), lines.map { it.timestampMs })
        assertEquals(listOf("First", "First", "Second"), lines.map { it.text })
    }

    @Test
    fun fetchesExactLyricsAndUsesPersistentCache() = runBlocking {
        val root = Files.createTempDirectory("resonance-lyrics-test-")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api/get") { exchange ->
                requests.incrementAndGet()
                val body = """
                    {
                      "instrumental": false,
                      "plainLyrics": "First line\nSecond line",
                      "syncedLyrics": "[00:01.20] First line\n[00:04.50] Second line"
                    }
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        try {
            val track = Track("id", "Song", "Artist", "Album", "0:10", 1, sourceUri = "sample.mp3")
            val endpoint = URI("http://127.0.0.1:${server.address.port}")
            val first = LrclibLyricsRepository(root, endpoint).fetch(track)
            val fetched = assertIs<LyricsFetchResult.Found>(first).lyrics
            assertTrue(fetched.synchronized)
            assertEquals(2, fetched.lines.size)
            assertEquals(1, requests.get())

            server.stop(0)
            val cached = LrclibLyricsRepository(root, endpoint).fetch(track)
            assertTrue(assertIs<LyricsFetchResult.Found>(cached).lyrics.fromCache)
            assertEquals(1, requests.get())
        } finally {
            runCatching { server.stop(0) }
            Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
}
