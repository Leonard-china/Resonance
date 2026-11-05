package com.resonance.player.features

import com.resonance.player.ai.MusicAiClient
import com.resonance.player.model.AiConnection
import com.resonance.player.model.AiProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.*

class MusicAiWireTest {
    private open class Connection : HttpURLConnection(URL("https://example.invalid/chat/completions")) {
        val written = ByteArrayOutputStream()
        val payloadLength get() = fixedContentLength
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getOutputStream() = written
        override fun getResponseCode() = 200
        override fun getInputStream(): java.io.InputStream = ByteArrayInputStream("""{"choices":[{"message":{"role":"assistant","content":"1,2"}}],"usage":{"prompt_tokens":10,"completion_tokens":2}}""".toByteArray())
    }
    @Test fun officialFlashRequestsFinalContentWhileGenericEndpointsAndReasonersKeepTheirContract() = runBlocking {
        for ((host, model, thinking) in listOf(
            Triple("api.deepseek.com", "deepseek-flash", "disabled"),
            Triple("api.deepseek.com", "deepseek-v4-pro", "disabled"),
            Triple("api.deepseek.com", "deepseek-chat", "disabled"),
            Triple("api.deepseek.com", "deepseek-reasoner", "enabled"),
            Triple("example.invalid", "deepseek-flash", null),
            Triple("example.invalid", "deepseek-chat", null),
        )) {
            val connection = Connection()
            val client = MusicAiClient { connection }
            val reply = client.complete(AiConnection(AiProvider.DeepSeek, "https://$host/v1", model, "fixture-only"),
                JSONArray().put(JSONObject().put("role", "user").put("content", "请选择编号")), maxTokens = 128)
            val body = JSONObject(connection.written.toString("UTF-8"))
            assertEquals(thinking != null, body.has("thinking"))
            if (thinking != null) assertEquals(thinking, body.getJSONObject("thinking").getString("type"))
            assertEquals(if (host == "api.deepseek.com" && model in setOf("deepseek-chat", "deepseek-reasoner")) "deepseek-flash" else model, body.getString("model"))
            assertEquals(if (model.contains("reasoner")) 1024 else 128, body.getInt("max_tokens"))
            assertEquals(!model.contains("reasoner"), body.has("temperature"))
            assertEquals("1,2", reply.message.getString("content"))
            assertEquals(2L, reply.usage.outputTokens)
            assertFalse(connection.instanceFollowRedirects)
            assertEquals(connection.written.size(), connection.payloadLength)
        }
    }

    @Test fun brokenConnectionIsNotReplayedAndReportsUnknownUsage() = runBlocking {
        var opened = 0
        val client = MusicAiClient {
            opened++
            object : Connection() {
                override fun getInputStream(): java.io.InputStream = throw java.io.IOException("connection closed")
            }
        }
        val error = assertFailsWith<java.io.IOException> {
            client.complete(AiConnection(AiProvider.DeepSeek, "https://example.invalid/v1", "fixture", "fixture-only"),
                JSONArray().put(JSONObject().put("role", "user").put("content", "fixture")))
        }
        assertEquals(1, opened)
        assertContains(error.message.orEmpty(), "未自动重发")
        assertContains(error.message.orEmpty(), "未取得本次用量")
    }
}
