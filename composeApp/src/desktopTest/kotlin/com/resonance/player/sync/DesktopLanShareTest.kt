package com.resonance.player.sync

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLanShareTest {
    @Test
    fun servesPackageExactlyOnceAndDeletesSecretsOnClose() {
        val root = Files.createTempDirectory("resonance-lan-test-")
        try {
            val packagePath = root.resolve("payload.resonance")
            val qrPath = root.resolve("pairing.png")
            val payload = "RESONPKG-test-payload".encodeToByteArray()
            Files.write(packagePath, payload)

            val share = DesktopLanShare(packagePath, "one-time-secret")
            share.writeQr(qrPath)
            assertTrue(Files.isRegularFile(qrPath))
            assertTrue(share.link.startsWith("resonance://sync?"))
            val query = URI(share.link).rawQuery.split('&').associate { part ->
                val (key, value) = part.split('=', limit = 2)
                key to URLDecoder.decode(value, Charsets.UTF_8)
            }
            assertEquals("one-time-secret", query["key"])
            val sourceUrl = requireNotNull(query["url"])

            val first = URI(sourceUrl).toURL().openConnection() as HttpURLConnection
            assertEquals(200, first.responseCode)
            assertContentEquals(payload, first.inputStream.use { it.readBytes() })
            first.disconnect()

            val second = URI(sourceUrl).toURL().openConnection() as HttpURLConnection
            assertEquals(403, second.responseCode)
            second.disconnect()

            share.close()
            assertFalse(Files.exists(packagePath))
            assertFalse(Files.exists(qrPath))
        } finally {
            if (Files.exists(root)) {
                Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
            }
        }
    }
}
