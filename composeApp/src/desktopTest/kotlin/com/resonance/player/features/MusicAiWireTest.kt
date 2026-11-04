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
    private class Connection : HttpURLConnection(URL("https://example.invalid/chat/completions")) {
        val written = ByteArrayOutputStream()
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getOutputStream() = written
        override fun getResponseCode() = 200
        override fun getInputStream() = ByteArrayInputStream("""{"choices":[{"message":{"role":"assistant","content":"1,2"}}],"usage":{"prompt_tokens":10,"completion_tokens":2}}""".toByteArray())
    }
    @Test fun officialFlashRequestsFinalContentWhileGenericEndpointsAndReasonersKeepTheirContract() = runBlocking {
        for ((host, model, disabled) in listOf(
            Triple("api.deepseek.com", "deepseek-flash", true),
            Triple("api.deepseek.com", "deepseek-v4-pro", true),
            Triple("api.deepseek.com", "deepseek-reasoner", false),
            Triple("example.invalid", "deepseek-flash", false),
        )) {
            val connection = Connection()
            val client = MusicAiClient { connection }
            val reply = client.complete(AiConnection(AiProvider.DeepSeek, "https://$host/v1", model, "fixture-only"),
                JSONArray().put(JSONObject().put("role", "user").put("content", "请选择编号")), maxTokens = 128)
            val body = JSONObject(connection.written.toString("UTF-8"))
            assertEquals(disabled, body.has("thinking"))
            if (disabled) assertEquals("disabled", body.getJSONObject("thinking").getString("type"))
            assertEquals(if (model.contains("reasoner")) 1024 else 128, body.getInt("max_tokens"))
            assertEquals(!model.contains("reasoner"), body.has("temperature"))
            assertEquals("1,2", reply.message.getString("content"))
            assertEquals(2L, reply.usage.outputTokens)
            assertFalse(connection.instanceFollowRedirects)
        }
    }
}
