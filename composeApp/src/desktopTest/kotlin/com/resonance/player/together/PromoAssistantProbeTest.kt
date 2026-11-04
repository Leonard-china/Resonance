package com.resonance.player.together

import com.resonance.player.model.AssistantMessage
import com.resonance.player.platform.DesktopPlatformServices
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.json.JSONObject
import java.nio.file.*
import kotlin.test.Test

/** Opt-in real request with original demo text, never exporting connection credentials. */
class PromoAssistantProbeTest {
    @Test fun captureRealAssistantAnswerWhenConfigured() = runBlocking<Unit> {
        assumeTrue(System.getenv("RESONANCE_PROMO_AI") == "1")
        val directory = Path.of("build/promo-assistant-proof").toAbsolutePath()
        Files.createDirectories(directory)
        val services = DesktopPlatformServices()
        try {
            val assistant = services.musicAssistant
            val settings = assistant.settings()
            val configured = settings.connections.any { it.provider == settings.assistantProvider && it.configured }
            val proof = JSONObject().put("configured", configured).put("success", false)
            if (configured) {
                val prompt = "从这份原创演示曲库里挑两首适合深夜聊天的歌，简短解释，用不超过100字中文回答，不调用工具：晚风与海（舒缓氛围）；城市入夜以后（轻柔钢琴）；沿着光慢慢走（明快电子）。"
                try {
                    val answer = assistant.completeText(listOf(AssistantMessage("user", prompt)), 256)
                    check(answer.isNotBlank())
                    proof.put("success", true).put("prompt", prompt).put("answer", answer.take(2000))
                        .put("provider", settings.assistantProvider.name).put("capturedAt", System.currentTimeMillis())
                } catch (_: Exception) { proof.put("error", "Configured model request did not complete") }
            }
            Files.writeString(directory.resolve("proof.json"), proof.toString(2))
        } finally { services.close() }
    }
}
