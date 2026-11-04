package com.resonance.player

import android.net.LocalServerSocket
import androidx.test.platform.app.InstrumentationRegistry
import com.resonance.player.features.AndroidMusicAccess
import com.resonance.player.model.*
import kotlinx.coroutines.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Explicit private-camera setup; credentials arrive only over an owned ADB local socket. */
class PromoAiSetupDeviceTest {
    @Test fun configurePrivateCameraThroughEphemeralSocket() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("configurePromoAi") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        require(context.packageName.endsWith(".codex")) { "Use the isolated camera package" }
        val nonce = args.getString("promoSocketNonce").orEmpty()
        require(nonce.matches(Regex("[a-f0-9]{32}")))
        val channel = LocalServerSocket("resonance-promo-ai-$nonce")
        val timeout = launch(Dispatchers.IO) { delay(60_000); channel.close() }
        try {
            println("PROMO_AI_CHANNEL_READY")
            val connection = withContext(Dispatchers.IO) { channel.accept() }
            connection.use { socket ->
                require(socket.peerCredentials.uid in setOf(0, 2000)) { "Only the ADB fixture may configure this camera" }
                socket.soTimeout = 15_000
                val bytes = withContext(Dispatchers.IO) {
                    val input = socket.inputStream
                    val result = java.io.ByteArrayOutputStream()
                    while (true) {
                        val value = input.read()
                        if (value == -1 || value == 10) break
                        require(result.size() < 256) { "Invalid credential input" }
                        result.write(value)
                    }
                    result.toByteArray()
                }
                try {
                    val credential = bytes.toString(Charsets.UTF_8).trim()
                    require(credential.length in 16..256 && !credential.any(Char::isWhitespace)) { "Invalid credential input" }
                    val assistant = AndroidMusicAccess.runtime(context)
                    val current = assistant.settings()
                    assistant.saveSettings(current.copy(assistantProvider = AiProvider.DeepSeek,
                        connections = current.connections.filterNot { it.provider == AiProvider.DeepSeek } +
                            AiConnection(AiProvider.DeepSeek, apiKey = credential)))
                    assistant.updateLibrarySnapshot(listOf(
                        Track("promo-wind", "晚风与海", "Resonance Studio", "原创演示", "3:20", 1),
                        Track("promo-city", "城市入夜以后", "Resonance Studio", "原创演示", "3:40", 2),
                        Track("promo-light", "沿着光慢慢走", "Resonance Studio", "原创演示", "3:10", 3)), emptyList())
                    File(context.filesDir, "promo-ai-setup.txt").writeText("configured; Android Keystore protected")
                } finally { bytes.fill(0) }
            }
        } finally { timeout.cancel(); channel.close() }
    }
}
