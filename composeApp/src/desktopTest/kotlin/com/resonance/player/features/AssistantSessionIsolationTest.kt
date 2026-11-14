package com.resonance.player.features

import com.resonance.player.ai.*
import com.resonance.player.model.*
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.*

class AssistantSessionIsolationTest {
    private suspend fun fixture(block: suspend (JvmMusicAssistant, CompletableDeferred<Unit>, CompletableDeferred<Unit>) -> Unit) {
        val directory = Files.createTempDirectory("resonance-session-isolation-")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val library = Proxy.newProxyInstance(MusicLibraryAccess::class.java.classLoader,
            arrayOf(MusicLibraryAccess::class.java)) { _, method, _ ->
            when (method.name) {
                "tracks", "playlists" -> emptyList<Any>()
                else -> error("Unexpected library operation: ${method.name}")
            }
        } as MusicLibraryAccess
        val transport = object : MusicAiTransport {
            override suspend fun complete(connection: AiConnection, messages: JSONArray, tools: JSONArray?, maxTokens: Int): AiReply {
                entered.complete(Unit)
                release.await()
                return AiReply(JSONObject().put("role", "assistant").put("content", "reply for original session"), AssistantUsage())
            }
        }
        val runtime = JvmMusicAssistant(MusicJsonStore(directory), directory, library,
            client = transport, autoInstallBuiltinSources = false)
        try {
            runtime.saveSettings(MusicFeatureSettings(connections = listOf(AiConnection(apiKey = "fixture-key")), assistantWebSearchEnabled = false))
            runtime.settings()
            block(runtime, entered, release)
        } finally {
            release.complete(Unit)
            directory.toFile().deleteRecursively()
        }
    }

    @Test fun runningReplyCannotBeRedirectedBySessionMutations() = runBlocking {
        val changes: List<suspend (JvmMusicAssistant, String) -> Unit> = listOf(
            { runtime, _ -> runtime.createSession(); Unit },
            { runtime, id -> runtime.switchSession(id) },
            { runtime, id -> runtime.deleteSession(id) },
            { runtime, _ -> runtime.clearCurrentSession() },
        )
        for (change in changes) fixture { runtime, entered, release ->
            val other = runtime.createSession("other")
            val original = runtime.createSession("original")
            supervisorScope {
                val reply = async { runtime.chat("Give a short music recommendation") }
                try {
                    withTimeout(5_000) { entered.await() }
                    assertFailsWith<IllegalStateException> { change(runtime, other) }
                    assertEquals(original, runtime.state.value.currentSessionId)
                } finally { release.complete(Unit); reply.await() }
            }
            assertEquals("reply for original session", runtime.state.value.sessions.single { it.id == original }.messages.last().content)
            assertTrue(runtime.state.value.sessions.single { it.id == other }.messages.isEmpty())
            runtime.switchSession(other)
            assertEquals(other, runtime.state.value.currentSessionId)
        }
    }

    @Test fun rapidlyCreatedSessionsHaveDistinctIdsAndRetainTheLatestThirty() = runBlocking {
        fixture { runtime, _, _ ->
            val ids = (1..50).map { runtime.createSession("session $it") }
            assertEquals(ids.size, ids.toSet().size)
            assertEquals(ids.takeLast(30).reversed(), runtime.state.value.sessions.map { it.id })
        }
    }
}
